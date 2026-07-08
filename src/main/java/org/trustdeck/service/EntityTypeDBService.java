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

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.MappingException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.exception.CreationException;
import org.trustdeck.exception.DuplicateEntityTypeException;
import org.trustdeck.exception.UnexpectedResultSizeException;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.jooq.generated.tables.pojos.EntityType;
import org.trustdeck.jooq.generated.tables.records.EntityTypeRecord;
import org.trustdeck.utils.Assertion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import static org.trustdeck.jooq.generated.Tables.ENTITY_TYPE;
import static org.trustdeck.jooq.generated.Tables.ENTITY_INSTANCE;

/**
 * This class encapsulates the database access for entity types.
 * 
 * @author Armin Müller
 */
@Slf4j
@Service
public class EntityTypeDBService {
    
	/** References a jOOQ configuration object that configures jOOQ's behavior when executing queries. */
    @Autowired
	private DSLContext dsl;
    
    /** Enables access to the data base interaction methods for project objects. */
    @Autowired
    private ProjectDBService projectDBService;
	
	/** Enables access to domain database functions. */
	@Autowired
	private DomainDBAccessService ddba;
	
	/** Enables access to the mapper to transform JsonNode into JSONB and back. */
	@Autowired
	private ObjectMapper objectMapper;
	
	/**
     * Method to insert a new entity type into the database.
     * 
     * @param entityTypeDTO the entity type data transfer object containing the necessary data
     * @return The newly inserted entity type object when the insertion was successful,
     * 		   the original entity type object if the given one was a duplicate, and
     * 		   {@code null} when the insertion failed.
     * @throws CreationException when the creation of the entity type or the partition 
     * 		   in the database failed to abort and roll back this transaction.
	 * @throws DuplicateEntityTypeException when an entity type with the same name already exists
     */
    @Transactional
    public EntityTypeDTO createEntityType(EntityTypeDTO entityTypeDTO) throws CreationException, DuplicateEntityTypeException {
    	// Create the record and send it to the database
    	EntityTypeRecord createdEntityType;
    	try {
    		createdEntityType = dsl.newRecord(ENTITY_TYPE);
	    	createdEntityType.setName(entityTypeDTO.getName());
	    	createdEntityType.setVersion(entityTypeDTO.getVersion());
	    	createdEntityType.setIsDeprecated(entityTypeDTO.getIsDeprecated());
	    	createdEntityType.setIsBaseType(entityTypeDTO.getIsBaseType());
	    	createdEntityType.setTypeDefinition(toJSONB(entityTypeDTO.getTypeDefinition()));
	    	createdEntityType.setBaseTypeId(entityTypeDTO.getBaseTypeId());
	    	createdEntityType.setAssociatedDomainId(getDomainIdFromName(entityTypeDTO.getAssociatedDomainName()));
	    	createdEntityType.setProjectId(entityTypeDTO.getProjectId());
	
	        // Store and determine success
	        if (createdEntityType.insert() != 1) {
	        	log.debug("Inserting the entity type failed.");
	        	return null;
	        }
    	
	    	// Create a new partition in the database for this type
	    	// jOOQ doesn't model "PARTITION OF" yet, so use plain SQL
	        dsl.query("CREATE TABLE {0} PARTITION OF {1} FOR VALUES IN ({2})", 
	        		DSL.name("entityinstance_t" + createdEntityType.getId()), 
	        		DSL.name("entity_instance"), 
	        		DSL.inline(createdEntityType.getId()))
	        .execute();
	    } catch (DataAccessException e) {
	    	if (e.getMessage().contains(" already exists.")) {
	    		// Found duplicate, return the original
	    		throw new DuplicateEntityTypeException(entityTypeDTO.getName());
	    	} else {
		    	// Inserting the new entity type into the database failed; throw exception to abort
		    	throw new CreationException(e.getMessage());
	    	}
	    }
	    
	    // Return the entity type
        log.debug("Creating the entity type \"" + entityTypeDTO.getName() + "\" was successful.");
	    return new EntityTypeDTO().assignPojoValues(new EntityType(createdEntityType));
    }

