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

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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
import org.trustdeck.dto.PermissionDTO;
import org.trustdeck.dto.ProjectDTO;
import org.trustdeck.exception.DuplicateProjectException;
import org.trustdeck.exception.UnexpectedResultSizeException;
import org.trustdeck.security.audittrail.annotation.Audit;
import org.trustdeck.service.PermissionDBService;
import org.trustdeck.service.ProjectDBService;
import org.trustdeck.service.ResponseService;
import org.trustdeck.utils.Assertion;
import org.trustdeck.utils.Utility;

import lombok.extern.slf4j.Slf4j;

/**
 * This class offers REST API endpoints for interacting with project entities.
 *
 * @author Armin Müller
 */
@RestController
@EnableMethodSecurity
@Slf4j
@RequestMapping(value = "/api")
public class ProjectController {
	
	/** Enables service for working with predefined responses. */
    @Autowired
    private ResponseService responseService;
    
    /** Enables access to the data base interaction methods. */
    @Autowired
    private ProjectDBService projectDBService;
    
    /** Enables access to the permission grants database methods. */
    @Autowired
    private PermissionDBService permissionDBService;
    
    /** Default value for the project's validity time. */
    private static final Period DEFAULT_PROJECT_VALIDITY_TIME = Period.ofYears(10);
    
    /** Default value for the flag whether or not this project stores entities. */
    private static final boolean DEFAULT_STORE_ENTITIES = true;
    
    /** Default value for the flag whether or not this project stores pseudonyms. */
    private static final boolean DEFAULT_STORE_PSEUDONYMS = true;
    
    /** Pattern/Regex of allowed characters for the abbreviation attribute of projects. */
    private static final Pattern VALID_ABBREVIATION_CHAR_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

