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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.trustdeck.dto.DomainDTO;
import org.trustdeck.dto.EntityDTO;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.service.AssertWebRequestService;

import tools.jackson.databind.ObjectMapper;

/**
 * This class offers tests for the search endpoints.
 *
 * @author Armin Müller
 */
public class TestsSearchServiceIT extends AssertWebRequestService {

    /** The domain created for every test. */
    private static final String DOMAIN = "TestStudie";

    /** The abbreviation of the project that is created for every test. */
    private static final String PROJECT = "TEST";

    /** The maximum number of results of a pseudonym search (see application.yml). */
    private static final int PSEUDONYM_SEARCH_RESULT_LIMIT = 30;

    /** Used to create the type definitions and entity data. */
    private final ObjectMapper om = new ObjectMapper();

    /**
     * Tests searching for pseudonyms, including multi-word and wildcard searches.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("pseudonymSearchTest")
    public void pseudonymSearchTest() throws Exception {
        String pseudonymPath = "/api/domains/" + DOMAIN + "/pseudonyms";

        // Create more pseudonyms than a search returns
        List<PseudonymDTO> pseudonyms = new ArrayList<>();
        for (int i = 1; i <= PSEUDONYM_SEARCH_RESULT_LIMIT + 1; i++) {
            PseudonymDTO pseudonym = new PseudonymDTO();
            pseudonym.setIdentifierItem(IdentifierItem.builder().identifier("search-" + i).idType("SEARCH-ID").build());
            pseudonyms.add(pseudonym);
        }
        this.assertCreatedRequest("createPseudonymsForSearch", post(pseudonymPath + "/batch"), null, pseudonyms, this.getAccessToken());

        // Search for a part of an identifier
        assertEquals(Set.of("search-12"), this.searchPseudonyms("search-12"));

        // All words of a multi-word search must match
        assertEquals(Set.of("search-3", "search-30", "search-31"), this.searchPseudonyms("search-3 search-id"));

        // The search is case insensitive and also matches the ID type and the pseudonym
        assertEquals(Set.of("10000008912"), this.searchPseudonyms("any-id"));
        assertEquals(Set.of("10000008912"), this.searchPseudonyms("9EEEE39F0D5C"));

        // No matches
        assertEquals(Set.of(), this.searchPseudonyms("nothing-to-find"));

        // A wildcard search returns everything, but is truncated
        MockHttpServletResponse response = this.assertRequestFromRequestBuilder("searchPseudonymsWildcard", get(pseudonymPath), Map.of("query", "*"),
                null, this.getAccessToken(), status().isPartialContent());
        assertEquals(PSEUDONYM_SEARCH_RESULT_LIMIT, this.mapJsonObjectsInStringToList(response.getContentAsString(), PseudonymDTO.class).size());

        // Searching in non-existing domains is forbidden
        this.assertForbiddenRequest("searchPseudonymsUnknownDomain", get("/api/domains/Unknown/pseudonyms"), Map.of("query", "search"), null, this.getAccessToken());
    }

    /**
     * Tests searching for domains.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("domainSearchTest")
    public void domainSearchTest() throws Exception {
        DomainDTO child = new DomainDTO();
        child.setName("TestStudie-Biobank");
        child.setPrefix("TS-B");
        child.setDescription("Samples of the study");
        child.setSuperDomainName(DOMAIN);
        child.setProjectAbbreviation(PROJECT);
        this.assertCreatedRequest("createDomainForSearch", post("/api/domains"), null, child, this.getAccessToken());

        assertEquals(Set.of(DOMAIN, "TestStudie-Biobank"), this.searchDomains("teststudie"));
        assertEquals(Set.of("TestStudie-Biobank"), this.searchDomains("Biobank"));
        assertEquals(Set.of("TestStudie-Biobank"), this.searchDomains("samples"));
        assertEquals(Set.of("TestStudie-Biobank"), this.searchDomains("TS-B"));
        assertEquals(Set.of(DOMAIN, "TestStudie-Biobank"), this.searchDomains("*"));
        assertEquals(Set.of(), this.searchDomains("nothing-to-find"));
    }

    /**
     * Tests searching for entities and entity types.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("entitySearchTest")
    public void entitySearchTest() throws Exception {
        String definition = """
                {
                  "attributes": [
                    { "name": "firstName", "type": "string", "required": true, "linkage": false },
                    { "name": "lastName", "type": "string", "required": true, "linkage": false }
                  ],
                  "recordLinkage": { "enabled": false }
                }
                """;
        this.assertCreatedRequest("createBaseTypeForSearch", post("/api/entities/base-types"), null,
                this.entityType("searchable-person", definition, null), this.getAccessToken());
        this.assertCreatedRequest("createProjectTypeForSearch", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.entityType("searchable-participant", definition, "searchable-person"), this.getAccessToken());
        String entityPath = "/api/projects/" + PROJECT + "/entities/searchable-participant";

        // Search for entity types
        assertEquals(Set.of("searchable-person"), this.searchEntityTypes("/api/entities/base-types", "searchable"));
        assertEquals(Set.of("searchable-participant"), this.searchEntityTypes("/api/projects/" + PROJECT + "/entities", "participant"));
        assertEquals(Set.of(), this.searchEntityTypes("/api/projects/" + PROJECT + "/entities", "searchable-person"));
        assertEquals(Set.of(), this.searchEntityTypes("/api/entities/base-types", "nothing-to-find"));

        // Search for entities
        this.assertCreatedRequest("createEntityForSearch", post(entityPath), null, this.entity("{ \"firstName\": \"Erika\", \"lastName\": \"Mustermann\" }"), this.getAccessToken());
        this.assertCreatedRequest("createEntityForSearch", post(entityPath), null, this.entity("{ \"firstName\": \"Max\", \"lastName\": \"Mustermann\" }"), this.getAccessToken());
        this.assertCreatedRequest("createEntityForSearch", post(entityPath), null, this.entity("{ \"firstName\": \"Erika\", \"lastName\": \"Meier\" }"), this.getAccessToken());

        assertEquals(Set.of("Erika Mustermann", "Max Mustermann"), this.searchEntities(entityPath, "mustermann"));
        assertEquals(Set.of("Erika Mustermann"), this.searchEntities(entityPath, "Erika Mustermann"));
        assertEquals(Set.of("Erika Mustermann", "Max Mustermann", "Erika Meier"), this.searchEntities(entityPath, "*"));

        // No matches
        assertEquals(Set.of(), this.searchEntities(entityPath, "Nobody"));
    }

    /**
     * Helper to search for pseudonyms in the test domain.
     *
     * @param query the search query
     * @return the set of identifiers of the found pseudonyms
     * @throws Exception forwards any internally thrown exceptions
     */
    private Set<String> searchPseudonyms(String query) throws Exception {
        MockHttpServletResponse response = this.assertOkRequest("searchPseudonyms", get("/api/domains/" + DOMAIN + "/pseudonyms"), Map.of("query", query), null, this.getAccessToken());

        return this.mapJsonObjectsInStringToList(response.getContentAsString(), PseudonymDTO.class).stream()
                .map(p -> p.getIdentifierItem().getIdentifier()).collect(Collectors.toSet());
    }

