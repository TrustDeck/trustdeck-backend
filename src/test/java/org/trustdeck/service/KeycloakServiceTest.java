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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;
import org.trustdeck.configuration.JwtProperties;
import org.trustdeck.dto.UserDTO;

/**
 * Tests Keycloak user discovery for regular users, federated users, exact user IDs,
 * and client service accounts.
 * 
 * @author Armin Müller
 */
class KeycloakServiceTest {

	/** Mocked Keycloak administration client. */
    private final Keycloak keycloak = mock(Keycloak.class);

    /** Mocked Keycloak realm resource. */
    private final RealmResource realm = mock(RealmResource.class);

    /** Mocked Keycloak user-management resource. */
    private final UsersResource users = mock(UsersResource.class);

    /** Mocked Keycloak client-management resource. */
    private final ClientsResource clients = mock(ClientsResource.class);

    /** Service under test. */
    private KeycloakService service;

    /**
     * Initializes the mocked Keycloak resources and the service under test before
     * each test.
     */
    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setRealm("test");
        
        when(keycloak.realm("test")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(realm.clients()).thenReturn(clients);
        when(users.search(anyString(), anyInt(), anyInt(), eq(true))).thenReturn(Collections.emptyList());
        
        service = Mockito.spy(new KeycloakService(keycloak, properties));
        
        doReturn(Collections.emptyMap()).when(service).getFederationProviderMap();
        doReturn(Collections.emptyList()).when(service).getClientRolesForUser(anyString());
    }

    /**
     * Verifies that users returned by the regular Keycloak search remain
     * discoverable when they originate from an LDAP federation provider.
     */
    @Test
    void returnsNormalAndLdapUsers() {
        UserRepresentation user = user("normal-id", "ldap-user");
        user.setFederationLink("ldap");
        when(users.search("ldap-user", 0, 10, true)).thenReturn(List.of(user));

        List<UserDTO> result = service.searchUsers("ldap-user", 10);

        assertEquals(List.of("normal-id"), result.stream().map(UserDTO::getUserId).toList());
    }

    /**
     * Verifies that a user can be found through an exact Keycloak user-ID search.
     */
    @Test
    void findsUserByExactId() {
        UserRepresentation user = user("uuid-123", "ordinary-user");
        when(users.search("id:uuid-123", 0, 10, true)).thenReturn(List.of(user));

        assertEquals("uuid-123", service.searchUsers("uuid-123", 10).get(0).getUserId());
    }

    /**
     * Verifies that a complete service-account username resolves to the actual
     * user representation returned by Keycloak.
     */
    @Test
    void findsFullServiceAccountUsernameAndUsesReturnedRepresentation() {
        UserRepresentation account = user("real-service-id", "service-account-example-client");
        ClientResource resource = serviceAccountClient(account);
        when(clients.findAll("example-client", true, true, 0, 10)).thenReturn(List.of(client("client-uuid", "example-client", true)));
        when(clients.get("client-uuid")).thenReturn(resource);

        List<UserDTO> result = service.searchUsers("service-account-example-client", 10);

        assertEquals("real-service-id", result.get(0).getUserId());
        assertEquals("service-account-example-client", result.get(0).getUsername());
    }

    /**
     * Verifies that a partial client name finds its service account without
     * case-sensitive matching.
     */
    @Test
    void findsPartialClientNameCaseInsensitively() {
        UserRepresentation account = user("service-id", "service-account-example-client");
        when(clients.findAll("EXAMPLE", true, true, 0, 10)).thenReturn(List.of(client("client-uuid", "Example-Client", true)));
        
        ClientResource resource = serviceAccountClient(account);
        when(clients.get("client-uuid")).thenReturn(resource);

        assertEquals("service-id", service.searchUsers("EXAMPLE", 10).get(0).getUserId());
    }

