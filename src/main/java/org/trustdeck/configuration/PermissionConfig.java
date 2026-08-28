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

package org.trustdeck.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configuration class that holds the operational permissions for the application.
 *
 * This class is used to map configuration properties defined under the `app` prefix in the application’s
 * configuration file (e.g., `application.yml` or `application.properties`). It contains a list of permission names
 * that define the operations required by the application.
 *
 * For example, a configuration in `application.yml` might look like:
 * <pre>
 * app:
 *   permissions:
 *     project:
 *       - project:read
 *       - project:delete
 *       ...
 *     domain:
 *       - domain:read
 *       - domain:update
 *       ...
 *     entity-type:
 *       - entity:create
         - entity:read
         ...
 *     global:
 *       - domain:create
 *       - project:create
 *       ...
 * </pre>
 *
 * @author Armin Müller
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class PermissionConfig {

    /**
     * List of actions defined for the application under `app.permissions` in the yml-file.
     * It represents the names of various rights and permissions (e.g., create, read, update, delete) 
     * that the application will use to define permissions.
     */
    private Map<String, List<String>> permissions;
    
    /** The key to extract the project-specific permissions from the yml. */
    public static final String PROJECT_PERMISSIONS_GROUP_KEY = "project";
    
    /** The key to extract the domain-specific permissions from the yml. */
    public static final String DOMAIN_PERMISSIONS_GROUP_KEY = "domain";

    /** The key to extract the entity-type-specific permissions from the yml. */
    public static final String ENTITY_TYPE_PERMISSIONS_GROUP_KEY = "entity-type";
    
    /** The key to extract the administrative permissions from the yml. */
    public static final String GLOBAL_PERMISSIONS_GROUP_KEY = "global";
    
    /**
     * Retrieves the permissions for a configured resource scope (e.g. "domain").
     * 
     * @param groupName the name that indicates the sublist of permissions
     * @return a list of permissions found
     */
    public List<String> getPermissionSublist(String groupName) {
    	List<String> permissionSublist = permissions.get(groupName);
        return permissionSublist == null ? null : permissionSublist;
    }
    
    /**
     * Retrieves the project-specific permissions.
     * 
     * @return a list of permissions found
     */
    public List<String> getProjectPermissions() {
        return getPermissionSublist(PROJECT_PERMISSIONS_GROUP_KEY);
    }
    
    /**
     * Retrieves the domain-specific permissions.
     * 
     * @return a list of permissions found
     */
    public List<String> getDomainPermissions() {
        return getPermissionSublist(DOMAIN_PERMISSIONS_GROUP_KEY);
    }

    /**
     * Retrieves the entity-type-specific permissions.
     * 
     * @return a list of permissions found
     */
    public List<String> getEntityTypePermissions() {
        return getPermissionSublist(ENTITY_TYPE_PERMISSIONS_GROUP_KEY);
    }
    
    /**
     * Retrieves the global permissions.
     * 
     * @return a list of permissions found
     */
    public List<String> getGlobalPermissions() {
        return getPermissionSublist(GLOBAL_PERMISSIONS_GROUP_KEY);
    }
    
    /**
     * Returns a list of all defined permissions including all global permissions.
     * 
     * @return a list of all permissions defined in the application.yml
     */
    public List<String> getAllPermissions() {
    	List<String> allPermissions = new ArrayList<>();
		allPermissions.addAll(getProjectPermissions());
		allPermissions.addAll(getDomainPermissions());
		allPermissions.addAll(getEntityTypePermissions());
    	allPermissions.addAll(getGlobalPermissions());
    	
    	return allPermissions;
    }
}
