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

package org.trustdeck.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.trustdeck.dto.EntityDTO;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.dto.PermissionDTO;
import org.trustdeck.service.AssertWebRequestService;
import org.trustdeck.service.PermissionDBService;

import tools.jackson.databind.ObjectMapper;

/**
 * This class offers tests for the permission endpoints.
 * The permissions are granted by the user "test" to the user "test2".
 *
 * @author Armin Müller
 */
public class TestsPermissionServiceIT extends AssertWebRequestService {

    /** The Keycloak ID of the user "test2", who receives the permissions in these tests. */
    private static final String TEST2_ID = "2719369c-4354-4303-aab3-a5f43a9aa4d6";

    /** The domain created for every test. */
    private static final String DOMAIN = "TestStudie";

    /** The project created for every test. */
    private static final String PROJECT = "TEST";

    /** Enables access to the permission grants database methods. */
    @Autowired
    private PermissionDBService permissionDBService;

    /** Used to create the type definitions and entity data. */
    private final ObjectMapper om = new ObjectMapper();

    /** The query parameters identifying the user "test2". */
    private final Map<String, String> test2Parameter = Map.of("userId", TEST2_ID);

    /**
     * Removes all permissions of the user "test2", since global permissions are not removed by the test data reset.
     */
    @BeforeEach
    @AfterEach
    public void removePermissionsOfTest2() {
        permissionDBService.removePermissionsForSubjectByID(TEST2_ID);
    }