	/**
     * Method to retrieve an entity type from the database by explicitly providing the two variables
     * name and projectID. Tuples of these are unique in the database.
     * Base types will have no projectID, so it can be null.
     * 
     * @param name the entity type's name
     * @param projectID the project to which the entity type is assigned to
     * @return the retrieved entity type when successfully found, or {@code null} when nothing was found.
     */
    @Transactional
    public EntityTypeDTO getEntityTypeByName(String name, Integer projectID) {
    	// Check if all the necessary arguments are available
    	if (Assertion.isNullOrEmpty(name)) {
    		log.debug("For retrieving the entity type, there is an argument missing or empty.");
    		return null;
    	}
    	
    	// Build and execute the query
    	List<EntityType> entityTypes = null;
    	try {
			Condition cond = DSL.trueCondition()
			        .and(ENTITY_TYPE.NAME.equalIgnoreCase(name))
			        .and(ENTITY_TYPE.IS_DEPRECATED.eq(false));

			// Add the projectID as a condition, if available
			if (projectID != null) {
				cond = cond.and(ENTITY_TYPE.PROJECT_ID.eq(projectID));
			} else {
				// If the projectID is null, assume we are searching for a base type (which has no associated project)
				cond = cond.and(ENTITY_TYPE.IS_BASE_TYPE.eq(true));
			}

			entityTypes = dsl.selectFrom(ENTITY_TYPE)
					.where(cond)
					.fetchInto(EntityType.class);
        } catch (MappingException e) {
        	log.debug("Could not map the entity type search result into the EntityType-POJO.", e);
        	return null;
        } catch (DataAccessException f) {
        	log.debug("Searching for the entity type in the database failed.", f);
        	return null;
        }
    	
    	// Check if the search was successful
    	if (entityTypes == null || entityTypes.size() == 0) {
    		log.debug("No entity type was found.");
            return null;
    	} else if (entityTypes.size() > 1) {
        	log.debug("Too many entity types were found.");
        	return null;
        } else if (entityTypes.getFirst().getIsDeprecated()) {
        	log.debug("The entity type is marked as deleted/deprecated.");
        	return null;
        }

        // Create a DTO, populate and return it
        return new EntityTypeDTO().assignPojoValues(entityTypes.getFirst());
    }

	/**
     * Method to retrieve an entity type from the database by explicitly providing the two variables
     * name and projectID. Tuples of these are unique in the database.
     * Base types will have no projectID, so it can be null.
     * 
     * @param entityTypeId the entity type's ID
     * @param projectID the project to which the entity type is assigned to
     * @return the retrieved entity type when successfully found, or {@code null} when nothing was found.
     */
    @Transactional
    public EntityTypeDTO getEntityTypeById(int entityTypeId, Integer projectID) {
    	// Check if all the necessary arguments are available
    	if (entityTypeId <= 0) {
    		log.debug("For retrieving the entity type, there is an argument missing or empty.");
    		return null;
    	}
    	
    	// Build and execute the query
    	List<EntityType> entityTypes = null;
    	try {
    		Condition cond = DSL.trueCondition()
			        .and(ENTITY_TYPE.ID.equal(entityTypeId))
			        .and(ENTITY_TYPE.IS_DEPRECATED.eq(false));

			// Add the projectID as a condition, if available
			if (projectID != null) {
				cond = cond.and(ENTITY_TYPE.PROJECT_ID.eq(projectID));
			} else {
				// If the projectID is null, assume we are searching for a base type (which has no associated project)
				cond = cond.and(ENTITY_TYPE.IS_BASE_TYPE.eq(true));
			}

			entityTypes = dsl.selectFrom(ENTITY_TYPE)
					.where(cond)
					.fetchInto(EntityType.class);
        } catch (MappingException e) {
        	log.debug("Could not map the entity type search result into the EntityType-POJO.", e);
        	return null;
        } catch (DataAccessException f) {
        	log.debug("Searching for the entity type in the database failed.", f);
        	return null;
        }
    	
    	// Check if the search was successful
    	if (entityTypes == null || entityTypes.size() == 0) {
    		log.debug("No entity type was found.");
            return null;
    	} else if (entityTypes.size() > 1) {
        	log.debug("Too many entity types were found.");
        	return null;
        } else if (entityTypes.getFirst().getIsDeprecated()) {
        	log.debug("The entity type is marked as deleted/deprecated.");
        	return null;
        }

        // Create a DTO, populate and return it
        return new EntityTypeDTO().assignPojoValues(entityTypes.getFirst());
    }
    
