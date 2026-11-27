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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.trustdeck.jooq.generated.Tables.DOMAIN;
import static org.trustdeck.jooq.generated.Tables.ENTITY;
import static org.trustdeck.jooq.generated.Tables.DOMAIN_PSEUDONYM_COUNTS;
import static org.trustdeck.jooq.generated.Tables.ENTITY_COUNT;
import static org.trustdeck.jooq.generated.Tables.ENTITY_COUNT_DELTA;
import static org.trustdeck.jooq.generated.Tables.ENTITY_TYPE;
import static org.trustdeck.jooq.generated.Tables.PROJECT;
import static org.trustdeck.jooq.generated.Tables.PSEUDONYM;
import static org.trustdeck.jooq.generated.Tables.ENTITY_TYPE_ENTITY_COUNTS;
import static org.trustdeck.jooq.generated.Tables.PSEUDONYM_COUNT;
import static org.trustdeck.jooq.generated.Tables.PSEUDONYM_COUNT_DELTA;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.trustdeck.dto.EntityDTO;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.dto.ProjectStatisticsDTO;
import org.trustdeck.dto.ProjectStatisticsDTO.NamedCount;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.jooq.generated.tables.records.DomainRecord;
import org.trustdeck.dto.SystemStatisticsDTO;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.service.AssertWebRequestService;
import org.trustdeck.service.CountFoldService;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the project and system statistics endpoints and the counters behind them.
 *
 * @author Armin Müller
 */
public class TestsStatisticsServiceIT extends AssertWebRequestService {

    /** The type definition of the base type and of the project type extending it. */
    private static final String TYPE_DEFINITION = """
            {
              "attributes": [
                { "name": "name", "type": "string", "required": true, "linkage": false, "minLength": 1, "maxLength": 50 }
              ],
              "recordLinkage": { "enabled": false }
            }
            """;

    /** Reads the counter tables and the counted tables directly. */
    @Autowired
    private DSLContext dsl;

    /** Folds the logged count changes into the counter tables. */
    @Autowired
    private CountFoldService countFoldService;

    /** Used to create the type definitions and entity data. */
    private final ObjectMapper om = new ObjectMapper();

