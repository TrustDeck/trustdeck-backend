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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.trustdeck.dto.EntityDTO;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.service.AssertWebRequestService;

import tools.jackson.databind.ObjectMapper;

/**
 * This class offers tests for the record linkage of entities.
 *
 * @author Armin Müller
 */
public class TestsRecordLinkageServiceIT extends AssertWebRequestService {

    /** The abbreviation of the project that is created for every test. */
    private static final String PROJECT = "TEST";

    /** The name of the base type used in the tests. */
    private static final String BASE_TYPE_NAME = "linkage-person";

    /** The name of the project specific type used in the tests. */
    private static final String PROJECT_TYPE_NAME = "linkage-participant";

    /** The type definition with the placeholder "RECORD_LINKAGE" for the record linkage configuration. */
    private static final String DEFINITION = """
            {
              "attributes": [
                { "name": "firstName", "type": "string", "required": true, "linkage": true },
                { "name": "lastName", "type": "string", "required": true, "linkage": true },
                { "name": "birthDate", "type": "date", "required": true, "linkage": true },
                { "name": "comment", "type": "string", "required": false, "linkage": false }
              ],
              "recordLinkage": RECORD_LINKAGE
            }
            """;

    /** The data of the entity that is created first. */
    private static final String ERIKA = "{ \"firstName\": \"Erika\", \"lastName\": \"Mustermann\", \"birthDate\": \"1964-08-12\" }";

    /** Used to create the type definitions and entity data. */
    private final ObjectMapper om = new ObjectMapper();

