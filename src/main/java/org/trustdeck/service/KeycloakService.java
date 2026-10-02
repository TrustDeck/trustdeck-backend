/*
 * Trust Deck Services
 * Copyright 2026 Armin Müller
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.trustdeck.configuration.JwtProperties;
import org.trustdeck.dto.UserDTO;
import org.trustdeck.exception.UnexpectedResultSizeException;
import org.trustdeck.utils.Assertion;

import lombok.extern.slf4j.Slf4j;

/**
 * This class encapsulates the communication with Keycloak.
 * 
 * @author Armin Müller
 */
@Slf4j
@Service
public class KeycloakService {

	/** The Keycloak client object. */
	private final Keycloak keycloakAdminClient;
	
	/** The properties object for the JWTs. */
	private final JwtProperties jwtProperties;
    
	/** The name of the realm where the admin client lives in. */
	private final String realmName;

	/**
	 * Constructor that is automatically called by Spring with the correct
	 * attributes from the application context.
	 * 
	 * @param keycloakAdminClient Spring-managed Keycloak admin client (singleton) created in
	 * {@link org.trustdeck.configuration.KeycloakAdminClientConfig} and used to call the 
	 * Keycloak Admin REST API
	 * @param jwtProperties JWT/Keycloak configuration provided by
	 * {@link org.trustdeck.configuration.JwtProperties}, e.g. realm 
	 * and server settings used by this service
	 */
    public KeycloakService(Keycloak keycloakAdminClient, JwtProperties jwtProperties) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.jwtProperties = jwtProperties;
        this.realmName = jwtProperties.getRealm();
    }

    /**
     * Convenience accessor for the configured realm.
     * 
     * @return the realm resource from the keycloak client
     */
    protected RealmResource realm() {
        return keycloakAdminClient.realm(realmName);
    }
	
	/**
     * Looks up the Keycloak subject/user id for a given username.
     *
     * @param username the Keycloak username
     * @return String of Keycloak subject/user id, {@code null} if nothing found
     * @throws UnexpectedResultSizeException if multiple exact matches exist
     */
    public String subjectIdByUsername(String username) {
        if (username == null || username.isBlank()) {
        	log.trace("No username given.");
            return null;
        }

        // Search candidates (can include partial matches), limit to 50 results
        List<UserRepresentation> candidates = realm().users().search(username, 0, 50);

        // Filter exact matches
        List<UserRepresentation> exact = candidates.stream()
                .filter(u -> u.getUsername() != null)
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .toList();

        // Check if any exact matches were found
        if (exact == null || exact.isEmpty()) {
        	log.debug("No exact matches for the given username (" + username + ") were found.");
            return null;
        }

        // Ensure that we found exactly one user
        if (exact.size() > 1) {
            log.warn("Multiple Keycloak users found for username = " + username + ": " + exact.stream().map(UserRepresentation::getId).toList());
            throw new UnexpectedResultSizeException(1, exact.size());
        }

        return exact.get(0).getId();
    }

    /**
     * Looks up the Keycloak subject/user id for a given email (often more user-friendly).
     *
     * @param username the Keycloak username
     * @return String of Keycloak subject/user id, {@code null} if nothing found
     * @throws UnexpectedResultSizeException if multiple exact matches exist
     */
    public String findSubjectIdByEmail(String email) {
        if (email == null || email.isBlank()) {
        	log.trace("No email given.");
            return null;
        }

        // Get a list of candidates, limit to 50 results
        List<UserRepresentation> candidates = realm().users().search(null, null, null, email, 0, 50);

        // Filter exact matches
        List<UserRepresentation> exact = candidates.stream()
                .filter(u -> u.getEmail() != null)
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .toList();

        // Check if any exact matches were found
        if (exact == null || exact.isEmpty()) {
        	log.debug("No exact matches for the given email (" + email + ") were found.");
            return null;
        }

        // Ensure that we found exactly one user
        if (exact.size() > 1) {
            log.warn("Multiple Keycloak users found for email = " + email + ": " + exact.stream().map(UserRepresentation::getId).toList());
            throw new UnexpectedResultSizeException(1, exact.size());
        }

        return exact.get(0).getId();
    }
    
    /**
     * Searches normal Keycloak users, LDAP-federated users returned by Keycloak, client
     * service accounts, and exact Keycloak user IDs for the given query string.
     *
     * Note: Keycloak search behavior is not guaranteed to be an exact match.
     *
     * @param query the search term, e.g. username, name, email
     * @param maxResults maximum number of results to return (must be > 0)
     * @return a list of users found in Keycloak, or an empty list when nothing was found
     */
    public List<UserDTO> searchUsers(String query, int maxResults) {
        if (!Assertion.isNotNullOrEmpty(query) || maxResults <= 0) {
        	log.debug("Query was empty or maxResults was <= 0.");
            return Collections.emptyList();
        }

        RealmResource realm = keycloakAdminClient.realm(realmName);
        List<UserRepresentation> userReps = new ArrayList<>();

        // Search in Keycloak for normal and LDAP-federated users, limit to maxResults 
        List<UserRepresentation> genericUsers = realm.users().search(query, 0, maxResults, true);
        if (genericUsers != null) {
            userReps.addAll(genericUsers);
        }

        // Also search in the IDs
        List<UserRepresentation> usersById = realm.users().search("id:" + query, 0, maxResults, true);
        if (usersById != null) {
            userReps.addAll(usersById);
        }

        // Also search for matching client service accounts
        userReps.addAll(searchServiceAccountUsers(realm, query, maxResults));

        // Ensure uniqueness for each Keycloak user ID and retain Keycloak's order
        Map<String, UserRepresentation> uniqueUsers = new LinkedHashMap<>();
        for (UserRepresentation user : userReps) {
            if (user != null && user.getId() != null) {
                uniqueUsers.putIfAbsent(user.getId(), user);
            }
        }

        List<UserRepresentation> orderedUsers = new ArrayList<>(uniqueUsers.values());
        orderedUsers.sort(Comparator.comparingInt(user -> isExactUserMatch(user, query) ? 0 : 1));
        if (orderedUsers.size() > maxResults) {
            orderedUsers = orderedUsers.subList(0, maxResults);
        }
        
        if (orderedUsers.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch federation provider map
        Map<String, String> federationProviderMap = getFederationProviderMap();

        // Transform into DTOs and add user federation info
        List<UserDTO> users = new ArrayList<>(orderedUsers.size());
        for (UserRepresentation u : orderedUsers) {
            if (u == null) {
            	continue;
            }

            UserDTO user = new UserDTO();
            user.assignPojoValues(u);

            // Add federation provider info, if available
            String providerId = user.getFederationProviderId();
            if (providerId != null && federationProviderMap.containsKey(providerId)) {
                user.setFederationProviderName(federationProviderMap.get(providerId));
            }
            
            // Add Keycloak client roles
            user.setKeycloakRoles(getClientRolesForUser(user.getUserId()));

            users.add(user);
        }

        return users;
    }

    /**
     * Finds service-account users through the client service-account endpoint. The client
     * search is bounded and only clients with service accounts enabled are inspected.
     *
     * @param realm the Keycloak realm resource
     * @param query the user search query
     * @param maxResults maximum number of candidate clients to request
     * @return matching service-account user representations
     */
    private List<UserRepresentation> searchServiceAccountUsers(RealmResource realm, String query, int maxResults) {
    	// Strip the service-account prefix so Keycloak can search by the client ID
    	String clientSearchTerm = query.regionMatches(true, 0, "service-account-", 0, "service-account-".length())
                ? query.substring("service-account-".length()) : query;
    	
    	// Get a list of all clients matching the search term
        List<ClientRepresentation> clients = realm.clients().findAll(clientSearchTerm, true, true, 0, maxResults);
        if (clients == null || clients.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter to those clients that have active service accounts
        List<UserRepresentation> matches = new ArrayList<>();
        for (ClientRepresentation client : clients) {
            if (client == null || !Boolean.TRUE.equals(client.isServiceAccountsEnabled()) || client.getId() == null) {
                continue;
            }

            UserRepresentation serviceAccount;
            try {
                ClientResource clientResource = realm.clients().get(client.getId());
                serviceAccount = clientResource.getServiceAccountUser();
            } catch (NotFoundException e) {
                // A client can be service-account-enabled while its account is temporarily absent
                continue;
            }

            // Add the user account to the list of matches
            String username = serviceAccount == null ? null : serviceAccount.getUsername();
            String clientId = client.getClientId();
            if (serviceAccount != null && (containsIgnoreCase(username, query) || containsIgnoreCase(clientId, query))) {
                matches.add(serviceAccount);
            }
        }

        matches.sort(Comparator.comparingInt(user -> isExactUserMatch(user, query) ? 0 : 1));
        return matches;
    }

    /**
     * Checks whether a value contains a query without regard to letter case.
     *
     * @param value value to inspect
     * @param query query to find
     * @return whether the query occurs in the value
     */
    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query.toLowerCase());
    }

    /**
     * Checks whether a returned user has the exact requested username.
     *
     * @param user user representation returned by Keycloak
     * @param query requested username
     * @return whether the username matches exactly, ignoring letter case
     */
    private boolean isExactUserMatch(UserRepresentation user, String query) {
        return user.getUsername() != null && user.getUsername().equalsIgnoreCase(query);
    }
    
    /**
     * Method to retrieve the client roles of a user from Keycloak.
     * 
     * @param userId the user's Keycloak ID
     * @return a list of the roles' names
     */
	public List<String> getClientRolesForUser(String userId) {
		if (!Assertion.isNotNullOrEmpty(userId)) {
			return Collections.emptyList();
		}

		// Note: this requires that your service account can read role mappings
		UserResource userResource = keycloakAdminClient.realm(realmName).users().get(userId);

		// Get the Keycloak internal, UUID-like ID from the given public clientId
		String clientUuid = keycloakAdminClient.realm(realmName).clients().findByClientId(jwtProperties.getClientId())
				.stream().findFirst().map(c -> c.getId()).orElse(null);

		if (clientUuid == null) {
			return Collections.emptyList();
		}

		// Retrieve the client roles for the given user for the trustdeck client
		List<RoleRepresentation> roles = userResource.roles().clientLevel(clientUuid).listAll();
		if (roles == null || roles.isEmpty()) {
			return Collections.emptyList();
		}

		// Return only the names of the roles as a list
		return roles.stream().map(RoleRepresentation::getName).filter(Assertion::isNotNullOrEmpty).distinct().sorted()
				.collect(Collectors.toList());
	}

	/**
     * Retrieves a map of all configured user storage providers 
     * (e.g. LDAP, Kerberos, etc.) aka federation providers from Keycloak.
     *
     * @return a map that associates the IDs of the federation providers with their names.
     */
    public Map<String, String> getFederationProviderMap() {
        List<ComponentRepresentation> federations = keycloakAdminClient.realm(realmName)
        		.components().query(null, "org.keycloak.storage.UserStorageProvider");

        if (federations == null || federations.isEmpty()) {
        	return Collections.emptyMap();
        }
        
        return federations.stream().collect(Collectors.toMap(ComponentRepresentation::getId, ComponentRepresentation::getName));
    } 
}
