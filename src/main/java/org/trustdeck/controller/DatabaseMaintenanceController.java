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

    /**
     * Endpoint to truncate a table in the database.
     * Restarts identity counters. Might fail, when foreign keys exist.
     * Access to this method should be highly restricted.
     * 
     * @param tableName (required) the name of the table the user wants to truncate
     * @return a <b>200-OK</b> status
     */
    @DeleteMapping("/tables/{tableName}")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'table:delete')")
    @Audit
    public ResponseEntity<?> clearTable(@PathVariable("tableName") String tableName) {
        try {
			dsl.truncate(DSL.table(DSL.name(tableName))).restartIdentity().execute();
        } catch (DataAccessException e) {
            log.error("Trincating the table " + tableName + " from the database was unsuccessfull.", e);
            return responseService.internalServerError(MediaType.TEXT_PLAIN_VALUE);
        }

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
