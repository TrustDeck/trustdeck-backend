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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.trustdeck.jooq.generated.Tables.ENTITY;
import static org.trustdeck.jooq.generated.Tables.ENTITY_TYPE;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.dto.EntityDTO;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.model.SortableAttribute;
import org.trustdeck.service.AssertWebRequestService;
import org.trustdeck.service.EntitySortIndexService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * This class offers tests for the paged entity search and the sort indexes.
 *
 * @author Armin Müller
 */
public class TestsEntityPagedSearchServiceIT extends AssertWebRequestService {

    /** The abbreviation of the project that is created for every test. */
    private static final String PROJECT = "TEST";

    /** The path of the entity endpoints of the entity type created by {@link #createTypes(String, String)}. */
    private static final String ENTITY_PATH = "/api/projects/" + PROJECT + "/entities/paged-participant";

    /** The definition of the test types; the city is indexed for sorting (in the base type). */
    private static final String DEFINITION = """
            {
              "attributes": [
                { "name": "firstName", "type": "string", "required": true, "linkage": false },
                { "name": "lastName", "type": "string", "required": true, "linkage": false },
                { "name": "age", "type": "number", "required": false, "linkage": false },
                { "layout": "row", "attributes": [
                  { "name": "birthDate", "type": "date", "required": false, "linkage": false }
                ] },
                { "layout": "group", "name": "address", "attributes": [
                  { "name": "city", "type": "string", "required": false, "linkage": false%s }
                ] },
                { "layout": "group", "name": "contacts", "repeatable": true, "attributes": [
                  { "name": "phone", "type": "string", "required": false, "linkage": false }
                ] }
              ],
              "recordLinkage": { "enabled": false }
            }
            """;

    /** Used to create the type definitions and entity data. */
    private final ObjectMapper om = new ObjectMapper();

    /** Provides the count limit of the search, which a test lowers. */
    @Autowired
    private DefaultProperties defaults;

    /** Synchronizes the sort indexes. */
    @Autowired
    private EntitySortIndexService entitySortIndexService;

    /** Reads the indexes and plans from the database. */
    @Autowired
    private DSLContext dsl;

