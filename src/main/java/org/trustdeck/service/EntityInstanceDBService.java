/*
 * Trust Deck Services
 * Copyright 2024-2025 Armin Müller
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

package org.trustdeck.service;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.MappingException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.trustdeck.dto.EntityInstanceDTO;
import org.trustdeck.exception.CreationException;
import org.trustdeck.exception.DuplicateEntityInstanceException;
import org.trustdeck.exception.UnexpectedResultSizeException;
import org.trustdeck.exception.UpdateException;
import org.trustdeck.jooq.generated.tables.pojos.EntityInstance;
import org.trustdeck.jooq.generated.tables.records.EntityInstanceRecord;
import org.trustdeck.linkage.LinkageIndexService;
import org.trustdeck.linkage.model.LinkageToken;
import org.trustdeck.linkage.model.LinkageTokenType;
import org.trustdeck.utils.Assertion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import static org.trustdeck.jooq.generated.Tables.ENTITY_INSTANCE;
import static org.trustdeck.jooq.generated.Tables.LINKAGE_TOKEN;

/**
 * This class encapsulates the database access for entity instances.
 * 
 * @author Armin Müller
 */
@Slf4j
@Service
public class EntityInstanceDBService {
    
	/** References a jOOQ configuration object that configures jOOQ's behavior when executing queries. */
    @Autowired
	private DSLContext dsl;
	
	/** Enables access to the mapper to transform JsonNode into JSONB and back. */
	@Autowired
	private ObjectMapper objectMapper;
	
	/** Used to create, update, and remove record linkage index entries for entity instances. */
	@Autowired
	private LinkageIndexService linkageIndexService;
	
	/**
     * Method to insert a new entity instance into the database.
     * 
     * @param entityInstanceDTO the entity instance data transfer object containing the necessary data
     * @return The newly inserted entity instance object when the insertion was successful,
     * 		   the original entity instance object if the given one was a duplicate, and
     * 		   {@code null} when the insertion failed.
     */
    @Transactional
    public EntityInstanceDTO createEntityInstance(EntityInstanceDTO entityInstanceDTO) {
    	// Create the insert statement and execute it
    	EntityInstanceRecord createdEntityInstance;
    	try {
    		// Let DB defaults generate trustdeck_id, created_at, updated_at, is_deleted if null
    		createdEntityInstance = dsl.insertInto(ENTITY_INSTANCE)
    				// The UUID will be automatically generated
	    			.set(ENTITY_INSTANCE.PROJECT_ID, entityInstanceDTO.getProjectID())
	                .set(ENTITY_INSTANCE.ENTITY_TYPE_ID, entityInstanceDTO.getEntityTypeID())
	                .set(ENTITY_INSTANCE.DATA, toJSONB(entityInstanceDTO.getData()))
	                // The attributes is_deleted, created_at, and updated_at will be automatically set by the DB using defaults
	                .returning()
	                .fetchOne();

	        // Determine success
	        if (createdEntityInstance == null) {
	        	log.debug("Inserting the entity instance failed.");
	        	return null;
	        }
	    } catch (DataAccessException e) {
	    	log.debug(e.getMessage());
	    	
	    	SQLException sqlException = e.getCause(SQLException.class);
	        if ((sqlException != null && "23505".equals(sqlException.getSQLState())) || e.getMessage().contains(" already exists.")) {
	    		// Found duplicate, abort and tell the calling method why we aborted with an exception
	    		throw new DuplicateEntityInstanceException("Found duplicate.");
	    	} else {
		    	// Inserting the new entity instance into the database failed; throw exception to abort
		    	throw new CreationException("Inserting the new entity instance into the database failed.");
	    	}
	    }
    	
    	// Add instance's record linkage tokens to the database
    	EntityInstanceDTO dto = new EntityInstanceDTO().assignPojoValues(new EntityInstance(createdEntityInstance));
    	if (!linkageIndexService.rebuildIndex(dto)) {
			log.debug("Failed to add the instance's record linkage tokens to the database.");
			throw new CreationException("Failed to create record linkage tokens.");
		}
	    
	    // Return the entity type
        log.trace("Creating the entity instance \"" + createdEntityInstance.getTrustdeckId() + "\" was successful.");
	    return dto;
    }