    /**
     * Checks project identity, UTC generation time, the counts, and not-found behavior.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    public void projectStatisticsTest() throws Exception {
        ProjectStatisticsDTO statistics = getProjectStatistics("projectStatistics");

        assertNotNull(statistics.getGeneratedAt());
        assertEquals(ZoneOffset.UTC, statistics.getGeneratedAt().atOffset(ZoneOffset.UTC).getOffset());
        assertEquals("TEST", statistics.getProject().getAbbreviation());
        assertEquals("Test Study", statistics.getProject().getName());
        assertEquals(1L, statistics.getCounts().getDomains());
        assertEquals(0L, statistics.getCounts().getEntityTypes());
        assertEquals(0L, statistics.getCounts().getEntities());
        assertEquals(1L, statistics.getCounts().getPseudonyms());
        assertEquals(List.of(), statistics.getEntitiesByType());
        assertEquals(1, statistics.getPseudonymsByDomain().size());
        assertEquals("TestStudie", statistics.getPseudonymsByDomain().getFirst().getName());
        assertEquals(1L, statistics.getPseudonymsByDomain().getFirst().getCount());

        // Requests on non-existing projects are forbidden, so that their existence is not revealed
        assertForbiddenRequest("projectStatisticsNotFound", get("/api/projects/UNKNOWN/statistics"), null, null, getAccessToken());
        assertUnauthorizedRequest("projectStatisticsWithoutToken", get("/api/projects/TEST/statistics"), null, null, "");
        assertUnauthorizedRequest("projectStatisticsUnauthenticated", get("/api/projects/TEST/statistics"), null, null, "SomeToken");
    }

    /**
     * Checks that the pseudonym counters follow single and batch inserts, single and batch deletions,
     * pseudonyms moved to another domain, and the deletion of the domain.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    public void pseudonymCountersTest() throws Exception {
        String domain = "TestStudie";
        int domainId = dsl.select(DOMAIN.ID).from(DOMAIN).where(DOMAIN.NAME.eq(domain)).fetchSingle(DOMAIN.ID);
        assertPseudonymCounters();

        // Batch insert
        List<PseudonymDTO> batch = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            PseudonymDTO pseudonym = new PseudonymDTO();
            pseudonym.setIdentifierItem(IdentifierItem.builder().identifier("counter-" + i).idType("ANY-ID").build());
            pseudonym.setPsn("TS-COUNTER-" + i);
            batch.add(pseudonym);
        }
        assertCreatedRequest("pseudonymCountersBatch", post("/api/domains/" + domain + "/pseudonyms/batch"), null, batch, getAccessToken());
        assertEquals(4L, getProjectStatistics("pseudonymCountersAfterBatch").getCounts().getPseudonyms());
        assertPseudonymCounters();

        // Single deletion
        assertNoContent("pseudonymCountersDelete", delete("/api/domains/" + domain + "/pseudonyms"),
                Map.of("id", "counter-1", "idType", "ANY-ID"), null, getAccessToken());
        assertEquals(3L, getProjectStatistics("pseudonymCountersAfterDelete").getPseudonymsByDomain().getFirst().getCount());
        assertPseudonymCounters();

        // A pseudonym moved into another domain leaves the old domain's count and enters the new one's
        DomainRecord other = dsl.selectFrom(DOMAIN).where(DOMAIN.ID.eq(domainId)).fetchSingle().copy();
        other.setName("TestStudie-Other");
        other.setPrefix("TO-");
        other.store();
        int otherId = other.getId();
        dsl.update(PSEUDONYM).set(PSEUDONYM.DOMAINID, otherId).where(PSEUDONYM.IDENTIFIER.eq("counter-2")).execute();
        assertEquals(List.of(2L, 1L), getProjectStatistics("pseudonymCountersAfterMove").getPseudonymsByDomain().stream()
                .map(NamedCount::getCount).toList());
        assertPseudonymCounters();

        // Updates that leave the domain unchanged write no log entries
        int logged = dsl.fetchCount(PSEUDONYM_COUNT_DELTA);
        dsl.update(PSEUDONYM).set(PSEUDONYM.VALIDTO, LocalDateTime.of(2050, 1, 1, 0, 0)).execute();
        assertEquals(logged, dsl.fetchCount(PSEUDONYM_COUNT_DELTA));
        assertPseudonymCounters();

        // A multi-row insert counts only the rows actually inserted, not the skipped duplicates
        dsl.insertInto(PSEUDONYM, PSEUDONYM.IDENTIFIER, PSEUDONYM.IDTYPE, PSEUDONYM.PSEUDONYM_, PSEUDONYM.VALIDFROM,
                        PSEUDONYM.VALIDFROMINHERITED, PSEUDONYM.VALIDTO, PSEUDONYM.VALIDTOINHERITED, PSEUDONYM.DOMAINID)
                .select(dsl.select(PSEUDONYM.IDENTIFIER, PSEUDONYM.IDTYPE, PSEUDONYM.PSEUDONYM_, PSEUDONYM.VALIDFROM,
                        PSEUDONYM.VALIDFROMINHERITED, PSEUDONYM.VALIDTO, PSEUDONYM.VALIDTOINHERITED, PSEUDONYM.DOMAINID)
                        .from(PSEUDONYM).where(PSEUDONYM.DOMAINID.eq(domainId)))
                .onConflictDoNothing()
                .execute();
        assertPseudonymCounters();

        // A rolled-back insert leaves the counts unchanged
        long before = getProjectStatistics("pseudonymCountersBeforeRollback").getCounts().getPseudonyms();
        try {
            dsl.transaction(configuration -> {
                DSL.using(configuration).insertInto(PSEUDONYM, PSEUDONYM.IDENTIFIER, PSEUDONYM.IDTYPE, PSEUDONYM.PSEUDONYM_,
                                PSEUDONYM.VALIDFROM, PSEUDONYM.VALIDFROMINHERITED, PSEUDONYM.VALIDTO, PSEUDONYM.VALIDTOINHERITED, PSEUDONYM.DOMAINID)
                        .values("rolled-back", "ANY-ID", "TS-ROLLED-BACK", LocalDateTime.now(), true, LocalDateTime.of(2050, 1, 1, 0, 0), true, domainId)
                        .execute();
                throw new IllegalStateException("roll back");
            });
        } catch (IllegalStateException expected) {
            // The transaction was rolled back
        }
        assertEquals(before, getProjectStatistics("pseudonymCountersAfterRollback").getCounts().getPseudonyms());
        assertPseudonymCounters();

        // Batch deletion of all pseudonyms in the domain
        assertNoContent("pseudonymCountersDeleteBatch", delete("/api/domains/" + domain + "/pseudonyms/batch"), null, null, getAccessToken());
        assertEquals(1L, getProjectStatistics("pseudonymCountersAfterBatchDelete").getCounts().getPseudonyms());
        assertPseudonymCounters();

        // Folding keeps the counts and empties the log
        countFoldService.foldCountDeltas();
        assertEquals(0, dsl.fetchCount(PSEUDONYM_COUNT_DELTA));
        assertPseudonymCounters();

        // Deleting the domain removes its counter with the next fold
        assertNoContent("pseudonymCountersDeleteDomain", delete("/api/domains"), Map.of("name", domain), null, getAccessToken());
        assertPseudonymCounters();
        countFoldService.foldCountDeltas();
        assertEquals(0, dsl.fetchCount(PSEUDONYM_COUNT, PSEUDONYM_COUNT.DOMAIN_ID.eq(domainId)));
        assertEquals(0, dsl.fetchCount(PSEUDONYM_COUNT_DELTA));
        assertPseudonymCounters();
    }

    /**
     * Checks that the entity counters follow created, updated, soft-deleted, and restored entities of
     * project entity types, ignore entities of global base types, and are reset by TRUNCATE.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    public void entityCountersTest() throws Exception {
        assertCreatedRequest("entityCountersBaseType", post("/api/entities/base-types"), null,
                createEntityType("counted-base", null), getAccessToken());
        assertCreatedRequest("entityCountersProjectType", post("/api/projects/TEST/entities/config"), null,
                createEntityType("counted-type", "counted-base"), getAccessToken());
        String entityPath = "/api/projects/TEST/entities/counted-type";

        // Project type without entities: listed with zero
        ProjectStatisticsDTO statistics = getProjectStatistics("entityCountersEmpty");
        assertEquals(1L, statistics.getCounts().getEntityTypes());
        assertEquals(List.of(new NamedCount("counted-type", 0L)), statistics.getEntitiesByType());

        List<String> ids = new ArrayList<>();
        for (String name : List.of("Erika", "Max", "Lea")) {
            EntityDTO entity = new EntityDTO();
            entity.setData(om.readTree("{ \"name\": \"" + name + "\" }"));
            MockHttpServletResponse response = assertCreatedRequest("entityCountersCreate", post(entityPath), null, entity, getAccessToken());
            ids.add(applySingleJsonContentToClass(response.getContentAsString(), EntityDTO.class).getTrustdeckID().toString());
        }
        assertEquals(3L, getProjectStatistics("entityCountersCreated").getCounts().getEntities());
        assertEntityCounters();

        // Soft deletion: the entity stays in the table but is no longer counted
        assertNoContent("entityCountersDelete", delete(entityPath + "/" + ids.getFirst()), null, null, getAccessToken());
        statistics = getProjectStatistics("entityCountersDeleted");
        assertEquals(2L, statistics.getCounts().getEntities());
        assertEquals(List.of(new NamedCount("counted-type", 2L)), statistics.getEntitiesByType());
        assertEntityCounters();

        // Entities of a global base type are not counted
        int projectId = dsl.select(PROJECT.ID).from(PROJECT).where(PROJECT.ABBREVIATION.eq("TEST")).fetchSingle(PROJECT.ID);
        int baseTypeId = dsl.select(ENTITY_TYPE.ID).from(ENTITY_TYPE).where(ENTITY_TYPE.NAME.eq("counted-base")).fetchSingle(ENTITY_TYPE.ID);
        int typeId = dsl.select(ENTITY_TYPE.ID).from(ENTITY_TYPE).where(ENTITY_TYPE.NAME.eq("counted-type")).fetchSingle(ENTITY_TYPE.ID);
        dsl.insertInto(ENTITY, ENTITY.PROJECT_ID, ENTITY.ENTITY_TYPE_ID, ENTITY.DATA)
                .values(projectId, baseTypeId, JSONB.valueOf("{\"name\": \"Global\"}")).execute();
        statistics = getProjectStatistics("entityCountersGlobal");
        assertEquals(1L, statistics.getCounts().getEntityTypes());
        assertEquals(2L, statistics.getCounts().getEntities());
        assertEquals(List.of(new NamedCount("counted-type", 2L)), statistics.getEntitiesByType());
        assertEntityCounters();

        // A multi-row insert counts only the rows actually inserted, not the skipped duplicates
        dsl.insertInto(ENTITY, ENTITY.PROJECT_ID, ENTITY.ENTITY_TYPE_ID, ENTITY.DATA)
                .values(projectId, typeId, JSONB.valueOf("{\"name\": \"Max\"}"))
                .values(projectId, typeId, JSONB.valueOf("{\"name\": \"Mia\"}"))
                .values(projectId, typeId, JSONB.valueOf("{\"name\": \"Ben\"}"))
                .onConflictDoNothing()
                .execute();
        assertEquals(4L, getProjectStatistics("entityCountersMultiRowInsert").getCounts().getEntities());
        assertEntityCounters();

        // Plain data updates write no log entries
        int logged = dsl.fetchCount(ENTITY_COUNT_DELTA);
        dsl.update(ENTITY).set(ENTITY.DATA, JSONB.valueOf("{\"name\": \"Mia Maria\"}"))
                .where(ENTITY.ENTITY_TYPE_ID.eq(typeId)).and(ENTITY.DATA.cast(String.class).contains("Mia")).execute();
        assertEquals(logged, dsl.fetchCount(ENTITY_COUNT_DELTA));

        // A set-based soft deletion of several entities, and restoring one of them
        dsl.update(ENTITY).set(ENTITY.IS_DELETED, true).where(ENTITY.ENTITY_TYPE_ID.eq(typeId)).and(ENTITY.IS_DELETED.isFalse()).execute();
        assertEquals(0L, getProjectStatistics("entityCountersSoftDeleteBatch").getCounts().getEntities());
        assertEntityCounters();
        dsl.update(ENTITY).set(ENTITY.IS_DELETED, false).where(ENTITY.TRUSTDECK_ID.eq(java.util.UUID.fromString(ids.getLast()))).execute();
        assertEquals(1L, getProjectStatistics("entityCountersRestored").getCounts().getEntities());
        assertEntityCounters();

        // Folding keeps the counts and empties the log
        countFoldService.foldCountDeltas();
        assertEquals(0, dsl.fetchCount(ENTITY_COUNT_DELTA));
        assertEquals(1L, getProjectStatistics("entityCountersFolded").getCounts().getEntities());
        assertEntityCounters();

        // TRUNCATE resets the counters
        dsl.truncate(ENTITY).execute();
        assertEquals(0, dsl.fetchCount(ENTITY_COUNT));
        assertEquals(0, dsl.fetchCount(ENTITY_COUNT_DELTA));
        assertEquals(0L, getProjectStatistics("entityCountersTruncated").getCounts().getEntities());
    }

    /**
     * Checks that two transactions inserting pseudonyms into the same domain don't wait for each
     * other because of the counters, and that a fold during an open transaction keeps its changes.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    public void concurrentCounterWritesTest() throws Exception {
        int domainId = dsl.select(DOMAIN.ID).from(DOMAIN).where(DOMAIN.NAME.eq("TestStudie")).fetchSingle(DOMAIN.ID);
        ConnectionProvider connections = dsl.configuration().connectionProvider();
        Connection first = connections.acquire();
        Connection second = connections.acquire();
        try {
            first.setAutoCommit(false);
            second.setAutoCommit(false);
            insertPseudonym(DSL.using(first, dsl.dialect()), "concurrent-1", domainId);

            // Would fail with a lock timeout if the second insert waited for the first transaction
            DSL.using(second, dsl.dialect()).execute("SET LOCAL lock_timeout = '2s'");
            insertPseudonym(DSL.using(second, dsl.dialect()), "concurrent-2", domainId);
            second.commit();

            // A fold while the first transaction is still open moves only committed changes
            countFoldService.foldCountDeltas();
            first.commit();
        } finally {
            first.setAutoCommit(true);
            second.setAutoCommit(true);
            connections.release(first);
            connections.release(second);
        }

        assertEquals(3L, getProjectStatistics("concurrentCounterWrites").getCounts().getPseudonyms());
        assertPseudonymCounters();
        countFoldService.foldCountDeltas();
        assertEquals(0, dsl.fetchCount(PSEUDONYM_COUNT_DELTA));
        assertPseudonymCounters();
    }

    /**
     * Inserts a pseudonym directly into the database.
     *
     * @param context the context of the connection to use
     * @param identifier the identifier, also used for the pseudonym
     * @param domainId the ID of the domain
     */
    private void insertPseudonym(DSLContext context, String identifier, int domainId) {
        context.insertInto(PSEUDONYM, PSEUDONYM.IDENTIFIER, PSEUDONYM.IDTYPE, PSEUDONYM.PSEUDONYM_, PSEUDONYM.VALIDFROM,
                        PSEUDONYM.VALIDFROMINHERITED, PSEUDONYM.VALIDTO, PSEUDONYM.VALIDTOINHERITED, PSEUDONYM.DOMAINID)
                .values(identifier, "ANY-ID", "TS-" + identifier, LocalDateTime.now(), true, LocalDateTime.of(2050, 1, 1, 0, 0), true, domainId)
                .execute();
    }

