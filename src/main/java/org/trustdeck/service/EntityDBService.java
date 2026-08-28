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
import java.util.Objects;
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
import org.trustdeck.dto.EntityDTO;
import org.trustdeck.exception.CreationException;
import org.trustdeck.exception.DuplicateEntityException;
import org.trustdeck.exception.TooManyRecordLinkageCandidatesException;
import org.trustdeck.exception.UnexpectedResultSizeException;
import org.trustdeck.exception.UpdateException;
import org.trustdeck.jooq.generated.tables.pojos.Entity;
import org.trustdeck.jooq.generated.tables.records.EntityRecord;
import org.trustdeck.linkage.LinkageIndexService;
import org.trustdeck.linkage.model.LinkageToken;
import org.trustdeck.linkage.model.LinkageTokenType;
import org.trustdeck.utils.Assertion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import static org.trustdeck.jooq.generated.Tables.ENTITY;
import static org.trustdeck.jooq.generated.Tables.LINKAGE_TOKEN;

/**
 * This class encapsulates the database access for entities.
 * 
 * @author Armin Müller
 */
@Slf4j
@Service
public class EntityDBService {
    
	/** References a jOOQ configuration object that configures jOOQ's behavior when executing queries. */
    @Autowired
	private DSLContext dsl;
	
	/** Enables access to the mapper to transform JsonNode into JSONB and back. */
	@Autowired
	private ObjectMapper objectMapper;
	
	/** Used to create, update, and remove record linkage index entries for entities. */
	@Autowired
	private LinkageIndexService linkageIndexService;
	
	/**
     * Method to insert a new entity into the database.
     * 
     * @param entityDTO the entity data transfer object containing the necessary data
     * @return The newly inserted entity object when the insertion was successful,
     * 		   the original entity object if the given one was a duplicate, and
     * 		   {@code null} when the insertion failed.
     */
    @Transactional
    public EntityDTO createEntity(EntityDTO entityDTO) {
    	// Create the insert statement and execute it
    	EntityRecord createdEntity;
    	try {
    		// Let DB defaults generate trustdeck_id, created_at, updated_at, is_deleted if null
    		createdEntity = dsl.insertInto(ENTITY)
    				// The UUID will be automatically generated
	    			.set(ENTITY.PROJECT_ID, entityDTO.getProjectID())
	                .set(ENTITY.ENTITY_TYPE_ID, entityDTO.getEntityTypeID())
	                .set(ENTITY.DATA, toJSONB(entityDTO.getData()))
	                // The attributes is_deleted, created_at, and updated_at will be automatically set by the DB using defaults
	                .returning()
	                .fetchOne();

	        // Determine success
	        if (createdEntity == null) {
	        	log.debug("Inserting the entity failed.");
	        	return null;
	        }
	    } catch (DataAccessException e) {
	    	log.debug(e.getMessage());
	    	
	    	SQLException sqlException = e.getCause(SQLException.class);
	        if ((sqlException != null && "23505".equals(sqlException.getSQLState())) || e.getMessage().contains(" already exists.")) {
	    		// Found duplicate, abort and tell the calling method why we aborted with an exception
	    		throw new DuplicateEntityException("Found duplicate.");
	    	} else {
		    	// Inserting the new entity into the database failed; throw exception to abort
		    	throw new CreationException("Inserting the new entity into the database failed.");
	    	}
	    }
    	
    	// Add entity's record linkage tokens to the database
    	EntityDTO dto = new EntityDTO().assignPojoValues(new Entity(createdEntity));
    	if (!linkageIndexService.rebuildIndex(dto)) {
			log.debug("Failed to add the entity's record linkage tokens to the database.");
			throw new CreationException("Failed to create record linkage tokens.");
		}
	    
	    // Return the entity type
        log.trace("Creating the entity \"" + createdEntity.getTrustdeckId() + "\" was successful.");
	    return dto;
    }