    /**
     * Method to retrieve an entity instance from the database by explicitly providing the 
     * trustDeckID, which is unique in the database.
     * 
     * @param trustDeckID the entity instance's publicly accessible TrustDeck ID
     * @return the retrieved entity instance when successfully found, or {@code null} when nothing was found
     */
    @Transactional
    public EntityInstanceDTO getEntityInstance(UUID trustDeckID) {
    	// Check if all the necessary arguments are available
    	if (trustDeckID == null) {
    		log.debug("Could not retrieve the entity instance, because the TrustDeckID is missing or empty.");
    		return null;
    	}
    	
    	// Build and execute the query
    	EntityInstance instance = dsl.selectFrom(ENTITY_INSTANCE)
                .where(ENTITY_INSTANCE.TRUSTDECK_ID.equal(trustDeckID))
                .and(ENTITY_INSTANCE.IS_DELETED.equal(false))
                .fetchOneInto(EntityInstance.class);
    	
    	// Check if the search was successful
    	if (instance == null) {
    		log.debug("No entity instance was found.");
            return null;
    	}

        // Create a DTO, populate and return it
        return new EntityInstanceDTO().assignPojoValues(instance);
    }
    
    /**
     * Method to retrieve an entity instance from the database by explicitly providing the 
     * trustDeckID as a String within a project.
     * 
     * @param trustDeckID the entity instance's publicly accessible TrustDeck ID as a String
     * @param projectID the database ID of the project that scopes the lookup
     * @return the retrieved entity instance when successfully found, or {@code null} when nothing was found
     */
    @Transactional(readOnly = true)
    public EntityInstanceDTO getEntityInstance(String trustDeckID, int projectID) {
    	// Check if all the necessary arguments are available
    	if (Assertion.isNullOrEmpty(trustDeckID)) {
    		log.debug("Could not retrieve the entity instance, because to the TrustDeckID is missing or empty.");
    		return null;
    	}
    	
    	// Try parsing the string as a UUID object
    	UUID tdid = null;
    	try {
    		tdid = UUID.fromString(trustDeckID);
    	} catch (IllegalArgumentException e) {
			log.debug("Could not transform the given String into the TrustDeckID-UUID-format.", e);
			return null;
		}
    	
        try {
            EntityInstance instance = dsl.selectFrom(ENTITY_INSTANCE)
                    .where(ENTITY_INSTANCE.TRUSTDECK_ID.equal(tdid))
                    .and(ENTITY_INSTANCE.PROJECT_ID.equal(projectID))
                    .and(ENTITY_INSTANCE.IS_DELETED.equal(false))
                    .fetchOneInto(EntityInstance.class);
            return instance == null ? null : new EntityInstanceDTO().assignPojoValues(instance);
        } catch (MappingException e) {
            log.debug("Could not map the entity instance search result into the EntityInstance-POJO.", e);
            return null;
        } catch (DataAccessException e) {
            log.debug("Searching for the entity instance in the database failed.", e);
            return null;
        }
    }

    /**
     * Method to retrieve an entity instance from the database by providing the data JSON.
     * 
     * @param data the entity instance's data object
     * @return the retrieved entity instance when successfully found, or {@code null} when nothing was found
     */
    @Transactional
    public EntityInstanceDTO getEntityInstanceByData(JSONB data) {
    	// Check if all the necessary arguments are available
    	if (data == null) {
    		log.debug("Could not retrieve the entity instance, because the data-object is missing or empty.");
    		return null;
    	}
    	
    	// Build and execute the query
    	EntityInstance instance = null;
    	try {
    		instance = dsl.selectFrom(ENTITY_INSTANCE)
                .where(ENTITY_INSTANCE.DATA.equal(data))
                .and(ENTITY_INSTANCE.IS_DELETED.equal(false))
                .fetchOneInto(EntityInstance.class);
        } catch (MappingException e) {
        	log.debug("Could not map the entity instance search result into the EntityInstance-POJO.", e);
        	return null;
        } catch (DataAccessException f) {
        	log.debug("Searching for the entity instance in the database failed.", f);
        	return null;
        }
    	
    	// Check if the search was successful
    	if (instance == null) {
    		log.debug("No entity instance was found.");
            return null;
    	}

        // Create a DTO, populate and return it
        return new EntityInstanceDTO().assignPojoValues(instance);
    }
    