	/**
	 * Method to create a new project.
	 * 
	 * @param projectDTO (required) the project object
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status with the existing project when a duplicate was detected</li>
     *         <li>a <b>201-CREATED</b> status with the created project and a <b>Location</b> header on success</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when the name/abbreviation is missing or dates are invalid</li>
     *         <li>a <b>406-NOT_ACCEPTABLE</b> status when the abbreviation contains disallowed characters</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when creation failed</li>
	 */
	@PostMapping("/projects")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'project:create')")
    @Audit
    public ResponseEntity<?> createProject(@RequestBody ProjectDTO projectDTO,
                                           @RequestHeader(name = "accept", required = false) String responseContentType) {
		ProjectDTO p = new ProjectDTO();
		
		// Sanitize all the necessary attributes are given and store them in p
		String name = projectDTO.getName().trim();
		String abbr = projectDTO.getAbbreviation().trim();
		OffsetDateTime start = projectDTO.getStartDate();
		OffsetDateTime end = projectDTO.getEndDate();
		Boolean storeEntities = projectDTO.getStoreEntities();
		Boolean storePseudonyms = projectDTO.getStorePseudonyms();
		String desc = projectDTO.getDescription();
		
		// Check if name and abbreviation are given
		if (Assertion.isNullOrEmpty(name) || Assertion.isNullOrEmpty(abbr)) {
			log.debug("Creating a new project failed due to a missing name or abbreviation.");
			return responseService.badRequest(responseContentType);
		}
		
    	// Ensure that only whitelisted characters are in the abbreviation, so that they do not break endpoints when used in an URI
    	if (!VALID_ABBREVIATION_CHAR_PATTERN.matcher(abbr).matches()) {
    		log.debug("Invalid project abbreviation. Must only contain letters, digits, underscores, or hyphens.");
    	    return responseService.badRequest(responseContentType);
    	}
        	
        URI location = URI.create("/api/projects/" + abbr);
		
		p.setName(name);
		p.setAbbreviation(abbr);
		
		// Handle start and end of project
		start = start != null ? start : OffsetDateTime.now();
		end = end != null ? end : start.plus(DEFAULT_PROJECT_VALIDITY_TIME);
		
		if (end.isBefore(start)) {
			log.debug("Creating a new project failed due to an invalid start and/or end date of the project.");
			log.trace("Start-time: " + start.toString() + ", end-time: " + end.toString());
			return responseService.badRequest(responseContentType);
		}
		
		p.setStartDate(start);
		p.setEndDate(end);
		
		// Handle store entities / pseudonyms flags
		storeEntities = storeEntities != null ? storeEntities : DEFAULT_STORE_ENTITIES;
		storePseudonyms = storePseudonyms != null ? storePseudonyms : DEFAULT_STORE_PSEUDONYMS;
		
		p.setStoreEntities(storeEntities);
		p.setStorePseudonyms(storePseudonyms);
		
		p.setDescription(desc);
		
		// Create the project in the database
		ProjectDTO createdProject;
		try {
			createdProject = projectDBService.createProject(p);
		} catch (DuplicateProjectException e) {
			log.debug("Encountered a project duplicate. Return the original.");
			return responseService.ok(responseContentType, projectDBService.getProjectByName(name));
		}
		
		// Evaluate creation result
		if (createdProject == null) {
			log.info("Creation of a new project failed.");
			return responseService.unprocessableEntity(responseContentType);
		}

		// If we reach this point, creation was successful --> Return a 201-CREATED status
		return responseService.created(responseContentType, location, createdProject);
	}

	/**
	 * Method to retrieve a list of all projects where the 
	 * requesting user has access to.
	 * 
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status and a list of projects 
     * 		   where the user has access to</li>
	 */
	@GetMapping("/projects")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'project:list-all')")
    @Audit
    public ResponseEntity<?> getProjectsWithAccessTo(@RequestHeader(name = "accept", required = false) String responseContentType) {
		// Get subject/user from security context
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
        	log.debug("Could not retrieve the authentication from the security context.");
        	return responseService.unprocessableEntity(responseContentType);
        }
        
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof Jwt jwt)) {
			log.debug("The authentication principal was not a JWT.");
        	return responseService.unprocessableEntity(responseContentType);
		}
        
		// Get all permissions of the current subject/user
		List<PermissionDTO> permissions = permissionDBService.getAllPermissionsForSubject(jwt.getSubject());
		
		// Collect all projects with read-access into a list
		Set<ProjectDTO> projects = new HashSet<>();
		for (PermissionDTO p : permissions) {
			if (p.getResourceType().equalsIgnoreCase("PROJECT") && p.getAction().equalsIgnoreCase("project:read")
					&& p.getDecision().equalsIgnoreCase("ALLOW") && p.getValidFrom().isBefore(OffsetDateTime.now())
					&& p.getValidTo().isAfter(OffsetDateTime.now())) {
				projects.add(projectDBService.getProjectByID(p.getResourceId()));
			}
		}
        
        log.debug("Succesfully retrieved a list of " + projects.size() + (projects.size() == 1 ? " project" : " projects") + " where the requesting user has (read) access to.");
        return responseService.ok(responseContentType, projects);
	}
	
	/**
	 * Method to retrieve a certain project identified by its
	 * abbreviation.
	 * 
	 * @param projectAbbreviation the project's abbreviation
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status with the requested project on success</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when the abbreviation is missing or empty</li>
     *         <li>a <b>404-NOT_FOUND</b> status when no project exists for the given abbreviation</li>
	 */
	@GetMapping("/projects/{projectAbbreviation}")
    @PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'project:read')")
    @Audit
    public ResponseEntity<?> getProject(@PathVariable(name = "projectAbbreviation", required = true) String projectAbbreviation,
                                        @RequestHeader(name = "accept", required = false) String responseContentType) {
		
		// Null-check the given string
		if (Assertion.isNullOrEmpty(projectAbbreviation)) {
			log.debug("No project abbreviation was given.");
			return responseService.badRequest(responseContentType);
		}
		
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		
		if (project == null) {
			log.debug("No project found for the given abbreviation.");
			return responseService.notFound(responseContentType);
		} else if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			log.debug("Project has already ended. No changes allowed.");
			return responseService.gone(responseContentType);
		}
		
		// At this point a project was found, return it to the user
		log.debug("Successfully retrieved a project for abbreviation \"" + projectAbbreviation + "\".");
		return responseService.ok(responseContentType, project);
	}
	
	/**
	 * Method to retrieve statistics about a certain project
	 * identified by it's abbreviation.
	 * 
	 * @param projectAbbreviation the project's abbreviation
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>501-NOT_IMPLEMENTED</b> status (endpoint not implemented yet)</li>
	 */
	@GetMapping("projects/{projectAbbreviation}/statistics")
    @PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'project:statistics')")
    @Audit
    public ResponseEntity<?> getProjectStatistics(@PathVariable("projectAbbreviation") String projectAbbreviation,
                                				  @RequestHeader(name = "accept", required = false) String responseContentType) {
		
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
	}

	/**
	 * Method to update a project identity identified by it's abbreviation.
	 * 
	 * @param projectAbbreviation the abbreviation of the project that is to be updated
	 * @param newProjectDTO (required) the project object containing all updated values, null-values will lead to keeping the old values
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status with the updated project on success</li>
     * 		   <li>a <b>404-NOT_FOUND</b> status when the project does not exist</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the update failed</li>
	 */
	@PutMapping("/projects/{projectAbbreviation}")
    @PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'project:update')")
    @Audit
    public ResponseEntity<?> updateProject(@PathVariable("projectAbbreviation") String projectAbbreviation,
    									   @RequestBody ProjectDTO newProjectDTO,
    									   @RequestHeader(name = "accept", required = false) String responseContentType) {
		
		// Check if the original project exists
		ProjectDTO oldProjectDTO = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (oldProjectDTO == null) {
			log.debug("The project that should be updated was not found.");
			return responseService.notFound(responseContentType);
		} else if (oldProjectDTO.getEndDate().isBefore(OffsetDateTime.now())) {
			log.debug("Project has already ended. No updates allowed.");
			return responseService.gone(responseContentType);
		}
		
		// Sanitize the new abbreviation and name
		String abbr = Assertion.isNullOrEmpty(newProjectDTO.getAbbreviation()) ? null : newProjectDTO.getAbbreviation();
		String name = Assertion.isNullOrEmpty(newProjectDTO.getName()) ? null : newProjectDTO.getName();
		newProjectDTO.setAbbreviation(abbr);
		newProjectDTO.setName(name);
		
		// Send the DTO to the database service
		ProjectDTO updatedDTO = projectDBService.updateProject(oldProjectDTO, newProjectDTO);
		
		// Check success of the update
		if (updatedDTO == null) {
			log.debug("Updating the project data failed.");
			return responseService.unprocessableEntity(responseContentType);
		}
		
		// At this point the update was successful, return the updated project object (when still not "expired"
		updatedDTO = updatedDTO.getEndDate().isBefore(OffsetDateTime.now()) ? null : updatedDTO;
		return responseService.ok(responseContentType, updatedDTO);
	}
	
	/**
	 * Method to delete a project identified by it's abbreviation.
	 * The project will not be removed from the database but rather
	 * tombstoned, meaning the end-date will be set properly so that
	 * retrieving, updating, inserting, etc. is not allowed anymore.
	 * 
	 * @param projectAbbreviation the project's abbreviation
	 * @param deleteDate (optional) the date from which the project should be considered as deleted 
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>204-NO_CONTENT</b> status when the project was successfully deleted/tombstoned</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when the abbreviation is missing or empty</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project does not exist</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the deletion failed</li>
	 */
	@DeleteMapping("/projects/{projectAbbreviation}")
    @PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'project:delete')")
    @Audit
    public ResponseEntity<?> deleteProject(@PathVariable("projectAbbreviation") String projectAbbreviation,
    									   @RequestParam(name = "deleteDate", required = false) String deleteDate,
    									   @RequestHeader(name = "accept", required = false) String responseContentType) {
		// Check if the original project exists
		ProjectDTO projectDTO = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (projectDTO == null) {
			log.debug("The project that should be deleted was not found.");
			return responseService.notFound(responseContentType);
		} else if (projectDTO.getEndDate().isBefore(OffsetDateTime.now())) {
			log.debug("Project has already ended. Deletion is not allowed.");
			return responseService.gone(responseContentType);
		}
		
		// Check if an end date was given
		OffsetDateTime end;
		if (deleteDate == null) {
			log.debug("No end date was given. Using the current time instead.");
			end = OffsetDateTime.now();
		} else {
			// An end date was given --> parse it
			OffsetDateTime parsed = Utility.parseDateTimeString(deleteDate);
			if (parsed == null) {
				log.debug("Parsing the given deletion time failed. Using the current time instead.");
				end = OffsetDateTime.now();
			} else {
				end = parsed;
			}
		}
		
		// Sanitize the date that should be used for deletion
		if (end.isAfter(OffsetDateTime.now())) {
			log.debug("The delete date cannot be in the future and was therefore set to now.");
			end = OffsetDateTime.now();
		}
		
		// Delete the project
		boolean isDeleted;
		try {
			isDeleted = projectDBService.deleteProject(projectDTO, end);
		} catch (UnexpectedResultSizeException e) {
			log.debug("Deletion attempt failed and was rolled back.", e);
			return responseService.unprocessableEntity(responseContentType);
		}
		
		// Evaluate the result
		if (isDeleted) {
			log.info("Project with abbreviation \"" + projectAbbreviation + "\" was succesfully deleted (tombstoned).");
			return responseService.noContent(responseContentType);
		} else {
			log.debug("Project deletion was unsuccesful.");
			return responseService.unprocessableEntity(responseContentType);
		}
	}
}