    /**
     * Method to retrieve an entity from the database by explicitly providing the 
     * trustDeckID, which is unique in the database.
     * 
     * @param trustDeckID the entity's publicly accessible TrustDeck ID
     * @return the retrieved entity when successfully found, or {@code null} when nothing was found
     */
    @Transactional
    public EntityDTO getEntity(UUID trustDeckID) {
    	// Check if all the necessary arguments are available
    	if (trustDeckID == null) {
    		log.debug("Could not retrieve the entity, because the TrustDeckID is missing or empty.");
    		return null;
    	}
    	
    	// Build and execute the query
		Entity entity = dsl.selectFrom(ENTITY)
                .where(ENTITY.TRUSTDECK_ID.equal(trustDeckID))
                .and(ENTITY.IS_DELETED.equal(false))
                .fetchOneInto(Entity.class);
    	
    	// Check if the search was successful
		if (entity == null) {
    		log.debug("No entity was found.");
            return null;
    	}

        // Create a DTO, populate and return it
        return new EntityDTO().assignPojoValues(entity);
    }
    
    /**
     * Method to retrieve an entity from the database by explicitly providing the 
     * trustDeckID as a String within a project.
     * 
     * @param trustDeckID the entity's publicly accessible TrustDeck ID as a String
     * @param projectID the database ID of the project that scopes the lookup
     * @return the retrieved entity when successfully found, or {@code null} when nothing was found
     */
    @Transactional(readOnly = true)
    public EntityDTO getEntity(String trustDeckID, int projectID) {
    	// Check if all the necessary arguments are available
    	if (Assertion.isNullOrEmpty(trustDeckID)) {
    		log.debug("Could not retrieve the entity, because to the TrustDeckID is missing or empty.");
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
            Entity entity = dsl.selectFrom(ENTITY)
                    .where(ENTITY.TRUSTDECK_ID.equal(tdid))
                    .and(ENTITY.PROJECT_ID.equal(projectID))
                    .and(ENTITY.IS_DELETED.equal(false))
                    .fetchOneInto(Entity.class);
            return entity == null ? null : new EntityDTO().assignPojoValues(entity);
        } catch (MappingException e) {
            log.debug("Could not map the entity search result into the Entity-POJO.", e);
            return null;
        } catch (DataAccessException e) {
            log.debug("Searching for the entity in the database failed.", e);
            return null;
        }
    }

    @Transactional(readOnly = true)
    public EntityDTO getEntity(String trustDeckID, int projectID, int entityTypeID) {
        if (Assertion.isNullOrEmpty(trustDeckID)) {
            return null;
        }

        try {
            UUID tdid = UUID.fromString(trustDeckID);
            Entity entity = dsl.selectFrom(ENTITY)
                    .where(ENTITY.TRUSTDECK_ID.equal(tdid))
                    .and(ENTITY.PROJECT_ID.equal(projectID))
                    .and(ENTITY.ENTITY_TYPE_ID.equal(entityTypeID))
                    .and(ENTITY.IS_DELETED.equal(false))
                    .fetchOneInto(Entity.class);
            return entity == null ? null : new EntityDTO().assignPojoValues(entity);
        } catch (IllegalArgumentException | DataAccessException e) {
            log.debug("Could not retrieve the entity for the requested type.", e);
            return null;
        }
    }

    /**
     * Method to retrieve an entity from the database by providing the data JSON.
     * 
     * @param data the entity's data object
     * @return the retrieved entity when successfully found, or {@code null} when nothing was found
     */
    @Transactional
    public EntityDTO getEntityByData(JSONB data) {
    	// Check if all the necessary arguments are available
    	if (data == null) {
    		log.debug("Could not retrieve the entity, because the data-object is missing or empty.");
    		return null;
    	}
    	
    	// Build and execute the query
		Entity entity = null;
    	try {
			entity = dsl.selectFrom(ENTITY)
                .where(ENTITY.DATA.equal(data))
                .and(ENTITY.IS_DELETED.equal(false))
                .fetchOneInto(Entity.class);
        } catch (MappingException e) {
        	log.debug("Could not map the entity search result into the Entity-POJO.", e);
        	return null;
        } catch (DataAccessException f) {
        	log.debug("Searching for the entity in the database failed.", f);
        	return null;
        }
    	
    	// Check if the search was successful
		if (entity == null) {
    		log.debug("No entity was found.");
            return null;
    	}

        // Create a DTO, populate and return it
        return new EntityDTO().assignPojoValues(entity);
    }
    