    /**
     * Method to retrieve an entity instance from the database by providing an EntityInstanceDTO. 
     * Internally it uses only the trustDeckID, which is unique in the database.
     * 
     * @param entityInstance the DTO containing at least the instance's publicly accessible TrustDeck ID
     * @return the retrieved entity instance when successfully found, or {@code null} when nothing was found
     */
    @Transactional
    public EntityInstanceDTO getEntityInstance(EntityInstanceDTO entityInstance) {
    	// Check if all the necessary arguments are available
    	if (entityInstance == null || entityInstance.getTrustdeckID() == null) {
    		// No trustDeckId available --> try identification via the data object
    		if (entityInstance != null && entityInstance.getData() != null) {
    			return getEntityInstanceByData(toJSONB(entityInstance.getData()));
    		} else {
	    		log.debug("Could not retrieve the entity instance, because an argument is missing or empty.");
	    		return null;
    		}
    	} else {
    		// Use trustDeckId to find the instance
    		return getEntityInstance(entityInstance.getTrustdeckID());
    	}
    }
    
    /**
     * Method to retrieve a list of entity instances from 
     * the database by providing a list of database IDs.
     * 
     * @param instanceIDs the List of database IDs that should be searched for
     * @param entityTypeID the database ID of the type the instances are of
     * @param includeDeleted whether or not soft-deleted entity instances should be included
     * @return the list of retrieved entity instances when successful, or {@code null} when nothing was found
     */
    @Transactional(readOnly = true)
    public List<EntityInstanceDTO> getEntityInstancesByIDs(List<Long> instanceIDs, int entityTypeID, boolean includeDeleted) {
    	// Check if all the necessary arguments are available
    	if (instanceIDs == null || instanceIDs.isEmpty()) {
    		log.debug("Could not retrieve the entity instances, because the given list of IDs is missing or empty.");
    		return null;
    	}

    	// Restrict lookup to the requested entity type and requested instance IDs
    	Condition condition = ENTITY_INSTANCE.ENTITY_TYPE_ID.eq(entityTypeID)
    			.and(ENTITY_INSTANCE.ID.in(instanceIDs));

    	// Optionally restrict to active records only
    	if (!includeDeleted) {
    		condition = condition.and(ENTITY_INSTANCE.IS_DELETED.eq(false));
    	}
    	
    	// Build and execute the query
    	List<EntityInstance> instances = null;
    	try {
    		instances = dsl.selectFrom(ENTITY_INSTANCE)
    			.where(condition)
                .fetchInto(EntityInstance.class);
        } catch (MappingException e) {
        	log.debug("Could not map the entity instance search result into the EntityInstance-POJO.", e);
        	return null;
        } catch (DataAccessException f) {
        	log.debug("Searching for entity instances in the database failed.", f);
        	return null;
        }

        // Create list of DTOs and return it
    	return instances.stream().map(i -> new EntityInstanceDTO().assignPojoValues(i)).toList();
    }

	/**
	 * Retrieves active entity instances by their internal database IDs.
	 * Soft-deleted entity instances are excluded.
	 * 
	 * @param instanceIDs the internal database IDs of the entity instances that should be retrieved
	 * @param entityTypeID the ID of the entity type to which the entity instances belong
	 * @return the list of retrieved active entity instances, or {@code null} if retrieval failed
	 */
	@Transactional(readOnly = true)
	public List<EntityInstanceDTO> getEntityInstancesByIDs(List<Long> instanceIDs, int entityTypeID) {
		return getEntityInstancesByIDs(instanceIDs, entityTypeID, false);
	}
    
