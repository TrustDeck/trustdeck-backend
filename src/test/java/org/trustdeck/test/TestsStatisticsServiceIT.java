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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;

import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.trustdeck.dto.ProjectStatisticsDTO;
import org.trustdeck.dto.SystemStatisticsDTO;
import org.trustdeck.service.AssertWebRequestService;

/**
 * Integration tests for the project and system statistics endpoints.
 *
 * @author Armin Müller
 */
public class TestsStatisticsServiceIT extends AssertWebRequestService {

    /**
     * Checks project identity, UTC generation time, counts, and not-found behavior.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    public void projectStatisticsTest() throws Exception {
        MockHttpServletResponse response = assertOkRequest("projectStatistics", 
        		get("/api/projects/TEST/statistics"), null, null, getAccessToken());
        ProjectStatisticsDTO statistics = applySingleJsonContentToClass(response.getContentAsString(), ProjectStatisticsDTO.class);

        assertNotNull(statistics);
        assertNotNull(statistics.getGeneratedAt());
        assertEquals(ZoneOffset.UTC, statistics.getGeneratedAt().atOffset(ZoneOffset.UTC).getOffset());
        assertEquals("TEST", statistics.getProject().getAbbreviation());
        assertEquals("Test Study", statistics.getProject().getName());
        assertEquals(1L, statistics.getCounts().getDomains());
        assertEquals(0L, statistics.getCounts().getEntityTypes());
        assertEquals(0L, statistics.getCounts().getEntities());
        assertEquals(1L, statistics.getCounts().getPseudonyms());
        assertNotNull(statistics.getEntitiesByType());
        assertNotNull(statistics.getPseudonymsByDomain());
        assertEquals(1, statistics.getPseudonymsByDomain().size());
        assertEquals(1L, statistics.getPseudonymsByDomain().getFirst().getCount());

        assertNotFoundRequest("projectStatisticsNotFound", get("/api/projects/UNKNOWN/statistics"), null, null, getAccessToken());
        assertUnauthorizedRequest("projectStatisticsUnauthenticated", get("/api/projects/TEST/statistics"), null, null, "");
    }

    /**
     * Checks system metadata, exact table counts, and that the legacy endpoint is absent.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    public void systemStatisticsTest() throws Exception {
        MockHttpServletResponse response = assertOkRequest("systemStatistics",
                get("/api/system/statistics"), null, null, getAccessToken());
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
        assertTrue(statistics.getDatabase().getTables().stream().noneMatch(table -> table.getTableName().startsWith("entity_t")));
        assertTrue(statistics.getConnectionPool().getTotalConnections() >= 0);
        assertTrue(statistics.getJvmMemory().getHeapUsedBytes() >= 0);
        assertTrue(statistics.getJvmMemory().getNonHeapUsedBytes() >= 0);

        assertNotFoundRequest("legacyTableStorageRemoved", get("/api/tables/project/storage"), null, null, getAccessToken());
    }
}