    /**
     * Reads the statistics of the test project.
     *
     * @param identifier the identifier of the request in the documentation
     * @return the statistics
     * @throws Exception forwards any internally thrown exceptions
     */
    private ProjectStatisticsDTO getProjectStatistics(String identifier) throws Exception {
        MockHttpServletResponse response = assertOkRequest(identifier, get("/api/projects/TEST/statistics"), null, null, getAccessToken());
        return applySingleJsonContentToClass(response.getContentAsString(), ProjectStatisticsDTO.class);
    }

    /** Asserts that every domain's pseudonym count, folded counter plus log, equals the number of its pseudonyms. */
    private void assertPseudonymCounters() {
        Map<Integer, Long> counted = dsl.select(DOMAIN_PSEUDONYM_COUNTS.DOMAIN_ID, DOMAIN_PSEUDONYM_COUNTS.RECORD_COUNT)
                .from(DOMAIN_PSEUDONYM_COUNTS).where(DOMAIN_PSEUDONYM_COUNTS.RECORD_COUNT.ne(0L))
                .fetchMap(DOMAIN_PSEUDONYM_COUNTS.DOMAIN_ID, DOMAIN_PSEUDONYM_COUNTS.RECORD_COUNT);
        Map<Integer, Long> actual = dsl.select(PSEUDONYM.DOMAINID, DSL.count().cast(Long.class)).from(PSEUDONYM)
                .where(PSEUDONYM.DOMAINID.isNotNull()).groupBy(PSEUDONYM.DOMAINID).fetchMap(r -> r.value1(), r -> r.value2());
        assertEquals(actual, counted);
    }