    /**
     * Method to retrieve an entity type from the database by providing an 
     * EntityTypeDTO containing at least name and projectID. Tuples of 
     * these are unique in the database.
     * 
     * @param entityTypeDTO the entity type data transfer object containing the necessary data (at least name and projectID)
     * @return the entity type as a DTO, or {@code null} if nothing was found or the search failed
     */
    @Transactional
    public EntityTypeDTO getEntityType(EntityTypeDTO entityTypeDTO) {
    	// Check if all the necessary arguments are available
    	if (entityTypeDTO == null) {
    		return null;
    	}
    	
    	if (entityTypeDTO.getId() != null) {
    		return getEntityTypeById(entityTypeDTO.getId(), entityTypeDTO.getProjectId());
    	} else if (entityTypeDTO.getName() != null) {
    		return getEntityTypeByName(entityTypeDTO.getName(), entityTypeDTO.getProjectId());
    	} else {
    		log.debug("Neither name, nor ID were given to search for in the database.");
    		return null;
    	}
    }
    
    /**
     * Method to delete an entity type. Only possible if the entity type is not used anywhere.
     * 
     * @param type the entity type's DTO including the name and the projectID
     * @return {@code true} when deletion was successful, {@code false} when anything went wrong during the deletion
     * @throws UnexpectedResultSizeException when the deletion would have affected an unexpected number of database entries
     */
    @Transactional
    public boolean deleteEntityType(EntityTypeDTO type) throws UnexpectedResultSizeException {
    	// Check if all the necessary arguments are available
    	if (Assertion.isNullOrEmpty(type.getName()) || type.getProjectId() == null) {
    		log.debug("For retrieving the entity type, there is an argument missing or empty.");
    		return false;
    	}
    	
    	// Check that we do not delete base-types
    	if (type.getIsBaseType()) {
    		log.info("Cannot delete base type.");
    		return false;
    	}
    	
    	// Check if the type is already "deleted"
    	if (type.getIsDeprecated()) {
    		log.debug("The type was already deleted. Nothing to do.");
    		return true;
    	}
    	
    	// Check if the entity type is used anywhere --> only delete it if it's not used
    	int usedBy = 0;
    	try {
    		usedBy = dsl.selectCount()
    				.from(ENTITY_INSTANCE)
    				.where(ENTITY_INSTANCE.ENTITY_TYPE_ID.equal(type.getId()))
    				.fetchOne(0, int.class);
    	} catch (DataAccessException e) {
    		log.debug("Searching for entity type refrences in the database failed.", e);
    		return false;
    	}
    	
    	if (usedBy != 0) {
    		log.debug("The entity type was referenced by " + usedBy + "entit" + (usedBy == 1 ? "y" : "ies") + " and can therefore not be deleted.");
    		return false;
    	}
    	
    	// The entity type is not used --> perform deletion by setting the is_deprecated flag
    	int deletedEntityTypes = 0;
    	try {
    		// Build and execute the query
    		deletedEntityTypes = dsl.update(ENTITY_TYPE)
		            .set(ENTITY_TYPE.IS_DEPRECATED, true)
		            .where(ENTITY_TYPE.NAME.equalIgnoreCase(type.getName()))
		            .and(ENTITY_TYPE.PROJECT_ID.equal(type.getProjectId()))
		            .execute();
        } catch (DataAccessException e) {
        	log.debug("Deleting the entity type in the database failed.", e);
        	return false;
        }
    	
    	// Check if the deletion was successful
    	if (deletedEntityTypes != 1) {
    		// An unexpected number of records was affected. Log it and abort by throwing
            // an exception (which will rollback everything from the transaction).
            log.error("Couldn't delete the entity type \"" + type.getName() + "\" from the database.");
            throw new UnexpectedResultSizeException(1, deletedEntityTypes);
    	}
    	
    	// If we reach this point, the deletion was successful
    	return true;
    }
    