    /**
     * Method to delete an entity instance.
     * The deletion is done by marking the entry as deleted and not
     * by actually removing the record from the database.
     * 
     * @param trustDeckID the entity instance's publicly accessible TrustDeck ID
     * @param projectID the database ID of the project that scopes the deletion
     * @return {@code true} when deletion was successful, {@code false} when anything went wrong during the deletion
     * @throws UnexpectedResultSizeException when the deletion would have affected an unexpected number of database entries
     */
    @Transactional
    public boolean deleteEntityInstance(UUID trustDeckID, int projectID) throws UnexpectedResultSizeException {
    	// Check if all the necessary arguments are available
    	if (trustDeckID == null) {
    		log.debug("For retrieving the entity instance, there is an argument missing or empty.");
    		return false;
    	}
    	
    	// Perform deletion by updating the is_deleted flag
    	int deletedEntityInstances = 0;
    	try {
    		// Build and execute the query
    		deletedEntityInstances = dsl.update(ENTITY_INSTANCE)
	                .set(ENTITY_INSTANCE.IS_DELETED, true)
	                .set(ENTITY_INSTANCE.UPDATED_AT, OffsetDateTime.now())
                .where(ENTITY_INSTANCE.TRUSTDECK_ID.eq(trustDeckID))
                .and(ENTITY_INSTANCE.PROJECT_ID.eq(projectID))
                .and(ENTITY_INSTANCE.IS_DELETED.eq(false))
	                .execute();
        } catch (DataAccessException e) {
        	log.debug("Deleting the entity instance in the database failed.", e);
        	return false;
        }
    	
    	// Check if the deletion was successful
    	if (deletedEntityInstances != 1) {
    		// An unexpected number of records was affected. Log it and abort by throwing
            // an exception (which will rollback everything from the transaction).
    		log.error("Too many records would have been affected by the deletion, which was therefore aborted and rolled back.");
        	throw new UnexpectedResultSizeException(1, deletedEntityInstances);
    	}
    	
    	// If we reach this point, the deletion was successful
    	return true;
    }
    
    /**
     * Method to update an entity instance.
     * 
     * @param oldInstanceID the entity instance database id that is needed to identify the instance that should be updated
     * @param projectID the database ID of the project that scopes the update
     * @param newEntityInstanceDTO the entity instance object containing the data to use for the update
     * @return the updated entity instance object when successful, {@code null} when anything went wrong
     */
    @Transactional
    public EntityInstanceDTO updateEntityInstance(long oldInstanceID, int projectID, EntityInstanceDTO newEntityInstanceDTO) {
    	// Create the update-record and send it to the database
        EntityInstanceRecord updatedRecord = null;
    	try {
    		// Update and return the updated record (as long as it's not already deleted)
    		updatedRecord = dsl.update(ENTITY_INSTANCE)
	                .set(ENTITY_INSTANCE.TRUSTDECK_ID, newEntityInstanceDTO.getTrustdeckID())
	                .set(ENTITY_INSTANCE.PROJECT_ID, newEntityInstanceDTO.getProjectID())
	                .set(ENTITY_INSTANCE.ENTITY_TYPE_ID, newEntityInstanceDTO.getEntityTypeID())
	                .set(ENTITY_INSTANCE.DATA, toJSONB(newEntityInstanceDTO.getData()))
	                .set(ENTITY_INSTANCE.UPDATED_AT, OffsetDateTime.now())
                .where(ENTITY_INSTANCE.ID.eq(oldInstanceID))
	                .and(ENTITY_INSTANCE.PROJECT_ID.eq(projectID))
                .and(ENTITY_INSTANCE.IS_DELETED.ne(true))
	                .returning()
	                .fetchOne();
    	} catch (DataAccessException e) {
	    	log.error("Updating the entity instance failed.", e);
	    	return null;
	    }
    	
    	// Add instance's record linkage tokens to the database
    	EntityInstanceDTO dto = new EntityInstanceDTO().assignPojoValues(new EntityInstance(updatedRecord));
    	if (!linkageIndexService.rebuildIndex(dto)) {
			log.debug("Failed to update the instance's record linkage tokens to the database.");
			throw new UpdateException("Failed to update record linkage tokens.");
		}
	    
	    // Return the updated entity instance
        log.debug("Updating the entity instance \"" + newEntityInstanceDTO.getTrustdeckID() + "\" was successful.");
	    return dto;
    }
    