    /**
     * Asserts that every project-owned entity type's count, folded counter plus log, equals the
     * number of its non-deleted entities in its project.
     */
    private void assertEntityCounters() {
        Map<Integer, Long> counted = dsl.select(ENTITY_TYPE_ENTITY_COUNTS.ENTITY_TYPE_ID, ENTITY_TYPE_ENTITY_COUNTS.RECORD_COUNT)
                .from(ENTITY_TYPE_ENTITY_COUNTS).where(ENTITY_TYPE_ENTITY_COUNTS.RECORD_COUNT.ne(0L))
                .fetchMap(ENTITY_TYPE_ENTITY_COUNTS.ENTITY_TYPE_ID, ENTITY_TYPE_ENTITY_COUNTS.RECORD_COUNT);
        Map<Integer, Long> actual = dsl.select(ENTITY.ENTITY_TYPE_ID, DSL.count().cast(Long.class)).from(ENTITY)
                .join(ENTITY_TYPE).on(ENTITY_TYPE.ID.eq(ENTITY.ENTITY_TYPE_ID)).and(ENTITY_TYPE.PROJECT_ID.eq(ENTITY.PROJECT_ID))
                .where(ENTITY.IS_DELETED.isFalse()).groupBy(ENTITY.ENTITY_TYPE_ID)
                .fetchMap(r -> r.value1(), r -> r.value2());
        assertEquals(actual, counted);
    }