    /**
     * Tests creating, retrieving, replacing, and deleting domain permissions and that they take effect.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("domainPermissionTest")
    public void domainPermissionTest() throws Exception {
        String permissionPath = "/api/permissions/domains/" + DOMAIN;
        List<PermissionDTO> readPermission = List.of(this.permission("DOMAIN", "domain:read"));

        // Without permissions, test2 can't read the domain
        this.assertForbiddenRequest("getDomainWithoutPermission", get("/api/domains/" + DOMAIN), null, null, this.getTest2AccessToken());

        // Unauthorized tests for creating permissions
        this.assertBadRequestRequest("createDomainPermissionBadRequest", post(permissionPath), test2Parameter, readPermission, "");
        this.assertUnauthorizedRequest("createDomainPermissionUnauth", post(permissionPath), test2Parameter, readPermission, "SomeToken");

        // Grant the permission
        MockHttpServletResponse response = this.assertCreatedRequest("createDomainPermission", post(permissionPath), test2Parameter, readPermission, this.getAccessToken());
        List<PermissionDTO> created = this.mapJsonObjectsInStringToList(response.getContentAsString(), PermissionDTO.class);
        assertEquals(1, created.size());
        assertEquals("domain:read", created.getFirst().getAction());
        assertEquals(TEST2_ID, created.getFirst().getSubjectId());

        // Granting it again only reports the duplicate
        this.assertOkRequest("createDomainPermissionDuplicate", post(permissionPath), test2Parameter, readPermission, this.getAccessToken());

        // The permission takes effect
        this.assertOkRequest("getDomainWithPermission", get("/api/domains/" + DOMAIN), null, null, this.getTest2AccessToken());
        assertEquals(Set.of("domain:read"), this.getActions(permissionPath));

        // Invalid permissions are not created
        this.assertBadRequestRequest("createDomainPermissionWrongSubject", post(permissionPath), Map.of("userId", "another-user"), readPermission, this.getAccessToken());
        this.assertBadRequestRequest("createDomainPermissionGlobalAction", post(permissionPath), test2Parameter,
                List.of(this.permission("DOMAIN", "domain:create")), this.getAccessToken());
        this.assertBadRequestRequest("createDomainPermissionUnknownAction", post(permissionPath), test2Parameter,
                List.of(this.permission("DOMAIN", "domain:fly")), this.getAccessToken());
        this.assertBadRequestRequest("createDomainPermissionWrongResourceType", post(permissionPath), test2Parameter,
                List.of(this.permission("PROJECT", "domain:read")), this.getAccessToken());
        this.assertBadRequestRequest("createDomainPermissionEmptyList", post(permissionPath), test2Parameter, List.of(), this.getAccessToken());

        // Replace the permissions
        List<PermissionDTO> replacement = List.of(this.permission("DOMAIN", "pseudonym:read"), this.permission("DOMAIN", "pseudonym:search"));
        replacement.forEach(p -> p.setDomainName(DOMAIN));
        this.assertOkRequest("updateDomainPermissions", put(permissionPath), test2Parameter, replacement, this.getAccessToken());
        assertEquals(Set.of("pseudonym:read", "pseudonym:search"), this.getActions(permissionPath));
        this.assertForbiddenRequest("getDomainAfterReplacement", get("/api/domains/" + DOMAIN), null, null, this.getTest2AccessToken());

        // Delete one of the permissions (deleting requires the domain name in the permission)
        PermissionDTO searchPermission = this.permission("DOMAIN", "pseudonym:search");
        this.assertOkRequest("deleteDomainPermissionWithoutDomainName", delete(permissionPath), test2Parameter, List.of(searchPermission), this.getAccessToken());
        assertEquals(Set.of("pseudonym:read", "pseudonym:search"), this.getActions(permissionPath));
        searchPermission.setDomainName(DOMAIN);
        this.assertNoContent("deleteDomainPermission", delete(permissionPath), test2Parameter, List.of(searchPermission), this.getAccessToken());
        assertEquals(Set.of("pseudonym:read"), this.getActions(permissionPath));

        // Users without permissions have none to retrieve
        this.assertNotFoundRequest("getDomainPermissionsOfUserWithoutPermissions", get(permissionPath), Map.of("userId", "another-user"), null, this.getAccessToken());
    }

    /**
     * Tests creating, retrieving, replacing, and deleting project permissions and that they take effect.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("projectPermissionTest")
    public void projectPermissionTest() throws Exception {
        String permissionPath = "/api/permissions/projects/" + PROJECT;

        this.assertForbiddenRequest("getProjectWithoutPermission", get("/api/projects/" + PROJECT), null, null, this.getTest2AccessToken());

        // Grant the permissions
        this.assertCreatedRequest("createProjectPermissions", post(permissionPath), test2Parameter,
                List.of(this.permission("PROJECT", "project:read"), this.permission("PROJECT", "project:statistics")), this.getAccessToken());
        assertEquals(Set.of("project:read", "project:statistics"), this.getActions(permissionPath));
        this.assertOkRequest("getProjectWithPermission", get("/api/projects/" + PROJECT), null, null, this.getTest2AccessToken());
        this.assertOkRequest("getProjectStatisticsWithPermission", get("/api/projects/" + PROJECT + "/statistics"), null, null, this.getTest2AccessToken());

        // Domain actions can't be granted on projects
        this.assertBadRequestRequest("createProjectPermissionDomainAction", post(permissionPath), test2Parameter,
                List.of(this.permission("PROJECT", "domain:read")), this.getAccessToken());

        // Replace the permissions
        List<PermissionDTO> replacement = List.of(this.permission("PROJECT", "project:read"));
        replacement.forEach(p -> p.setProjectAbbreviation(PROJECT));
        this.assertOkRequest("updateProjectPermissions", put(permissionPath), test2Parameter, replacement, this.getAccessToken());
        assertEquals(Set.of("project:read"), this.getActions(permissionPath));
        this.assertForbiddenRequest("getProjectStatisticsAfterReplacement", get("/api/projects/" + PROJECT + "/statistics"), null, null, this.getTest2AccessToken());

        // Delete the permission
        this.assertNoContent("deleteProjectPermission", delete(permissionPath), test2Parameter, replacement, this.getAccessToken());
        this.assertNotFoundRequest("getProjectPermissionsAfterDeletion", get(permissionPath), test2Parameter, null, this.getAccessToken());
        this.assertForbiddenRequest("getProjectAfterDeletion", get("/api/projects/" + PROJECT), null, null, this.getTest2AccessToken());
    }

    /**
     * Tests creating, retrieving, and deleting global permissions and that they take effect.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("globalPermissionTest")
    public void globalPermissionTest() throws Exception {
        String permissionPath = "/api/permissions/global";
        List<PermissionDTO> listProjects = List.of(this.permission("GLOBAL", "project:list-all"));

        this.assertForbiddenRequest("listProjectsWithoutPermission", get("/api/projects"), null, null, this.getTest2AccessToken());

        // Grant the permission
        this.assertCreatedRequest("createGlobalPermission", post(permissionPath), test2Parameter, listProjects, this.getAccessToken());
        assertEquals(Set.of("project:list-all"), this.getActions(permissionPath));
        this.assertOkRequest("listProjectsWithPermission", get("/api/projects"), null, null, this.getTest2AccessToken());

        // Domain actions can't be granted globally
        this.assertBadRequestRequest("createGlobalPermissionDomainAction", post(permissionPath), test2Parameter,
                List.of(this.permission("GLOBAL", "domain:read")), this.getAccessToken());

        // Delete the permission
        this.assertNoContent("deleteGlobalPermission", delete(permissionPath), test2Parameter, listProjects, this.getAccessToken());
        this.assertForbiddenRequest("listProjectsAfterDeletion", get("/api/projects"), null, null, this.getTest2AccessToken());
    }

    /**
     * Tests creating, retrieving, replacing, and deleting entity type permissions and that they take effect.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("entityTypePermissionTest")
    public void entityTypePermissionTest() throws Exception {
        String definition = """
                {
                  "attributes": [ { "name": "name", "type": "string", "required": true, "linkage": false } ],
                  "recordLinkage": { "enabled": false }
                }
                """;
        this.assertCreatedRequest("createBaseTypeForPermissions", post("/api/entities/base-types"), null,
                this.entityType("permission-person", definition, null), this.getAccessToken());
        this.assertCreatedRequest("createProjectTypeForPermissions", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.entityType("permission-participant", definition, "permission-person"), this.getAccessToken());
        String entityPath = "/api/projects/" + PROJECT + "/entities/permission-participant";
        EntityDTO entity = new EntityDTO();
        entity.setData(om.readTree("{ \"name\": \"Erika\" }"));
        MockHttpServletResponse response = this.assertCreatedRequest("createEntityForPermissions", post(entityPath), null, entity, this.getAccessToken());
        String entityUrl = entityPath + "/" + this.applySingleJsonContentToClass(response.getContentAsString(), EntityDTO.class).getTrustdeckID();
        String permissionPath = "/api/permissions/projects/" + PROJECT + "/entity-types/permission-participant";

        this.assertForbiddenRequest("getEntityWithoutPermission", get(entityUrl), null, null, this.getTest2AccessToken());

        // Grant the permissions
        this.assertCreatedRequest("createEntityTypePermissions", post(permissionPath), test2Parameter,
                List.of(this.permission("ENTITY_TYPE", "entity:read"), this.permission("ENTITY_TYPE", "entity:search")), this.getAccessToken());
        assertEquals(Set.of("entity:read", "entity:search"), this.getActions(permissionPath));
        this.assertOkRequest("getEntityWithPermission", get(entityUrl), null, null, this.getTest2AccessToken());
        this.assertForbiddenRequest("createEntityWithoutPermission", post(entityPath), null, entity, this.getTest2AccessToken());

        // Domain actions can't be granted on entity types
        this.assertBadRequestRequest("createEntityTypePermissionDomainAction", post(permissionPath), test2Parameter,
                List.of(this.permission("ENTITY_TYPE", "domain:read")), this.getAccessToken());

        // Replace the permissions
        List<PermissionDTO> replacement = List.of(this.permission("ENTITY_TYPE", "entity:search"));
        replacement.forEach(p -> p.setEntityTypeName("permission-participant"));
        this.assertOkRequest("updateEntityTypePermissions", put(permissionPath), test2Parameter, replacement, this.getAccessToken());
        assertEquals(Set.of("entity:search"), this.getActions(permissionPath));
        this.assertForbiddenRequest("getEntityAfterReplacement", get(entityUrl), null, null, this.getTest2AccessToken());

        // Delete the permission
        this.assertNoContent("deleteEntityTypePermission", delete(permissionPath), test2Parameter, replacement, this.getAccessToken());
        this.assertNotFoundRequest("getEntityTypePermissionsAfterDeletion", get(permissionPath), test2Parameter, null, this.getAccessToken());

        // Requests on non-existing entity types are forbidden, so that their existence is not revealed
        this.assertForbiddenRequest("createPermissionUnknownEntityType", post("/api/permissions/projects/" + PROJECT + "/entity-types/unknown-type"),
                test2Parameter, List.of(this.permission("ENTITY_TYPE", "entity:read")), this.getAccessToken());
    }

    /**
     * Tests that users can only manage permissions when they are allowed to and only
     * delegate the actions they have themselves (bounded delegation).
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("boundedDelegationTest")
    public void boundedDelegationTest() throws Exception {
        String permissionPath = "/api/permissions/domains/" + DOMAIN;

        // Without the permission to manage permissions, test2 can't grant anything
        this.assertForbiddenRequest("createDomainPermissionWithoutManagePermission", post(permissionPath), test2Parameter,
                List.of(this.permission("DOMAIN", "domain:read")), this.getTest2AccessToken());
        this.assertForbiddenRequest("getAllPermissionsWithoutManagePermission", get("/api/permissions"), null, null, this.getTest2AccessToken());

        // Allow test2 to manage the domain's permissions, but nothing else
        this.assertCreatedRequest("createDomainManagePermission", post(permissionPath), test2Parameter,
                List.of(this.permission("DOMAIN", "domain:manage-permissions")), this.getAccessToken());
        this.assertOkRequest("getDomainPermissionsAsManager", get(permissionPath), test2Parameter, null, this.getTest2AccessToken());

        // test2 can't grant itself actions it doesn't have
        this.assertForbiddenRequest("createDomainPermissionBeyondOwnActions", post(permissionPath), test2Parameter,
                List.of(this.permission("DOMAIN", "domain:delete")), this.getTest2AccessToken());
        assertFalse(this.getActions(permissionPath).contains("domain:delete"));
    }

    /**
     * Tests the user search and the listing of all available permissions.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("userSearchAndPermissionListTest")
    public void userSearchAndPermissionListTest() throws Exception {
        // Search for users
        MockHttpServletResponse response = this.assertOkRequest("searchUsers", get("/api/permissions/users"), Map.of("query", "test2"), null, this.getAccessToken());
        assertTrue(response.getContentAsString().contains(TEST2_ID));
        this.assertBadRequestRequest("searchUsersWithoutQuery", get("/api/permissions/users"), Map.of("query", " "), null, this.getAccessToken());
        this.assertForbiddenRequest("searchUsersWithoutPermission", get("/api/permissions/users"), Map.of("query", "test"), null, this.getTest2AccessToken());

        // List all available permissions
        response = this.assertOkRequest("getAllPermissions", get("/api/permissions"), null, null, this.getAccessToken());
        String content = response.getContentAsString();
        for (String action : List.of("domain:read", "project:read", "entity:create", "system:statistics")) {
            assertTrue(content.contains("\"" + action + "\""), "Missing action " + action);
        }
    }

    /**
     * Helper to create a permission for the user "test2".
     *
     * @param resourceType the type of the resource (DOMAIN, PROJECT, ENTITY_TYPE, or GLOBAL)
     * @param action the action that is permitted
     * @return the permission DTO
     */
    private PermissionDTO permission(String resourceType, String action) {
        PermissionDTO permission = new PermissionDTO();
        permission.setSubjectId(TEST2_ID);
        permission.setResourceType(resourceType);
        permission.setAction(action);
        permission.setDecision("ALLOW");

        return permission;
    }