    /**
     * Method to update an entity type. Only works when the entity type is not used anywhere.
     * 
     * @param oldEntityTypeDTO the entity type data transfer object containing the necessary data to identify the type that should be updated
     * @param newEntityTypeDTO the entity type object containing the data to use for the update; null-values lead to keeping the old values
     * @return the updated entity type object when successful, {@code null} when anything went wrong
     */
    @Transactional
    public EntityTypeDTO updateEntityType(EntityTypeDTO oldEntityTypeDTO, EntityTypeDTO newEntityTypeDTO) {
    	// Ensure that all necessary information is given --> retrieve the database version of the old record
    	EntityTypeDTO oldType = getEntityType(oldEntityTypeDTO);
    	
    	if (oldType == null) {
    		return null;
    	}
    	
    	// Check that the project in which this type is defined is not yet deleted
    	if (projectDBService.getProjectByID(oldType.getProjectId()).getEndDate().isBefore(OffsetDateTime.now())) {
    		log.debug("The project, in which this type is defined, is already deleted. No updates to the type are allowed.");
    		return null;
    	}
    	
    	// Check if the entity type is used anywhere --> only update it if it's not used
    	int usedBy = 0;
    	try {
    		usedBy = dsl.selectCount()
    				.from(ENTITY_INSTANCE)
    				.where(ENTITY_INSTANCE.ENTITY_TYPE_ID.equal(oldType.getId()))
    				.fetchOne(0, int.class);
    	} catch (DataAccessException e) {
    		log.debug("Searching for entity type references in the database failed.", e);
    		return null;
    	}
    	
    	if (usedBy != 0) {
    		log.debug("The entity type was referenced by " + usedBy + " entit" + (usedBy == 1 ? "y" : "ies") + " and can therefore not be updated.");
    		return null;
    	}
    	
    	// Decide on which values to use
    	String name = Assertion.isNotNullOrEmpty(newEntityTypeDTO.getName()) ? newEntityTypeDTO.getName() : oldType.getName();
        String version = Assertion.isNotNullOrEmpty(newEntityTypeDTO.getVersion()) ? newEntityTypeDTO.getVersion() : oldType.getVersion();
        JSONB typeDef = newEntityTypeDTO.getTypeDefinition() != null ? toJSONB(newEntityTypeDTO.getTypeDefinition()) : toJSONB(oldType.getTypeDefinition());
        Integer domainID = Assertion.isNotNullOrEmpty(newEntityTypeDTO.getAssociatedDomainName()) ? getDomainIdFromName(newEntityTypeDTO.getAssociatedDomainName()) : getDomainIdFromName(oldEntityTypeDTO.getAssociatedDomainName());
        
    	// Create the update-record and send it to the database
        EntityTypeRecord updatedRecord = null;
    	try {
    		// Update and return the updated record
    		updatedRecord = dsl.update(ENTITY_TYPE)
	                .set(ENTITY_TYPE.NAME, name)
	                .set(ENTITY_TYPE.VERSION, version)
	                .set(ENTITY_TYPE.TYPE_DEFINITION, typeDef)
	                .set(ENTITY_TYPE.ASSOCIATED_DOMAIN_ID, domainID)
	                .where(ENTITY_TYPE.ID.eq(oldType.getId()))
	                .and(ENTITY_TYPE.IS_DEPRECATED.equal(false))
	                .returning()
	                .fetchOne();
	    } catch (DataAccessException e) {
	    	log.error("Updating the entity type failed.", e);
	    	return null;
	    }
	    
	    // Return the entity type
        log.debug("Updating the entity type \"" + updatedRecord.getName() + "\" was successful.");
	    return new EntityTypeDTO().assignPojoValues(new EntityType(updatedRecord));
    }
    