    /**
     * Tests that pages do not overlap, the default orders, and sorting by the fixed keys and by attributes.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("pagedEntitySearchSortTest")
    public void pagedEntitySearchSortTest() throws Exception {
        this.createTypes("paged-person", "paged-participant");

        // Create 25 entities; some have a city, some have phone numbers
        List<String> created = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            String address = i % 5 == 0 ? "" : ", \"address\": { \"city\": \"City" + (char) ('A' + (i * 7) % 26) + "\" }";
            String contacts = i % 2 == 0 ? ", \"contacts\": [ { \"phone\": \"" + (100 + i) + "\" }, { \"phone\": \"000\" } ]" : "";
            created.add(this.createEntity("{ \"firstName\": \"Person\", \"lastName\": \"Name" + i + "\", \"age\": " + i + address + contacts + " }"));
        }

        // Pages split the matches without overlapping
        JsonNode first = this.searchPage(Map.of("query", "*", "page", "0", "size", "10"));
        assertEquals(10, first.get("items").size());
        assertEquals(25, first.get("total").asLong());
        assertFalse(first.get("numberOfMatchesExceedsSearchLimit").asBoolean());
        assertEquals(10, first.get("size").asInt());
        Set<String> seen = new HashSet<>();
        for (int page = 0; page < 3; page++) {
            this.searchPage(Map.of("query", "*", "page", String.valueOf(page), "size", "10")).get("items")
                    .forEach(item -> seen.add(item.get("trustdeckID").asString()));
        }
        assertEquals(25, seen.size());
        assertEquals(5, this.searchPage(Map.of("query", "*", "page", "2", "size", "10")).get("items").size());

        // "*" is sorted by the update time, newest first
        assertEquals(created.reversed(), this.ids(Map.of("query", "*")));
        assertEquals(created, this.ids(Map.of("query", "*", "sort", "updatedAt", "direction", "asc")));

        // Relevance ignores the direction; for "*" it falls back to the update time, newest first
        assertEquals(created.reversed(), this.ids(Map.of("query", "*", "sort", "relevance", "direction", "asc")));

        // Equally relevant matches keep the order of their creation
        assertEquals(created, this.ids(Map.of("query", "person")));

        // Fixed keys in both directions
        assertEquals(created, this.ids(Map.of("query", "*", "sort", "createdAt")));
        assertEquals(created.reversed(), this.ids(Map.of("query", "*", "sort", "createdAt", "direction", "desc")));
        List<String> byTrustDeckId = new ArrayList<>(created);
        byTrustDeckId.sort(Comparator.naturalOrder());
        assertEquals(byTrustDeckId, this.ids(Map.of("query", "*", "sort", "trustDeckId")));
        assertEquals(byTrustDeckId.reversed(), this.ids(Map.of("query", "*", "sort", "trustDeckId", "direction", "DESC")));

        // Numbers are sorted numerically (9 before 10)
        List<Integer> ages = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            ages.add(i);
        }
        assertEquals(ages, this.values(Map.of("query", "*", "sort", "attribute:age"), "age").stream().map(Integer::valueOf).toList());
        assertEquals(ages.reversed(), this.values(Map.of("query", "*", "sort", "attribute:age", "direction", "desc"), "age").stream().map(Integer::valueOf).toList());

        // Strings are sorted as text ("Name10" before "Name2")
        List<String> names = this.values(Map.of("query", "*", "sort", "attribute:lastName"), "lastName");
        assertEquals("Name1", names.get(0));
        assertEquals("Name10", names.get(1));
        assertEquals("Name9", names.get(24));
        assertEquals(names.reversed(), this.values(Map.of("query", "*", "sort", "attribute:lastName", "direction", "desc"), "lastName"));

        // Attributes in groups are sorted by their path; missing values come last in both directions
        List<String> cities = this.values(Map.of("query", "*", "sort", "attribute:address.city"), "address.city");
        List<String> descendingCities = this.values(Map.of("query", "*", "sort", "attribute:address.city", "direction", "desc"), "address.city");
        List<String> expectedCities = cities.subList(0, 20).stream().sorted().toList();
        assertEquals(expectedCities, cities.subList(0, 20));
        assertEquals(expectedCities.reversed(), descendingCities.subList(0, 20));
        assertEquals(List.of("", "", "", "", ""), cities.subList(20, 25));
        assertEquals(List.of("", "", "", "", ""), descendingCities.subList(20, 25));

        // Repeatable groups are sorted by their first element
        List<String> phones = this.values(Map.of("query", "*", "sort", "attribute:contacts.phone", "direction", "desc"), "contacts.phone");
        assertEquals("124", phones.get(0));
        assertEquals("122", phones.get(1));
        assertEquals("102", phones.get(11));
        assertEquals("", phones.get(12));

        // Attributes in rows are sorted by their name alone
        this.searchPage(Map.of("query", "*", "sort", "attribute:birthDate"));
    }

    /**
     * Tests prefix and contains matching, the lookup by TrustDeck ID, and the ranking by relevance.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("pagedEntitySearchMatchTest")
    public void pagedEntitySearchMatchTest() throws Exception {
        this.createTypes("paged-person", "paged-participant");
        String meier = this.createEntity("{ \"firstName\": \"Erika\", \"lastName\": \"Meier\" }");
        String mustermann = this.createEntity("{ \"firstName\": \"Max\", \"lastName\": \"Mustermann-Schmidt\" }");
        String erikason = this.createEntity("{ \"firstName\": \"Erika\", \"lastName\": \"Erikason\", \"address\": { \"city\": \"Erikastadt\" } }");

        // Prefix matching finds the beginning of words; every part must match
        assertEquals(Set.of(mustermann), Set.copyOf(this.ids(Map.of("query", "must"))));
        assertEquals(Set.of(mustermann), Set.copyOf(this.ids(Map.of("query", "SCHMI"))));
        assertEquals(Set.of(mustermann), Set.copyOf(this.ids(Map.of("query", "Mustermann-Schmidt"))));
        assertEquals(Set.of(meier), Set.copyOf(this.ids(Map.of("query", "erika mei"))));
        assertEquals(Set.of(), Set.copyOf(this.ids(Map.of("query", "ustermann"))));
        assertEquals(Set.of(), Set.copyOf(this.ids(Map.of("query", "!!!"))));

        // Contains matching finds any part of the data
        assertEquals(Set.of(mustermann), Set.copyOf(this.ids(Map.of("query", "ustermann", "match", "contains"))));
        assertEquals(Set.of(), Set.copyOf(this.ids(Map.of("query", "ust%mann", "match", "contains"))));

        // A TrustDeck ID finds its entity
        assertEquals(List.of(meier), this.ids(Map.of("query", meier)));
        assertEquals(List.of(meier), this.ids(Map.of("query", meier.toUpperCase(), "match", "contains")));

        // The entity that matches more often is more relevant, although it was created later
        assertEquals(List.of(erikason, meier), this.ids(Map.of("query", "erika")));
        assertEquals(List.of(erikason, meier), this.ids(Map.of("query", "erika", "sort", "relevance", "direction", "asc")));

        // Other orders apply to the matches
        assertEquals(List.of(meier, erikason), this.ids(Map.of("query", "erika", "sort", "createdAt")));
    }

    /**
     * Tests that invalid parameters are rejected.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("pagedEntitySearchBadRequestTest")
    public void pagedEntitySearchBadRequestTest() throws Exception {
        this.createTypes("paged-person", "paged-participant");

        for (Map<String, String> params : List.of(
                Map.of("query", "*", "page", "-1"),
                Map.of("query", "*", "page", "0", "size", "0"),
                Map.of("query", "*", "page", "0", "size", "101"),
                Map.of("query", "*", "page", "0", "match", "fuzzy"),
                Map.of("query", "ab cde", "page", "0", "match", "contains"),
                Map.of("query", "*", "page", "0", "sort", "name"),
                Map.of("query", "*", "page", "0", "sort", "createdAt", "direction", "up"),
                Map.of("query", "*", "page", "0", "sort", "attribute:unknown"),
                Map.of("query", "*", "page", "0", "sort", "attribute:address"),
                Map.of("query", "*", "page", "0", "sort", "attribute:city"))) {
            this.assertBadRequestRequest("searchEntitiesPageBadRequest", get(ENTITY_PATH), params, null, this.getAccessToken());
        }
    }

    /**
     * Tests that sorting by an attribute without a sort index is refused when more entities match
     * than the count limit, and that the other orders still work.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("pagedEntitySearchCountLimitTest")
    public void pagedEntitySearchCountLimitTest() throws Exception {
        this.createTypes("paged-person", "paged-participant");
        List<String> created = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            created.add(this.createEntity("{ \"firstName\": \"Person\", \"lastName\": \"Limit" + i + "\", \"age\": " + i
                    + ", \"address\": { \"city\": \"City" + i + "\" } }"));
        }

        int countLimit = defaults.getEntity().getSearchCountLimit();
        defaults.getEntity().setSearchCountLimit(3);
        try {
            // Not indexed: refused for too many matches, but allowed for a narrower search
            this.assertUnprocessableEntity("searchEntitiesPageUnindexedSort", get(ENTITY_PATH),
                    Map.of("query", "*", "page", "0", "sort", "attribute:age"), null, this.getAccessToken());
            this.assertUnprocessableEntity("searchEntitiesPageUnindexedSort", get(ENTITY_PATH),
                    Map.of("query", "person", "page", "0", "sort", "attribute:age"), null, this.getAccessToken());
            assertEquals(List.of("1"), this.values(Map.of("query", "limit1", "sort", "attribute:age"), "age"));

            // Indexed and fixed keys are allowed; the total stops at the count limit
            JsonNode byCity = this.searchPage(Map.of("query", "*", "page", "0", "sort", "attribute:address.city", "direction", "desc"));
            assertEquals(3, byCity.get("total").asLong());
            assertTrue(byCity.get("numberOfMatchesExceedsSearchLimit").asBoolean());
            assertEquals(5, byCity.get("items").size());
            assertEquals("City5", byCity.get("items").get(0).get("data").get("address").get("city").asString());
            assertEquals(created, this.ids(Map.of("query", "*", "sort", "createdAt")));

            // Relevance ranks only the first matches up to the count limit
            assertEquals(created.subList(0, 3), this.ids(Map.of("query", "person")));
        } finally {
            defaults.getEntity().setSearchCountLimit(countLimit);
        }
    }

    /**
     * Tests that the sort indexes follow the type definition and are used by the searches.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("entitySortIndexSyncTest")
    public void entitySortIndexSyncTest() throws Exception {
        this.createTypes("paged-person", "paged-participant");
        this.assertCreatedRequest("createProjectTypeForPagedSearch", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.entityType("paged-other", DEFINITION.formatted(""), "paged-person"), this.getAccessToken());
        int participantId = this.typeId("paged-participant");
        int otherId = this.typeId("paged-other");
        int baseId = this.typeId("paged-person");
        assertTrue(entitySortIndexService.awaitBackgroundSyncs(Duration.ofMinutes(1)));

        // The derived types inherit the indexed city from the base type; the base type itself has no indexes
        assertEquals(2, this.sortIndexes(participantId).size());
        assertEquals(2, this.sortIndexes(otherId).size());
        assertEquals(0, this.sortIndexes(baseId).size());

        // The searches use the indexes
        SortableAttribute city = entitySortIndexService.getSortableAttributes(participantId).get("address.city");
        assertTrue(city.sortIndex());
        Field<?> expression = EntitySortIndexService.sortExpression(city);
        Set<String> indexes = this.sortIndexes(participantId).keySet();
        String ascending = indexes.stream().filter(name -> !name.endsWith("_desc")).findFirst().orElseThrow();
        assertTrue(this.plan(participantId, expression.asc().nullsLast()).contains("using " + ascending + " on"));
        assertTrue(this.plan(participantId, expression.desc().nullsLast()).contains("using " + ascending + "_desc on"));

        // Removing the flag drops the indexes
        EntityTypeDTO update = new EntityTypeDTO();
        update.setTypeDefinition(om.readTree(DEFINITION.formatted(", \"sortIndex\": false")));
        this.assertOkRequest("updateTypeSortIndex", put("/api/projects/" + PROJECT + "/entities/config/paged-other"), null, update, this.getAccessToken());
        assertTrue(entitySortIndexService.syncSortIndexes(otherId));
        assertEquals(0, this.sortIndexes(otherId).size());
        assertFalse(entitySortIndexService.getSortableAttributes(otherId).get("address.city").sortIndex());

        // An invalid index (left over by a failed concurrent build) is rebuilt
        dsl.execute("UPDATE pg_index SET indisvalid = false WHERE indexrelid = to_regclass({0})", DSL.inline("public." + ascending));
        assertFalse(this.sortIndexes(participantId).get(ascending));
        assertTrue(entitySortIndexService.syncSortIndexes(participantId));
        assertEquals(Map.of(ascending, true, ascending + "_desc", true), this.sortIndexes(participantId));
    }

    /**
     * Helper to create the base type and the project-specific type of the tests.
     *
     * @param baseTypeName the name of the base type
     * @param typeName the name of the project-specific type
     * @throws Exception forwards any internally thrown exceptions
     */
    private void createTypes(String baseTypeName, String typeName) throws Exception {
        this.assertCreatedRequest("createBaseTypeForPagedSearch", post("/api/entities/base-types"), null,
                this.entityType(baseTypeName, DEFINITION.formatted(", \"sortIndex\": true"), null), this.getAccessToken());
        this.assertCreatedRequest("createProjectTypeForPagedSearch", post("/api/projects/" + PROJECT + "/entities/config"), null,
                this.entityType(typeName, DEFINITION.formatted(""), baseTypeName), this.getAccessToken());
    }