    /**
     * Verifies that clients without enabled service accounts and returned
     * accounts that do not match the query are excluded.
     */
    @Test
    void ignoresDisabledClientsAndNonmatchingReturnedAccounts() {
        ClientRepresentation disabled = client("disabled", "disabled-client", false);
        ClientRepresentation mismatch = client("mismatch", "unrelated-name", true);
        when(clients.findAll("client", true, true, 0, 10)).thenReturn(List.of(disabled, mismatch));
        
        ClientResource resource = serviceAccountClient(user("id", "service-account-other"));
        when(clients.get("mismatch")).thenReturn(resource);

        assertTrue(service.searchUsers("client", 10).isEmpty());
    }

    /**
     * Verifies that results from different search paths are deduplicated by user
     * ID and that the configured result limit is respected.
     */
    @Test
    void deduplicatesOrdinaryAndServiceAccountResultsAndHonorsLimit() {
        UserRepresentation first = user("same-id", "service-account-example-client");
        when(users.search("example", 0, 1, true)).thenReturn(List.of(first));
        when(clients.findAll("example", true, true, 0, 1)).thenReturn(List.of(client("first", "example-client", true)));
        
        ClientResource resource = serviceAccountClient(first);
        when(clients.get("first")).thenReturn(resource);

        List<UserDTO> result = service.searchUsers("example", 1);

        assertEquals(1, result.size());
        assertEquals("same-id", result.get(0).getUserId());
    }

    /**
     * Verifies that blank queries and nonpositive result limits return no users.
     */
    @Test
    void blankQueryAndNonpositiveLimitReturnEmpty() {
        assertTrue(service.searchUsers(" ", 10).isEmpty());
        assertTrue(service.searchUsers("query", 0).isEmpty());
    }

    /**
     * Verifies that an absent service account is skipped without discarding other
     * matching service accounts.
     */
    @Test
    void skipsAbsentServiceAccountButKeepsOtherResults() {
        ClientRepresentation absent = client("absent", "absent-client", true);
        ClientRepresentation present = client("present", "present-client", true);
        when(clients.findAll("client", true, true, 0, 10)).thenReturn(List.of(absent, present));
        when(clients.get("absent")).thenThrow(new NotFoundException());
        
        ClientResource resource = serviceAccountClient(user("present-id", "service-account-present-client"));
        when(clients.get("present")).thenReturn(resource);

        assertEquals("present-id", service.searchUsers("client", 10).get(0).getUserId());
    }

    /**
     * Verifies that general Keycloak client-search failures remain visible to
     * callers.
     */
    @Test
    void propagatesClientSearchFailures() {
        RuntimeException failure = new RuntimeException("Keycloak unavailable");
        when(clients.findAll("query", true, true, 0, 10)).thenThrow(failure);

        assertThrows(RuntimeException.class, () -> service.searchUsers("query", 10));
    }

    /**
     * Creates a Keycloak user representation for a test.
     *
     * @param id Keycloak user ID
     * @param username username represented by the user
     * @return initialized user representation
     */
    private UserRepresentation user(String id, String username) {
        UserRepresentation user = new UserRepresentation();
        user.setId(id);
        user.setUsername(username);
        
        return user;
    }

    /**
     * Creates a Keycloak client representation for a test.
     *
     * @param id internal Keycloak client ID
     * @param clientId public client identifier
     * @param serviceAccountsEnabled whether service accounts are enabled
     * @return initialized client representation
     */
    private ClientRepresentation client(String id, String clientId, boolean serviceAccountsEnabled) {
        ClientRepresentation client = new ClientRepresentation();
        client.setId(id);
        client.setClientId(clientId);
        client.setServiceAccountsEnabled(serviceAccountsEnabled);
        
        return client;
    }

    /**
     * Creates a mocked client resource that returns the supplied service-account
     * user.
     * 
     * @param account service-account user returned by Keycloak
     * @return configured client-resource mock
     */
    private ClientResource serviceAccountClient(UserRepresentation account) {
        ClientResource resource = mock(ClientResource.class);
        when(resource.getServiceAccountUser()).thenReturn(account);
        
        return resource;
    }
}
