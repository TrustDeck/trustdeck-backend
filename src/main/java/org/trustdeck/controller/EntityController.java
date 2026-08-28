/*
 * Trust Deck Services
 * Copyright 2025-2026 Armin Müller and Eric Wündisch
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

package org.trustdeck.controller;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.trustdeck.algorithms.PseudonymizationFactory;
import org.trustdeck.algorithms.Pseudonymizer;
import org.trustdeck.dto.DomainDTO;
import org.trustdeck.dto.EntityDTO;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.dto.ProjectDTO;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.dto.RecordLinkageCandidateDTO;
import org.trustdeck.exception.DuplicateEntityException;
import org.trustdeck.exception.TooManyRecordLinkageCandidatesException;
import org.trustdeck.exception.UnexpectedResultSizeException;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.linkage.model.CandidateStatus;
import org.trustdeck.linkage.model.EntityLinkageConfig;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.security.audittrail.annotation.Audit;
import org.trustdeck.service.AuthorizationService;
import org.trustdeck.service.DomainDBAccessService;
import org.trustdeck.service.AlgorithmDBService;
import org.trustdeck.service.EntityDBService;
import org.trustdeck.service.EntityTypeDBService;
import org.trustdeck.service.JsonSchemaService;
import org.trustdeck.service.ProjectDBService;
import org.trustdeck.service.PseudonymDBAccessService;
import org.trustdeck.service.RecordLinkageService;
import org.trustdeck.service.ResponseService;
import org.trustdeck.utils.Assertion;
import org.trustdeck.utils.Utility.Pair;

import com.networknt.schema.JsonSchema;

import lombok.extern.slf4j.Slf4j;

/**
 * This class offers a REST API for interacting with entities.
 *
 * @author Armin Müller
 */
@RestController
@EnableMethodSecurity
@Slf4j
@RequestMapping(value = "/api")
public class EntityController {
	
	/** Enables service for working with predefined responses. */
    @Autowired
    private ResponseService responseService;
    
    /** Enables access to the data base interaction methods for the entity type. */
    @Autowired
    private EntityTypeDBService entityTypeDBService;
    
    /** Enables access to the data base interaction methods for the entity. */
    @Autowired
    private EntityDBService entityDBService;
    
    /** Enables access to the data base interaction methods for project objects. */
    @Autowired
    private ProjectDBService projectDBService;
	
	/** Enables access to the domain data base interaction methods. */
	@Autowired
	private DomainDBAccessService ddba;

	/** Enables the access to the domain specific database access methods. */
	@Autowired
	private AlgorithmDBService algorithmDBService;

	/** Creates pseudonymizers with the configured dependencies. */
	@Autowired
	private PseudonymizationFactory pseudonymizationFactory;

	/** Enables access to the pseudonym data base interaction methods. */
	@Autowired
	private PseudonymDBAccessService pdba;
	
	/** Enables access to the JSON schema validation functionalities. */
	@Autowired
	private JsonSchemaService jsonSchemaService;
	
	/** Enables access to record linkage candidate search. */
	@Autowired
	private RecordLinkageService recordLinkageService;

    /** Provides functionality to ensure proper rights and roles when accessing the endpoints. */
    @Autowired
    private AuthorizationService authorizationService;
	
	/** The maximum number of search results that will be returned to the caller. */
	private final static int MAX_NUMBER_OF_SEARCH_RESULTS = 20;
	