    /**
     * Method to retrieve an entity from the database by providing an EntityDTO. 
     * Internally it uses only the trustDeckID, which is unique in the database.
     * 
     * @param entity the DTO containing at least the entity's publicly accessible TrustDeck ID
     * @return the retrieved entity when successfully found, or {@code null} when nothing was found
     */
    @Transactional
    public EntityDTO getEntity(EntityDTO entity) {
    	// Check if all the necessary arguments are available
    	if (entity == null || entity.getTrustdeckID() == null) {
    		// No trustDeckId available --> try identification via the data object
    		if (entity != null && entity.getData() != null) {
    			return getEntityByData(toJSONB(entity.getData()));
    		} else {
	    		log.debug("Could not retrieve the entity, because an argument is missing or empty.");
	    		return null;
    		}
    	} else {
    		// Use trustDeckId to find the entity
    		return getEntity(entity.getTrustdeckID());
    	}
    }
    
    /**
     * Method to retrieve a list of entities from 
     * the database by providing a list of database IDs.
     * 
     * @param entityIDs the List of database IDs that should be searched for
     * @param entityTypeID the database ID of the type the entities are of
     * @param includeDeleted whether or not soft-deleted entities should be included
     * @return the list of retrieved entities when successful, or {@code null} when nothing was found
     */
    @Transactional(readOnly = true)
    public List<EntityDTO> getEntitiesByIDs(List<Long> entityIDs, int entityTypeID, boolean includeDeleted) {
    	// Check if all the necessary arguments are available
    	if (entityIDs == null || entityIDs.isEmpty()) {
    		log.debug("Could not retrieve the entities, because the given list of IDs is missing or empty.");
    		return null;
    	}

    	// Restrict lookup to the requested entity type and requested entity IDs
    	Condition condition = ENTITY.ENTITY_TYPE_ID.eq(entityTypeID)
    			.and(ENTITY.ID.in(entityIDs));

    	// Optionally restrict to active records only
    	if (!includeDeleted) {
    		condition = condition.and(ENTITY.IS_DELETED.eq(false));
    	}
    	
    	// Build and execute the query
    	List<Entity> entities = null;
    	try {
    		entities = dsl.selectFrom(ENTITY)
    			.where(condition)
                .fetchInto(Entity.class);
        } catch (MappingException e) {
        	log.debug("Could not map the entity search result into the Entity-POJO.", e);
        	return null;
        } catch (DataAccessException f) {
        	log.debug("Searching for entities in the database failed.", f);
        	return null;
        }

        // Create list of DTOs and return it
    	return entities.stream().map(i -> new EntityDTO().assignPojoValues(i)).toList();
    }

	/**
	 * Retrieves active entities by their internal database IDs.
	 * Soft-deleted entities are excluded.
	 * 
	 * @param entityIDs the internal database IDs of the entities that should be retrieved
	 * @param entityTypeID the ID of the entity type to which the entities belong
	 * @return the list of retrieved active entities, or {@code null} if retrieval failed
	 */
	@Transactional(readOnly = true)
	public List<EntityDTO> getEntitiesByIDs(List<Long> entityIDs, int entityTypeID) {
		return getEntitiesByIDs(entityIDs, entityTypeID, false);
	}
    