    /**
     * Helper to search one page of entities of the test type.
     *
     * @param params the request parameters (page defaults to 0, size to 100)
     * @return the returned page
     * @throws Exception forwards any internally thrown exceptions
     */
    private JsonNode searchPage(Map<String, String> params) throws Exception {
        Map<String, String> all = new HashMap<>(params);
        all.putIfAbsent("page", "0");
        all.putIfAbsent("size", "100");
        MockHttpServletResponse response = this.assertOkRequest("searchEntitiesPage", get(ENTITY_PATH), all, null, this.getAccessToken());
        return om.readTree(response.getContentAsString());
    }

    /**
     * Helper to search the TrustDeck IDs of the matching entities in their order.
     *
     * @param params the request parameters
     * @return the TrustDeck IDs
     * @throws Exception forwards any internally thrown exceptions
     */
    private List<String> ids(Map<String, String> params) throws Exception {
        List<String> ids = new ArrayList<>();
        this.searchPage(params).get("items").forEach(item -> ids.add(item.get("trustdeckID").asString()));
        return ids;
    }

    /**
     * Helper to search the values of an attribute of the matching entities in their order.
     *
     * @param params the request parameters
     * @param path the attribute's path; the first element of a repeatable group is used
     * @return the values; an empty string for a missing value
     * @throws Exception forwards any internally thrown exceptions
     */
    private List<String> values(Map<String, String> params, String path) throws Exception {
        List<String> values = new ArrayList<>();
        for (JsonNode item : this.searchPage(params).get("items")) {
            JsonNode node = item.get("data");
            for (String name : path.split("\\.")) {
                node = node.path(name);
                if (node.isArray()) {
                    node = node.path(0);
                }
            }
            values.add(node.isMissingNode() ? "" : node.asString());
        }
        return values;
    }