    /**
     * Method to search for entity instance.
     * This search supports full-text-searching over all attributes of an entity instance,
     * as well as multiple words.
     * 
     * @param query the (multi-word) search query
     * @return a list of entity instances that match the search query
     */
    @Transactional
    public List<EntityInstanceDTO> searchEntityInstance(String query, Integer entityTypeId) {
    	if (Assertion.isNullOrEmpty(query)) {
            log.debug("Search query is empty.");
            return null;
        }
    	
    	// Support wildcard-search (limited to 250)
		if (query.trim().equals("*")) {
			Condition condition = ENTITY_INSTANCE.IS_DELETED.eq(false);
			if (entityTypeId != null) {
				condition = condition.and(ENTITY_INSTANCE.ENTITY_TYPE_ID.eq(entityTypeId));
			}

			try {
				List<EntityInstance> results = dsl.selectFrom(ENTITY_INSTANCE)
						.where(condition)
						.orderBy(ENTITY_INSTANCE.UPDATED_AT.desc(), ENTITY_INSTANCE.CREATED_AT.desc(), ENTITY_INSTANCE.TRUSTDECK_ID.asc())
						.limit(250)
						.fetchInto(EntityInstance.class);

				if (results == null || results.isEmpty()) {
					log.trace("No entity instance matched the find-all query.");
					return null;
				}

				return results.stream().map(e -> new EntityInstanceDTO().assignPojoValues(e)).toList();
            } catch (MappingException e) {
                log.debug("Could not map entity instance search result.", e);
                return null;
            } catch (DataAccessException f) {
                log.debug("Searching entity instances failed.", f);
                return null;
            }
    	}

        // Split query-parts on whitespace; every part should match at least one column
        String[] parts = query.trim().split("\\s+");
        
        // Built the search conditions statement
        Condition condition = DSL.trueCondition();
        for (String part : parts) {
            String pattern = "%" + part + "%";

            // Search across columns; cast non-text columns to text for LIKE matching
            Condition partCond = DSL.cast(ENTITY_INSTANCE.TRUSTDECK_ID, String.class).likeIgnoreCase(pattern)
                .or(DSL.cast(ENTITY_INSTANCE.PROJECT_ID, String.class).likeIgnoreCase(pattern))
                .or(DSL.cast(ENTITY_INSTANCE.ENTITY_TYPE_ID, String.class).likeIgnoreCase(pattern))
                // JSONB via full text search on the tsvector (uses the GIN index on the tsvector named 'full_text_search_vector')
                // Removes all non-alphanumeric characters and adds the prefix-operator (:*)
                .or(DSL.condition("full_text_search_vector @@ to_tsquery('simple', regexp_replace({0}, '[^[:alnum:]]+', '', 'g') || ':*')", DSL.val(part)))
                .or(DSL.cast(ENTITY_INSTANCE.CREATED_AT, String.class).likeIgnoreCase(pattern))
                .or(DSL.cast(ENTITY_INSTANCE.UPDATED_AT, String.class).likeIgnoreCase(pattern));

            // If the query is long enough, perform a typo-tolerant search via the data_text column
			if (part.length() >= 3) {
				partCond = partCond.or(DSL.cast(ENTITY_INSTANCE.DATA_TEXT, String.class).likeIgnoreCase(pattern));
			}
            
            // AND-connect all search parts
            condition = condition.and(partCond);
        }

        // Exclude deleted instances
        condition = condition.and(ENTITY_INSTANCE.IS_DELETED.eq(false));
        
        if (entityTypeId != null) {
        	condition = condition.and(ENTITY_INSTANCE.ENTITY_TYPE_ID.eq(entityTypeId));
        }

        // Execute the search
        List<EntityInstance> results;
        try {
            results = dsl.selectFrom(ENTITY_INSTANCE)
                      .where(condition)
                      .orderBy(DSL.field("ts_rank(full_text_search_vector, plainto_tsquery('simple', {0}))", Double.class, DSL.val(query)).desc(),
                    		   DSL.field("similarity(data_text, {0})", Double.class, DSL.val(query)).desc())
                      .limit(100)
                      .fetchInto(EntityInstance.class);
        } catch (MappingException e) {
            log.debug("Could not map entity instance search result.", e);
            return null;
        } catch (DataAccessException f) {
            log.debug("Searching entity instances failed.", f);
            return null;
        }

        // Evaluate the search results
        if (results == null || results.isEmpty()) {
            log.debug("No entity instance matched the query \"" + query + "\".");
            return null;
        }

        // Return the found types
        return results.stream().map(row -> new EntityInstanceDTO().assignPojoValues(row)).toList();
    }
    