    /**
     * Method to search for entity types.
     * This search supports full-text-searching over all attributes of an entity type,
     * as well as multiple words.
     * 
     * @param query the (multi-word) search query
     * @param projectId the id of the project
     * @return a list of entity types that match the search query
     */
    @Transactional
    public List<EntityTypeDTO> searchEntityType(String query, Integer projectId) {
    	if (Assertion.isNullOrEmpty(query)) {
            log.debug("Search query is empty.");
            return null;
        }
    	
    	// Allow wildcard-search with '*'
    	if (query.trim().equals("*")) {
    		List<EntityType> results;
            try {
        		// Check if we are searching for a base-type (projectId not given)
        		if (projectId == null) {
        			results = dsl.selectFrom(ENTITY_TYPE)
        						.where(ENTITY_TYPE.PROJECT_ID.isNull())
        						.and(ENTITY_TYPE.IS_BASE_TYPE.eq(true))
		            			.and(ENTITY_TYPE.IS_DEPRECATED.eq(false))
    	            			.orderBy(ENTITY_TYPE.NAME.asc(), ENTITY_TYPE.VERSION.asc())
        						.fetchInto(EntityType.class);
        		} else {
        			// Return all project-specific entity types
        			results = dsl.selectFrom(ENTITY_TYPE)
		            			.where(ENTITY_TYPE.PROJECT_ID.eq(projectId))
		            			.and(ENTITY_TYPE.IS_DEPRECATED.eq(false))
		            			.orderBy(ENTITY_TYPE.NAME.asc(), ENTITY_TYPE.VERSION.asc())
		                        .fetchInto(EntityType.class);
        		}
            } catch (MappingException e) {
                log.debug("Could not map entity type search result.", e);
                return null;
            } catch (DataAccessException e) {
                log.debug("Searching entity types failed.", e);
                return null;
            }
    		
            // Return all the types
    		return results.stream().map(e -> new EntityTypeDTO().assignPojoValues(e)).toList();
    	}

        // Split query-parts on whitespaces; every part should match at least one column
        String[] parts = query.trim().split("\\s+");
        
        // Built the search conditions statement
        Condition condition = DSL.trueCondition();
        for (String part : parts) {
            String pattern = "%" + part + "%";

            // Search across name, version, and type definition columns
            Condition partCond = ENTITY_TYPE.NAME.likeIgnoreCase(pattern)
                .or(ENTITY_TYPE.VERSION.likeIgnoreCase(pattern))
                // JSONB via full text search on the tsvector (uses the GIN index on the tsvector named 'full_text_search_vector')
                .or(DSL.condition("full_text_search_vector @@ plainto_tsquery('simple', {0})", DSL.val(part)));

            // AND-connect all search parts
            condition = condition.and(partCond);
        }

        // Exclude deprecated/deleted types
        condition = condition.and(ENTITY_TYPE.IS_DEPRECATED.eq(false));
        
        // Handle base type searches
        if (projectId == null) {
        	condition = condition.and(ENTITY_TYPE.PROJECT_ID.isNull())
        						 .and(ENTITY_TYPE.IS_BASE_TYPE.eq(true));
        } else {
        	// Only search in a project scope
        	condition = condition.and(ENTITY_TYPE.PROJECT_ID.eq(projectId));
        }

        // Execute the search
        List<EntityType> results;
        try {
            results = dsl.selectFrom(ENTITY_TYPE)
                      .where(condition)
                      .orderBy(ENTITY_TYPE.NAME.asc(), ENTITY_TYPE.VERSION.asc())
                      .fetchInto(EntityType.class);
        } catch (MappingException e) {
            log.debug("Could not map entity type search result.", e);
            return null;
        } catch (DataAccessException e) {
            log.debug("Searching entity types failed.", e);
            return null;
        }

        // Evaluate the search results
        if (results == null || results.isEmpty()) {
            log.debug("No entity type matched the query \"" + query + "\".");
            return null;
        }

        // Return the found types
        return results.stream().map(row -> new EntityTypeDTO().assignPojoValues(row)).toList();
    }
    
