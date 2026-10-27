/*
 * Trust Deck Services
 * Copyright 2024-2026 Armin Müller and contributors
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

import java.util.Set;

import jakarta.ws.rs.NotFoundException;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trustdeck.security.audittrail.annotation.Audit;
import org.trustdeck.service.CachingService;
import org.trustdeck.service.PermissionDBService;
import org.trustdeck.service.ResponseService;
import lombok.extern.slf4j.Slf4j;

/**
 * This class provides database maintenance access.
 *
 * @author Armin Müller
 */
@RestController
@EnableMethodSecurity
@Slf4j
@RequestMapping(value = "/api")
public class DatabaseMaintenanceController {

    /** References a jOOQ configuration object that configures jOOQ's behavior when executing queries. */
    @Autowired
    private DSLContext dsl;

    /** Enables services for better working with responses. */
    @Autowired
    private ResponseService responseService;

    /** Enables access to the permission grants database methods. */
    @Autowired
    private PermissionDBService permissionDBService;
    
    /** Cache for the permission look-ups. */
    @Autowired
    private CachingService cachingService;

    /** 
     * The tables that may be truncated. The audit trail is not included, so that it can't be 
     * removed through the API.
     */
    private static final Set<String> TRUNCATABLE_TABLES = Set.of("algorithm", "domain", "entity", "entity_type", 
    		"linkage_token", "permission_grant", "project", "project_image", "pseudonym");

    /**
     * Endpoint to truncate a table in the database.
     * Restarts identity counters. Might fail, when foreign keys exist.
     * Only the tables holding TrustDeck's data can be truncated; afterwards, the permission caches are cleared.
     * Access to this method should be highly restricted.
     * 
     * @param tableName (required) the name of the table the user wants to truncate
     * @return <li>a <b>200-OK</b> status when the table was truncated</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when the table may not be truncated</li>
     *         <li>a <b>500-INTERNAL_SERVER_ERROR</b> status when truncating failed</li>
     */
    @DeleteMapping("/tables/{tableName}")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'table:delete')")
    @Audit
    public ResponseEntity<?> clearTable(@PathVariable("tableName") String tableName) {
    	if (tableName == null || !TRUNCATABLE_TABLES.contains(tableName.trim().toLowerCase())) {
    		log.debug("The table \"" + tableName + "\" may not be truncated.");
    		return responseService.badRequest(MediaType.TEXT_PLAIN_VALUE);
    	}
    	
        try {
			dsl.truncate(DSL.table(DSL.name(tableName.trim().toLowerCase()))).restartIdentity().execute();
        } catch (DataAccessException e) {
            log.error("Truncating the table " + tableName + " from the database was unsuccessful.", e);
            return responseService.internalServerError(MediaType.TEXT_PLAIN_VALUE);
        }
        
        // The cached permissions might refer to removed permissions or resources
        cachingService.clearAllPermissionCaches();

        return responseService.ok(MediaType.TEXT_PLAIN_VALUE);
    }

    /**
     * Endpoint to delete the roles associated with a domain from the database.
     * Access to this method should be highly restricted.
     * 
     * @param domainName (required) the name of the domain for which the user wants to remove the roles
     * @return a <b>200-OK</b> status
     */
    @DeleteMapping("/roles/{domainName}")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'roles:delete')")
    @Audit
    public ResponseEntity<?> deleteDomainRightsAndRoles(@PathVariable("domainName") String domainName) {
        try {
            // Remove all roles from table
            permissionDBService.removeDomainPermissionsForSubject(domainName);
        } catch (NotFoundException e) {
            // Domain does not exist. Nothing to do.
        } catch (Exception f) {
            log.error("Deleting the roles for domain " + domainName + " from the database was unsuccessfull.", f);
            return responseService.internalServerError(MediaType.TEXT_PLAIN_VALUE);
        }

        log.debug("Removed roles for domain \"" + domainName + "\".");
        return responseService.ok(MediaType.TEXT_PLAIN_VALUE);
    }
}
