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

package org.trustdeck.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.trustdeck.dto.ProjectDTO;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.utils.Assertion;

/**
 * This class encapsulates utility functionalities to check roles and relationships within a given OIDC token.
 *
 * @author Eric Wündisch and Armin Müller
 */
@Slf4j
@Component("auth")
public class AuthorizationService {

    /** Enables access to project database methods. */
    @Autowired
    private ProjectDBService projectDBService;

    @Autowired
    private EntityTypeDBService entityTypeDBService;
    
    /** Enables access to domain database methods. */
    @Autowired
    private DomainDBAccessService domainDBService;
    
    /** Enables access to the permission grants database methods. */
    @Autowired
    private PermissionDBService permissionDBService;

    /**
     * Method that checks whether the user is allowed to perform the requested action on the given domain.
     *
     * @param root method-level security control context object that includes information about the authenticated user
     * @param domainName the domain name as a string
     * @param action the action that is to be performed as a string
     * @return {@code true} only if the given action is permitted on the given domain for the requesting user, {@code false} if not
     */
    public boolean hasDomainPermission(MethodSecurityExpressionOperations root, String domainName, String action) {
        if (!Assertion.isNotNullOrEmpty(domainName, action)) {
			return false;
		}
        
        Authentication authentication = root.getAuthentication();
        if (authentication == null) {
        	return false;
        }
        
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof Jwt jwt)) {
			return false;
		}
        
        // Get database ID for the domain 
		Domain d = domainDBService.getDomainByName(domainName);
        Integer id = d == null ? null : d.getId();
        if (id == null || id <= 0) {
        	log.trace("Could not retrieve the domain's ID.");
        	return false;
        }
        
        return permissionDBService.isActionAllowed(jwt.getSubject(), "DOMAIN", id, action);
    }

    /**
     * Method that checks whether the user is allowed to perform the requested action on the given domain.
     * Here, the subject information will be extracted from Spring's SecurityContext.
     *
     * @param domainName the domain name as a string
     * @param action the action that is to be performed as a string
     * @return {@code true} only if the given action is permitted on the given domain for the requesting user, {@code false} if not
     */
    public boolean hasDomainPermission(String domainName, String action) {
        if (!Assertion.isNotNullOrEmpty(domainName, action)) {
			return false;
		}
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
        	return false;
        }
        
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof Jwt jwt)) {
			return false;
		}
        
        // Get database ID for the domain
		Domain d = domainDBService.getDomainByName(domainName);
        Integer id = d == null ? null : d.getId();
        if (id == null || id <= 0) {
        	log.trace("Could not retrieve the domain's ID.");
        	return false;
        }
        
        return permissionDBService.isActionAllowed(jwt.getSubject(), "DOMAIN", id, action);
    }

    /**
     * Method that checks whether the user is allowed to perform the requested action on the given project.
     *
     * @param root method-level security control context object that includes information about the authenticated user
     * @param projectAbbreviation the project abbreviation as a string
     * @param action the action that is to be performed as a string
     * @return {@code true} only if the given action is permitted on the given project for the requesting user, {@code false} if not
     */
    public boolean hasProjectPermission(MethodSecurityExpressionOperations root, String projectAbbreviation, String action) {
    	if (!Assertion.isNotNullOrEmpty(projectAbbreviation, action)) {
			return false;
		}
        
        Authentication authentication = root.getAuthentication();
        if (authentication == null) {
        	return false;
        }
        
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof Jwt jwt)) {
			return false;
		}
        
        // Get database ID for the project
		ProjectDTO p = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		Integer id = p == null ? null : p.getId();
        if (id == null || id <= 0) {
        	log.trace("Could not retrieve the project's ID.");
        	return false;
        }
        
        return permissionDBService.isActionAllowed(jwt.getSubject(), "PROJECT", id, action);
    }

    /**
     * Method that checks whether the user is allowed to perform the requested action on the given project.
     * Here, the subject information will be extracted from Spring's SecurityContext.
     *
     * @param projectAbbreviation the project abbreviation as a string
     * @param action the action that is to be performed as a string
     * @return {@code true} only if the given action is permitted on the given project for the requesting user, {@code false} if not
     */
    public boolean hasProjectPermission(String projectAbbreviation, String action) {
    	if (!Assertion.isNotNullOrEmpty(projectAbbreviation, action)) {
			return false;
		}
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
        	return false;
        }
        
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof Jwt jwt)) {
			return false;
		}
        
        // Get database ID for the project
		ProjectDTO p = projectDBService.getProjectByAbbreviation(projectAbbreviation);
		Integer id = p == null ? null : p.getId();
        if (id == null || id <= 0) {
        	log.trace("Could not retrieve the project's ID.");
        	return false;
        }
        
        return permissionDBService.isActionAllowed(jwt.getSubject(), "PROJECT", id, action);
    }

    public boolean hasEntityTypePermission(MethodSecurityExpressionOperations root, String projectAbbreviation, String entityTypeName, String action) {
        if (!Assertion.isNotNullOrEmpty(projectAbbreviation, entityTypeName, action) || root.getAuthentication() == null || !(root.getAuthentication().getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        ProjectDTO project = projectDBService.getProjectByAbbreviation(projectAbbreviation);
        EntityTypeDTO type = project == null ? null : entityTypeDBService.getEntityTypeByName(entityTypeName, project.getId());
        return type != null && permissionDBService.isActionAllowed(jwt.getSubject(), "ENTITY_TYPE", type.getId(), action);
    }

    /**
     * Method that checks whether the user is allowed to perform the requested action.
     * This is to check an action which is globally scoped and not tied to any specific object (e.g. project:create).
     *
     * @param root method-level security control context object that includes information about the authenticated user
     * @param action the action that is to be performed as a string
     * @return {@code true} only if the given action is permitted for the requesting user, {@code false} if not
     */
    public boolean hasGlobalPermission(MethodSecurityExpressionOperations root, String action) {
    	if (!Assertion.isNotNullOrEmpty(action)) {
			return false;
		}
        
        Authentication authentication = root.getAuthentication();
        if (authentication == null) {
        	return false;
        }
        
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof Jwt jwt)) {
			return false;
		}
        
        return permissionDBService.isActionAllowed(jwt.getSubject(), "GLOBAL", 0, action);
    }

    /**
     * Method that checks whether the user is allowed to perform the requested action.
     * This is to check an action which is globally scoped and not tied to any specific object (e.g. project:create).
     * Here, the subject information will be extracted from Spring's SecurityContext.
     * 
     * @param action the action that is to be performed as a string
     * @return {@code true} only if the given action is permitted for the requesting user, {@code false} if not
     */
    public boolean hasGlobalPermission(String action) {
    	if (!Assertion.isNotNullOrEmpty(action)) {
			return false;
		}
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
        	return false;
        }
        
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof Jwt jwt)) {
			return false;
		}
        
        return permissionDBService.isActionAllowed(jwt.getSubject(), "GLOBAL", 0, action);
    }
}
