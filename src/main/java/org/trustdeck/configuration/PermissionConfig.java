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
 * Configuration class that holds the operational roles for the application.
 *
 * This class is used to map configuration properties defined under the `app` prefix in the application’s
 * configuration file (e.g., `application.yml` or `application.properties`). It contains a list of role names
 * that define the operations required by the application.
 *
 * For example, a configuration in `application.yml` might look like:
 * <pre>
 * app:
 *   roles:
 *     ACE:
 *       - domain:read
 *       - domain:update
 *       ...
 *     KING:
 *       - project:read
 *       - project:delete
 *       ...
 *     global:
 *       - domain:create
 *       - project:create
 * </pre>
 *
 * @author Armin Müller and Eric Wündisch
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class RoleConfig {

    /**
     * List of actions defined for the application under `app.roles` in the yml-file.
     * It represents the names of various rights and roles (e.g., create, read, update, delete) that the application
     * will use to define roles and permissions.
     */
    private Map<String, List<String>> roles;
    
    /** The key to extract the administrative roles from the yml. */
    public static final String GLOBAL_ROLES_GROUP_KEY = "global";
    
    /** The key to extract the ACE-specific roles from the yml. */
    public static final String ACE_ROLES_GROUP_KEY = "ACE";
    
    /** The key to extract the KING-specific roles from the yml. */
    public static final String KING_ROLES_GROUP_KEY = "KING";
    
    /**
     * Retrieves the roles for a group defined by it's name (e.g. "ACE").
     * 
     * @param groupName the name that indicates the sublist of roles
     * @return a list of roles found
     */
    public List<String> getRoleSublist(String groupName) {
    	List<String> roleSublist = roles.get(groupName);
        return roleSublist == null ? null : roleSublist;
    }
    
    /**
     * Retrieves the roles for ACE.
     * 
     * @return a list of roles found
     */
    public List<String> getACERoles() {
        return getRoleSublist(ACE_ROLES_GROUP_KEY);
    }
    
    /**
     * Retrieves the roles for KING.
     * 
     * @return a list of roles found
     */
    public List<String> getKINGRoles() {
        return getRoleSublist(KING_ROLES_GROUP_KEY);
    }
    
    /**
     * Retrieves the roles that are neither ACE- nor KING-specific but global.
     * 
     * @return a list of roles found
     */
    public List<String> getGlobalRoles() {
        return getRoleSublist(GLOBAL_ROLES_GROUP_KEY);
    }
    
    /**
     * Returns a list of all defined roles including all global roles.
     * 
     * @return a list of all roles defined in the application.yml
     */
    public List<String> getAllRoles() {
    	List<String> allRoles = new ArrayList<>();
    	allRoles.addAll(getACERoles());
    	allRoles.addAll(getKINGRoles());
    	allRoles.addAll(getGlobalRoles());
    	
    	return allRoles;
    }
}