    /**
     * Helper to search for domains.
     *
     * @param query the search query
     * @return the set of names of the found domains
     * @throws Exception forwards any internally thrown exceptions
     */
    private Set<String> searchDomains(String query) throws Exception {
        MockHttpServletResponse response = this.assertOkRequest("searchDomains", get("/api/domains"), Map.of("query", query), null, this.getAccessToken());

        return this.mapJsonObjectsInStringToList(response.getContentAsString(), DomainDTO.class).stream()
                .map(DomainDTO::getName).collect(Collectors.toSet());
    }

    /**
     * Helper to search for entity types.
     *
     * @param searchPath the path of the search endpoint
     * @param query the search query
     * @return the set of names of the found entity types
     * @throws Exception forwards any internally thrown exceptions
     */
    private Set<String> searchEntityTypes(String searchPath, String query) throws Exception {
        MockHttpServletResponse response = this.assertOkRequest("searchEntityTypes", get(searchPath), Map.of("query", query), null, this.getAccessToken());

        return this.mapJsonObjectsInStringToList(response.getContentAsString(), EntityTypeDTO.class).stream()
                .map(EntityTypeDTO::getName).collect(Collectors.toSet());
    }

    /**
     * Helper to search for entities.
     *
     * @param entityPath the path of the entity endpoints
     * @param query the search query
     * @return the set of the found entities' names ("firstName lastName")
     * @throws Exception forwards any internally thrown exceptions
     */
    private Set<String> searchEntities(String entityPath, String query) throws Exception {
        MockHttpServletResponse response = this.assertOkRequest("searchEntities", get(entityPath), Map.of("query", query), null, this.getAccessToken());

        return this.mapJsonObjectsInStringToList(response.getContentAsString(), EntityDTO.class).stream()
                .map(e -> e.getData().get("firstName").asString() + " " + e.getData().get("lastName").asString()).collect(Collectors.toSet());
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