    /**
     * Helper to create an entity type DTO.
     *
     * @param name the name of the entity type
     * @param definition the type definition as JSON string
     * @param baseTypeName the name of the base type (only for project specific types)
     * @return the entity type DTO
     */
    private EntityTypeDTO entityType(String name, String definition, String baseTypeName) {
        EntityTypeDTO entityType = new EntityTypeDTO();
        entityType.setName(name);
        entityType.setTypeDefinition(om.readTree(definition));
        entityType.setBaseTypeName(baseTypeName);

        return entityType;
    }

    /**
     * Helper to retrieve the actions the user "test2" has on the resource of the given permission path.
     *
     * @param permissionPath the path of the permission endpoint
     * @return the set of actions
     * @throws Exception forwards any internally thrown exceptions
     */
    private Set<String> getActions(String permissionPath) throws Exception {
        MockHttpServletResponse response = this.assertOkRequest("getPermissions", get(permissionPath), test2Parameter, null, this.getAccessToken());
        List<PermissionDTO> permissions = this.mapJsonObjectsInStringToList(response.getContentAsString(), PermissionDTO.class);

        return new ArrayList<>(permissions).stream().map(PermissionDTO::getAction).collect(Collectors.toSet());
    }

    /**
     * Helper to obtain an access token for the user "test2" without replacing the token of the user "test".
     *
     * @return the access token of the user "test2"
     * @throws Exception forwards any internally thrown exceptions
     */
    private String getTest2AccessToken() throws Exception {
        String test2AccessToken = this.obtainNewAccessToken("test2", "test");

        // Restore the token of the user "test"
        this.obtainNewAccessToken("test", "test");

        return test2AccessToken;
    }
}
