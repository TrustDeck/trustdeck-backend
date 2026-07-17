/*
 * Trust Deck Services
 * Copyright 2026 Armin Müller and Eric Wündisch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.trustdeck.linkage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.trustdeck.dto.EntityInstanceDTO;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.linkage.model.EntityLinkageConfig;
import org.trustdeck.linkage.model.LinkageFieldRule;
import org.trustdeck.linkage.model.LinkageToken;
import org.trustdeck.service.EntityTypeDBService;
import org.trustdeck.service.JsonSchemaService;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

import static org.trustdeck.jooq.generated.Tables.ENTITY_INSTANCE;
import static org.trustdeck.jooq.generated.Tables.LINKAGE_TOKEN;

/**
 * This service manages the record linkage index for entity instances.
 * It resolves the effective linkage field rules for an entity type, generates
 * linkage tokens from an entity instance's payload, and persists those tokens
 * in the database.
 * 
 * @author Armin Müller
 */
@Service
@Slf4j
public class LinkageIndexService {

	/** The jOOQ DSL context used for database access. */
    @Autowired
    private DSLContext dsl;

    /** The service used to resolve effective linkage field rules from type definitions. */
    @Autowired
    private JsonSchemaService jsonSchemaService;

    /** The service used to retrieve base types for given entity types. */
    @Autowired
    private EntityTypeDBService entityTypeService;

    /** The service used to generate linkage tokens from payload data. */
    @Autowired
    private LinkageTokenService linkageTokenService;

    /**
     * Rebuilds the record linkage index entries for a given entity instance.
     * Existing linkage tokens for the instance (if there are any) are 
     * deleted first and then replaced by newly generated tokens based on the 
     * effective linkage field rules.
     * 
     * @param instance the entity instance whose linkage index should be rebuilt
     * @param entityType the entity type of the given instance
     * @return {@code true} when rebuilding was successful, {@code false} otherwise
     */
    @Transactional
    public boolean rebuildIndex(EntityInstanceDTO instance) {
    	// Retrieve the entity type corresponding to the given instance
    	EntityTypeDTO entityType = entityTypeService.getEntityTypeById(instance.getEntityTypeID(), instance.getProjectID());
        
    	// Check if we found anything
        if (entityType == null) {
        	log.debug("Could not retrieve the type for the entity instance: " + instance.getTrustdeckID());
        	return false;
        }
        
        // Retrieve the base type for the instance's entity type, which might then be used for defaults for some linkage settings
        EntityTypeDTO baseType = entityTypeService.getEntityTypeByName(entityType.getBaseTypeName(), null);
        if (baseType == null) {
        	log.trace("Could not retrieve base type for the type \"" + entityType.getName() + "\". Defaults will be used where necessary.");
        }

        // Resolve the effective linkage field rules for the entity type
        JsonNode baseDef = baseType == null ? null : baseType.getTypeDefinition();
        EntityLinkageConfig entityConfig = jsonSchemaService.resolveEntityLinkageConfig(entityType.getTypeDefinition(), baseDef);
        List<LinkageFieldRule> rules = jsonSchemaService.resolveLinkageFieldRules(entityType.getTypeDefinition(), baseDef);

        // Generate all linkage tokens for the current entity instance payload
        List<LinkageToken> tokens = linkageTokenService.buildTokens(entityConfig, rules, instance.getData(), instance.getProjectID(), instance.getEntityTypeID());
        
		// If no tokens were generated, the index is still valid after deleting old tokens
		// (this can happen for entity types without linkage-enabled fields or empty linkage values).
		boolean noTokensGenerated = tokens.isEmpty();

        // Remove any previously stored tokens for this entity instance
        try {
			int deleted = dsl.deleteFrom(LINKAGE_TOKEN)
			   .where(LINKAGE_TOKEN.ENTITY_TYPE_ID.eq(instance.getEntityTypeID()))
			   .and(LINKAGE_TOKEN.ENTITY_INSTANCE_ID.eq(instance.getId()))
			   .execute();
			
			log.trace("Removed " + deleted + " old linkage token" + (deleted == 1 ? "." : "s."));
		} catch (DataAccessException e) {
			log.debug("Could not delete old linkage tokens for the instance with TrustDeckID = " + instance.getTrustdeckID(), e);
			return false;
		}
        
        // Ensure that we do not have any duplicates
        Map<String, LinkageToken> uniqueTokens = new LinkedHashMap<>();
        for (LinkageToken token : tokens) {
        	// Inserting into the linked hash map automatically deduplicates based on the given key and retains input order
        	uniqueTokens.putIfAbsent(tokenDeduplicationKey(token), token);
        }

        // Insert the newly generated linkage tokens into the linkage index table
        int inserted = 0, failed = 0;
        for (LinkageToken token : uniqueTokens.values()) {
            try {
				dsl.insertInto(LINKAGE_TOKEN)
				   .set(LINKAGE_TOKEN.ENTITY_TYPE_ID, instance.getEntityTypeID())
				   .set(LINKAGE_TOKEN.ENTITY_INSTANCE_ID, instance.getId())
				   .set(LINKAGE_TOKEN.PROJECT_ID, instance.getProjectID())
				   .set(LINKAGE_TOKEN.FIELD_PATH, token.getFieldPath())
				   .set(LINKAGE_TOKEN.TAG, token.getTag())
				   .set(LINKAGE_TOKEN.TOKEN_TYPE, token.getTokenType().dbName())
				   .set(LINKAGE_TOKEN.TOKEN_VALUE, token.getTokenValue())
				   .set(LINKAGE_TOKEN.WEIGHT, token.getWeight())
				   .execute();
				
				inserted++;
			} catch (DataAccessException e) {
				log.debug("Could not create new linkage token for the instance with TrustDeckID = " 
						+ instance.getTrustdeckID() + " for token: " + token.toString(), e);
				failed++;
			}
        }
        
        log.debug("Inserted " + inserted + " linkage tokens successfully, failed to insert " + failed + " linkage tokens.");
        if (failed > 0) {
        	return false;
        }

        if (noTokensGenerated) {
        	log.trace("No linkage tokens were generated for the instance with TrustDeckID = " + instance.getTrustdeckID() + ".");
        }

        return true;
    }