    /**
     * Tests the record linkage endpoint for exact, similar, and different entities.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("recordLinkageSearchTest")
    public void recordLinkageSearchTest() throws Exception {
        String entityPath = this.createEntityTypes("{ \"enabled\": true }");
        String linkagePath = entityPath + "/record-linkage";
        String trustDeckId = this.createEntity(entityPath, ERIKA);

        // Unauthorized tests
        this.assertUnauthorizedRequest("recordLinkageNoToken", post(linkagePath), null, this.entity(ERIKA), "");
        this.assertUnauthorizedRequest("recordLinkageUnauth", post(linkagePath), null, this.entity(ERIKA), "SomeToken");

        // The identical entity is found
        MockHttpServletResponse response = this.assertOkRequest("recordLinkageExactMatch", post(linkagePath), null, this.entity(ERIKA), this.getAccessToken());
        assertTrue(response.getContentAsString().contains(trustDeckId));

        // Differences in case and white spaces are normalized
        response = this.assertOkRequest("recordLinkageNormalizedMatch", post(linkagePath), null,
                this.entity("{ \"firstName\": \" ERIKA \", \"lastName\": \"mustermann\", \"birthDate\": \"1964-08-12\" }"), this.getAccessToken());
        assertTrue(response.getContentAsString().contains(trustDeckId));

        // Attributes that are not used for the linkage don't matter
        response = this.assertOkRequest("recordLinkageIgnoredAttribute", post(linkagePath), null,
                this.entity("{ \"firstName\": \"Erika\", \"lastName\": \"Mustermann\", \"birthDate\": \"1964-08-12\", \"comment\": \"Other\" }"), this.getAccessToken());
        assertTrue(response.getContentAsString().contains(trustDeckId));

        // Entities that only share the name are no candidates
        this.assertNoContent("recordLinkageSameNameOtherBirthDate", post(linkagePath), null,
                this.entity("{ \"firstName\": \"Erika\", \"lastName\": \"Mustermann\", \"birthDate\": \"1990-01-01\" }"), this.getAccessToken());
        this.assertNoContent("recordLinkageNoMatch", post(linkagePath), null,
                this.entity("{ \"firstName\": \"Max\", \"lastName\": \"Meier\", \"birthDate\": \"1980-02-02\" }"), this.getAccessToken());

        // Invalid payloads
        this.assertBadRequestRequest("recordLinkageInvalidPayload", post(linkagePath), null,
                this.entity("{ \"firstName\": \"Erika\" }"), this.getAccessToken());
        this.assertBadRequestRequest("recordLinkageWithoutPayload", post(linkagePath), null, new EntityDTO(), this.getAccessToken());

        // Deleted entities are still found as candidates
        this.assertNoContent("deleteLinkedEntity", delete(entityPath + "/" + trustDeckId), null, null, this.getAccessToken());
        response = this.assertOkRequest("recordLinkageDeletedEntity", post(linkagePath), null, this.entity(ERIKA), this.getAccessToken());
        assertTrue(response.getContentAsString().contains(trustDeckId));
    }

    /**
     * Tests the automatic record linkage when creating entities that reject duplicates.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("automaticRecordLinkageRejectTest")
    public void automaticRecordLinkageRejectTest() throws Exception {
        String entityPath = this.createEntityTypes("{ \"enabled\": true, \"autoLinkOnCreate\": true, \"onMatch\": \"reject\" }");
        String trustDeckId = this.createEntity(entityPath, ERIKA);

        // A matching entity is rejected and the candidates are returned
        MockHttpServletResponse response = this.assertRequestFromRequestBuilder("createMatchingEntityRejected", post(entityPath), null,
                this.entity("{ \"firstName\": \"Erika\", \"lastName\": \"Mustermann\", \"birthDate\": \"1964-08-12\", \"comment\": \"Duplicate\" }"),
                this.getAccessToken(), status().isConflict());
        assertTrue(response.getContentAsString().contains(trustDeckId));

        // The match can deliberately be ignored
        String original = this.createEntity(entityPath + "?recordLinkageResolution=CREATE_ORIGINAL",
                "{ \"firstName\": \"Erika\", \"lastName\": \"Mustermann\", \"birthDate\": \"1964-08-12\", \"comment\": \"Twin\" }");
        assertNotEquals(trustDeckId, original);

        // Non-matching entities are created as usual
        this.createEntity(entityPath, "{ \"firstName\": \"Max\", \"lastName\": \"Meier\", \"birthDate\": \"1980-02-02\" }");
    }

    /**
     * Tests the automatic record linkage when creating entities that return the existing entity on a match.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("automaticRecordLinkageReturnExistingTest")
    public void automaticRecordLinkageReturnExistingTest() throws Exception {
        String entityPath = this.createEntityTypes("{ \"enabled\": true, \"autoLinkOnCreate\": true, \"onMatch\": \"returnExisting\" }");
        String trustDeckId = this.createEntity(entityPath, ERIKA);

        MockHttpServletResponse response = this.assertOkRequest("createMatchingEntityReturnsExisting", post(entityPath), null,
                this.entity("{ \"firstName\": \"Erika\", \"lastName\": \"Mustermann\", \"birthDate\": \"1964-08-12\", \"comment\": \"Duplicate\" }"),
                this.getAccessToken());
        EntityDTO existing = this.applySingleJsonContentToClass(response.getContentAsString(), EntityDTO.class);
        assertEquals(trustDeckId, existing.getTrustdeckID().toString());
        assertEquals(this.entity(ERIKA).getData(), existing.getData());
    }

    /**
     * Tests the privacy preserving record linkage, where only encoded values are compared.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("privacyPreservingRecordLinkageTest")
    public void privacyPreservingRecordLinkageTest() throws Exception {
        String entityPath = this.createEntityTypes("{ \"enabled\": true, \"privacyMode\": \"pprl\" }");
        String linkagePath = entityPath + "/record-linkage";
        String trustDeckId = this.createEntity(entityPath, ERIKA);

        MockHttpServletResponse response = this.assertOkRequest("pprlExactMatch", post(linkagePath), null, this.entity(ERIKA), this.getAccessToken());
        assertTrue(response.getContentAsString().contains(trustDeckId));
        this.assertNoContent("pprlNoMatch", post(linkagePath), null,
                this.entity("{ \"firstName\": \"Max\", \"lastName\": \"Meier\", \"birthDate\": \"1980-02-02\" }"), this.getAccessToken());
    }

    /**
     * Helper to create the base type and the project specific type with the given record linkage configuration.
     *
     * @param recordLinkage the record linkage configuration as JSON string
     * @return the path for the entity endpoints of the project specific type
     * @throws Exception forwards any internally thrown exceptions
     */
    private String createEntityTypes(String recordLinkage) throws Exception {
        String definition = DEFINITION.replace("RECORD_LINKAGE", recordLinkage);
        this.assertCreatedRequest("createLinkageBaseType", post("/api/entities/base-types"), null,
                this.entityType(BASE_TYPE_NAME, definition, null), this.getAccessToken());
        this.assertCreatedRequest("createLinkageProjectType", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.entityType(PROJECT_TYPE_NAME, definition, BASE_TYPE_NAME), this.getAccessToken());

        return "/api/projects/" + PROJECT + "/entities/" + PROJECT_TYPE_NAME;
    }

    /**
     * Helper to create an entity.
     *
     * @param entityPath the path for the entity endpoints (may include query parameters)
     * @param data the entity's data as JSON string
     * @return the TrustDeck ID of the created entity
     * @throws Exception forwards any internally thrown exceptions
     */
    private String createEntity(String entityPath, String data) throws Exception {
        MockHttpServletResponse response = this.assertCreatedRequest("createLinkageEntity", post(entityPath), null, this.entity(data), this.getAccessToken());

        return this.applySingleJsonContentToClass(response.getContentAsString(), EntityDTO.class).getTrustdeckID().toString();
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
     * Helper to create an entity DTO.
     *
     * @param data the entity's data as JSON string
     * @return the entity DTO
     */
    private EntityDTO entity(String data) {
        EntityDTO entity = new EntityDTO();
        entity.setData(om.readTree(data));

        return entity;
    }
}
