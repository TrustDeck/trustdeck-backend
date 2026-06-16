/*
 * Trust Deck Services
 * Copyright 2024-2026 Armin Müller and Eric Wündisch
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

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
import org.trustdeck.configuration.RoleConfig;
import org.trustdeck.dto.UserDTO;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.dto.EffectivePermissionDTO;
import org.trustdeck.dto.PermissionDTO;
import org.trustdeck.dto.ProjectDTO;
import org.trustdeck.security.audittrail.annotation.Audit;
import org.trustdeck.service.DomainDBAccessService;
import org.trustdeck.service.KeycloakService;
import org.trustdeck.service.PermissionDBService;
import org.trustdeck.service.ProjectDBService;
import org.trustdeck.service.ResponseService;
import org.trustdeck.utils.Assertion;
import org.trustdeck.utils.Utility.Pair;

/**
 * Provides the REST API for the permission endpoints.
 * 
 * This controller handles requests related to users and permissions.
 *
 * @author Eric Wündisch, Armin Müller
 */
@RestController
@EnableMethodSecurity
@Slf4j
@RequestMapping(value = "/api/permissions")
public class PermissionRESTController {

	/** Enables services for better working with responses. */
	@Autowired
	private ResponseService responseService;

	/** Service that provides the methods for the interaction with Keycloak. */
	@Autowired
	private KeycloakService keycloakService;

	/** Configuration for roles and operations. This is used to validate the operations and permissions. */
	@Autowired
	private RoleConfig roleConfig;

	/** Enables access to the permission grants database methods. */
    @Autowired
    private PermissionDBService permissionDBService;
    
    /** Enables the access to the domain specific database access methods. */
    @Autowired
    private DomainDBAccessService domainDBAccessService;
    
    /** Enables access to the data base interaction methods. */
    @Autowired
    private ProjectDBService projectDBService;

	/** The default number of maximum allowed query results. If a query would result in more records, the surplus is omitted. */
	private static final int DEFAULT_MAX_NUMBER_OF_QUERY_RESULTS = 20;