    /**
     * Removes all record linkage index entries for a given entity instance.
     * This method should only be used when an entity instance is physically 
     * deleted or permanently purged. It should not be called during normal 
     * soft deletion, because tombstoned entity instances should still be 
     * detectable during record linkage.
     * 
     * @param trustDeckID the TrustDeck ID of the entity instance whose linkage index entries should be removed
     * @return {@code true} when the linkage index entries were removed, {@code false} otherwise
     */
    @Transactional
    public boolean removeAllIndicesForInstance(UUID trustDeckID) {
        try {
        	// Delete all linkage tokens that belong to the entity instance identified by the given TrustDeck ID
        	dsl.deleteFrom(LINKAGE_TOKEN)
	            .whereExists(
	            	// Check whether there is a matching entity instance for the current linkage token
	            	dsl.selectOne()
	                   .from(ENTITY_INSTANCE)
	                   // Find the entity instance by its external TrustDeck ID
	                   .where(ENTITY_INSTANCE.TRUSTDECK_ID.eq(trustDeckID))
	                   // Match the linkage token to the entity instance by entity type and database ID
	                   .and(ENTITY_INSTANCE.ENTITY_TYPE_ID.eq(LINKAGE_TOKEN.ENTITY_TYPE_ID))
	                   .and(ENTITY_INSTANCE.ID.eq(LINKAGE_TOKEN.ENTITY_INSTANCE_ID))
	            )
	            .execute();
		} catch (DataAccessException e) {
			log.debug("Could not remove all record linkage index entries for entity instance with TrustDeckID = " 
					+ trustDeckID.toString(), e);
			return false;
		}
        
        return true;
    }
    
    /**
     * Creates a text key that identifies one generated linkage token.
     * Tokens with the same tag, token type, and token value would produce the 
     * same database row for one entity instance, so this key is used to keep 
     * only one of them before inserting the tokens.
     * 
     * @param token the linkage token for which the key should be generated
     * @return a text key composed of the token's tag, type, and value
     */
    private String tokenDeduplicationKey(LinkageToken token) {
    	return token.getTag() + "|" + token.getTokenType().name().toLowerCase() + "|" + token.getTokenValue();
    }
}