	/**
	 * Endpoint to create a new entity of an entity type.
	 * If a domain is associated with the given type, this endpoint automatically
	 * creates a pseudonym in the associated domain.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param entityTypeName the name of the entity type associated with this entity
	 * @param entityDTO the data transfer object containing this entity's data
	 * @param recordLinkageResolution (optional) resolution-option for automatic record linkage candidates
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>201-CREATED</b> status with the created entity on success</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when the entity payload is 
     *         missing/invalid or fails schema validation</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project or entity type does not 
     *         cannot be found</li>
     *         <li>a <b>410-GONE</b> status when the project has ended or the entity type 
     *         is marked as deprecated</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when creation failed or when 
     *         the record linkage tokens could not be created/stored in the database</li>
	 */
	@PostMapping("/projects/{projectAbbreviation}/entities/{entityTypeName}")
	@PreAuthorize("isAuthenticated() and @auth.hasEntityTypePermission(#root, #projectAbbreviation, #entityTypeName, 'entity:create') "
			+ "and (#recordLinkageResolution != 'CREATE_ORIGINAL' or @auth.hasEntityTypePermission(#root, #projectAbbreviation, #entityTypeName, 'entity:resolve-linkage'))")
	@Audit
	public ResponseEntity<?> createEntity(@PathVariable("projectAbbreviation") String projectAbbreviation,
										  @PathVariable("entityTypeName") String entityTypeName,
										  @RequestBody EntityDTO entityDTO,
										  @RequestParam(name = "recordLinkageResolution", required = false) String recordLinkageResolution,
										  @RequestHeader(name = "accept", required = false) String responseContentType) {
		// Check if project exists and still active
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("Project \"" + projectAbbreviation + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			// Project end was in the past
			log.debug("The project already ended so that no entity types can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Check if entity type exists and is still active
		EntityTypeDTO entityType = entityTypeDBService.getEntityTypeByName(entityTypeName, project.getId());
		if (entityType == null) {
			log.debug("Entity type \"" + entityTypeName + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (entityType.getIsDeprecated()) {
			log.debug("The entity type is marked as deprecated and cannot be used anymore.");
			return responseService.gone(responseContentType);
		}
		
		// Check that the provided DTO in the body is there and has data in it
	    if (entityDTO == null || entityDTO.getData() == null) {
	        log.debug("No entity payload provided.");
	        return responseService.badRequest(responseContentType);
	    }
	    
	    // Retrieve or build the compiled type schema
    	JsonSchema compiledTypeSchema = jsonSchemaService.getCompiledSchemaFromDefinition(entityType.getTypeDefinition());
    	
    	// Validate the payload data against the type definition
        	List<String> errors = jsonSchemaService.validateInstance(entityDTO.getData(), compiledTypeSchema);

    	// Check if there are any errors
        if (!errors.isEmpty()) {
            log.debug("Entity payload validation failed.");
            for (String s : errors) {
            	log.trace(s);
            }
            
            return responseService.badRequest(responseContentType);
        }
        
        // Apply entity-level automatic record linkage before insertion
        EntityTypeDTO baseType = entityType.getBaseTypeName() == null ? null : entityTypeDBService.getEntityTypeByName(entityType.getBaseTypeName(), null);
        EntityLinkageConfig linkageConfig = jsonSchemaService.resolveEntityLinkageConfig(entityType.getTypeDefinition(), baseType == null ? null : baseType.getTypeDefinition());

        // Check if linkage is enabled and automatic linkage should be done
        if (linkageConfig.isEnabled() && linkageConfig.isAutoLinkOnCreate()) {
            // Generate candidates
        	List<RecordLinkageCandidateDTO> candidates;
			try {
				candidates = recordLinkageService.findCandidates(project.getId(), entityType, entityDTO.getData(), true);
			} catch (TooManyRecordLinkageCandidatesException e) {
				log.warn("Record linkage was aborted because the blocking rules produced more than " + e.getCandidateLimit() + " candidates.");
			    return responseService.unprocessableEntity(responseContentType);
			}
            
        	if (candidates == null) {
                log.debug("Automatic record linkage failed.");
                return responseService.unprocessableEntity(responseContentType);
            }

            // Return matching entity or list of possible matches
            if (!candidates.isEmpty() && !"CREATE_ORIGINAL".equals(recordLinkageResolution)) {
                RecordLinkageCandidateDTO bestCandidate = candidates.getFirst();
                if (linkageConfig.returnsExistingOnMatch() && bestCandidate.getCandidateStatus() == CandidateStatus.ACTIVE) {
                    log.info("Automatic record linkage found an existing entity; returning it instead of creating a duplicate.");
                    return responseService.ok(responseContentType, bestCandidate.getEntity());
                }

                log.info("Automatic record linkage found candidate entities; creation was rejected.");
                return responseService.conflict(responseContentType, candidates);
            }
        }
		
		// Fill the DTO that encapsulates the necessary information
		EntityDTO createEntity = new EntityDTO();
		createEntity.setProjectID(project.getId());
		createEntity.setEntityTypeID(entityType.getId());
		createEntity.setData(entityDTO.getData());
		
		EntityDTO created;
		try {
			created = entityDBService.createEntity(createEntity);
		} catch (DuplicateEntityException e) {
			log.info("While creating an entity, an identical one was found and will be used instead.");
			return responseService.ok(responseContentType, entityDBService.getEntity(createEntity));
		}
		
		// Evaluate the creation success
		if (created == null) {
			log.info("Entity creation failed during database access.");
			return responseService.unprocessableEntity(responseContentType);
		}
		
		// Check if a domain is connected to the entity's type
		if (Assertion.isNotNullOrEmpty(entityType.getAssociatedDomainName())) {
			// There is an associated domain --> a pseudonym should automatically be created
			
			// Retrieve domain
			Domain domain = ddba.getDomainByName(entityType.getAssociatedDomainName());
			
			// If a valid domain object is available, generate and store the psn-value
			if (domain != null) {
				String identifier = created.getTrustdeckID().toString();
				String idType = "TrustDeckID";
				
				// Generate a new pseudonym-value
	            Algorithm algorithm = algorithmDBService.getAlgorithmByID(domain.getAlgorithmId());
	            Pseudonymizer pseudonymizer = pseudonymizationFactory.getPseudonymizer(algorithm);
	            String rawPseudonym = pseudonymizer.pseudonymize(identifier + idType + algorithm.getSalt(), domain.getPrefix());
	            String psn = algorithm.getAddCheckDigit() ? pseudonymizer.addCheckDigit(rawPseudonym, algorithm.getLengthIncludesCheckDigit(), domain.getName(), domain.getPrefix()) : rawPseudonym;
				
				// Build pseudonym object
	            IdentifierItem idItem = IdentifierItem.builder().identifier(identifier).idType(idType).build();
				PseudonymDTO p = new PseudonymDTO();
				p.setIdentifierItem(idItem);
				p.setPsn(psn);
				p.setValidFrom(domain.getValidfrom());
	            p.setValidFromInherited(true);
	            p.setValidTo(domain.getValidto());
	            p.setValidToInherited(true);
	            p.setDomainName(domain.getName());
				
	            // Sent to database
	            List<String> results = pdba.createPseudonyms(List.of(p), domain.getId(), false);
				String result = results == null ? null : results.getFirst();
				
				// Evaluate creation result
				if (!result.equals(PseudonymDBAccessService.INSERTION_SUCCESS)) {
					log.debug("The automatic pseudonym generation failed: " + result);
				} else {
					log.debug("Successfully created a pseudonym for the entity.");
					// TODO: is currently silently failing; maybe needs to rollback the entity creation
					// Might be solved by extracting logic from controllers
				}
			} else {
				log.debug("Could not find the associated domain. No pseudonym was created.");
			}
		}
		
		log.debug("Successfully created the new entity: " + created.getTrustdeckID().toString());
		return responseService.created(responseContentType, created);
	}
	
	/**
	 * Endpoint to retrieve an entity.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param entityTypeName the name of the entity type associated with this entity
	 * @param trustDeckId the unique UUID for this entity
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with the requested entity on success</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project, entity type, or 
     *         entity cannot be found</li>
     *         <li>a <b>410-GONE</b> status when the project has ended, the entity type 
     *         is marked as deprecated, or the entity is marked as deleted</li>
	 */
	@GetMapping("/projects/{projectAbbreviation}/entities/{entityTypeName}/{trustDeckId}")
	@PreAuthorize("isAuthenticated() and @auth.hasEntityTypePermission(#root, #projectAbbreviation, #entityTypeName, 'entity:read')")
	@Audit
	public ResponseEntity<?> getEntity(@PathVariable("projectAbbreviation") String projectAbbreviation,
											   @PathVariable("entityTypeName") String entityTypeName,
											   @PathVariable("trustDeckId") String trustDeckId,
											   @RequestHeader(name = "accept", required = false) String responseContentType) {
		
		// Check if project exists and still active
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("Project \"" + projectAbbreviation + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			// Project end was in the past
			log.debug("The project already ended so that no entity types can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Check if entity type exists and is still active
		EntityTypeDTO entityType = entityTypeDBService.getEntityTypeByName(entityTypeName, project.getId());
		if (entityType == null) {
			log.debug("Entity type \"" + entityTypeName + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (entityType.getIsDeprecated()) {
			log.debug("The entity type is marked as deprecated and cannot be used anymore.");
			return responseService.gone(responseContentType);
		}
		
		// Retrieve entity
		EntityDTO entity = entityDBService.getEntity(trustDeckId, project.getId(), entityType.getId());
		
		// Check result
		if (entity == null) {
			log.debug("Entity with TrustDeckID\"" + trustDeckId + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (entity.getIsDeleted()) {
			log.debug("The entity is marked as deleted and cannot be used anymore.");
			return responseService.gone(responseContentType);
		}
		
		log.debug("Successfully retrieved an entity.");
		return responseService.ok(responseContentType, entity);
	}
	
	/**
	 * Endpoint to update an entity object. Updatable attributes are
	 * data, is_deleted, created_at, and updated_at.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param entityTypeName the name of the entity type associated with this entity
	 * @param trustDeckId the unique UUID for this entity
	 * @param entityDTO the data transfer object containing this entity's data
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with the updated entity on success</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when no updatable fields are 
     *         provided or the payload is invalid/fails schema validation</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project, entity type, or 
     *         target entity cannot be found</li>
     *         <li>a <b>410-GONE</b> status when the project has ended or the entity type 
     *         is marked as deprecated</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the update failed</li>
	 */
	@PutMapping("/projects/{projectAbbreviation}/entities/{entityTypeName}/{trustDeckId}")
	@PreAuthorize("isAuthenticated() and @auth.hasEntityTypePermission(#root, #projectAbbreviation, #entityTypeName, 'entity:update')")
	@Audit
	public ResponseEntity<?> updateEntity(@PathVariable("projectAbbreviation") String projectAbbreviation,
												  @PathVariable("entityTypeName") String entityTypeName,
												  @PathVariable("trustDeckId") String trustDeckId,
												  @RequestBody EntityDTO entityDTO,
												  @RequestHeader(name = "accept", required = false) String responseContentType) {
		
		// Check if project exists and still active
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("Project \"" + projectAbbreviation + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			// Project end was in the past
			log.debug("The project already ended so that no entity types can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Check if entity type exists and is still active
		EntityTypeDTO entityType = entityTypeDBService.getEntityTypeByName(entityTypeName, project.getId());
		if (entityType == null) {
			log.debug("Entity type \"" + entityTypeName + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (entityType.getIsDeprecated()) {
			log.debug("The entity type is marked as deprecated and cannot be used anymore.");
			return responseService.gone(responseContentType);
		}
		
		// Check that the provided DTO in the body is there
	    if (entityDTO == null) {
	        log.debug("No update entity provided.");
	        return responseService.badRequest(responseContentType);
	    }
		
	    // Check that we have something to update
	    if (Assertion.assertNullAll(entityDTO.getData(), entityDTO.getIsDeleted(), entityDTO.getCreatedAt(), entityDTO.getUpdatedAt())) {
	    	log.debug("No updatable values given, nothing to update.");
	    	return responseService.badRequest(responseContentType);
	    }
	    
	    // Validate a possibly updated JSONB data part
	    if (entityDTO.getData() != null) {
	    	// Retrieve the compiled type schema
	    	JsonSchema compiledTypeSchema = jsonSchemaService.getCompiledSchemaFromDefinition(entityType.getTypeDefinition());
	    	
	    	// Validate the new payload data against the type definition
	    	List<String> errors = jsonSchemaService.validateInstance(entityDTO.getData(), compiledTypeSchema);
	
	    	// Check if there are any errors
	        if (!errors.isEmpty()) {
	            log.debug("Entity payload validation failed" + (log.isTraceEnabled() ? ":" : "."));
	            if (log.isTraceEnabled()) {
	            	errors.forEach(e -> log.trace("\t" + e));
	            }
	            return responseService.badRequest(responseContentType);
	        }
	    }
	    
	    // Retrieve the old entity
	    EntityDTO oldEntity = entityDBService.getEntity(trustDeckId, project.getId(), entityType.getId());
	    
	    if (oldEntity == null) {
	    	log.debug("Could not find the entity that should be updated.");
	    	return responseService.notFound(responseContentType);
	    }
	    
	    // Collect attributes
	    EntityDTO newEntity = new EntityDTO();
	    newEntity.setTrustdeckID(oldEntity.getTrustdeckID());
	    newEntity.setProjectID(oldEntity.getProjectID());
	    newEntity.setEntityTypeID(oldEntity.getEntityTypeID());
	    newEntity.setData(entityDTO.getData() != null ? entityDTO.getData() : oldEntity.getData());
	    
		// Update the entity
		EntityDTO updated = entityDBService.updateEntity(oldEntity.getId(), project.getId(), entityType.getId(), newEntity);
		
		// Evaluate the update success
		if (updated == null) {
			log.info("Entity creation failed during database access.");
			return responseService.unprocessableEntity(responseContentType);
		}
		
		log.debug("Successfully updated the entity: " + updated.getTrustdeckID().toString());
		return responseService.ok(responseContentType, updated);
	}
	
	/**
	 * Endpoint to delete an entity. Deletion is performed by tombstoning the entity
	 * through setting the is_deleted-flag accordingly.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param entityTypeName the name of the entity type associated with this entity
	 * @param trustDeckId the unique UUID for this entity
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>204-NO_CONTENT</b> status when the entity was successfully 
	 * 		   tombstoned</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when the TrustDeckID is not a valid 
     *         UUID</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project or entity type cannot 
     *         be found</li>
     *         <li>a <b>410-GONE</b> status when the project has ended or the entity type 
     *         is marked as deprecated</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the deletion failed</li>
	 */
	@DeleteMapping("/projects/{projectAbbreviation}/entities/{entityTypeName}/{trustDeckId}")
	@PreAuthorize("isAuthenticated() and @auth.hasEntityTypePermission(#root, #projectAbbreviation, #entityTypeName, 'entity:delete')")
	@Audit
	public ResponseEntity<?> deleteEntity(@PathVariable("projectAbbreviation") String projectAbbreviation,
												  @PathVariable("entityTypeName") String entityTypeName,
												  @PathVariable("trustDeckId") String trustDeckId,
												  @RequestHeader(name = "accept", required = false) String responseContentType) {
		// Check if project exists and still active
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("Project \"" + projectAbbreviation + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			// Project end was in the past
			log.debug("The project already ended so that no entity types can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Check if entity type exists and is still active
		EntityTypeDTO entityType = entityTypeDBService.getEntityTypeByName(entityTypeName, project.getId());
		if (entityType == null) {
			log.debug("Entity type \"" + entityTypeName + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (entityType.getIsDeprecated()) {
			log.debug("The entity type is marked as deprecated and cannot be used anymore.");
			return responseService.gone(responseContentType);
		}
		
		// Parse trustDeckId into a UUID
		UUID tdid;
		try {
			tdid = UUID.fromString(trustDeckId);
		} catch (IllegalArgumentException e) {
			log.debug("The given TrustDeckID was not a valid UUID.");
			return responseService.badRequest(responseContentType);
		}
		
		// Delete the entity and evaluate the result
		boolean deleted = false;
		try {
			deleted = entityDBService.deleteEntity(tdid, project.getId(), entityType.getId());
		} catch (UnexpectedResultSizeException e) {
			if (e.getActual() == 0) {
				log.debug("Could not find the entity that should be deleted.");
				return responseService.notFound(responseContentType);
			}
		}
		
		// Evaluate deletion result
		if (!deleted) {
			log.debug("Could not delete the requested entity.");
			return responseService.unprocessableEntity(responseContentType);
		}
		
		log.debug("Successfully deleted (tombstoned) the requested entity.");
		return responseService.noContent(responseContentType);
	}
	
	/**
	 * Endpoint to search for entities. Multi-word searches are supported.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param entityTypeName the name of the entity type associated with this entity
	 * @param query the search string that should be looked up 
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with the list of matching entities on 
	 * 		   success</li>
     *         <li>a <b>206-PARTIAL_CONTENT</b> status with a truncated result set when 
     *         more than the maximum number of allowed results are found</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project or entity type cannot 
     *         be found, or when no entities match the query</li>
     *         <li>a <b>410-GONE</b> status when the project has ended or the entity type 
     *         is marked as deprecated</li>
	 */
	@GetMapping(value = "/projects/{projectAbbreviation}/entities/{entityTypeName}", params = {"query"})
	@PreAuthorize("isAuthenticated() and @auth.hasEntityTypePermission(#root, #projectAbbreviation, #entityTypeName, 'entity:search')")
	@Audit
	public ResponseEntity<?> searchEntity(@PathVariable("projectAbbreviation") String projectAbbreviation,
												  @PathVariable("entityTypeName") String entityTypeName,
												  @RequestParam(name = "query", required = true) String query,
												  @RequestHeader(name = "accept", required = false) String responseContentType) {
		
		// Check if project exists and still active
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("Project \"" + projectAbbreviation + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			// Project end was in the past
			log.debug("The project already ended so that no entity types can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Check if entity type exists and is still active
		EntityTypeDTO entityType = entityTypeDBService.getEntityTypeByName(entityTypeName, project.getId());
		if (entityType == null) {
			log.debug("Entity type \"" + entityTypeName + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (entityType.getIsDeprecated()) {
			log.debug("The entity type is marked as deprecated and cannot be used anymore.");
			return responseService.gone(responseContentType);
		}
		
		// Send query to the database
		List<EntityDTO> foundEntities = entityDBService.searchEntity(query, entityType.getId());
		
		// Evaluate findings
		if (foundEntities == null || foundEntities.size() == 0) {
			log.debug("No entities were found for the given search string.");
			return responseService.notFound(responseContentType);
		} else if (foundEntities.size() > MAX_NUMBER_OF_SEARCH_RESULTS) {
			log.debug("Successfully queried the database and found more than " + MAX_NUMBER_OF_SEARCH_RESULTS + " search results, so the result list was truncated.");
			return responseService.partialContent(responseContentType, foundEntities.subList(0, MAX_NUMBER_OF_SEARCH_RESULTS));
		} else {
			log.debug("Successfully queried the database and found " + foundEntities.size() + " search results.");
			return responseService.ok(responseContentType, foundEntities);
		}
	}
	
	/**
	 * Endpoint to retrieve all pseudonyms for an entity.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param entityTypeName the name of the entity type associated with this entity
	 * @param trustDeckId the unique UUID for this entity
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with the list of linked pseudonyms on 
	 * 		   success</li>
     *         <li>a <b>206-PARTIAL_CONTENT</b> status with a truncated result set when 
     *         more than the maximum number of allowed resulting pseudonyms were found</li>
     *         <li>a <b>403-FORBIDDEN</b> status when the rights to read or link 
     *         pseudonyms in any of the involved domains is missing</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project, the entity type, or  
     *         the entity cannot be found</li>
     *         <li>a <b>410-GONE</b> status when the project has ended, the entity type 
     *         is marked as deprecated, or the entity is marked as deleted</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when there was no domain 
     *         associated with the entity type</li>
	 */
	@GetMapping("/projects/{projectAbbreviation}/entities/{entityTypeName}/{trustDeckId}/pseudonyms")
	@PreAuthorize("isAuthenticated() and @auth.hasEntityTypePermission(#root, #projectAbbreviation, #entityTypeName, 'entity:list-pseudonyms')")
	@Audit
	public ResponseEntity<?> getAllPseudonymsForEntity(@PathVariable("projectAbbreviation") String projectAbbreviation,
												  			   @PathVariable("entityTypeName") String entityTypeName,
															   @PathVariable("trustDeckId") String trustDeckId,
												  			   @RequestHeader(name = "accept", required = false) String responseContentType) {
		
		// Check if project exists and still active
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("Project \"" + projectAbbreviation + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			// Project end was in the past
			log.debug("The project already ended so that no entity types can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Check if entity type exists and is still active
		EntityTypeDTO entityType = entityTypeDBService.getEntityTypeByName(entityTypeName, project.getId());
		if (entityType == null) {
			log.debug("Entity type \"" + entityTypeName + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (entityType.getIsDeprecated()) {
			log.debug("The entity type is marked as deprecated and cannot be used anymore.");
			return responseService.gone(responseContentType);
		}
		
		// Check if entity exists and is still active
		EntityDTO entity = entityDBService.getEntity(trustDeckId, project.getId(), entityType.getId());
		if (entity == null) {
			log.debug("Entity with TrustDeckID\"" + trustDeckId + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (entity.getIsDeleted()) {
			log.debug("The entity is marked as deleted and cannot be used anymore.");
			return responseService.gone(responseContentType);
		}
		
		// Retrieve domain
		String domain = entityType.getAssociatedDomainName();
		if (Assertion.isNullOrEmpty(domain)) {
			log.debug("There was no domain associated with the type of the given entity.");
			return responseService.unprocessableEntity(responseContentType);
		}
		
		if (!authorizationService.hasDomainPermission(domain, "pseudonym:read")) {
			log.debug("Read access to the pseudonyms in the associated domain was forbidden.");
			return responseService.forbidden(responseContentType);
		}
		
		// Retrieve direct pseudonyms
		IdentifierItem ii = IdentifierItem.builder().identifier(trustDeckId).idType("TrustDeckID").build();
		List<PseudonymDTO> pseudonyms = pdba.getPseudonymFromIdentifier(domain, ii);
		
		// Also retrieve secondary pseudonyms (pseudonyms of pseudonyms)
		// Start by retrieving the domain subtree for the starting domain
		List<DomainDTO> tree = ddba.getSubtreeFromDomainName(domain);
		
		// For every domain in the subtree, find the pseudonyms linked by psn-id-connection in it
		List<Pair<PseudonymDTO, PseudonymDTO>> linkedPseudonyms = new ArrayList<>();
		for (DomainDTO d : tree) {
			if (d.getName().equalsIgnoreCase(domain)) {
				// Ignore the already processed domain
				continue;
			}
			
			// Check permissions
			if (!authorizationService.hasDomainPermission(domain, "pseudonym:link")
					|| !authorizationService.hasDomainPermission(d.getName(), "pseudonym:read")
					|| !authorizationService.hasDomainPermission(d.getName(), "pseudonym:link")) {
				log.debug("Read or link access to the domains involved in searching linked pseudonyms was forbidden.");
				return responseService.forbidden(responseContentType);
			}
			
			// Get secondary pseudonyms for every pseudonym in the "root"-domain
			for (PseudonymDTO p : pseudonyms) {
				List<Pair<PseudonymDTO, PseudonymDTO>> linked = ddba.getLinkedPseudonyms(domain, p.getIdentifierItem().getIdentifier(),
						p.getIdentifierItem().getIdType(), p.getPsn(), d.getName());
				
				if (linked != null && !linked.isEmpty()) {
					linkedPseudonyms.addAll(linked);
				}
			}
		}
		
		// Add linked pseudonyms to result list
		for (Pair<PseudonymDTO, PseudonymDTO> pair : linkedPseudonyms) {
			// pair.first() is the source-pseudonym and is already part of the pseudonyms-list
			pseudonyms.add(pair.second());
		}
		
		// Ensure that we do not flood the user with too many pseudonyms
		if (pseudonyms.size() > MAX_NUMBER_OF_SEARCH_RESULTS) {
			log.debug("Successfully retrieved more than " + MAX_NUMBER_OF_SEARCH_RESULTS + " pseudonyms "
					+ "for the given entity, so the result list was truncated.");
			return responseService.partialContent(responseContentType, pseudonyms.subList(0, MAX_NUMBER_OF_SEARCH_RESULTS));
		}
		
		log.debug("Successfully retrieved the pseudonyms connected to an entity.");
		return responseService.ok(responseContentType, pseudonyms);
	}
	
	/**
	 * Endpoint to check if there are entities in the database similar/equal to the 
	 * given one. 
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param entityTypeName the name of the entity type associated with the entity to check
	 * @param entityDTO the data transfer object containing the data of the entity that should be checked
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with a list of candidate entities when matches are found</li>
     *         <li>a <b>204-NO_CONTENT</b> status when no record-linkage candidates are found</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when the entity payload is missing/empty or 
     *         none of the required linkage attributes have values or are not defined at all, or 
     *         when payload validation fails</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project or entity type does not exist</li>
     *         <li>a <b>410-GONE</b> status when the project has ended or the entity type is deprecated</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the record-linkage search fails</li>
	 */
	@PostMapping("/projects/{projectAbbreviation}/entities/{entityTypeName}/record-linkage")
	@PreAuthorize("isAuthenticated() and @auth.hasEntityTypePermission(#root, #projectAbbreviation, #entityTypeName, 'entity:record-linkage')")
	@Audit
	public ResponseEntity<?> recordLinkage(@PathVariable("projectAbbreviation") String projectAbbreviation,
			   							   @PathVariable("entityTypeName") String entityTypeName,
			   							   @RequestBody EntityDTO entityDTO,
			   							   @RequestHeader(name = "accept", required = false) String responseContentType) {
		// Check if project exists and still active
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("Project \"" + projectAbbreviation + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			// Project end was in the past
			log.debug("The project already ended so that no entity types can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Check if entity type exists and is still active
		EntityTypeDTO entityType = entityTypeDBService.getEntityTypeByName(entityTypeName, project.getId());
		if (entityType == null) {
			log.debug("Entity type \"" + entityTypeName + "\" was not found.");
			return responseService.notFound(responseContentType);
		} else if (entityType.getIsDeprecated()) {
			log.debug("The entity type is marked as deprecated and cannot be used anymore.");
			return responseService.gone(responseContentType);
		}
		
		// Check if a payload was given
	    if (entityDTO == null || entityDTO.getData() == null) {
	        log.debug("No entity payload or empty data provided for record linkage.");
	        return responseService.badRequest(responseContentType);
	    }
	    
	    // Retrieve or build the compiled type schema
    	JsonSchema compiledTypeSchema = jsonSchemaService.getCompiledSchemaFromDefinition(entityType.getTypeDefinition());
    	
    	// Validate the payload data against the type definition
        	List<String> errors = jsonSchemaService.validateInstance(entityDTO.getData(), compiledTypeSchema);

    	// Check if there are any errors
        if (!errors.isEmpty()) {
            log.debug("Entity payload validation failed.");
            for (String s : errors) {
            	log.trace(s);
            }
            
            return responseService.badRequest(responseContentType);
        }
        
        // Find candidates that (partially) match the given payload data
        // During registration-time linkage, include soft-deleted records so that tombstoned 
        // identities can still be detected as possible duplicates
        List<RecordLinkageCandidateDTO> candidates = recordLinkageService.findCandidates(project.getId(), entityType, 
        		entityDTO.getData(), true);
        
        // Evaluate the candidate search result
        if (candidates == null) {
            log.debug("Record linkage candidate search failed.");
            return responseService.unprocessableEntity(responseContentType);
        } else if (candidates.isEmpty()) {
        	// Nothing found, payload is probably unique
            log.debug("No record linkage candidates were found.");
            return responseService.noContent(responseContentType);
        } else {
            return responseService.ok(responseContentType, candidates);
        }
	}
}
