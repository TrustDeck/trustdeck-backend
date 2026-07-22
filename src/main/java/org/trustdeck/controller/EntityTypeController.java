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
import java.util.List;
import java.util.regex.Pattern;

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
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.dto.ProjectDTO;
import org.trustdeck.exception.DuplicateEntityTypeException;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.security.audittrail.annotation.Audit;
import org.trustdeck.service.DomainDBAccessService;
import org.trustdeck.service.EntityTypeDBService;
import org.trustdeck.service.JsonSchemaService;
import org.trustdeck.service.ProjectDBService;
import org.trustdeck.service.ResponseService;
import org.trustdeck.utils.Assertion;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

/**
 * This class offers a REST API for interacting with project entity types.
 *
 * @author Armin Müller
 */
@RestController
@EnableMethodSecurity
@Slf4j
@RequestMapping(value = "/api")
public class EntityTypeController {

	/** Enables service for working with predefined responses. */
    @Autowired
    private ResponseService responseService;
    
    /** Enables access to the data base interaction methods. */
    @Autowired
    private EntityTypeDBService entityTypeDBService;
    
    /** Enables access to the data base interaction methods for project objects. */
    @Autowired
    private ProjectDBService projectDBService;
	
	/** Enables access to domain database functions. */
	@Autowired
	private DomainDBAccessService ddba;
	
	/** Enables access to the JSON schema validation functionalities. */
	@Autowired
	private JsonSchemaService jsonSchemaService;

    /** Pattern/Regex of allowed characters for the name attribute of entity types. */
    private static final Pattern VALID_NAME_CHAR_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    
    /** Pattern/Regex of allowed characters for the version attribute of entity types. */
    private static final Pattern VALID_VERSION_CHAR_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");
	