    /**
     * This method retrieves the names for the given entity type IDs from a project.
     * 
     * @param entityTypeIDs the array of entity type IDs stored in a project
     * @return an array of the names of the entity types in the same order as the IDs were given.
     */
    @Transactional(readOnly = true)
    public String[] getEntityTypeNames(Integer[] entityTypeIDs) {
    	if (entityTypeIDs == null || entityTypeIDs.length == 0) {
    		log.debug("The given list of entity type IDs was empty.");
            return null;
        }

        // Retrieve all matching entries with id and name
        Map<Integer, String> namesById;
		try {
			namesById = dsl
			    .select(ENTITY_TYPE.ID, ENTITY_TYPE.NAME)
			    .from(ENTITY_TYPE)
			    .where(ENTITY_TYPE.ID.in(Arrays.asList(entityTypeIDs)))
			    .fetchMap(ENTITY_TYPE.ID, ENTITY_TYPE.NAME);
		} catch (DataAccessException e) {
			log.debug("Resolving an entity type ID to it's name failed.", e);
			return null;
		}

        // Create the resulting output array in the same order as the input
        String[] result = new String[entityTypeIDs.length];
        for (int i = 0; i < entityTypeIDs.length; i++) {
            Integer id = entityTypeIDs[i];
            result[i] = namesById.get(id); // will be null if id not found
        }
        
        return result;
    }

    /**
     * This method retrieves the IDs for the given entity type names from a project.
     * 
     * @param projectId the ID of the project where the entity types are scoped in
     * @param entityTypeNames the array of entity type names stored in a project
     * @return an array of the IDs of the entity types in the same order as the names were given,
     * 		   or {@code null} when something went wrong
     */
    @Transactional(readOnly = true)
    public Integer[] getEntityTypeIDs(Integer projectId, String[] entityTypeNames) {
        // Ensure the given attributes are not null or similar
    	if (entityTypeNames == null || entityTypeNames.length == 0 || projectId == null || projectId <= 0) {
    		log.debug("Either the projectID or the list of entity type names was not as expected.");
            return null;
        }

        // Create lowercased inputs for case-insensitive matching
        List<String> loweredEntityNames = Arrays.stream(entityTypeNames).map(s -> s == null ? null : s.toLowerCase()).toList();

        // Query the database: (name, projectID) are unique tuples --> search for name scoped to projectID
        Map<String, Integer> idByName;
		try {
			idByName = dsl
			    .select(ENTITY_TYPE.NAME, ENTITY_TYPE.ID)
			    .from(ENTITY_TYPE)
			    .where(ENTITY_TYPE.PROJECT_ID.eq(projectId))
			    .and(DSL.lower(ENTITY_TYPE.NAME).in(loweredEntityNames))
			    .fetchMap(r -> r.get(ENTITY_TYPE.NAME).toLowerCase(),
			              r -> r.get(ENTITY_TYPE.ID));
		} catch (MappingException e) {
			log.debug("Failed to map the query result into the designated type.", e);
			return null;
		} catch (DataAccessException f) {
			log.debug("Failed to query the database while resolving entity type names to IDs.", f);
			return null;
		}

        // Preserve input order
        Integer[] result = new Integer[entityTypeNames.length];
        for (int i = 0; i < entityTypeNames.length; i++) {
            String key = entityTypeNames[i] != null ? entityTypeNames[i].toLowerCase() : null;
            result[i] = key != null ? idByName.get(key) : null;
        }
        
        return result;
    }

    /**
     * Helper method to retrieve the database internal ID of a domain
     * specified by its name. Also handles null-cases.
     * 
     * @param domainName the name of the domain
     * @return the ID of the domain or {@code null} when the name was empty or not found
     */
    private Integer getDomainIdFromName(String domainName) {
    	Domain d = Assertion.isNullOrEmpty(domainName) ? null : ddba.getDomainByName(domainName);
    	return d == null ? null : d.getId();
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
}
