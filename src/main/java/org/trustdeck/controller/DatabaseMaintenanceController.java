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
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
     * Endpoint to retrieve the size of a database table
     * 
     * @param tableName (required) the name of the table from which the user wants to read the table size
     * @return<li>a <b>200-OK</b> status and the table size on success</li>
     */
    @GetMapping("/tables/{table}/storage")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'table:read-storage')")
    @Audit
    public ResponseEntity<?> monitorDatabaseMetrics(@PathVariable("table") String tableName) {
    	TableMetrics metrics = getTableMetrics(tableName);
    	Long dbSize = getTotalDatabaseSize();
    	
    	if (metrics == null | dbSize == null) {
    		log.debug("Retrieval of database space usage was unsuccessful.");
    		return responseService.internalServerError(MediaType.TEXT_PLAIN_VALUE);
    	}
    	
    	// Create response string
    	String response = "tableSize: " + metrics.size + ", recordCount: " + metrics.recordCount + ", totalSize: " + dbSize;
    	
    	return responseService.ok(MediaType.TEXT_PLAIN_VALUE, response);
    }
    
    /**
     * Endpoint to delete a table from the database.
     * Performs an additional "VACUUM FULL" after deletion.
     * Access to this method should be highly restricted.
     * 
     * @param tableName (required) the name of the table the user wants to delete
     * @return <li>a <b>200-OK</b> status</li>
     */
    @DeleteMapping("/tables/{tableName}")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'table:delete')")
    @Audit
    public ResponseEntity<?> clearTable(@PathVariable("tableName") String tableName) {
    	try {
    		// Remove all records from table
			dsl.deleteFrom(DSL.table(DSL.name(tableName))).execute();
			
			// Perform vacuum to free up space
			dsl.execute("VACUUM FULL " + DSL.name(tableName).toString() + ";");
		} catch (DataAccessException e) {
			log.error("Deleting the table " + tableName + " from the database was unsuccessfull.", e);
			return responseService.internalServerError(MediaType.TEXT_PLAIN_VALUE);
		}
    	
    	return responseService.ok(MediaType.TEXT_PLAIN_VALUE);
    }
        
    /**
     * Endpoint to delete the roles associated with a domain from the database.
     * Access to this method should be highly restricted.
     * 
     * @param domainName (required) the name of the domain for which the user wants to remove the roles
     * @return <li>a <b>200-OK</b> status</li>
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

    /**
     * Helper method that retrieves the used amount of storage space for a particular table.
     * 
     * @param tableName the name of the table
     * @return the size in bytes or {@code null}, if unsuccessful
     */
    private TableMetrics getTableMetrics(String tableName) {
		Field<Long> totalSize = DSL.field("pg_total_relation_size({0}::regclass)", Long.class, DSL.val(tableName));
    	
		Result<Record2<Long, Integer>> result;
		try {
			result = dsl.select(totalSize, DSL.count()).from(DSL.table(DSL.name(tableName))).fetch();
		} catch (DataAccessException e) {
			log.debug("Retrieving the storage used by table " + tableName + " was unsuccessfull.", e);
			return null;
		}
		
		long size = result.get(0).component1();
		int recordCount = result.get(0).component2();
		
		return new TableMetrics(size, recordCount);
    }

    /**
     * Helper method that retrieves the used amount of storage space for the whole database.
     * 
     * @return the size in bytes or {@code null}, if unsuccessful
     */ 
    private Long getTotalDatabaseSize() {
    	Field<Long> dbSize = DSL.field("pg_database_size(current_database())", Long.class);

    	Long size;
		try {
			size = dsl.select(dbSize).fetchOne(0, Long.class);
		} catch (DataAccessException e) {
			log.debug("Retrieving the storage used by the database was unsuccessfull.", e);
			return null;
		}
		
		return size;
    }

    /**
     * Helper class that encapsulates the size and the amount of records from a table into one object.
     * 
     * @param size the of the table
     * @param recordCount the number of records stored in a table
     */
    private record TableMetrics(long size, long recordCount) {};
}