    /**
     * Endpoint to create base entity types in the system. These will be used
     * as blueprints for creating other entity types, which are only allowed 
     * to extend an already existing base entity type.
     * 
     * @param entityTypeDTO the DTO containing the type definition and some meta data
     * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status with the base entity type when it was already existing</li>
     * 		   <li>a <b>201-CREATED</b> status with the created base entity type on success</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when the name/version is 
     *         invalid or the type definition fails validation</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when creation failed</li>
     */
	@PostMapping("/entities/base-types")
	@PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'base-type:create')")
	@Audit
	public ResponseEntity<?> createBaseEntityType(@RequestBody EntityTypeDTO entityTypeDTO,
											  	  @RequestHeader(name = "accept", required = false) String responseContentType) {

		// Collect DTO values
		String name = entityTypeDTO.getName();
		String version = entityTypeDTO.getVersion();
		boolean isDeprecated = false;
		boolean isBaseType = true;
		JsonNode typeDefintion = entityTypeDTO.getTypeDefinition();
		String baseTypeName = null;
		Integer baseTypeId = null;
		String associatedDomainName = null;
		String projectName = null;
		Integer projectID = null;
		
		// Sanitize the DTO values
		// Name
		if (Assertion.isNullOrEmpty(name)) {
			log.debug("No name was given for the type.");
			return responseService.badRequest(responseContentType);
		}
		
		if (!VALID_NAME_CHAR_PATTERN.matcher(name).matches()) {
    		log.debug("Invalid entity type name. Must only contain letters, digits, underscores, or hyphens.");
    	    return responseService.badRequest(responseContentType);
    	}
		
		// Version
		if (Assertion.isNullOrEmpty(version)) {
			log.debug("No version was given. Assuming \"v1.0\".");
			version = "v1.0";
		}
		
		if (!VALID_VERSION_CHAR_PATTERN.matcher(version.trim()).matches()) {
			log.debug("Invalid version name.");
			return responseService.badRequest(responseContentType);
		}
		
		// Type definition
		if (!isTypeDefinitionValid(typeDefintion)) {
			log.debug("Invalid type definition.");
			return responseService.badRequest(responseContentType);
		}

		// Assemble the create-DTO
		EntityTypeDTO createDTO = new EntityTypeDTO();
		createDTO.setName(name);
		createDTO.setVersion(version);
		createDTO.setIsDeprecated(isDeprecated);
		createDTO.setIsBaseType(isBaseType);
		createDTO.setTypeDefinition(typeDefintion);
		createDTO.setBaseTypeName(baseTypeName);
		createDTO.setBaseTypeId(baseTypeId);
		createDTO.setAssociatedDomainName(associatedDomainName);
		createDTO.setProjectName(projectName);
		createDTO.setProjectId(projectID);
		
		// Create type
		EntityTypeDTO createdType;
		try {
			createdType = entityTypeDBService.createEntityType(createDTO);
		} catch (DuplicateEntityTypeException e) {
			log.info("While creating an entity type, an identical one was found and will be used instead.");
			return responseService.ok(responseContentType, entityTypeDBService.getEntityTypeByName(name, projectID));
		}
		
		// Evaluate success
		if (createdType == null) {
			log.debug("Creating a new base type failed.");
			return responseService.unprocessableEntity(responseContentType);
		}
		
		log.info("Successfully created a new base entity type.");
		return responseService.created(responseContentType, createdType);
	}

	/**
	 * Endpoint to create a new type in the system.
	 * This needs to be an extension from the base type that is 
	 * mentioned in the given DTO.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param entityTypeDTO the DTO containing the type definition and some meta data
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>201-CREATED</b> status with the created project-specific entity type on success</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when required fields are missing, name/version is 
     *         invalid</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project or referenced base type does not exist</li>
     *         <li>a <b>410-GONE</b> status when the project has already ended/is marked as deleted</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when creation failed or the type definition 
     *         is invalid/not a valid extension of the base type</li>
	 */
	@PostMapping("/projects/{projectAbbreviation}/entities/config")
	@PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'type:create')")
	@Audit
	public ResponseEntity<?> createProjectEntityType(@PathVariable("projectAbbreviation") String projectAbbreviation,
												 	 @RequestBody EntityTypeDTO entityTypeDTO,
												 	 @RequestHeader(name = "accept", required = false) String responseContentType) {
		// Check if project exists and still active
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("Project \"" + projectAbbreviation + "\" was not found.");
			return responseService.notFound(responseContentType);
		}
		
		if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			// Project end was in the past
			log.debug("The project already ended so that no entity types can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Collect attributes
		String typeName = entityTypeDTO.getName();
		String version = entityTypeDTO.getVersion();
		boolean isDeprecated = entityTypeDTO.getIsDeprecated() == null ? false : entityTypeDTO.getIsDeprecated();
		boolean isBaseType = false;
		JsonNode projectEntityTypeDef = entityTypeDTO.getTypeDefinition();
		String baseTypeName = entityTypeDTO.getBaseTypeName();
		String associatedDomainName = entityTypeDTO.getAssociatedDomainName();

		// Check for null values
		if (Assertion.isNullOrEmpty(typeName, baseTypeName) || projectEntityTypeDef == null) {
			log.debug("Missing type name, base type name, or type definition.");
			return responseService.badRequest(responseContentType);
		}
		
		// Sanitize name and version
		if (!VALID_NAME_CHAR_PATTERN.matcher(typeName).matches()) {
    		log.debug("Invalid entity type name. Must only contain letters, digits, underscores, or hyphens.");
    	    return responseService.badRequest(responseContentType);
    	}
		
		if (Assertion.isNullOrEmpty(version)) {
			log.debug("No version was given. Assuming \"v1.0\".");
			version = "v1.0";
		}
		
		if (!VALID_VERSION_CHAR_PATTERN.matcher(version).matches()) {
    		log.debug("Invalid version name.");
    	    return responseService.badRequest(responseContentType);
    	}

		// Retrieve base definition
		EntityTypeDTO baseType = entityTypeDBService.getEntityTypeByName(baseTypeName, null);
		if (baseType == null) {
			log.debug("Could not find base type.");
			return responseService.notFound(responseContentType);
		}
		
		if (!baseType.getIsBaseType()) {
			log.debug("Cannot extend from \"" + baseType.getName() + "\", as it is not a base type.");
			return responseService.badRequest(projectAbbreviation);
		}

		// Validate the definition against the meta-schema
		if (!isTypeDefinitionValid(projectEntityTypeDef)) {
			log.debug("Validation of the project specific type definition failed.");
			return responseService.badRequest(responseContentType);
		}

		// Ensure project type definition is an extension of the base type definition
		List<String> extensionErrors = jsonSchemaService.validateProjectTypeIsSuperset(baseType.getTypeDefinition(), projectEntityTypeDef);
		if (!extensionErrors.isEmpty()) {
			log.debug("The project specific type definition is not a valid extension/superset of the base type.");
			log.trace("Encountered errors:");
			for (String s : extensionErrors) {
				log.trace(s);
			}
			
			return responseService.unprocessableEntity(responseContentType);
		}

		// Build DTO
		EntityTypeDTO createDTO = new EntityTypeDTO();
		createDTO.setName(typeName);
		createDTO.setVersion(version);
		createDTO.setIsDeprecated(isDeprecated);
		createDTO.setIsBaseType(isBaseType);
		createDTO.setTypeDefinition(projectEntityTypeDef);
		createDTO.setBaseTypeName(baseType.getName());
		createDTO.setBaseTypeId(baseType.getId());
		createDTO.setAssociatedDomainName(associatedDomainName == null ? null : ddba.getDomainByName(associatedDomainName).getName());
		createDTO.setProjectName(project.getName());
		createDTO.setProjectId(project.getId());
		
		// Create type
		EntityTypeDTO createdType;
		try {
			createdType = entityTypeDBService.createEntityType(createDTO);
		} catch (DuplicateEntityTypeException e) {
			log.info("While creating an entity type, an identical one was found and will be used instead.");
			return responseService.ok(responseContentType, entityTypeDBService.getEntityTypeByName(typeName, project.getId()));
		}
		
		// Evaluate success
		if (createdType == null) {
			log.debug("Creating a new base type failed.");
			responseService.unprocessableEntity(responseContentType);
		}
		
		log.info("Successfully created a new project specific entity type.");
		return responseService.created(responseContentType, createdType);
	}
	
	/**
	 * Endpoint to retrieve a base entity type given it's name.
	 * 
	 * @param entityTypeName the name of the base entity type the user wants to retrieve
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with the requested entity type on success</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project or the entity type does not exist</li>
     *         <li>a <b>410-GONE</b> status when the project has already ended/is marked as deleted</li>
	 */
	@GetMapping("/entities/base-types/{entityTypeName}")
	@PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'base-type:read')")
	@Audit
	public ResponseEntity<?> getBaseEntityType(@PathVariable("entityTypeName") String entityTypeName,
											   @RequestHeader(name = "accept", required = false) String responseContentType) {
		
		// Retrieve base type from the database
		EntityTypeDTO type = entityTypeDBService.getEntityTypeByName(entityTypeName, null);
		
		// Evaluate result
		if (type == null) {
			log.debug("No entity type with the name \"" + entityTypeName + "\" was found.");
			return responseService.notFound(responseContentType);
		}
		
		if (type.getIsDeprecated()) {
			log.debug("The base entity type with the name \"" + entityTypeName + "\" is marked as deprecated.");
			return responseService.gone(responseContentType);
		}
		
		log.debug("Successfully retrieved the requested entity type.");
		return responseService.ok(responseContentType, type);
	}
	
	/**
	 * Endpoint to retrieve an entity type given it's name.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param entityTypeName the name of the entity type the user wants to retrieve
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with the requested entity type on success</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project or the entity type does not exist</li>
     *         <li>a <b>410-GONE</b> status when the project has already ended/is marked as deleted</li>
	 */
	@GetMapping("/projects/{projectAbbreviation}/entities/config/{entityTypeName}")
	@PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'type:read')")
	@Audit
	public ResponseEntity<?> getProjectEntityType(@PathVariable("projectAbbreviation") String projectAbbreviation,
											  	  @PathVariable("entityTypeName") String entityTypeName,
											  	  @RequestHeader(name = "accept", required = false) String responseContentType) {
		
		// Check if project exists and still active
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("Project \"" + projectAbbreviation + "\" was not found.");
			return responseService.notFound(responseContentType);
		}
		
		if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			// Project end was in the past
			log.debug("The project already ended so that no entity types can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Retrieve type from the database
		EntityTypeDTO type = entityTypeDBService.getEntityTypeByName(entityTypeName, project.getId());
		
		// Evaluate result
		if (type == null) {
			log.debug("No entity type with the name \"" + entityTypeName + "\" was found.");
			return responseService.notFound(responseContentType);
		}
		
		log.debug("Successfully retrieved the requested entity type.");
		return responseService.ok(responseContentType, type);
	}
	
	/**
	 * Endpoint to update entity types. Updates are allowed on empty types only.
	 * Updatable attributes are: name, version, type definition, and the 
	 * associated domain. Null-values in the given update DTO indicate that 
	 * the old value will be kept.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param oldEntityTypeName the name of the entity type the user wants to update
	 * @param newEntityTypeDTO the DTO containing the updated information
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with the updated entity type on success</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when no updatable attributes are provided, 
     *         name/version is invalid</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project, the old entity type, or 
     *         the base type cannot be found</li>
     *         <li>a <b>410-GONE</b> status when the project has already ended/is marked as 
     *         deleted</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the update failed or the 
     *         updated type definition is invalid/not a valid extension of the base type</li>
	 */
	@PutMapping("/projects/{projectAbbreviation}/entities/config/{entityTypeName}")
	@PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'type:update')")
	@Audit
	public ResponseEntity<?> updateEntityType(@PathVariable("projectAbbreviation") String projectAbbreviation,
											  @PathVariable("entityTypeName") String oldEntityTypeName,
											  @RequestBody EntityTypeDTO newEntityTypeDTO,
											  @RequestHeader(name = "accept", required = false) String responseContentType) {
		// Check if project exists and still active
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("Project \"" + projectAbbreviation + "\" was not found.");
			return responseService.notFound(responseContentType);
		}
		
		if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			// Project end was in the past
			log.debug("The project already ended so that no entity types can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Collect attributes
		String newTypeName = newEntityTypeDTO.getName();
		String newVersion = newEntityTypeDTO.getVersion();
		// is_deprecated is not updatable --> use delete endpoint for this
		// is_base_type is not updatable
		JsonNode newEntityTypeDef = newEntityTypeDTO.getTypeDefinition();
		// base_type_id is not updatable
		String newAssociatedDomainName = newEntityTypeDTO.getAssociatedDomainName();
		// project_id is not updatable
		
		// Check if anything was given to update
		if (Assertion.assertNullAll(newTypeName, newVersion, newEntityTypeDef, newAssociatedDomainName)) {
			log.debug("No attributes to update were provided.");
			return responseService.badRequest(responseContentType);
		}

		// Retrieve old entity type
		EntityTypeDTO oldType = entityTypeDBService.getEntityTypeByName(oldEntityTypeName, project.getId());
		if (oldType == null) {
			log.debug("Old entity type was not found.");
			return responseService.notFound(responseContentType);
		}
		
		// Sanitize new name
		if (newTypeName != null && !VALID_NAME_CHAR_PATTERN.matcher(newTypeName).matches()) {
    		log.debug("Invalid entity type name. Must only contain letters, digits, underscores, or hyphens. Using old one.");
    	    newTypeName = oldType.getName();
    	}
		
		// Update the version
		if (Assertion.isNotNullOrEmpty(newVersion)) {
			if (!VALID_VERSION_CHAR_PATTERN.matcher(newVersion).matches()) {
				log.debug("Invalid version name. Using an updated version of the old one.");
			    newVersion = oldType.getVersion() + " (" + OffsetDateTime.now().toString() + ")";
			}
    	} else {
    		log.debug("No new version name was given. Using an updated version of the old one.");
		    newVersion = oldType.getVersion() + " (" + OffsetDateTime.now().toString() + ")";
    	}
		
		// Retrieve project
		ProjectDTO projectDTO = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (projectDTO == null) {
			log.debug("Could not find the given project.");
			return responseService.badRequest(responseContentType);
		}
		
		// Check if the type definition should be updated
		if (newEntityTypeDef != null) {
			// Retrieve base type definition
			EntityTypeDTO baseType = entityTypeDBService.getEntityTypeByName(oldType.getBaseTypeName(), null);
			if (baseType == null) {
				log.debug("Could not find base type.");
				return responseService.notFound(responseContentType);
			}
			
			if (!baseType.getIsBaseType()) {
				log.debug("Cannot extend from \"" + baseType.getName() + "\", as it is not a base type.");
				return responseService.badRequest(responseContentType);
			}
	
			// Validate the new definition against the meta-schema
			if (!isTypeDefinitionValid(newEntityTypeDef)) {
				log.debug("Validation of the updated type definition failed.");
				return responseService.unprocessableEntity(responseContentType);
			}
	
			// Ensure new type definition is a superset of the base type definition
			List<String> extensionErrors = jsonSchemaService.validateProjectTypeIsSuperset(baseType.getTypeDefinition(), newEntityTypeDef);
			if (!extensionErrors.isEmpty()) {
				log.debug("The updated type definition is not a valid extension of the base type.");
				log.trace("Encountered errors:");
				for (String s : extensionErrors) {
					log.trace(s);
				}
				
				return responseService.unprocessableEntity(responseContentType);
			}
		}
		
		// Update associated domain
		String newDomainName = null;
		if (Assertion.isNotNullOrEmpty(newAssociatedDomainName)) {
			Domain d = ddba.getDomainByName(newAssociatedDomainName);
			newDomainName = d == null ? null : d.getName();
		}

		// Build DTO
		EntityTypeDTO updateDTO = new EntityTypeDTO();
		updateDTO.setName(newTypeName);
		updateDTO.setVersion(newVersion);
		updateDTO.setIsDeprecated(null);
		updateDTO.setIsBaseType(null);
		updateDTO.setTypeDefinition(newEntityTypeDef);
		updateDTO.setBaseTypeName(null);
		updateDTO.setBaseTypeId(null);
		updateDTO.setAssociatedDomainName(newDomainName);
		updateDTO.setProjectName(null);
		updateDTO.setProjectId(null);
		
		// Update type
		EntityTypeDTO updatedType = entityTypeDBService.updateEntityType(oldType, updateDTO);
		
		// Evaluate success
		if (updatedType == null) {
			log.debug("Updating an entity type failed.");
			return responseService.unprocessableEntity(responseContentType);
		}
		
		log.info("Successfully updated entity type \"" + updatedType.getName() + "\".");
		return responseService.ok(responseContentType, updatedType);
	}
	
	/**
	 * Endpoint to delete entity types with. The deletion is done by tombstoning
	 * (setting the is_deprecated flag) rather than actually removing the entry.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param entityTypeName the name of the entity type the user wants to delete
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>204-NO_CONTENT</b> status when the entity type was successfully tombstoned</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project or the entity type does not exist</li>
     *         <li>a <b>410-GONE</b> status when the project has already ended/is marked as deleted</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the deletion could not be performed</li>
	 */
	@DeleteMapping("/projects/{projectAbbreviation}/entities/config/{entityTypeName}")
	@PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'type:delete')")
	@Audit
	public ResponseEntity<?> deleteProjectEntityType(@PathVariable("projectAbbreviation") String projectAbbreviation,
												 	 @PathVariable("entityTypeName") String entityTypeName,
												 	 @RequestHeader(name = "accept", required = false) String responseContentType) {
		// Check if project exists and still active
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("Project \"" + projectAbbreviation + "\" was not found.");
			return responseService.notFound(responseContentType);
		}
		
		if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			// Project end was in the past
			log.debug("The project already ended so that no entity types can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Check if the entity type can be found at all
		EntityTypeDTO deleteDTO = entityTypeDBService.getEntityTypeByName(entityTypeName, project.getId());
		if (deleteDTO == null) {
			log.debug("Entity type \"" + entityTypeName + "\" was not found.");
			return responseService.notFound(responseContentType);
		}
		
		// Delete the entity type by setting the is_deprecated flag
		if (entityTypeDBService.deleteEntityType(deleteDTO)) {
			log.info("Successfully deleted the entity type.");
			log.info("Successfully deleted entity type \"" + deleteDTO.getName() + "\".");
			return responseService.noContent(responseContentType);
		} else {
			log.debug("Could not delete the entity type.");
			return responseService.unprocessableEntity(responseContentType);
		}
	}
	
	/**
	 * Endpoint to search for base entity types.
	 * The search supports multi-word search and searches in the type's
	 * name, version, and type definition.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param query the search string that should be used to find entity types
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with the list of matching entity types on success</li>
     *         <li>a <b>404-NOT_FOUND</b> status when no entity types match the query or the project does not exist</li>
     *         <li>a <b>410-GONE</b> status when the project has already ended/is marked as deleted</li>
	 */
	@GetMapping("/entities/base-types")
	@PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'base-type:search')")
	@Audit
	public ResponseEntity<?> searchBaseEntityType(@RequestParam(name = "query", required = true) String query,
												  @RequestHeader(name = "accept", required = false) String responseContentType) {
		
		// Retrieve base types from the database
		List<EntityTypeDTO> types = entityTypeDBService.searchEntityType(query, null);
		
		// Evaluate result
		if (types == null || types.size() == 0) {
			log.debug("No entity types for the given query string were found.");
			return responseService.ok(responseContentType, List.of());
		}
		
		log.debug("Successfully found " + types.size() + " entity types.");
		return responseService.ok(responseContentType, types);
	}
	
	/**
	 * Endpoint to search for entity types in a project.
	 * The search supports multi-word search and searches in the type's
	 * name, version, and type definition.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the request is scoped to
	 * @param query the search string that should be used to find entity types
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with the list of matching entity types on success</li>
     *         <li>a <b>404-NOT_FOUND</b> status when no entity types match the query or the project does not exist</li>
     *         <li>a <b>410-GONE</b> status when the project has already ended/is marked as deleted</li>
	 */
	@GetMapping("/projects/{projectAbbreviation}/entities")
	@PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'type:search')")
	@Audit
	public ResponseEntity<?> searchProjectEntityType(@PathVariable("projectAbbreviation") String projectAbbreviation,
													 @RequestParam(name = "query", required = true) String query,
													 @RequestHeader(name = "accept", required = false) String responseContentType) {
		
		// Check if project exists and still active
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("Project \"" + projectAbbreviation + "\" was not found.");
			return responseService.notFound(responseContentType);
		}
		
		if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			// Project end was in the past
			log.debug("The project already ended so that no entity types can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Retrieve types from the database
		List<EntityTypeDTO> types = entityTypeDBService.searchEntityType(query, project.getId());
		
		// Evaluate result
		if (types == null || types.size() == 0) {
			log.debug("No entity types for the given query string were found.");
			return responseService.ok(responseContentType, List.of());
		}
		
		log.debug("Successfully found " + types.size() + " entity types.");
		return responseService.ok(responseContentType, types);
	}
	
	/**
	 * Helper method that checks if a given definition has a valid format.
	 * 
	 * @param typeDefinition the definition to check as a JsonNode
	 * @return {@code true} if the validation was successful and no errors were encountered, {@code false} otherwise
	 */
	private boolean isTypeDefinitionValid(JsonNode typeDefinition) {
		List<String> defErrors = jsonSchemaService.validateDefinition(typeDefinition);
		
		if (!defErrors.isEmpty()) {
			log.trace("Encountered errors during type validation:");
			for (String s : defErrors) {
				log.trace(s);
			}
			
			return false;
		} else {
			return true;
		}
	}
}