    /**
     * Method to search for record linkage candidates that match a given set of attributes.
     * 
     * @param projectId the id of the project in which the linkage should be done
     * @param entityTypeId the id of the instance's type
     * @param linkageValues a map of attribute-value-combinations that should be searched for
     * @param limit the maximum amount of matches that should be returned
     * @return a list of entity instances that match the given linkage values
     */
    @Transactional(readOnly = true)
    public List<EntityInstanceDTO> searchRecordLinkageCandidates(int projectId, int entityTypeId, Map<String, JsonNode> linkageValues, int limit) {
    	if (linkageValues == null) {
            log.debug("Missing linkage values.");
            return null;
        }
    	
    	// Build condition statements for the attribute checks
    	Condition condition = DSL.trueCondition();
    	for (Map.Entry<String, JsonNode> entry : linkageValues.entrySet()) {
    	    // Format the attribute-value-combination properly: {"attribute": <value>}
    	    String fragment = null;
			try {
				fragment = objectMapper.writeValueAsString(Map.of(entry.getKey(), entry.getValue()));
			} catch (JsonProcessingException e) {
				log.debug("Could not transform an attribute-value-combination into a String.");
				continue;
			}

    	    // Add contains-search (i.e. data @> '{"attr": <val>}'::jsonb)
    	    condition = condition.and(DSL.condition("{0} @> {1}::jsonb", ENTITY_INSTANCE.DATA, DSL.inline(fragment)));
    	}
    	
    	// Perform the search
    	List<EntityInstance> candidates;
        try {
	    	candidates = dsl.selectFrom(ENTITY_INSTANCE)
		    	.where(ENTITY_INSTANCE.PROJECT_ID.eq(projectId))
		    	.and(ENTITY_INSTANCE.ENTITY_TYPE_ID.eq(entityTypeId))
		    	.and(ENTITY_INSTANCE.IS_DELETED.eq(false))
		    	.and(condition)
		    	.limit(limit)
		    	.fetchInto(EntityInstance.class);
        } catch (MappingException e) {
            log.debug("Could not map entity instance search result.", e);
            return null;
        } catch (DataAccessException f) {
            log.debug("Searching entity instances failed.", f);
            return null;
        }

        // Evaluate the search results
        if (candidates == null) {
            log.debug("No entity instance candidates found that match the given attributes.");
            return null;
        }

        // Return the found types
        return candidates.stream().map(row -> new EntityInstanceDTO().assignPojoValues(row)).toList();
    }
    
    /**
     * Finds candidate entity instance IDs by matching the provided blocking tokens against
     * the record linkage token index in the database.
     * 
     * Only tokens of type {@code block} are used for candidate generation. Matching records
     * are grouped by entity instance ID and ordered by the number of matching blocking tokens,
     * so records sharing more blocking tokens with the query payload are returned first.
     * 
     * @param projectId the ID of the project in which candidate records should be searched
     * @param entityTypeId the ID of the entity type to which the candidate records must belong
     * @param payloadTokens the linkage tokens generated from the input payload
     * @param limit the maximum number of candidate entity instance IDs to return
     * @param includeDeleted whether or not soft-deleted entity instances should be included as candidates
     * @return the list of candidate entity instance IDs ordered by descending number of matching blocking tokens
     */
    @Transactional(readOnly = true)
    public List<Long> findCandidateIdsByBlockingTokens(int projectId, int entityTypeId, List<LinkageToken> payloadTokens, int limit, boolean includeDeleted) {
    	// Only blocking tokens are used during candidate generation
    	List<LinkageToken> blockTokens = payloadTokens.stream().filter(t -> t.getTokenType() != null && t.getTokenType().isCandidateGenerationToken()).toList();

    	// Without blocking tokens, no efficient candidate generation can be performed
    	if (blockTokens.isEmpty()) {
    		log.trace("No blocking tokens were found for the given payload.");
    		return List.of();
    	}

    	// A candidate matches if it shares at least one blocking token with the query payload
    	Condition tokenCondition = DSL.falseCondition();
    	for (LinkageToken token : blockTokens) {
    		tokenCondition = tokenCondition
    				.or(LINKAGE_TOKEN.TAG.eq(token.getTag())
    					.and(LINKAGE_TOKEN.TOKEN_TYPE.eq(token.getTokenType().dbName()))
    					.and(LINKAGE_TOKEN.TOKEN_VALUE.eq(token.getTokenValue()))
    				);
    	}

    	// Build the common candidate condition
    	Condition condition = LINKAGE_TOKEN.PROJECT_ID.eq(projectId)
    			.and(LINKAGE_TOKEN.ENTITY_TYPE_ID.eq(entityTypeId))
    			.and(tokenCondition);

    	// Exclude tombstoned instances when the caller explicitly wants active candidates only
    	if (!includeDeleted) {
    		condition = condition.and(ENTITY_INSTANCE.IS_DELETED.eq(false));
    	}

    	// Retrieve matching entity instance IDs, exclude soft-deleted records,
    	// and rank candidates by the number of matching blocking tokens
    	return dsl.select(LINKAGE_TOKEN.ENTITY_INSTANCE_ID)
    			.from(LINKAGE_TOKEN)
    			.join(ENTITY_INSTANCE)
					.on(LINKAGE_TOKEN.PROJECT_ID.eq(ENTITY_INSTANCE.PROJECT_ID))
					.and(LINKAGE_TOKEN.ENTITY_TYPE_ID.eq(ENTITY_INSTANCE.ENTITY_TYPE_ID))
					.and(LINKAGE_TOKEN.ENTITY_INSTANCE_ID.eq(ENTITY_INSTANCE.ID))
				.where(condition)
				.groupBy(LINKAGE_TOKEN.ENTITY_INSTANCE_ID)
				.orderBy(DSL.count().desc())
				.limit(limit) // TODO: error
				.fetch(LINKAGE_TOKEN.ENTITY_INSTANCE_ID);
    }
    
