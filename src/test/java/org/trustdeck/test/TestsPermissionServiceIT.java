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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import org.trustdeck.dto.DomainDTO;
import org.trustdeck.dto.EffectivePermissionDTO;
import org.trustdeck.dto.EntityDTO;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.dto.PermissionDTO;
import org.trustdeck.dto.PermissionReplacementDTO;
import org.trustdeck.dto.ProjectDTO;
import org.trustdeck.dto.UserDTO;
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

    /** The Keycloak ID of the user "test", who runs the tests and holds all permissions. */
    private static final String TEST_ID = "3dfb6717-3def-493b-a237-b7345fc42718";

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
        this.assertUnauthorizedRequest("createDomainPermissionNoToken", post(permissionPath), test2Parameter, readPermission, "");
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
        Map<String, String> allProjects = Map.of("all", "true");

        // Every user can list the projects they can read, but listing all projects requires the global permission
        MockHttpServletResponse response = this.assertOkRequest("listOwnProjectsWithoutPermission", get("/api/projects"), null, null, this.getTest2AccessToken());
        assertTrue(this.mapJsonObjectsInStringToList(response.getContentAsString(), ProjectDTO.class).isEmpty());
        this.assertForbiddenRequest("listAllProjectsWithoutPermission", get("/api/projects"), allProjects, null, this.getTest2AccessToken());

        // Grant the permission
        this.assertCreatedRequest("createGlobalPermission", post(permissionPath), test2Parameter, listProjects, this.getAccessToken());
        assertEquals(Set.of("project:list-all"), this.getActions(permissionPath));
        response = this.assertOkRequest("listAllProjectsWithPermission", get("/api/projects"), allProjects, null, this.getTest2AccessToken());
        assertTrue(this.mapJsonObjectsInStringToList(response.getContentAsString(), ProjectDTO.class).stream().anyMatch(p -> PROJECT.equals(p.getAbbreviation())));

        // Domain actions can't be granted globally
        this.assertBadRequestRequest("createGlobalPermissionDomainAction", post(permissionPath), test2Parameter,
                List.of(this.permission("GLOBAL", "domain:read")), this.getAccessToken());

        // Delete the permission
        this.assertNoContent("deleteGlobalPermission", delete(permissionPath), test2Parameter, listProjects, this.getAccessToken());
        this.assertForbiddenRequest("listAllProjectsAfterDeletion", get("/api/projects"), allProjects, null, this.getTest2AccessToken());

        // A project read permission is enough to see the project in the own list
        this.assertCreatedRequest("createProjectReadPermission", post("/api/permissions/projects/" + PROJECT), test2Parameter,
                List.of(this.permission("PROJECT", "project:read")), this.getAccessToken());
        response = this.assertOkRequest("listOwnProjectsWithPermission", get("/api/projects"), null, null, this.getTest2AccessToken());
        assertEquals(List.of(PROJECT), this.mapJsonObjectsInStringToList(response.getContentAsString(), ProjectDTO.class).stream().map(ProjectDTO::getAbbreviation).toList());
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

        // Allow test2 to manage the domain's permissions and to read the domain, but nothing else
        this.assertCreatedRequest("createDomainManagePermission", post(permissionPath), test2Parameter,
                List.of(this.permission("DOMAIN", "domain:manage-permissions"), this.permission("DOMAIN", "domain:read")), this.getAccessToken());
        this.assertOkRequest("getDomainPermissionsAsManager", get(permissionPath), test2Parameter, null, this.getTest2AccessToken());

        // test2 can't grant itself actions it doesn't have
        this.assertForbiddenRequest("createDomainPermissionBeyondOwnActions", post(permissionPath), test2Parameter,
                List.of(this.permission("DOMAIN", "domain:delete")), this.getTest2AccessToken());
        assertFalse(this.getActions(permissionPath).contains("domain:delete"));

        // Replacing is rejected as a whole when an added action can't be granted, and the rejected actions are reported
        List<PermissionDTO> beyondOwnActions = this.domainPermissions(TEST2_ID, "domain:manage-permissions", "domain:read", "domain:delete");
        MockHttpServletResponse response = this.assertForbiddenRequest("updateDomainPermissionsBeyondOwnActions", put(permissionPath),
                test2Parameter, beyondOwnActions, this.getTest2AccessToken());
        assertEquals(List.of("domain:delete"), this.applySingleJsonContentToClass(response.getContentAsString(), PermissionReplacementDTO.class).getRejected());
        assertEquals(Set.of("domain:manage-permissions", "domain:read"), this.getActions(permissionPath));

        // A manager can edit a user who holds more actions than the manager, as long as nothing beyond the manager's actions is added
        Map<String, String> testParameter = Map.of("userId", TEST_ID);
        List<String> actionsOfTest = this.getActions(permissionPath, testParameter).stream().filter(a -> !a.equals("domain:delete")).toList();
        response = this.assertOkRequest("updateDomainPermissionsOfMorePrivilegedUser", put(permissionPath), testParameter,
                this.domainPermissions(TEST_ID, actionsOfTest.toArray(String[]::new)), this.getTest2AccessToken());
        assertEquals(List.of("domain:delete"), this.applySingleJsonContentToClass(response.getContentAsString(), PermissionReplacementDTO.class).getRemoved());
        assertEquals(Set.copyOf(actionsOfTest), this.getActions(permissionPath, testParameter));
    }

    /**
     * Tests that tokens issued for other clients of the realm are not accepted, since their audience
     * doesn't contain TrustDeck.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("foreignAudienceTest")
    public void foreignAudienceTest() throws Exception {
        String adminCliToken = this.obtainAccessTokenForClient("admin-cli", null, "test", "test");
        assertNotNull(adminCliToken);
        MockHttpServletResponse response = this.assertUnauthorizedRequest("getOwnPermissionsWithForeignToken", get("/api/permissions/me"), null, null, adminCliToken);
        assertTrue(response.getHeader("WWW-Authenticate").startsWith("Bearer"));
    }

    /**
     * Tests that only the decision ALLOW can be granted, since DENY is not evaluated.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("denyIsRejectedTest")
    public void denyIsRejectedTest() throws Exception {
        PermissionDTO deny = this.permission("DOMAIN", "domain:read");
        deny.setDecision("DENY");
        this.assertBadRequestRequest("createDenyPermission", post("/api/permissions/domains/" + DOMAIN), test2Parameter, List.of(deny), this.getAccessToken());
        this.assertNotFoundRequest("getPermissionsAfterDeny", get("/api/permissions/domains/" + DOMAIN), test2Parameter, null, this.getAccessToken());
    }

    /**
     * Tests that renaming a domain or a project doesn't change any permissions, so that
     * a user with only the update action can't gain further actions by renaming.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("renameKeepsPermissionsTest")
    public void renameKeepsPermissionsTest() throws Exception {
        // Allow test2 to read and update the domain, then let test2 rename it
        this.assertCreatedRequest("createDomainUpdatePermissions", post("/api/permissions/domains/" + DOMAIN), test2Parameter,
                List.of(this.permission("DOMAIN", "domain:read"), this.permission("DOMAIN", "domain:update")), this.getAccessToken());
        DomainDTO renamedDomain = new DomainDTO();
        renamedDomain.setName("RenamedStudie");
        this.assertOkRequest("renameDomain", put("/api/domains"), Map.of("name", DOMAIN), renamedDomain, this.getTest2AccessToken());

        // The renamer keeps exactly the actions it had and the other users keep theirs
        assertEquals(Set.of("domain:read", "domain:update"), this.getActions("/api/permissions/domains/RenamedStudie"));
        this.assertOkRequest("getRenamedDomain", get("/api/domains/RenamedStudie"), null, null, this.getAccessToken());

        // The same applies to projects (a project in use can't be renamed, so an unused one is created)
        ProjectDTO project = new ProjectDTO();
        project.setName("Rename Study");
        project.setAbbreviation("RENAME");
        project.setStartDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        project.setEndDate(OffsetDateTime.of(2034, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        this.assertCreatedRequest("createProjectForRenaming", post("/api/projects"), null, project, this.getAccessToken());
        this.assertCreatedRequest("createProjectUpdatePermissions", post("/api/permissions/projects/RENAME"), test2Parameter,
                List.of(this.permission("PROJECT", "project:read"), this.permission("PROJECT", "project:update")), this.getAccessToken());
        ProjectDTO renamedProject = new ProjectDTO();
        renamedProject.setAbbreviation("RENAMED");
        this.assertOkRequest("renameProject", put("/api/projects/RENAME"), null, renamedProject, this.getTest2AccessToken());

        assertEquals(Set.of("project:read", "project:update"), this.getActions("/api/permissions/projects/RENAMED"));
        this.assertOkRequest("getRenamedProject", get("/api/projects/RENAMED"), null, null, this.getAccessToken());
    }

    /**
     * Tests that deleting a domain or an entity type removes the permissions of all users on it.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("deleteRemovesPermissionsTest")
    public void deleteRemovesPermissionsTest() throws Exception {
        String definition = """
                {
                  "attributes": [ { "name": "name", "type": "string", "required": true, "linkage": false } ],
                  "recordLinkage": { "enabled": false }
                }
                """;
        this.assertCreatedRequest("createBaseTypeForDeletion", post("/api/entities/base-types"), null,
                this.entityType("deletion-person", definition, null), this.getAccessToken());
        this.assertCreatedRequest("createProjectTypeForDeletion", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.entityType("deletion-participant", definition, "deletion-person"), this.getAccessToken());

        // Grant test2 permissions on the domain and the entity type
        this.assertCreatedRequest("createDomainPermissionForDeletion", post("/api/permissions/domains/" + DOMAIN), test2Parameter,
                List.of(this.permission("DOMAIN", "domain:read")), this.getAccessToken());
        this.assertCreatedRequest("createEntityTypePermissionForDeletion", post("/api/permissions/projects/" + PROJECT + "/entity-types/deletion-participant"),
                test2Parameter, List.of(this.permission("ENTITY_TYPE", "entity:read")), this.getAccessToken());

        // Delete both resources as user "test"
        this.assertNoContent("deleteDomainWithPermissions", delete("/api/domains"), Map.of("name", DOMAIN), null, this.getAccessToken());
        this.assertNoContent("deleteEntityTypeWithPermissions", delete("/api/projects/" + PROJECT + "/entities/config/deletion-participant"),
                null, null, this.getAccessToken());

        // No permissions of test2 remain on either resource
        assertTrue(permissionDBService.getAllPermissionsForSubject(TEST2_ID).stream()
                .noneMatch(p -> "DOMAIN".equals(p.getResourceType()) || "ENTITY_TYPE".equals(p.getResourceType())));
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

        // A manager of a single domain can search users, but only sees their permissions on that domain
        this.assertCreatedRequest("createDomainManagePermissionForSearch", post("/api/permissions/domains/" + DOMAIN), test2Parameter,
                List.of(this.permission("DOMAIN", "domain:manage-permissions")), this.getAccessToken());
        response = this.assertOkRequest("searchUsersAsDomainManager", get("/api/permissions/users"), Map.of("query", TEST_ID), null, this.getTest2AccessToken());
        List<UserDTO> users = this.mapJsonObjectsInStringToList(response.getContentAsString(), UserDTO.class);
        assertEquals(1, users.size());
        assertFalse(users.getFirst().getEffectivePermissions().isEmpty());
        assertTrue(users.getFirst().getEffectivePermissions().stream().allMatch(p -> "DOMAIN".equals(p.getResourceType()) && DOMAIN.equals(p.getResourceName())));

        // Every user can retrieve their own permissions
        response = this.assertOkRequest("getOwnPermissions", get("/api/permissions/me"), null, null, this.getTest2AccessToken());
        List<EffectivePermissionDTO> ownPermissions = this.mapJsonObjectsInStringToList(response.getContentAsString(), EffectivePermissionDTO.class);
        assertEquals(List.of("domain:manage-permissions"), ownPermissions.stream().map(EffectivePermissionDTO::getAction).toList());
        this.assertUnauthorizedRequest("getOwnPermissionsUnauth", get("/api/permissions/me"), null, null, "SomeToken");

        // Every user can list all available permissions
        response = this.assertOkRequest("getAllPermissions", get("/api/permissions"), null, null, this.getTest2AccessToken());
        String content = response.getContentAsString();
        for (String action : List.of("domain:read", "project:read", "entity:create", "system:statistics")) {
            assertTrue(content.contains("\"" + action + "\""), "Missing action " + action);
        }

        // The list can be limited to one scope
        response = this.assertOkRequest("getDomainPermissionCatalogue", get("/api/permissions"), Map.of("scope", "domain"), null, this.getTest2AccessToken());
        List<EffectivePermissionDTO> catalogue = this.mapJsonObjectsInStringToList(response.getContentAsString(), EffectivePermissionDTO.class);
        assertTrue(catalogue.stream().allMatch(p -> "DOMAIN".equals(p.getResourceType())));
        assertTrue(catalogue.stream().anyMatch(p -> "pseudonym:create".equals(p.getAction())));
        this.assertBadRequestRequest("getUnknownPermissionCatalogue", get("/api/permissions"), Map.of("scope", "planet"), null, this.getTest2AccessToken());
    }

    /**
     * Helper to create domain permissions with the domain name, as needed for replacing permissions.
     *
     * @param subjectId the Keycloak ID of the user who receives the permissions
     * @param actions the actions that are permitted
     * @return the list of permission DTOs
     */
    private List<PermissionDTO> domainPermissions(String subjectId, String... actions) {
        List<PermissionDTO> permissions = new ArrayList<>();
        for (String action : actions) {
            PermissionDTO permission = this.permission("DOMAIN", action);
            permission.setSubjectId(subjectId);
            permission.setDomainName(DOMAIN);
            permissions.add(permission);
        }

        return permissions;
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
        return this.getActions(permissionPath, test2Parameter);
    }

    /**
     * Helper to retrieve the actions a user has on the resource of the given permission path.
     *
     * @param permissionPath the path of the permission endpoint
     * @param userParameter the query parameters identifying the user
     * @return the set of actions
     * @throws Exception forwards any internally thrown exceptions
     */
    private Set<String> getActions(String permissionPath, Map<String, String> userParameter) throws Exception {
        MockHttpServletResponse response = this.assertOkRequest("getPermissions", get(permissionPath), userParameter, null, this.getAccessToken());
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