    /**
     * Creates an entity type with a single required name attribute.
     *
     * @param name the name of the entity type
     * @param baseTypeName the base type of a project type, {@code null} for a base type
     * @return the entity type DTO
     * @throws Exception forwards any internally thrown exceptions
     */
    private EntityTypeDTO createEntityType(String name, String baseTypeName) throws Exception {
        EntityTypeDTO entityType = new EntityTypeDTO();
        entityType.setName(name);
        entityType.setTypeDefinition(om.readTree(TYPE_DEFINITION));
        entityType.setBaseTypeName(baseTypeName);
        return entityType;
    }

    /**
     * Checks system metadata, exact and estimated table counts, the insert counters, and that the legacy endpoint is absent.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    public void systemStatisticsTest() throws Exception {
        MockHttpServletResponse response = assertOkRequest("systemStatistics",
                get("/api/system/statistics").param("exact", "true"), null, null, getAccessToken());
        SystemStatisticsDTO statistics = applySingleJsonContentToClass(response.getContentAsString(), SystemStatisticsDTO.class);

        assertNotNull(statistics.getApplication());
        assertNotNull(statistics.getApplication().getVersion());
        assertNotNull(statistics.getApplication().getBuildCommit());
        assertNotNull(statistics.getDatabase().getDatabaseTime());
        assertTrue(statistics.getDatabase().getSizeBytes() >= 0);
        assertTrue(statistics.getDatabase().getTables().stream().anyMatch(table -> "audit_event".equals(table.getTableName())));
        assertTrue(statistics.getDatabase().getTables().stream().allMatch(table -> table.isRecordCountExact()
                && table.getRecordCount() >= 0 && table.getTableSizeBytes() >= 0
                && table.getIndexSizeBytes() >= 0 && table.getToastSizeBytes() >= 0
                && table.getTotalRelationSizeBytes() >= 0));
        assertTrue(statistics.getDatabase().getTables().stream().filter(table -> table.getToastSizeBytes() == 0)
                .allMatch(table -> table.getTotalRelationSizeBytes() == table.getTableSizeBytes() + table.getIndexSizeBytes()));
        assertTrue(statistics.getDatabase().getTables().stream().noneMatch(table -> table.getTableName().matches("entity_t\\d+")));
        assertTrue(statistics.getConnectionPool().getTotalConnections() >= 0);
        assertTrue(statistics.getJvmMemory().getHeapUsedBytes() >= 0);
        assertTrue(statistics.getJvmMemory().getNonHeapUsedBytes() >= 0);

        assertTrue(statistics.getDatabase().getTables().stream()
                .allMatch(table -> table.getInsertedRecordCount() != null && table.getInsertedRecordCount() >= 0));

        // Without exact, counts are live-row estimates from PostgreSQL's activity statistics
        MockHttpServletResponse estimatedResponse = assertOkRequest("systemStatisticsEstimated",
                get("/api/system/statistics"), null, null, getAccessToken());
        SystemStatisticsDTO estimated = applySingleJsonContentToClass(estimatedResponse.getContentAsString(), SystemStatisticsDTO.class);
        assertEquals(statistics.getDatabase().getTables().size(), estimated.getDatabase().getTables().size());
        assertTrue(estimated.getDatabase().getTables().stream().allMatch(table -> !table.isRecordCountExact()
                && table.getRecordCount() >= 0 && table.getInsertedRecordCount() != null && table.getInsertedRecordCount() >= 0));

        assertNotFoundRequest("legacyTableStorageRemoved", get("/api/tables/project/storage"), null, null, getAccessToken());
    }

    /**
     * Tests that the backend version can be read by every authenticated user, including one without
     * any global permission, but not without a login.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    public void versionTest() throws Exception {
        assertUnauthorizedRequest("versionNoToken", get("/api/version"), null, null, "");

        // The user "test2" may not read the system statistics, but the version
        String test2AccessToken = obtainNewAccessToken("test2", "test");
        try {
            assertForbiddenRequest("systemStatisticsWithoutPermission", get("/api/system/statistics"), null, null, test2AccessToken);
            MockHttpServletResponse response = assertOkRequest("versionWithoutPermission", get("/api/version"), null, null, test2AccessToken);
            assertTrue(response.getContentAsString().contains("\"version\""));
        } finally {
            // Restore the token of the user "test" for the other tests
            obtainNewAccessToken("test", "test");
        }
    }
}