    /**
     * Method to delete an entity.
     * The deletion is done by marking the entry as deleted and not
     * by actually removing the record from the database.
     * 
     * @param trustDeckID the entity's publicly accessible TrustDeck ID
     * @param projectID the database ID of the project that scopes the deletion
     * @return {@code true} when deletion was successful, {@code false} when anything went wrong during the deletion
     * @throws UnexpectedResultSizeException when the deletion would have affected an unexpected number of database entries
     */
    @Transactional
    public boolean deleteEntity(UUID trustDeckID, int projectID) throws UnexpectedResultSizeException {
    	// Check if all the necessary arguments are available
    	if (trustDeckID == null) {
    		log.debug("For retrieving the entity, there is an argument missing or empty.");
    		return false;
    	}
    	
    	// Perform deletion by updating the is_deleted flag
    	int deletedEntities = 0;
    	try {
    		// Build and execute the query
    		deletedEntities = dsl.update(ENTITY)
	                .set(ENTITY.IS_DELETED, true)
	                .set(ENTITY.UPDATED_AT, OffsetDateTime.now())
                .where(ENTITY.TRUSTDECK_ID.eq(trustDeckID))
                .and(ENTITY.PROJECT_ID.eq(projectID))
                .and(ENTITY.IS_DELETED.eq(false))
	                .execute();
        } catch (DataAccessException e) {
        	log.debug("Deleting the entity in the database failed.", e);
        	return false;
        }
    	
    	// Check if the deletion was successful
    	if (deletedEntities != 1) {
    		// An unexpected number of records was affected. Log it and abort by throwing
            // an exception (which will rollback everything from the transaction).
    		log.error("Too many records would have been affected by the deletion, which was therefore aborted and rolled back.");
        	throw new UnexpectedResultSizeException(1, deletedEntities);
    	}
    	
    	// If we reach this point, the deletion was successful
    	return true;
    }

    @Transactional
    public boolean deleteEntity(UUID trustDeckID, int projectID, int entityTypeID) throws UnexpectedResultSizeException {
        int deleted = dsl.update(ENTITY).set(ENTITY.IS_DELETED, true).set(ENTITY.UPDATED_AT, OffsetDateTime.now())
                .where(ENTITY.TRUSTDECK_ID.eq(trustDeckID)).and(ENTITY.PROJECT_ID.eq(projectID))
                .and(ENTITY.ENTITY_TYPE_ID.eq(entityTypeID)).and(ENTITY.IS_DELETED.eq(false)).execute();
        if (deleted != 1) throw new UnexpectedResultSizeException(1, deleted);
        return true;
    }
    
    /**
     * Method to update an entity.
     * 
     * @param oldEntityID the entity database id that is needed to identify the entity that should be updated
     * @param projectID the database ID of the project that scopes the update
     * @param newEntityDTO the entity object containing the data to use for the update
     * @return the updated entity object when successful, {@code null} when anything went wrong
     */
    @Transactional
    public EntityDTO updateEntity(long oldEntityID, int projectID, EntityDTO newEntityDTO) {
    	// Create the update-record and send it to the database
        EntityRecord updatedRecord = null;
    	try {
    		// Update and return the updated record (as long as it's not already deleted)
    		updatedRecord = dsl.update(ENTITY)
	                .set(ENTITY.TRUSTDECK_ID, newEntityDTO.getTrustdeckID())
	                .set(ENTITY.PROJECT_ID, newEntityDTO.getProjectID())
	                .set(ENTITY.ENTITY_TYPE_ID, newEntityDTO.getEntityTypeID())
	                .set(ENTITY.DATA, toJSONB(newEntityDTO.getData()))
	                .set(ENTITY.UPDATED_AT, OffsetDateTime.now())
                .where(ENTITY.ID.eq(oldEntityID))
	                .and(ENTITY.PROJECT_ID.eq(projectID))
	                .and(ENTITY.ENTITY_TYPE_ID.eq(newEntityDTO.getEntityTypeID()))
                .and(ENTITY.IS_DELETED.ne(true))
	                .returning()
	                .fetchOne();
    	} catch (DataAccessException e) {
	    	log.error("Updating the entity failed.", e);
	    	return null;
	    }
    	
    	// Add entity's record linkage tokens to the database
    	EntityDTO dto = new EntityDTO().assignPojoValues(new Entity(updatedRecord));
    	if (!linkageIndexService.rebuildIndex(dto)) {
			log.debug("Failed to update the entity's record linkage tokens to the database.");
			throw new UpdateException("Failed to update record linkage tokens.");
		}
	    
	    // Return the updated entity
        log.debug("Updating the entity \"" + newEntityDTO.getTrustdeckID() + "\" was successful.");
	    return dto;
    }