    /**
     * Finds active candidate entity instance IDs by matching the provided blocking tokens.
     * Soft-deleted entity instances are excluded.
     * 
     * @param projectId the ID of the project in which candidate records should be searched
     * @param entityTypeId the ID of the entity type to which the candidate records must belong
     * @param payloadTokens the linkage tokens generated from the input payload
     * @param limit the maximum number of candidate entity instance IDs to return
     * @return the list of active candidate entity instance IDs
     */
    @Transactional(readOnly = true)
    public List<Long> findCandidateIdsByBlockingTokens(int projectId, int entityTypeId, List<LinkageToken> payloadTokens, int limit) {
    	return findCandidateIdsByBlockingTokens(projectId, entityTypeId, payloadTokens, limit, false);
    }
    
    /**
     * Retrieves all record linkage tokens from the database for the given entity 
     * instances. The returned map is keyed by entity instance ID (so tuples of 
     * (instanceID, linkageTokens)) so that the tokens can be efficiently matched 
     * to their corresponding candidate records during later scoring.
     * 
     * @param instanceIDs the database IDs of the entity instances whose linkage tokens should be retrieved
     * @param entityTypeID the ID of the entity type to which the instances belong
     * @return a map from entity instance ID to the list of stored linkage tokens
     */
    @Transactional(readOnly = true)
    public Map<Long, List<LinkageToken>> getLinkageTokensForInstances(List<Long> instanceIDs, int entityTypeID) {
    	if (instanceIDs == null || instanceIDs.isEmpty()) {
    		return Map.of();
    	}

    	Map<Long, List<LinkageToken>> out = new HashMap<>();

    	// Retrieve all linkage tokens for the requested entity instances and group them by instance ID; add them to the map
    	dsl.selectFrom(LINKAGE_TOKEN)
    			.where(LINKAGE_TOKEN.ENTITY_TYPE_ID.eq(entityTypeID))
    			.and(LINKAGE_TOKEN.ENTITY_INSTANCE_ID.in(instanceIDs))
    			.fetch()
    			.forEach(tok -> {
    				Long id = tok.getEntityInstanceId();

    				// Add each token to the list belonging to its entity instance
    				out.computeIfAbsent(id, x -> new ArrayList<>())
    						.add(new LinkageToken(tok.getFieldPath(), tok.getTag(), toTypeEnum(tok.getTokenType()), tok.getTokenValue(), tok.getWeight()));
    			});

    	return out;
    }
    
    /**
     * Helper method to transform a JsonNode into a JSONB object.
     * 
     * @param node the JsonNode data
     * @return a JSONB representation of the given data, or {@code null} if parsing failed
     */
    private JSONB toJSONB(JsonNode node) {
    	JSONB jsonb = null;
    	
    	if (node != null) {
    		try {
				String nodeAsString = objectMapper.writeValueAsString(node);
				jsonb = JSONB.valueOf(nodeAsString);
			} catch (JsonProcessingException e) {
				log.debug("Could not parse JsonNode into JSONB.");
			}
    	}
    	
        return jsonb;
    }
    
    /**
     * Helper method that transforms a linkage token type given as a string
     * into the proper enum representation.
     * 
     * @param type the linkage token type string
     * @return the linkage token type enum representation
     */
    private LinkageTokenType toTypeEnum(String type) {
    	return LinkageTokenType.fromDbValue(type);
    }
}
