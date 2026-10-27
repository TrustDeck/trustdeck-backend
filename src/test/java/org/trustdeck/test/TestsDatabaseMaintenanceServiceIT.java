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

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.trustdeck.service.AssertWebRequestService;

/**
 * This class offers tests for the database maintenance endpoints.
 *
 * @author Armin Müller
 */
public class TestsDatabaseMaintenanceServiceIT extends AssertWebRequestService {

    /**
     * Tests that only TrustDeck's data tables can be truncated and that truncating 
     * the permissions takes effect immediately.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("truncateTableTest")
    public void truncateTableTest() throws Exception {
        // Other tables, like the audit trail, can't be truncated
        this.assertBadRequestRequest("truncateAuditTrail", delete("/api/tables/audit_event"), null, null, this.getAccessToken());
        this.assertBadRequestRequest("truncateUnknownTable", delete("/api/tables/pg_class"), null, null, this.getAccessToken());
        this.assertUnauthorizedRequest("truncateTableNoToken", delete("/api/tables/pseudonym"), null, null, "");

        // The permissions are cached after the first access
        this.assertOkRequest("getDomainBeforeTruncation", get("/api/domains/TestStudie"), null, null, this.getAccessToken());

        // Truncating the permissions clears the cache, so that they are gone immediately
        this.assertOkRequest("truncatePermissions", delete("/api/tables/permission_grant"), null, null, this.getAccessToken());
        this.assertForbiddenRequest("getDomainAfterTruncation", get("/api/domains/TestStudie"), null, null, this.getAccessToken());
    }
}