    @Transactional
    public EntityDTO updateEntity(long oldEntityID, int projectID, int entityTypeID, EntityDTO newEntityDTO) {
        if (newEntityDTO.getEntityTypeID() != entityTypeID) return null;
        EntityDTO old = getEntity(newEntityDTO.getTrustdeckID().toString(), projectID, entityTypeID);
        return old == null ? null : updateEntity(oldEntityID, projectID, newEntityDTO);
    }
    
    /**
     * Method to search for entity.
     * This search supports full-text-searching over all attributes of an entity,
     * as well as multiple words.
     * 
     * @param query the (multi-word) search query
     * @return a list of entities that match the search query
     */
    @Transactional
    public List<EntityDTO> searchEntity(String query, Integer entityTypeId) {
    	if (Assertion.isNullOrEmpty(query)) {
            log.debug("Search query is empty.");
            return null;
        }
    	
    	// Support wildcard-search (limited to 250)
		if (query.trim().equals("*")) {
			Condition condition = ENTITY.IS_DELETED.eq(false);
			if (entityTypeId != null) {
				condition = condition.and(ENTITY.ENTITY_TYPE_ID.eq(entityTypeId));
			}

			try {
				List<Entity> results = dsl.selectFrom(ENTITY)
						.where(condition)
						.orderBy(ENTITY.UPDATED_AT.desc(), ENTITY.CREATED_AT.desc(), ENTITY.TRUSTDECK_ID.asc())
						.limit(250)
						.fetchInto(Entity.class);

				if (results == null || results.isEmpty()) {
					log.trace("No entity matched the find-all query.");
					return null;
				}

				return results.stream().map(e -> new EntityDTO().assignPojoValues(e)).toList();
            } catch (MappingException e) {
                log.debug("Could not map entity search result.", e);
                return null;
            } catch (DataAccessException f) {
                log.debug("Searching entities failed.", f);
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
            Condition partCond = DSL.cast(ENTITY.TRUSTDECK_ID, String.class).likeIgnoreCase(pattern)
                .or(DSL.cast(ENTITY.PROJECT_ID, String.class).likeIgnoreCase(pattern))
                .or(DSL.cast(ENTITY.ENTITY_TYPE_ID, String.class).likeIgnoreCase(pattern))
                // JSONB via full text search on the tsvector (uses the GIN index on the tsvector named 'full_text_search_vector')
                // Removes all non-alphanumeric characters and adds the prefix-operator (:*)
                .or(DSL.condition("full_text_search_vector @@ to_tsquery('simple', regexp_replace({0}, '[^[:alnum:]]+', '', 'g') || ':*')", DSL.val(part)))
                .or(DSL.cast(ENTITY.CREATED_AT, String.class).likeIgnoreCase(pattern))
                .or(DSL.cast(ENTITY.UPDATED_AT, String.class).likeIgnoreCase(pattern));

            // If the query is long enough, perform a typo-tolerant search via the data_text column
			if (part.length() >= 3) {
				partCond = partCond.or(DSL.cast(ENTITY.DATA_TEXT, String.class).likeIgnoreCase(pattern));
			}
            
            // AND-connect all search parts
            condition = condition.and(partCond);
        }

        // Exclude deleted entities
        condition = condition.and(ENTITY.IS_DELETED.eq(false));
        
        if (entityTypeId != null) {
        	condition = condition.and(ENTITY.ENTITY_TYPE_ID.eq(entityTypeId));
        }

        // Execute the search
        List<Entity> results;
        try {
            results = dsl.selectFrom(ENTITY)
                      .where(condition)
                      .orderBy(DSL.field("ts_rank(full_text_search_vector, plainto_tsquery('simple', {0}))", Double.class, DSL.val(query)).desc(),
                    		   DSL.field("similarity(data_text, {0})", Double.class, DSL.val(query)).desc())
                      .limit(100)
                      .fetchInto(Entity.class);
        } catch (MappingException e) {
            log.debug("Could not map entity search result.", e);
            return null;
        } catch (DataAccessException f) {
            log.debug("Searching entities failed.", f);
            return null;
        }

        // Evaluate the search results
        if (results == null || results.isEmpty()) {
            log.debug("No entity matched the query \"" + query + "\".");
            return null;
        }

        // Return the found types
        return results.stream().map(row -> new EntityDTO().assignPojoValues(row)).toList();
    }
    
    /**
     * Method to search for record linkage candidates that match a given set of attributes.
     * 
     * @param projectId the id of the project in which the linkage should be done
     * @param entityTypeId the id of the entity's type
     * @param linkageValues a map of attribute-value-combinations that should be searched for
     * @param limit the maximum amount of matches that should be returned
     * @return a list of entities that match the given linkage values
     */
    @Transactional(readOnly = true)
    public List<EntityDTO> searchRecordLinkageCandidates(int projectId, int entityTypeId, Map<String, JsonNode> linkageValues, int limit) {
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
    	    condition = condition.and(DSL.condition("{0} @> {1}::jsonb", ENTITY.DATA, DSL.inline(fragment)));
    	}
    	
    	// Perform the search
    	List<Entity> candidates;
        try {
	    	candidates = dsl.selectFrom(ENTITY)
		    	.where(ENTITY.PROJECT_ID.eq(projectId))
		    	.and(ENTITY.ENTITY_TYPE_ID.eq(entityTypeId))
		    	.and(ENTITY.IS_DELETED.eq(false))
		    	.and(condition)
		    	.limit(limit)
		    	.fetchInto(Entity.class);
        } catch (MappingException e) {
            log.debug("Could not map entity search result.", e);
            return null;
        } catch (DataAccessException f) {
            log.debug("Searching entities failed.", f);
            return null;
        }

        // Evaluate the search results
        if (candidates == null) {
            log.debug("No entity candidates found that match the given attributes.");
            return null;
        }

        // Return the found types
        return candidates.stream().map(row -> new EntityDTO().assignPojoValues(row)).toList();
    }
    
