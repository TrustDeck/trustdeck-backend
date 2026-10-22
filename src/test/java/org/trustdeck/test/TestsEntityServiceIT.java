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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.trustdeck.dto.EntityDTO;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.service.AssertWebRequestService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * This class offers tests for the entity type and entity endpoints.
 *
 * @author Armin Müller
 */
public class TestsEntityServiceIT extends AssertWebRequestService {

    /** The abbreviation of the project that is created for every test. */
    private static final String PROJECT = "TEST";

    /** The name of the base type used in the tests. */
    private static final String BASE_TYPE_NAME = "person";

    /** The name of the project specific type used in the tests. */
    private static final String PROJECT_TYPE_NAME = "study-participant";

    /** The type definition of the base type. */
    private static final String BASE_DEFINITION = """
            {
              "attributes": [
                { "name": "firstName", "type": "string", "required": true, "linkage": false, "minLength": 1, "maxLength": 50 },
                { "name": "lastName", "type": "string", "required": true, "linkage": false, "minLength": 1, "maxLength": 50 },
                { "name": "birthYear", "type": "integer", "required": false, "linkage": false, "minimum": 1900, "maximum": 2100 }
              ],
              "recordLinkage": { "enabled": false }
            }
            """;

    /** The type definition of the project specific type, extending the base type. */
    private static final String PROJECT_DEFINITION = """
            {
              "attributes": [
                { "name": "firstName", "type": "string", "required": true, "linkage": false, "minLength": 1, "maxLength": 50 },
                { "name": "lastName", "type": "string", "required": true, "linkage": false, "minLength": 1, "maxLength": 50 },
                { "name": "birthYear", "type": "integer", "required": true, "linkage": false, "minimum": 1900, "maximum": 2100 },
                { "name": "cohort", "type": "enum", "required": false, "linkage": false, "values": ["A", "B"] }
              ],
              "recordLinkage": { "enabled": false }
            }
            """;

    /** Used to create the type definitions and entity data. */
    private final ObjectMapper om = new ObjectMapper();