	/**
	 * Searches for users based on a search term (e.g. username, email, userId).
	 *
	 * @param query the search term
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with the (possibly empty) list of
	 * 		   users matching the query</li>
	 *         <li>a <b>206-PARTIAL_CONTENT</b> status with a truncated list
	 *         when more than the maximum number of results are available</li>
	 *         <li>a <b>400-BAD_REQUEST</b> status when the required 
	 *         <i>query</i> parameter is missing</li>
	 */
	@GetMapping("/users")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'global:manage-permissions')")
    @Audit
    public ResponseEntity<?> searchUsers(@RequestParam(name = "query") String query,
                                         @RequestHeader(name = "accept", required = false) String responseContentType) {

        if (Assertion.isNullOrEmpty(query)) {
        	log.trace("No query string provided.");
            return responseService.badRequest(responseContentType);
        }

        // Search the users in keycloak
        List<UserDTO> users = keycloakService.searchUsers(query, DEFAULT_MAX_NUMBER_OF_QUERY_RESULTS + 1);

        // Add the list of allowed actions for this user
        users.forEach(u -> u.setEffectivePermissions(permissionDBService.getCurrentlyAllowedActionsForSubject(u.getUserId())));
        
        if (users.size() == DEFAULT_MAX_NUMBER_OF_QUERY_RESULTS + 1) {
        	log.debug("The list of found users was longer than the maximum (" + DEFAULT_MAX_NUMBER_OF_QUERY_RESULTS + " and was therefore truncated.");
        	return responseService.partialContent(responseContentType, users.subList(0, DEFAULT_MAX_NUMBER_OF_QUERY_RESULTS));
        } else {
        	log.debug("Successfully found " + users.size() + " user(s) for query: " + query);
        	return responseService.ok(responseContentType, users);
        }
    }
	
	/**
	 * Creates domain-specific permissions for a given user in a domain.
	 * Accepts a list of PermissionDTOs. For creating a single permission, 
	 * send a list with one element.
	 *
	 * @param domainName the name of the domain
	 * @param userId the ID of the user
	 * @param permissions the desired list of permissions
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status when after validation of the permissions, no 
	 * 		   permission remained to be inserted</li>
	 * 		   <li>a <b>201-CREATED</b> status and a list of the created permissions when 
	 * 		   all requested permissions were successfully created</li>
	 * 		   <li>a <b>206-PARTIAL_CONTENT</b> status and a list of the created permissions  
	 * 		   when the requested permissions were partly successfully created, e.g. due to
	 * 		   duplicates or errors on some permissions</li>
	 * 		   <li>a <b>400-BAD_REQUEST</b> status when there were missing parameters
	 * 		   or when none of the permissions could be inserted</li>
	 * 		   <li>a <b>404-NOT_FOUND</b> status when the specified domain was not found</li>
	 */
	@PostMapping("/domains/{domainName}")
	@PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'global:manage-permissions')")
	@Audit
	public ResponseEntity<?> createDomainPermissions(@PathVariable("domainName") String domainName,
	                                                 @RequestParam(name = "userId") String userId,
	                                                 @RequestBody List<PermissionDTO> permissions,
	                                                 @RequestHeader(name = "accept", required = false) String responseContentType) {

		if (Assertion.isNullOrEmpty(domainName, userId) || permissions == null || permissions.isEmpty()) {
			log.debug("Missing parameter (domainName, userId, or list of permissions).");
			return responseService.badRequest(responseContentType);
		}

		// Retrieve the domain from the database
		Domain domain = domainDBAccessService.getDomainByName(domainName);
		if (domain == null) {
			log.debug("No domain found.");
			return responseService.notFound(responseContentType);
		}

	    // Pre-fill output with nulls (preserve input order)
		final int n = permissions.size();
	    List<PermissionDTO> result = new ArrayList<>(n);
	    for (int i = 0; i < n; i++) {
	    	result.add(null);
	    }
		
		// Build a validated list for the batch insert and keep track of the indexes
		List<PermissionDTO> validated = new ArrayList<>();
	    List<Integer> originalIndex = new ArrayList<>();
		
		for (int i = 0; i < n; i++) {
			PermissionDTO p = permissions.get(i);
			
			// Ignore nulls
			if (p == null) {
				log.trace("Permission validation failed because it was null.");
				continue;
			}

			// Ensure that the correct user gets the permissions assigned
			if (!userId.equals(p.getSubjectId())) {
				log.trace("Permission validation failed because permission's subjectId did not match the userId "
						+ "given in the URI (expected = " + userId + ", actual = " + p.getSubjectId() + ").");
				continue;
			}
			
			// Ensure that the permissions are for the correct resource
			if (!"DOMAIN".equalsIgnoreCase(p.getResourceType())) {
				log.trace("Permission validation failed because the resource type was not \"DOMAIN\".");
				continue;
			}
			
			// Add resource ID
			p.setResourceId(domain.getId());
			
			// Ignore empty actions
			if (Assertion.isNullOrEmpty(p.getAction())) {
				log.trace("Permission validation failed because the action was null or empty.");
				continue;
			}
			
			// Ensure that the action is domain-specific
			if (!roleConfig.getACERoles().contains(p.getAction())) {
				if (roleConfig.getAllRoles().contains(p.getAction())) {
					log.trace("Permission validation failed because the action was not domain-specific.");
					continue;
				} else {
					log.trace("Permission validation failed because the action was not recognized.");
					continue;
				}
			}

			validated.add(p);
			originalIndex.add(i);
		}
		
		// Check if there is anything to do
		if (validated.isEmpty()) {
			log.debug("There were no valid permissions to be created.");
			return responseService.badRequest(responseContentType);
		}

		// Batch insert
		List<Pair<PermissionDTO, String>> created = permissionDBService.createPermissionsSecured(validated);
		if (created == null) {
			log.debug("Failed to insert any permissions.");
			return responseService.badRequest(responseContentType);
		}
		
		if (created.isEmpty() || created.size() == 0) {
			log.debug("No actions were added. This is usually because the requester failed bounded delegation checks.");
			return responseService.forbidden(responseContentType);
		}
		
		// Map the list of created permissions back into original order
	    for (int j = 0; j < created.size(); j++) {
	        int idx = originalIndex.get(j);
	        result.set(idx, created.get(j).first());
	    }
	    
	    // Return the results; decide on the proper status code
	    log.debug("Successfully created " + created.stream().filter(e -> e != null && e.first() != null).count() + " domain "
    			+ "permission(s) out of " + created.size() + " requested permissions for user " + userId 
    			+ " in domain " + domain.getName() + ".");

	    boolean hasErrors = created.stream().anyMatch(p -> p != null && PermissionDBService.INSERTION_ERROR.equals(p.second()));
	    boolean hasDuplicates = created.stream().anyMatch(p -> p != null && PermissionDBService.INSERTION_DUPLICATE_PERMISSION.equals(p.second()));
	    
	    if (hasErrors) {
	    	// There were errors --> partial success
	    	return responseService.partialContent(responseContentType, result);
	    } else if (hasDuplicates) {
	    	// There were duplicates --> return code 200
	    	return responseService.ok(responseContentType, result);
	    } else {
	    	// All requested permissions were created
	    	return responseService.created(responseContentType, result);
	    }
	}
	
	/**
	 * Creates project-specific permissions for a given user in a project.
	 * Accepts a list of PermissionDTOs. For creating a single permission, 
	 * send a list with one element.
	 *
	 * @param projectAbbreviation the abbreviation of the project
	 * @param userId the ID of the user
	 * @param permissions the desired list of permissions
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status when after validation of the permissions, no 
	 * 		   permission remained to be inserted</li>
	 * 		   <li>a <b>201-CREATED</b> status and a list of the created permissions when 
	 * 		   all requested permissions were successfully created</li>
	 * 		   <li>a <b>206-PARTIAL_CONTENT</b> status and a list of the created permissions  
	 * 		   when the requested permissions were partly successfully created, e.g. due to
	 * 		   duplicates or errors on some permissions</li>
	 * 		   <li>a <b>400-BAD_REQUEST</b> status when there were missing parameters
	 * 		   or when none of the permissions could be inserted</li>
	 * 		   <li>a <b>404-NOT_FOUND</b> status when the specified project was not found</li>
	 */
	@PostMapping("/projects/{projectAbbreviation}")
	@PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'global:manage-permissions')")
	@Audit
	public ResponseEntity<?> createProjectPermissions(@PathVariable("projectAbbreviation") String projectAbbreviation,
	                                                  @RequestParam(name = "userId") String userId,
	                                                  @RequestBody List<PermissionDTO> permissions,
	                                                  @RequestHeader(name = "accept", required = false) String responseContentType) {

		if (Assertion.isNullOrEmpty(projectAbbreviation, userId) || permissions == null || permissions.isEmpty()) {
			log.debug("Missing parameter (projectAbbreviation, userId, or list of permissions).");
			return responseService.badRequest(responseContentType);
		}

		// Retrieve the project from the database
		ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		if (project == null) {
			log.debug("No project found.");
			return responseService.notFound(responseContentType);
		}

	    // Pre-fill output with nulls (preserve input order)
	    List<PermissionDTO> result = new ArrayList<>();
	    for (int i = 0; i < permissions.size(); i++) {
	    	result.add(null);
	    }
		
		// Build a validated list for the batch insert and keep track of the indexes
		List<PermissionDTO> validated = new ArrayList<>();
	    List<Integer> originalIndex = new ArrayList<>();
		
		for (int i = 0; i < permissions.size(); i++) {
			PermissionDTO p = permissions.get(i);
			
			// Ignore nulls
			if (p == null) {
				log.trace("Permission validation failed because it was null.");
				continue;
			}

			// Ensure that the correct user gets the permissions assigned
			if (!userId.equals(p.getSubjectId())) {
				log.trace("Permission validation failed because permission's subjectId did not match the userId "
						+ "given in the URI (expected = " + userId + ", actual = " + p.getSubjectId() + ").");
				continue;
			}
			
			// Ensure that the permissions are for the correct resource
			if (!"PROJECT".equalsIgnoreCase(p.getResourceType())) {
				log.trace("Permission validation failed because the resource type was not \"PROJECT\".");
				continue;
			}
			
			// Add resource ID
			p.setResourceId(project.getId());
			
			// Ignore empty actions
			if (Assertion.isNullOrEmpty(p.getAction())) {
				log.trace("Permission validation failed because the action was null or empty.");
				continue;
			}
			
			// Ensure that the action is project-specific
			if (!roleConfig.getKINGRoles().contains(p.getAction())) {
				if (roleConfig.getAllRoles().contains(p.getAction())) {
					log.trace("Permission validation failed because the action was not project-specific.");
					continue;
				} else {
					log.trace("Permission validation failed because the action was not recognized.");
					continue;
				}
			}

			validated.add(p);
			originalIndex.add(i);
		}
		
		// Check if there is anything to do
		if (validated.isEmpty()) {
			log.debug("There were no valid permissions to be created.");
			return responseService.badRequest(responseContentType);
		}

		// Batch insert
		List<Pair<PermissionDTO, String>> created = permissionDBService.createPermissionsSecured(validated);
		if (created == null) {
			log.debug("Failed to insert any permissions.");
			return responseService.badRequest(responseContentType);
		}
		
		if (created.isEmpty() || created.size() == 0) {
			log.debug("No actions were added. This is usually because the requester failed bounded delegation checks.");
			return responseService.forbidden(responseContentType);
		}
		
		// Map the list of created permissions back into original order
	    for (int j = 0; j < created.size(); j++) {
	        int idx = originalIndex.get(j);
	        result.set(idx, created.get(j).first());
	    }
	    
	    // Return the results; decide on the proper status code
	    log.debug("Successfully created " + created.stream().filter(e -> e != null && e.first() != null).count() + " project "
    			+ "permission(s) out of " + created.size() + " requested permissions for user " + userId 
    			+ " in project " + project.getAbbreviation() + ".");

	    boolean hasErrors = created.stream().anyMatch(p -> p != null && PermissionDBService.INSERTION_ERROR.equals(p.second()));
	    boolean hasDuplicates = created.stream().anyMatch(p -> p != null && PermissionDBService.INSERTION_DUPLICATE_PERMISSION.equals(p.second()));
	    
	    if (hasErrors) {
	    	// There were errors --> partial success
	    	return responseService.partialContent(responseContentType, result);
	    } else if (hasDuplicates) {
	    	// There were duplicates
	    	return responseService.ok(responseContentType, result);
	    } else {
	    	// All requested permissions were created
	    	return responseService.created(responseContentType, result);
	    }
	}
	
	/**
	 * Creates globally scoped permissions for a given user.
	 * Accepts a list of PermissionDTOs. For creating a single permission, 
	 * send a list with one element.
	 * 
	 * @param userId the ID of the user
	 * @param permissions the desired list of permissions
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>201-CREATED</b> status and a list of the created permissions when 
	 * 		   all requested permissions were successfully created</li>
	 * 		   <li>a <b>206-PARTIAL_CONTENT</b> status and a list of the created permissions  
	 * 		   when the requested permissions were partly successfully created, e.g. due to
	 * 		   duplicates or errors on some permissions</li>
	 * 		   <li>a <b>400-BAD_REQUEST</b> status when there were missing parameters
	 * 		   or when none of the permissions could be inserted</li>
	 */
	@PostMapping("/global")
	@PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'global:manage-permissions')")
	@Audit
	public ResponseEntity<?> createGlobalPermissions(@RequestParam(name = "userId") String userId,
	                                                 @RequestBody List<PermissionDTO> permissions,
	                                                 @RequestHeader(name = "accept", required = false) String responseContentType) {

	    if (Assertion.isNullOrEmpty(userId) || permissions == null || permissions.isEmpty()) {
	        return responseService.badRequest(responseContentType);
	    }

	    // Pre-fill output with nulls (preserve input order)
	    List<PermissionDTO> result = new ArrayList<>();
	    for (int i = 0; i < permissions.size(); i++) {
	    	result.add(null);
	    }

		// Build a validated list for the batch insert and keep track of the indexes
	    List<PermissionDTO> validated = new ArrayList<>();
	    List<Integer> originalIndex = new ArrayList<>();

	    for (int i = 0; i < permissions.size(); i++) {
	        PermissionDTO p = permissions.get(i);
	        
	        // Ignore nulls
	        if (p == null) {
	        	continue;
	        }
	        
	        // Ensure that the correct user gets the permissions assigned
 			if (!userId.equals(p.getSubjectId())) {
 				log.trace("Permission validation failed because permission's subjectId did not match the userId "
 						+ "given in the URI (expected = " + userId + ", actual = " + p.getSubjectId() + ").");
 				continue;
 			}
 			
 			// Ensure that the permissions are for the correct resource
 			if (!"GLOBAL".equalsIgnoreCase(p.getResourceType())) {
 				log.trace("Permission validation failed because the resource type was not \"GLOBAL\".");
 				continue;
 			}
 			
 			// Add resource ID
 			p.setResourceId(0);
 			
 			// Ignore empty actions
 			if (Assertion.isNullOrEmpty(p.getAction())) {
 				log.trace("Permission validation failed because the action was null or empty.");
 				continue;
 			}
 			
 			// Ensure that the action is globally scoped
 			if (!roleConfig.getGlobalRoles().contains(p.getAction())) {
				if (roleConfig.getAllRoles().contains(p.getAction())) {
	 				log.trace("Permission validation failed because the action was not global-scoped.");
	 				continue;
				} else {
					log.trace("Permission validation failed because the action was not recognized.");
					continue;
				}
 			}
	        
 			// Ensure we don't have unnecessary information
	        p.setDomainName(null);
	        p.setProjectAbbreviation(null);

	        validated.add(p);
	        originalIndex.add(i);
	    }

	    // Check if there is anything to do
	    if (validated.isEmpty()) {
			log.debug("There were no valid permissions to be created.");
	        return responseService.badRequest(responseContentType);
	    }

		// Batch insert
	    List<Pair<PermissionDTO, String>> created = permissionDBService.createPermissionsSecured(validated);
	    if (created == null) {
			log.debug("Failed to insert any permissions.");
	        return responseService.badRequest(responseContentType);
	    }
		
		if (created.isEmpty() || created.size() == 0) {
			log.debug("No actions were added. This is usually because the requester failed bounded delegation checks.");
			return responseService.forbidden(responseContentType);
		}

		// Map the list of created permissions back into original order
	    for (int j = 0; j < created.size(); j++) {
	        int idx = originalIndex.get(j);
	        result.set(idx, created.get(j).first());
	    }
	    
	    // Return the results; decide on the proper status code
	    log.debug("Successfully created " + created.stream().filter(e -> e != null && e.first() != null).count() + " global "
    			+ "permission(s) out of " + created.size() + " requested permissions for user " + userId + ".");

	    boolean hasErrors = created.stream().anyMatch(p -> p != null && PermissionDBService.INSERTION_ERROR.equals(p.second()));
	    boolean hasDuplicates = created.stream().anyMatch(p -> p != null && PermissionDBService.INSERTION_DUPLICATE_PERMISSION.equals(p.second()));
	    
	    if (hasErrors) {
	    	// There were errors --> partial success
	    	return responseService.partialContent(responseContentType, result);
	    } else if (hasDuplicates) {
	    	// There were duplicates --> return code 200
	    	return responseService.ok(responseContentType, result);
	    } else {
	    	// All requested permissions were created
	    	return responseService.created(responseContentType, result);
	    }
	}

	/**
	 * Retrieves the permissions of a user for a specific domain.
	 *
	 * @param domainName the name of the domain
	 * @param userId the ID of the user
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status with the list of permissions 
     * 		   for the user in the given domain when successful</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when <i>domainName</i> 
     *         or <i>userId</i> is missing or empty</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the domain or the
     *         permissions for the domain were not found</li>
	 */
    @GetMapping("/domains/{domainName}")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'domain:manage-permissions')")
    @Audit
    public ResponseEntity<?> getDomainPermissions(@PathVariable("domainName") String domainName,
                                                  @RequestParam(name = "userId") String userId,
                                                  @RequestHeader(name = "accept", required = false) String responseContentType) {

        if (Assertion.isNullOrEmpty(domainName, userId)) {
        	log.debug("The domain name or the user ID was empty");
            return responseService.badRequest(responseContentType);
        }

        Domain domain = domainDBAccessService.getDomainByName(domainName);
        if (domain == null) {
        	log.debug("No domain found.");
            return responseService.notFound(responseContentType);
        }

        // Retrieve the permissions of this user
        List<PermissionDTO> permissions = permissionDBService.getPermissionsForDomain(domain.getId(), userId);
        if (permissions == null || permissions.isEmpty()) {
        	log.debug("No permissions found for the given user and domain.");
        	return responseService.notFound(responseContentType);
        }
        
        // Add domainName to non null domains
        permissions.stream().filter(Objects::nonNull).filter(p -> p.getDomainName() == null)
        	.forEach(p -> p.setDomainName(domainName));
        
        log.debug("Successfully retrieved the permissions for user " + userId + " in domain " + domain.getName() + ".");
        return responseService.ok(responseContentType, permissions);
    }

    /**
	 * Retrieves the permissions of a user for a specific project.
	 *
	 * @param projectAbbreviation the abbreviation of the project
	 * @param userId the ID of the user
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status with the list of permissions 
     * 		   for the user in the given project when successful</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when <i>projectAbbreviation</i> 
     *         or <i>userId</i> is missing or empty</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project or the
     *         permissions for the project were not found</li>
	 */
    @GetMapping("/projects/{projectAbbreviation}")
    @PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'project:manage-permissions')")
    @Audit
    public ResponseEntity<?> getProjectPermissions(@PathVariable("projectAbbreviation") String projectAbbreviation,
                                                   @RequestParam(name = "userId") String userId,
                                                   @RequestHeader(name = "accept", required = false) String responseContentType) {

        if (Assertion.isNullOrEmpty(projectAbbreviation, userId)) {
        	log.debug("The project abbreviation or the user ID was empty");
            return responseService.badRequest(responseContentType);
        }

        ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
        if (project == null) {
        	log.debug("No project found.");
            return responseService.notFound(responseContentType);
        }

        // Retrieve the permissions of this user
        List<PermissionDTO> permissions = permissionDBService.getPermissionsForProject(project.getId(), userId);
        if (permissions == null || permissions.isEmpty()) {
        	log.debug("No permissions found for the given user and project.");
        	return responseService.notFound(responseContentType);
        }
        
        // Add projectAbbreviation to non null permissions
        permissions.stream().filter(Objects::nonNull).filter(p -> p.getProjectAbbreviation() == null)
        	.forEach(p -> p.setProjectAbbreviation(projectAbbreviation));
        
        log.debug("Successfully retrieved the permissions for user " + userId + " in project " + project.getAbbreviation() + ".");
        return responseService.ok(responseContentType, permissions);
    }
    
    /**
     * Method for retrieving permissions that are globally scoped.
     * 
     * @param userId the ID of the user
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status with the list of permissions 
     * 		   for the user when successful</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when the <i>userId</i> 
     *         is missing or empty</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the permissions 
     *         for the project were not found</li>
     */
    @GetMapping("/global")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'global:manage-permissions')")
    @Audit
    public ResponseEntity<?> getGlobalPermissions(@RequestParam(name = "userId") String userId,
                                                  @RequestHeader(name = "accept", required = false) String responseContentType) {

        if (Assertion.isNullOrEmpty(userId)) {
            return responseService.badRequest(responseContentType);
        }

        // Retrieve global permissions
        List<PermissionDTO> permissions = permissionDBService.getPermissionsForResource("GLOBAL", 0, userId);
        if (permissions == null || permissions.isEmpty()) {
            return responseService.notFound(responseContentType);
        }

        return responseService.ok(responseContentType, permissions);
    }
    
    /**
     * Method for retrieving all permissions defined in the application.yml.
     * This method is needed by the frontend to discover what roles are currently available.
     * 
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status with the list of permissions 
     * 		   for the user when successful</li>
     */
    @GetMapping
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'global:manage-permissions')")
    @Audit
    public ResponseEntity<?> getAllPermissions(@RequestHeader(name = "accept", required = false) String responseContentType) {
    	List<EffectivePermissionDTO> permissions = new ArrayList<>();
    	
    	// Add domain-specific roles
    	for (String aceRole : roleConfig.getACERoles()) {
    		permissions.add(EffectivePermissionDTO.builder().resourceType("DOMAIN").action(aceRole).build());
    	}
    	
    	// Add project-specific roles
    	for (String kingRole : roleConfig.getKINGRoles()) {
    		permissions.add(EffectivePermissionDTO.builder().resourceType("PROJECT").action(kingRole).build());
    	}
    	
    	// Add global roles
    	for (String globalRole : roleConfig.getGlobalRoles()) {
    		permissions.add(EffectivePermissionDTO.builder().resourceType("GLOBAL").action(globalRole).build());
    	}
    	
        return responseService.ok(responseContentType, permissions);
    }

    /**
	 * Updates the domain-specific permission for a user in a domain.
	 *
	 * @param domainName the name of the domain
	 * @param userId the ID of the user
	 * @param permissions the desired list of permissions
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status when the user's permissions were 
     * 		   successfully synchronized with the provided list</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when a parameter is 
     *         missing or the update failed</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the specified domain does 
     *         not exist or cannot be resolved</li>
	 */
    @PutMapping("/domains/{domainName}")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'domain:manage-permissions')")
    @Audit
    public ResponseEntity<?> updateDomainPermissions(@PathVariable("domainName") String domainName,
                                                     @RequestParam(name = "userId") String userId,
                                                     @RequestBody List<PermissionDTO> permissions,
                                                     @RequestHeader(name = "accept", required = false) String responseContentType) {

        if (Assertion.isNullOrEmpty(domainName, userId) || permissions == null) {
        	log.debug("Missing parameter (domainName, userId, or list of permissions).");
            return responseService.badRequest(responseContentType);
        }

        // Retrieve domain
        Domain domain = domainDBAccessService.getDomainByName(domainName);
        if (domain == null) {
        	log.debug("No domain found.");
            return responseService.notFound(responseContentType);
        }

        // Validate the payload by enforcing correct subject/resource binding
        List<String> validatedActions = new ArrayList<>();
        for (PermissionDTO p : permissions) {
            if (p == null) {
            	log.trace("Permission validation failed because it was null.");
            	continue;
            }
            
            if (Assertion.isNullOrEmpty(p.getDomainName()) || !p.getDomainName().equals(domainName)) {
            	log.trace("Permission validation failed because the domain name was different from the URI-domain name.");
            	continue;
            }

            // Ensure that the correct user gets the permissions assigned
            if (!userId.equals(p.getSubjectId())) {
            	log.trace("Permission validation failed because permission's subjectId did not match the userId "
            			+ "given in the URI (expected = " + userId + ", actual = " + p.getSubjectId() + ").");
            	continue;
            }
            
            // Ensure that the permissions are for the correct resource
            if (!"DOMAIN".equalsIgnoreCase(p.getResourceType())) {
            	log.trace("Permission validation failed because the resource type was not \"DOMAIN\".");
            	continue;
            }
			
			// Add resource ID
			p.setResourceId(domain.getId());

			// Ignore empty actions
			if (Assertion.isNullOrEmpty(p.getAction())) {
				log.trace("Permission validation failed because the action was null or empty.");
				continue;
			}

			// Ensure that the action is domain-specific
			if (!roleConfig.getACERoles().contains(p.getAction())) {
				log.trace("Permission validation failed because the action was not domain-specific.");
				continue;
			}
            
            // At this point we have a valid permission --> add it to the list
            validatedActions.add(p.getAction());
        }

        // Replace the old actions with the set of new actions
        log.trace("Found " + validatedActions.size() + " valid permissions for the replacement process.");
        boolean result = permissionDBService.replacePermissionsForResourceSecured(userId, "DOMAIN", domain.getId(), validatedActions);
        
        if (result) {
        	log.debug("Successfully updated the domain permissions for user " + userId + " in domain " + domain.getName());
        	return responseService.ok(responseContentType);
        } else {
        	log.debug("Failed to update the domain permissions for user " + userId + " in domain " + domain.getName());
        	return responseService.badRequest(responseContentType);
        }
    }

    /**
	 * Updates the project-specific permission for a user in a project.
	 *
	 * @param projectAbbreviation the abbreviation of the project
	 * @param userId the ID of the user
	 * @param permissions the desired list of permissions
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status when the user's permissions were 
     * 		   successfully synchronized with the provided list</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when a parameter is 
     *         missing or the update failed</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the specified project does 
     *         not exist or cannot be resolved</li>
	 */
    @PutMapping("/projects/{projectAbbreviation}")
    @PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'project:manage-permissions')")
    @Audit
    public ResponseEntity<?> updateProjectPermissions(@PathVariable("projectAbbreviation") String projectAbbreviation,
                                                      @RequestParam(name = "userId") String userId,
                                                      @RequestBody List<PermissionDTO> permissions,
                                                      @RequestHeader(name = "accept", required = false) String responseContentType) {

    	if (Assertion.isNullOrEmpty(projectAbbreviation, userId) || permissions == null) {
        	log.debug("Missing parameter (projectAbbreviation, userId, or list of permissions).");
            return responseService.badRequest(responseContentType);
        }

        // Retrieve project
        ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
        if (project == null) {
        	log.debug("No project found.");
            return responseService.notFound(responseContentType);
        }

        // Validate the payload by enforcing correct subject/resource binding
        List<String> validatedActions = new ArrayList<>();
        for (PermissionDTO p : permissions) {
            if (p == null) {
            	log.trace("Permission validation failed because it was null.");
            	continue;
            }
            
            if (Assertion.isNullOrEmpty(p.getProjectAbbreviation()) || !p.getProjectAbbreviation().equals(projectAbbreviation)) {
            	log.trace("Permission validation failed because the project abbreviation was different from the URI-project abbreviation.");
            	continue;
            }

            // Ensure that the correct user gets the permissions assigned
            if (!userId.equals(p.getSubjectId())) {
            	log.trace("Permission validation failed because permission's subjectId did not match the userId "
            			+ "given in the URI (expected = " + userId + ", actual = " + p.getSubjectId() + ").");
            	continue;
            }

            // Ensure that the permissions are for the correct resource
            if (!"PROJECT".equalsIgnoreCase(p.getResourceType())) {
            	log.trace("Permission validation failed because the resource type was not \"PROJECT\".");
            	continue;
            }
			
			// Add resource ID
			p.setResourceId(project.getId());

			// Ignore empty actions
			if (Assertion.isNullOrEmpty(p.getAction())) {
				log.trace("Permission validation failed because the action was null or empty.");
				continue;
			}

			// Ensure that the action is project-specific
			if (!roleConfig.getKINGRoles().contains(p.getAction())) {
				log.trace("Permission validation failed because the action was not project-specific.");
				continue;
			}
            
            // At this point we have a valid permission --> add it to the list
            validatedActions.add(p.getAction());
        }

        // Replace the old actions with the set of new actions
        log.trace("Found " + validatedActions.size() + " valid permissions for the replacement process.");
        boolean result = permissionDBService.replacePermissionsForResourceSecured(userId, "PROJECT", project.getId(), validatedActions);
        
        if (result) {
        	log.debug("Successfully updated the project permissions for user " + userId + " in project " + project.getAbbreviation());
        	return responseService.ok(responseContentType);
        } else {
        	log.debug("Failed to update the project permissions for user " + userId + " in project " + project.getAbbreviation());
        	return responseService.badRequest(responseContentType);
        }
    }
    
    /**
     * Updates the global permission for a user.
     * 
	 * @param userId the ID of the user
	 * @param permissions the desired list of permissions
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status when the user's permissions were 
     * 		   successfully synchronized with the provided list</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when a parameter is 
     *         missing or the update failed</li>
     */
    @PutMapping("/global")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'global:manage-permissions')")
    @Audit
    public ResponseEntity<?> updateGlobalPermissions(@RequestParam(name = "userId") String userId,
                                                    @RequestBody List<PermissionDTO> permissions,
                                                    @RequestHeader(name = "accept", required = false) String responseContentType) {

        if (Assertion.isNullOrEmpty(userId) || permissions == null) {
            return responseService.badRequest(responseContentType);
        }

        // Validate the payload by enforcing correct subject/resource binding
        List<String> validatedActions = new ArrayList<>();
        for (PermissionDTO p : permissions) {
            if (p == null) {
            	log.trace("Permission validation failed because it was null.");
            	continue;
            }

	        // Ensure that the correct user is affected
 			if (!userId.equals(p.getSubjectId())) {
 				log.trace("Permission validation failed because permission's subjectId did not match the userId "
 						+ "given in the URI (expected = " + userId + ", actual = " + p.getSubjectId() + ").");
 				continue;
 			}
 			
 			// Ensure that the permissions are for the correct resource
 			if (!"GLOBAL".equalsIgnoreCase(p.getResourceType())) {
 				log.trace("Permission validation failed because the resource type was not \"GLOBAL\".");
 				continue;
 			}
 			
 			// Add resource ID
 			p.setResourceId(0);
 			
 			// Ignore empty actions
 			if (Assertion.isNullOrEmpty(p.getAction())) {
 				log.trace("Permission validation failed because the action was null or empty.");
 				continue;
 			}
 			
 			// Ensure that the action is globally scoped
 			if (!roleConfig.getGlobalRoles().contains(p.getAction())) {
 				log.trace("Permission validation failed because the action was not global-scoped.");
 				continue;
 			}
	        
 			// Ensure we don't have unnecessary information
	        p.setDomainName(null);
	        p.setProjectAbbreviation(null);

            validatedActions.add(p.getAction());
        }

        // Replace the old actions with the set of new actions
        log.trace("Found " + validatedActions.size() + " valid permissions for the replacement process.");
        boolean result = permissionDBService.replacePermissionsForResourceSecured(userId, "GLOBAL", 0, validatedActions);
        
        if (result) {
        	log.debug("Successfully updated the global permissions for user " + userId + ".");
        	return responseService.ok(responseContentType);
        } else {
        	log.debug("Failed to update the global permissions for user " + userId + ".");
        	return responseService.badRequest(responseContentType);
        }
    }
    
    /**
	 * Deletes the domain-specific permission for a user in a domain.
     * Accepts a list of PermissionDTOs. For deleting a single permission,
     * send a list with one element.
	 *
	 * @param domainName the name of the domain
	 * @param userId the ID of the user
	 * @param permissions the desired list of permissions
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>204-NO_CONTENT</b> status when the user's  
     * 		   permissions were successfully deleted</li>
     * 		   <li>a <b>206-PARTIAL_CONTENT</b> status with results if
     * 		   some deletions failed or were skipped </li>
     *         <li>a <b>400-BAD_REQUEST</b> status when a parameter is 
     *         missing or the deletion failed</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the specified domain does 
     *         not exist or cannot be resolved</li>
	 */
    @DeleteMapping("/domains/{domainName}")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'domain:manage-permissions')")
    @Audit
    public ResponseEntity<?> deleteDomainPermissions(@PathVariable("domainName") String domainName,
                                    				 @RequestParam(name = "userId") String userId,
                                                     @RequestBody List<PermissionDTO> permissions,
                                                     @RequestHeader(name = "accept", required = false) String responseContentType) {

        if (Assertion.isNullOrEmpty(domainName, userId) || permissions == null || permissions.isEmpty()) {
            log.debug("Missing parameter (domainName, userId, or list of permissions).");
            return responseService.badRequest(responseContentType);
        }

        // Retrieve domain
        Domain domain = domainDBAccessService.getDomainByName(domainName);
        if (domain == null) {
            log.debug("No domain found.");
            return responseService.notFound(responseContentType);
        }

        // Preserve input order in the response
        List<Boolean> result = new ArrayList<>();
        for (int i = 0; i < permissions.size(); i++) {
            result.add(null);
        }

        // Build sanitized list
        List<PermissionDTO> validated = new ArrayList<>();
        List<Integer> originalIndex = new ArrayList<>();

        for (int i = 0; i < permissions.size(); i++) {
            PermissionDTO p = permissions.get(i);

            if (p == null) {
            	log.trace("Permission validation failed because it was null.");
                continue;
            }
            
            if (Assertion.isNullOrEmpty(p.getDomainName()) || !p.getDomainName().equals(domainName)) {
            	log.trace("Permission validation failed because the domain name was different from the URI-domain name.");
            	continue;
            }

            // Ensure that the correct user is affected
            if (!userId.equals(p.getSubjectId())) {
            	log.trace("Permission validation failed because permission's subjectId did not match the userId "
            			+ "given in the URI (expected = " + userId + ", actual = " + p.getSubjectId() + ").");
            	continue;
            }

            // Ensure that the permissions are for the correct resource
            if (!"DOMAIN".equalsIgnoreCase(p.getResourceType())) {
            	log.trace("Permission validation failed because the resource type was not \"DOMAIN\".");
            	continue;
            }
            
            // Add resource ID
 			p.setResourceId(domain.getId());

	 		// Ignore empty actions
	 		if (Assertion.isNullOrEmpty(p.getAction())) {
	 			log.trace("Permission validation failed because the action was null or empty.");
	 			continue;
	 		}

			// Ensure that the action is domain-specific
			if (!roleConfig.getACERoles().contains(p.getAction())) {
				log.trace("Permission validation failed because the action was not domain-specific.");
				continue;
			}

            // At this point we have a valid permission --> add it to the list
            validated.add(p);
            originalIndex.add(i);
        }

		// Check if we there is anything to do
        if (validated.isEmpty()) {
            log.debug("There were no valid permissions to be deleted.");
            return responseService.ok(responseContentType, result);
        }

        // Delete permissions
        List<Boolean> deleted = permissionDBService.deletePermissionsSecured(validated);
        if (deleted == null) {
            log.debug("Deletion of " + validated.size() + " permissions failed.");
            return responseService.badRequest(responseContentType);
        }
		
		if (deleted.isEmpty() || deleted.size() == 0) {
			log.debug("No actions were deleted. This is usually because the requester failed bounded delegation checks.");
			return responseService.forbidden(responseContentType);
		}

        // Map the list of deleted permissions back into original order
        for (int j = 0; j < deleted.size(); j++) {
            int idx = originalIndex.get(j);
            result.set(idx, deleted.get(j));
        }

        // Return the results; decide on the proper status code
	    log.debug("Successfully deleted " + deleted.stream().filter(e -> e.equals(true)).count() + " domain "
    			+ "permission(s) out of " + deleted.size() + " requested permissions for user " + userId 
    			+ " in domain " + domain.getName() + ".");
	    if (deleted.contains(false)) {
	    	// There were errors --> partial success
	    	return responseService.partialContent(responseContentType, result);
	    } else {
	    	// All requested permissions were created
	    	return responseService.noContent(responseContentType);
	    }
    }
    
    /**
	 * Deletes the project-specific permission for a user in a project.
     * Accepts a list of PermissionDTOs. For deleting a single permission,
     * send a list with one element.
	 *
	 * @param projectAbbreviation the abbreviation of the project
	 * @param userId the ID of the user
	 * @param permissions the desired list of permissions
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>204-NO_CONTENT</b> status when the user's  
     * 		   permissions were successfully deleted</li>
     * 		   <li>a <b>206-PARTIAL_CONTENT</b> status with results if
     * 		   some deletions failed or were skipped </li>
     *         <li>a <b>400-BAD_REQUEST</b> status when a parameter is 
     *         missing or the deletion failed</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the specified project  
     *         does not exist or cannot be resolved</li>
	 */
    @DeleteMapping("/projects/{projectAbbreviation}")
    @PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'project:manage-permissions'))")
    @Audit
    public ResponseEntity<?> deleteProjectPermissions(@PathVariable("projectAbbreviation") String projectAbbreviation,
    												  @RequestParam(name = "userId") String userId,
                                                      @RequestBody List<PermissionDTO> permissions,
                                                      @RequestHeader(name = "accept", required = false) String responseContentType) {

        if (Assertion.isNullOrEmpty(projectAbbreviation, userId) || permissions == null || permissions.isEmpty()) {
            log.debug("Missing parameter (projectAbbreviation, userId, or list of permissions).");
            return responseService.badRequest(responseContentType);
        }

        // Retrieve project
        ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
        if (project == null) {
            log.debug("No project found.");
            return responseService.notFound(responseContentType);
        }

        // Preserve input order in the response
        List<Boolean> result = new ArrayList<>();
        for (int i = 0; i < permissions.size(); i++) {
            result.add(null);
        }

        // Build sanitized list
        List<PermissionDTO> validated = new ArrayList<>();
        List<Integer> originalIndex = new ArrayList<>();

        for (int i = 0; i < permissions.size(); i++) {
            PermissionDTO p = permissions.get(i);

            if (p == null) {
            	log.trace("Permission validation failed because it was null.");
                continue;
            }
            
            if (Assertion.isNullOrEmpty(p.getProjectAbbreviation()) || !p.getProjectAbbreviation().equals(projectAbbreviation)) {
            	log.trace("Permission validation failed because the project abbreviation was different from the URI-project abbreviation.");
            	continue;
            }

            // Ensure that the correct user is affected
            if (!userId.equals(p.getSubjectId())) {
            	log.trace("Permission validation failed because permission's subjectId did not match the userId "
            			+ "given in the URI (expected = " + userId + ", actual = " + p.getSubjectId() + ").");
            	continue;
            }

            // Ensure that the permissions are for the correct resource
            if (!"PROJECT".equalsIgnoreCase(p.getResourceType())) {
            	log.trace("Permission validation failed because the resource type was not \"PROJECT\".");
            	continue;
            }
            
            // Add resource ID
 			p.setResourceId(project.getId());

	 		// Ignore empty actions
	 		if (Assertion.isNullOrEmpty(p.getAction())) {
	 			log.trace("Permission validation failed because the action was null or empty.");
	 			continue;
	 		}

			// Ensure that the action is project-specific
			if (!roleConfig.getKINGRoles().contains(p.getAction())) {
				log.trace("Permission validation failed because the action was not project-specific.");
				continue;
			}

            // At this point we have a valid permission --> add it to the list
            validated.add(p);
            originalIndex.add(i);
        }

		// Check if we there is anything to do
        if (validated.isEmpty()) {
            log.debug("There were no valid permissions to be deleted.");
            return responseService.ok(responseContentType, result);
        }

        // Delete permissions
        List<Boolean> deleted = permissionDBService.deletePermissionsSecured(validated);
        if (deleted == null) {
            log.debug("Deletion of " + validated.size() + " permissions failed.");
            return responseService.badRequest(responseContentType);
        }
		
		if (deleted.isEmpty() || deleted.size() == 0) {
			log.debug("No actions were deleted. This is usually because the requester failed bounded delegation checks.");
			return responseService.forbidden(responseContentType);
		}

        // Map the list of deleted permissions back into original order
        for (int j = 0; j < deleted.size(); j++) {
            int idx = originalIndex.get(j);
            result.set(idx, deleted.get(j));
        }

        // Return the results; decide on the proper status code
	    log.debug("Successfully deleted " + deleted.stream().filter(e -> e.equals(true)).count() + " project "
    			+ "permission(s) out of " + deleted.size() + " requested permissions for user " + userId 
    			+ " in project " + project.getAbbreviation() + ".");
	    if (deleted.contains(false)) {
	    	// There were errors --> partial success
	    	return responseService.partialContent(responseContentType, result);
	    } else {
	    	// All requested permissions were created
	    	return responseService.noContent(responseContentType);
	    }
    }
    
    /**
	 * Deletes global permissions for a user.
     * Accepts a list of PermissionDTOs. For deleting a single permission,
     * send a list with one element.
	 *
	 * @param userId the ID of the user
	 * @param permissions the desired list of permissions
	 * @param responseContentType (optional) the response content type
     * @return <li>a <b>204-NO_CONTENT</b> status when the user's  
     * 		   permissions were successfully deleted</li>
     * 		   <li>a <b>206-PARTIAL_CONTENT</b> status with results if
     * 		   some deletions failed or were skipped </li>
     *         <li>a <b>400-BAD_REQUEST</b> status when a parameter is 
     *         missing or the deletion failed</li>
	 */
    @DeleteMapping("/global")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'global:manage-permissions')")
    @Audit
    public ResponseEntity<?> deleteGlobalPermissions(@RequestParam(name = "userId") String userId,
                                                     @RequestBody List<PermissionDTO> permissions,
                                                     @RequestHeader(name = "accept", required = false) String responseContentType) {

        if (Assertion.isNullOrEmpty(userId) || permissions == null || permissions.isEmpty()) {
            return responseService.badRequest(responseContentType);
        }

        // Preserve input order in the response
        List<Boolean> result = new ArrayList<>();
        for (int i = 0; i < permissions.size(); i++) {
        	result.add(null);
        }

        // Build sanitized list
        List<PermissionDTO> validated = new ArrayList<>();
        List<Integer> originalIndex = new ArrayList<>();

        for (int i = 0; i < permissions.size(); i++) {
            PermissionDTO p = permissions.get(i);
            
            if (p == null) {
            	log.trace("Permission validation failed because it was null.");
                continue;
            }
            
            // Ensure that the correct user is affected
 			if (!userId.equals(p.getSubjectId())) {
 				log.trace("Permission validation failed because permission's subjectId did not match the userId "
 						+ "given in the URI (expected = " + userId + ", actual = " + p.getSubjectId() + ").");
 				continue;
 			}
 			
 			// Ensure that the permissions are for the correct resource
 			if (!"GLOBAL".equalsIgnoreCase(p.getResourceType())) {
 				log.trace("Permission validation failed because the resource type was not \"GLOBAL\".");
 				continue;
 			}
 			
 			// Add resource ID
 			p.setResourceId(0);
 			
 			// Ignore empty actions
 			if (Assertion.isNullOrEmpty(p.getAction())) {
 				log.trace("Permission validation failed because the action was null or empty.");
 				continue;
 			}
 			
 			// Ensure that the action is globally scoped
 			if (!roleConfig.getGlobalRoles().contains(p.getAction())) {
 				log.trace("Permission validation failed because the action was not global-scoped.");
 				continue;
 			}
	        
 			// Ensure we don't have unnecessary information
	        p.setDomainName(null);
	        p.setProjectAbbreviation(null);

            validated.add(p);
            originalIndex.add(i);
        }

	    // Check if there is anything to do
        if (validated.isEmpty()) {
			log.debug("There were no valid permissions to be created.");
            return responseService.badRequest(responseContentType);
        }

        // Batch delete
        List<Boolean> deleted = permissionDBService.deletePermissionsSecured(validated);
        if (deleted == null) {
			log.debug("Failed to delete any permissions.");
            return responseService.badRequest(responseContentType);
        }
		
		if (deleted.isEmpty() || deleted.size() == 0) {
			log.debug("No actions were deleted. This is usually because the requester failed bounded delegation checks.");
			return responseService.forbidden(responseContentType);
		}

        // Map the list of deleted permissions back into original order
        for (int j = 0; j < deleted.size(); j++) {
            int idx = originalIndex.get(j);
            result.set(idx, deleted.get(j));
        }
        
        // Return the results; decide on the proper status code
	    log.debug("Successfully deleted " + deleted.stream().filter(e -> e.equals(true)).count()
    			+ " permission(s) out of " + deleted.size() + " requested permissions for user " + userId + ".");
	    if (deleted.contains(false)) {
	    	// There were errors --> partial success
	    	return responseService.partialContent(responseContentType, result);
	    } else {
	    	// All requested permissions were created
	    	return responseService.noContent(responseContentType);
	    }
    }
}