    /**
     * Finds candidate entity IDs by matching the provided blocking tokens against
     * the record linkage token index in the database.
     * 
     * Only tokens of type {@code block} are used for candidate generation. Matching records
     * are grouped by entity ID and ordered by the number of matching blocking tokens,
     * so records sharing more blocking tokens with the query payload are returned first.
     * 
     * @param projectId the ID of the project in which candidate records should be searched
     * @param entityTypeId the ID of the entity type to which the candidate records must belong
     * @param payloadTokens the linkage tokens generated from the input payload
     * @param limit the maximum number of candidate entity IDs to return
     * @param includeDeleted whether or not soft-deleted entities should be included as candidates
     * @return the list of candidate entity IDs ordered by descending number of matching blocking tokens
     */
    @Transactional(readOnly = true)
    public List<Long> findCandidateIdsByBlockingTokens(int projectId, int entityTypeId, List<LinkageToken> payloadTokens, int limit, boolean includeDeleted) {
    	// Only blocking tokens are used during candidate generation
    	List<LinkageToken> blockTokens = payloadTokens.stream()
    			.filter(Objects::nonNull)
    			.filter(t -> t.getTokenType() != null)
    			.filter(t -> t.getTokenType().isCandidateGenerationToken())
    			.filter(t -> t.getTag() != null)
                .filter(t -> t.getTokenValue() != null)
                .distinct().toList();

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

    	// Exclude tombstoned entities when the caller explicitly wants active candidates only
    	if (!includeDeleted) {
    		condition = condition.and(ENTITY.IS_DELETED.eq(false));
    	}

    	// Retrieve matching entity IDs, exclude soft-deleted records,
    	// and rank candidates by the number of matching blocking tokens.
    	// Fetch one additional candidate so that an exceeded limit can be
        // detected without retrieving the complete candidate set.
    	List<Long> candidateIds = dsl.select(LINKAGE_TOKEN.ENTITY_ID)
    			.from(LINKAGE_TOKEN)
    			.join(ENTITY)
					.on(LINKAGE_TOKEN.PROJECT_ID.eq(ENTITY.PROJECT_ID))
					.and(LINKAGE_TOKEN.ENTITY_TYPE_ID.eq(ENTITY.ENTITY_TYPE_ID))
					.and(LINKAGE_TOKEN.ENTITY_ID.eq(ENTITY.ID))
				.where(condition)
				.groupBy(LINKAGE_TOKEN.ENTITY_ID)
				.orderBy(DSL.count().desc(), LINKAGE_TOKEN.ENTITY_ID.asc())
				.limit(limit + 1)
				.fetch(LINKAGE_TOKEN.ENTITY_ID);
    	
    	if (candidateIds.size() > limit) {
            log.warn("Record-linkage blocking produced more than " + limit + " candidates for project "
            		+ projectId + " and entity type " + entityTypeId + ".");
            throw new TooManyRecordLinkageCandidatesException(limit);
        }

        return candidateIds;
    }
    