    /**
     * Helper to create an entity of the test type.
     *
     * @param data the entity's data as JSON string
     * @return the entity's TrustDeck ID
     * @throws Exception forwards any internally thrown exceptions
     */
    private String createEntity(String data) throws Exception {
        EntityDTO entity = new EntityDTO();
        entity.setData(om.readTree(data));
        MockHttpServletResponse response = this.assertCreatedRequest("createEntityForPagedSearch", post(ENTITY_PATH), null, entity, this.getAccessToken());
        return om.readTree(response.getContentAsString()).get("trustdeckID").asString();
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
     * Helper to read the ID of an entity type.
     *
     * @param name the name of the entity type
     * @return the ID
     */
    private int typeId(String name) {
        return dsl.select(ENTITY_TYPE.ID).from(ENTITY_TYPE).where(ENTITY_TYPE.NAME.eq(name)).fetchOne(ENTITY_TYPE.ID);
    }

    /**
     * Helper to read the sort indexes of an entity type's partition.
     *
     * @param entityTypeId the ID of the entity type
     * @return whether each index is valid, keyed by the index name
     */
    private Map<String, Boolean> sortIndexes(int entityTypeId) {
        return dsl.fetch("SELECT c.relname, i.indisvalid FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid "
                + "WHERE i.indrelid = to_regclass({0}) AND starts_with(c.relname, {1})",
                DSL.inline("public.entity_t" + entityTypeId), DSL.inline("entity_t" + entityTypeId + "_sort_"))
                .intoMap(r -> r.get(0, String.class), r -> r.get(1, Boolean.class));
    }

    /**
     * Helper to explain a query that reads the first entities of a type in the given order,
     * forbidding sorting, so that the plan shows whether an index provides the order.
     *
     * @param entityTypeId the ID of the entity type
     * @param order the order
     * @return the plan
     */
    private String plan(int entityTypeId, org.jooq.SortField<?> order) {
        String query = dsl.renderInlined(dsl.select(ENTITY.ID)
                .from(ENTITY)
                .where(ENTITY.ENTITY_TYPE_ID.eq(DSL.inline(entityTypeId)))
                .and(ENTITY.IS_DELETED.eq(DSL.inline(false)))
                .orderBy(order, ENTITY.ID.asc())
                .limit(DSL.inline(10)));
        return dsl.connectionResult(connection -> {
            StringBuilder plan = new StringBuilder();
            try (var statement = connection.createStatement()) {
                statement.execute("SET enable_sort = off");
                statement.execute("SET enable_seqscan = off");
                try (var result = statement.executeQuery("EXPLAIN " + query)) {
                    while (result.next()) {
                        plan.append(result.getString(1)).append('\n');
                    }
                }
                statement.execute("RESET enable_sort");
                statement.execute("RESET enable_seqscan");
            }
            return plan.toString();
        });
    }
}
