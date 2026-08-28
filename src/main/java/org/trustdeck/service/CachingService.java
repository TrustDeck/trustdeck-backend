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

package org.trustdeck.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.dto.EffectivePermissionDTO;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Caching service for caching permissions.
 * It uses Hazelcast as the underlying caching mechanism.
 *
 * @author Armin Müller
 */
@Service
@Slf4j
public class CachingService {

    /** Hazelcast instance used for caching data. */
    @Autowired
    private HazelcastInstance hazelcast;

    /** Name of the Hazelcast cache for caching actions by context (subjectId, RESOURCE_TYPE, resourceId). */
	private static final String MAP_ACTIONS_BY_CONTEXT = "permission-actions-by-context";
    
    /** Name of the Hazelcast cache for caching effective permissions by subject (subjectId). */
    private static final String MAP_EFFECTIVE_PERMISSIONS_BY_SUBJECT = "effective-permissions-by-subject";

    /** Enables access to default values. */
    @Autowired
    private DefaultProperties defaults;

    /**
     * Helper method to generate a context-key-String.
     * 
     * @param subjectId the subject's / user's (Keycloak) ID
     * @param resourceType the type of the resource, e.g. "DOMAIN", "PROJECT", "GLOBAL"
     * @param resourceId the (internal) database ID of the resource (or '0' for GLOBAL)
     * @return a String that merges the given information and can be used as a key in the cache-lookup
     */
    private static String contextKey(String subjectId, String resourceType, Integer resourceId) {
        return subjectId + "|" + resourceType.toUpperCase() + "|" + resourceId;
    }

    /**
     * Method to lookup all currently allowed actions for a specific (subject, resourceType, resourceId) in the cache.
     * 
     * @param subjectId the subject's / user's (Keycloak) ID
     * @param resourceType the type of the resource, e.g. "DOMAIN", "PROJECT", "GLOBAL"
     * @param resourceId the (internal) database ID of the resource (or '0' for GLOBAL)
     * @param loader a function that tells the cache how to fetch the data if it's not already cached
     * @return a set of the allowed actions for the given parameters
     */
    public Set<String> getAllowedActionsForContext(String subjectId, String resourceType, Integer resourceId, Supplier<Set<String>> loader) {
        // Get the cache instance
    	IMap<String, Set<String>> map = hazelcast.getMap(MAP_ACTIONS_BY_CONTEXT);
        
    	// Build the lookup key from the given parameters
    	String key = contextKey(subjectId, resourceType, resourceId);

        // Check if the data is cached
        Set<String> cached = map.get(key);
        if (cached != null) {
        	// Cache hit: return the data
            return cached;
        }

        // The data was not cached; lock the key while the data is being put into the cache
        map.lock(key);
        try {
            // Double-checke locking
            cached = map.get(key);
            if (cached != null) {
            	// Cache hit: return the data
                return cached;
            }

            // Still a cache miss: user the loader function to get the needed data anyways
            Set<String> loaded = loader.get();
            
            // Failed to retrieve the data: nothing to cache
            if (loaded == null) {
            	log.debug("Failed to retrieve the currently uncached data using the loader function.");
                return null;
            }

            // Add data to cache and return to user
            map.put(key, loaded, defaults.getCache().getTtlMinutes(), TimeUnit.MINUTES);
            return loaded;
        } finally {
            map.unlock(key);
        }
    }

    /**
     * Method to lookup all currently effective permissions for a specific subject.
     * 
     * @param subjectId the subject's / user's (Keycloak) ID
     * @param loader a function that tells the cache how to fetch the data if it's not already cached
     * @return a set of the effective permissions for the given parameters
     */
	public List<EffectivePermissionDTO> getEffectivePermissionsForSubject(String subjectId, Supplier<List<EffectivePermissionDTO>> loader) {
		// Get the cache instance
    	IMap<String, List<EffectivePermissionDTO>> map = hazelcast.getMap(MAP_EFFECTIVE_PERMISSIONS_BY_SUBJECT);

    	// Check if the data is cached
		List<EffectivePermissionDTO> cached = map.get(subjectId);
		if (cached != null) {
        	// Cache hit: return the data
            return cached;
        }

        // The data was not cached; lock the key while the data is being put into the cache
		map.lock(subjectId);
		try {
            // Double-checke locking
			cached = map.get(subjectId);
			if (cached != null) {
            	// Cache hit: return the data
                return cached;
            }

            // Still a cache miss: user the loader function to get the needed data anyways
			List<EffectivePermissionDTO> loaded = loader.get();

            // Failed to retrieve the data: nothing to cache
            if (loaded == null) {
            	log.debug("Failed to retrieve the currently uncached data using the loader function.");
                return null;
            }

            // Add data to cache and return to user
			map.put(subjectId, loaded, defaults.getCache().getTtlMinutes(), TimeUnit.MINUTES);
			return loaded;
		} finally {
			map.unlock(subjectId);
		}
	}

    /**
     * Invalidates a cache entry given the context.
     * 
     * @param subjectId the subject's / user's (Keycloak) ID
     * @param resourceType the type of the resource, e.g. "DOMAIN", "PROJECT", "GLOBAL"
     * @param resourceId the (internal) database ID of the resource (or '0' for GLOBAL)
     */
    public void invalidateContext(String subjectId, String resourceType, Integer resourceId) {
    	// Get the cache instance
    	IMap<String, Set<String>> map = hazelcast.getMap(MAP_ACTIONS_BY_CONTEXT);
        
    	// Remove from cache
    	map.delete(contextKey(subjectId, resourceType, resourceId));
    }

    /**
     * Invalidates a cache entry for effective permissions given the subject.
     * 
     * @param subjectId the subject's / user's (Keycloak) ID
     */
    public void invalidateSubject(String subjectId) {
    	// Get the cache instance
    	IMap<String, List<String>> map = hazelcast.getMap(MAP_EFFECTIVE_PERMISSIONS_BY_SUBJECT);

    	// Remove from cache
    	map.delete(subjectId);
    }

    /**
     * Invalidates all cache entries for all caches.
     * Useful for when all permissions get removed.
     */
    public void clearAllPermissionCaches() {
    	hazelcast.getMap(MAP_ACTIONS_BY_CONTEXT).clear();
    	hazelcast.getMap(MAP_EFFECTIVE_PERMISSIONS_BY_SUBJECT).clear();
    }
}