    /**
     * Finds active candidate entity IDs by matching the provided blocking tokens.
     * Soft-deleted entities are excluded.
     * 
     * @param projectId the ID of the project in which candidate records should be searched
     * @param entityTypeId the ID of the entity type to which the candidate records must belong
     * @param payloadTokens the linkage tokens generated from the input payload
     * @param limit the maximum number of candidate entity IDs to return
     * @return the list of active candidate entity IDs
     */
    @Transactional(readOnly = true)
    public List<Long> findCandidateIdsByBlockingTokens(int projectId, int entityTypeId, List<LinkageToken> payloadTokens, int limit) {
    	return findCandidateIdsByBlockingTokens(projectId, entityTypeId, payloadTokens, limit, false);
    }
    
    /**
     * Retrieves all record linkage tokens from the database for the given entity 
     * entities. The returned map is keyed by entity ID (so tuples of 
     * (entityID, linkageTokens)) so that the tokens can be efficiently matched 
     * to their corresponding candidate records during later scoring.
     * 
     * @param entityIDs the database IDs of the entities whose linkage tokens should be retrieved
     * @param entityTypeID the ID of the entity type to which the entities belong
     * @return a map from entity ID to the list of stored linkage tokens
     */
    @Transactional(readOnly = true)
    public Map<Long, List<LinkageToken>> getLinkageTokensForEntities(List<Long> entityIDs, int entityTypeID) {
    	if (entityIDs == null || entityIDs.isEmpty()) {
    		return Map.of();
    	}

    	Map<Long, List<LinkageToken>> out = new HashMap<>();

    	// Retrieve all linkage tokens for the requested entities and group them by entity ID; add them to the map
    	dsl.selectFrom(LINKAGE_TOKEN)
    			.where(LINKAGE_TOKEN.ENTITY_TYPE_ID.eq(entityTypeID))
    			.and(LINKAGE_TOKEN.ENTITY_ID.in(entityIDs))
    			.fetch()
    			.forEach(tok -> {
    				Long id = tok.getEntityId();

    				// Add each token to the list belonging to its entity
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