    /**
     * Tests creating, retrieving, and validating base entity types.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("baseEntityTypeTest")
    public void baseEntityTypeTest() throws Exception {
        EntityTypeDTO baseType = this.createEntityTypeDTO(BASE_TYPE_NAME, BASE_DEFINITION, null);

        // Unauthorized tests for creating a base type
        this.assertBadRequestRequest("createBaseTypeBadRequest", post("/api/entities/base-types"), null, baseType, "");
        this.assertUnauthorizedRequest("createBaseTypeUnauth", post("/api/entities/base-types"), null, baseType, "SomeToken");

        // Create the base type
        MockHttpServletResponse response = this.assertCreatedRequest("createBaseType", post("/api/entities/base-types"), null, baseType, this.getAccessToken());
        EntityTypeDTO created = this.applySingleJsonContentToClass(response.getContentAsString(), EntityTypeDTO.class);
        assertEquals(BASE_TYPE_NAME, created.getName());
        assertEquals("v1.0", created.getVersion());
        assertTrue(created.getIsBaseType());
        assertFalse(created.getIsDeprecated());
        assertNull(created.getProjectName());

        // Creating an identical base type again returns the existing one
        this.assertOkRequest("createBaseTypeAgain", post("/api/entities/base-types"), null, baseType, this.getAccessToken());

        // Retrieve the base type
        response = this.assertOkRequest("getBaseType", get("/api/entities/base-types/" + BASE_TYPE_NAME), null, null, this.getAccessToken());
        EntityTypeDTO retrieved = this.applySingleJsonContentToClass(response.getContentAsString(), EntityTypeDTO.class);
        assertEquals(BASE_TYPE_NAME, retrieved.getName());
        assertEquals(om.readTree(BASE_DEFINITION), retrieved.getTypeDefinition());
        this.assertNotFoundRequest("getBaseTypeNotFound", get("/api/entities/base-types/unknown-type"), null, null, this.getAccessToken());

        // Invalid names and definitions
        this.assertBadRequestRequest("createBaseTypeInvalidName", post("/api/entities/base-types"), null,
                this.createEntityTypeDTO("invalid name", BASE_DEFINITION, null), this.getAccessToken());
        this.assertBadRequestRequest("createBaseTypeWithoutAttributes", post("/api/entities/base-types"), null,
                this.createEntityTypeDTO("no-attributes", "{ \"attributes\": [] }", null), this.getAccessToken());
        this.assertBadRequestRequest("createBaseTypeUnknownAttributeType", post("/api/entities/base-types"), null,
                this.createEntityTypeDTO("unknown-attribute-type", """
                        { "attributes": [ { "name": "firstName", "type": "text", "required": true, "linkage": false } ] }
                        """, null), this.getAccessToken());
        this.assertBadRequestRequest("createBaseTypeDuplicatedAttribute", post("/api/entities/base-types"), null,
                this.createEntityTypeDTO("duplicated-attribute", """
                        { "attributes": [
                          { "name": "firstName", "type": "string", "required": true, "linkage": false },
                          { "name": "firstName", "type": "string", "required": false, "linkage": false } ] }
                        """, null), this.getAccessToken());
    }

    /**
     * Tests creating project specific entity types and validating them against their base type.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("projectEntityTypeTest")
    public void projectEntityTypeTest() throws Exception {
        this.assertCreatedRequest("createBaseType", post("/api/entities/base-types"), null,
                this.createEntityTypeDTO(BASE_TYPE_NAME, BASE_DEFINITION, null), this.getAccessToken());

        // Create the project specific type
        EntityTypeDTO projectType = this.createEntityTypeDTO(PROJECT_TYPE_NAME, PROJECT_DEFINITION, BASE_TYPE_NAME);
        MockHttpServletResponse response = this.assertCreatedRequest("createProjectType", post("/api/projects/" + PROJECT + "/entities/config"), null, projectType, this.getAccessToken());
        EntityTypeDTO created = this.applySingleJsonContentToClass(response.getContentAsString(), EntityTypeDTO.class);
        assertEquals(PROJECT_TYPE_NAME, created.getName());
        assertEquals(BASE_TYPE_NAME, created.getBaseTypeName());
        assertEquals("Test Study", created.getProjectName());
        assertFalse(created.getIsBaseType());

        // Retrieve the project specific type
        response = this.assertOkRequest("getProjectType", get("/api/projects/" + PROJECT + "/entities/config/" + PROJECT_TYPE_NAME), null, null, this.getAccessToken());
        EntityTypeDTO retrieved = this.applySingleJsonContentToClass(response.getContentAsString(), EntityTypeDTO.class);
        assertEquals(om.readTree(PROJECT_DEFINITION), retrieved.getTypeDefinition());

        // Missing information
        this.assertBadRequestRequest("createProjectTypeWithoutBaseType", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.createEntityTypeDTO("without-base-type", PROJECT_DEFINITION, null), this.getAccessToken());
        this.assertNotFoundRequest("createProjectTypeUnknownBaseType", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.createEntityTypeDTO("unknown-base-type", PROJECT_DEFINITION, "unknown-type"), this.getAccessToken());

        // Only base types can be extended (project specific types are not found as base types)
        this.assertNotFoundRequest("createProjectTypeFromProjectType", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.createEntityTypeDTO("extends-project-type", PROJECT_DEFINITION, PROJECT_TYPE_NAME), this.getAccessToken());

        // The project specific type must not weaken the base type
        this.assertUnprocessableEntity("createProjectTypeMissingBaseAttribute", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.createEntityTypeDTO("missing-base-attribute", """
                        { "attributes": [
                          { "name": "firstName", "type": "string", "required": true, "linkage": false, "minLength": 1, "maxLength": 50 } ] }
                        """, BASE_TYPE_NAME), this.getAccessToken());
        this.assertUnprocessableEntity("createProjectTypeWeakerConstraint", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.createEntityTypeDTO("weaker-constraint", PROJECT_DEFINITION.replace("\"maximum\": 2100", "\"maximum\": 3000"), BASE_TYPE_NAME),
                this.getAccessToken());
        this.assertUnprocessableEntity("createProjectTypeOptionalRequiredAttribute", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.createEntityTypeDTO("optional-required-attribute", PROJECT_DEFINITION.replace(
                        "\"name\": \"lastName\", \"type\": \"string\", \"required\": true", "\"name\": \"lastName\", \"type\": \"string\", \"required\": false"),
                        BASE_TYPE_NAME), this.getAccessToken());

        // Requests on non-existing projects are forbidden, so that their existence is not revealed
        this.assertForbiddenRequest("getProjectTypeUnknownProject", get("/api/projects/UNKNOWN/entities/config/" + PROJECT_TYPE_NAME), null, null, this.getAccessToken());
    }

    /**
     * Tests creating, retrieving, updating, and deleting entities as well as validating their data.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("entityTest")
    public void entityTest() throws Exception {
        this.assertCreatedRequest("createBaseType", post("/api/entities/base-types"), null,
                this.createEntityTypeDTO(BASE_TYPE_NAME, BASE_DEFINITION, null), this.getAccessToken());
        this.assertCreatedRequest("createProjectType", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.createEntityTypeDTO(PROJECT_TYPE_NAME, PROJECT_DEFINITION, BASE_TYPE_NAME), this.getAccessToken());
        String entityPath = "/api/projects/" + PROJECT + "/entities/" + PROJECT_TYPE_NAME;

        // Unauthorized tests for creating an entity
        EntityDTO entity = this.createEntityDTO("{ \"firstName\": \"Erika\", \"lastName\": \"Mustermann\", \"birthYear\": 1964, \"cohort\": \"A\" }");
        this.assertBadRequestRequest("createEntityBadRequest", post(entityPath), null, entity, "");
        this.assertUnauthorizedRequest("createEntityUnauth", post(entityPath), null, entity, "SomeToken");

        // Create an entity
        MockHttpServletResponse response = this.assertCreatedRequest("createEntity", post(entityPath), null, entity, this.getAccessToken());
        EntityDTO created = this.applySingleJsonContentToClass(response.getContentAsString(), EntityDTO.class);
        assertNotNull(created.getTrustdeckID());
        assertEquals(entity.getData(), created.getData());

        // Retrieve the entity
        response = this.assertOkRequest("getEntity", get(entityPath + "/" + created.getTrustdeckID()), null, null, this.getAccessToken());
        EntityDTO retrieved = this.applySingleJsonContentToClass(response.getContentAsString(), EntityDTO.class);
        assertEquals(created.getTrustdeckID(), retrieved.getTrustdeckID());
        assertEquals(entity.getData(), retrieved.getData());
        assertFalse(retrieved.getIsDeleted());

        // Entities that don't match the type definition are rejected
        this.assertBadRequestRequest("createEntityMissingRequiredAttribute", post(entityPath), null,
                this.createEntityDTO("{ \"firstName\": \"Max\", \"lastName\": \"Mustermann\" }"), this.getAccessToken());
        this.assertBadRequestRequest("createEntityWrongType", post(entityPath), null,
                this.createEntityDTO("{ \"firstName\": \"Max\", \"lastName\": \"Mustermann\", \"birthYear\": \"1970\" }"), this.getAccessToken());
        this.assertBadRequestRequest("createEntityConstraintViolation", post(entityPath), null,
                this.createEntityDTO("{ \"firstName\": \"Max\", \"lastName\": \"Mustermann\", \"birthYear\": 1800 }"), this.getAccessToken());
        this.assertBadRequestRequest("createEntityInvalidEnumValue", post(entityPath), null,
                this.createEntityDTO("{ \"firstName\": \"Max\", \"lastName\": \"Mustermann\", \"birthYear\": 1970, \"cohort\": \"C\" }"), this.getAccessToken());
        this.assertBadRequestRequest("createEntityUnknownAttribute", post(entityPath), null,
                this.createEntityDTO("{ \"firstName\": \"Max\", \"lastName\": \"Mustermann\", \"birthYear\": 1970, \"nickname\": \"Maxi\" }"), this.getAccessToken());
        this.assertBadRequestRequest("createEntityWithoutData", post(entityPath), null, new EntityDTO(), this.getAccessToken());

        // Update the entity
        EntityDTO update = this.createEntityDTO("{ \"firstName\": \"Erika\", \"lastName\": \"Musterfrau\", \"birthYear\": 1964, \"cohort\": \"B\" }");
        response = this.assertOkRequest("updateEntity", put(entityPath + "/" + created.getTrustdeckID()), null, update, this.getAccessToken());
        EntityDTO updated = this.applySingleJsonContentToClass(response.getContentAsString(), EntityDTO.class);
        assertEquals(update.getData(), updated.getData());
        this.assertBadRequestRequest("updateEntityInvalidData", put(entityPath + "/" + created.getTrustdeckID()), null,
                this.createEntityDTO("{ \"firstName\": \"Erika\" }"), this.getAccessToken());

        // Delete the entity, which only marks it as deleted, so that it can't be found anymore
        this.assertBadRequestRequest("deleteEntityInvalidId", delete(entityPath + "/not-a-uuid"), null, null, this.getAccessToken());
        this.assertNoContent("deleteEntity", delete(entityPath + "/" + created.getTrustdeckID()), null, null, this.getAccessToken());
        this.assertNotFoundRequest("getDeletedEntity", get(entityPath + "/" + created.getTrustdeckID()), null, null, this.getAccessToken());
        this.assertNotFoundRequest("deleteDeletedEntity", delete(entityPath + "/" + created.getTrustdeckID()), null, null, this.getAccessToken());

        // Requests on non-existing entity types are forbidden, so that their existence is not revealed
        this.assertForbiddenRequest("createEntityUnknownType", post("/api/projects/" + PROJECT + "/entities/unknown-type"), null, entity, this.getAccessToken());
    }

    /**
     * Helper to create an entity type DTO.
     *
     * @param name the name of the entity type
     * @param definition the type definition as JSON string
     * @param baseTypeName the name of the base type (only for project specific types)
     * @return the entity type DTO
     * @throws Exception forwards any internally thrown exceptions
     */
    private EntityTypeDTO createEntityTypeDTO(String name, String definition, String baseTypeName) throws Exception {
        EntityTypeDTO entityType = new EntityTypeDTO();
        entityType.setName(name);
        entityType.setTypeDefinition(om.readTree(definition));
        entityType.setBaseTypeName(baseTypeName);

        return entityType;
    }

    /**
     * Helper to create an entity DTO.
     *
     * @param data the entity's data as JSON string
     * @return the entity DTO
     * @throws Exception forwards any internally thrown exceptions
     */
    private EntityDTO createEntityDTO(String data) throws Exception {
        JsonNode node = om.readTree(data);
        EntityDTO entity = new EntityDTO();
        entity.setData(node);

        return entity;
    }
}
