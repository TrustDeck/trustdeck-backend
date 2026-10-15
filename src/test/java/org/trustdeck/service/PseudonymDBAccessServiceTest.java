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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.trustdeck.jooq.generated.Tables.PSEUDONYM;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.Record4;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.jooq.generated.tables.records.PseudonymRecord;
import org.trustdeck.model.IdentifierItem;

/**
 * Verifies the set-based and conflict-safe pseudonym persistence contract without requiring a
 * production database.
 *
 * @author Armin Müller
 */
class PseudonymDBAccessServiceTest {

    /** Fixed validity start used by all mocked persistence rows. */
    private static final LocalDateTime VALID_FROM = LocalDateTime.of(2025, 1, 1, 0, 0);

    /** Fixed validity end used by all mocked persistence rows. */
    private static final LocalDateTime VALID_TO = LocalDateTime.of(2035, 1, 1, 0, 0);

    
    @Test
    void reconstructsExistingRepeatedAndNewRowsInRequestOrder() {
        PseudonymRecord existing = record("existing-id", "TYPE", 7, "existing-psn");
        PseudonymRecord inserted = record("new-id", "TYPE", 7, "new-psn");
        ScriptedProvider provider = new ScriptedProvider(List.of(existing), List.of(inserted), List.of());

        List<String> result = service(provider).createPseudonyms(List.of(
                dto("existing-id", "TYPE", "existing-psn"),
                dto("new-id", "TYPE", "new-psn"),
                dto("new-id", "TYPE", "new-psn")), 7, true);

        assertEquals(List.of(
                PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM,
                PseudonymDBAccessService.INSERTION_SUCCESS,
                PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM), result);
        assertEquals(1, provider.insertCount);
        assertEquals(1, provider.selectCount);
    }

    @Test
    void resolvesAConcurrentConflictWithOneSetBasedReload() {
        PseudonymRecord winner = record("shared-id", "TYPE", 7, "shared-psn");
        ScriptedProvider provider = new ScriptedProvider(List.of(), List.of(), List.of(winner));

        List<String> result = service(provider).createPseudonyms(List.of(dto("shared-id", "TYPE", "shared-psn")), 7, true);

        assertEquals(List.of(PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM), result);
        assertEquals(1, provider.insertCount);
        assertEquals(2, provider.selectCount);
        assertTrue(provider.sql.stream().anyMatch(sql -> sql.contains("on conflict")
                && sql.contains("pseudonym_identifier_idtype_domainid_pseudonym_key")
                && sql.contains("do nothing")), provider.sql.toString());
    }

    @Test
    void preservesIdentifierDuplicatePrecedenceWhenMultiplePseudonymsAreDisabled() {
        ScriptedProvider provider = new ScriptedProvider(List.of(record("same-id", "TYPE", 7, "existing-psn")), List.of(), List.of());
        List<String> result = service(provider).createPseudonyms(List.of(dto("same-id", "TYPE", "new-psn")), 7, false);

        assertEquals(List.of(PseudonymDBAccessService.INSERTION_DUPLICATE_IDENTIFIER), result);
        assertEquals(0, provider.insertCount);
        assertEquals(1, provider.selectCount);
    }

    @Test
    void doesNotClassifyAnUnexpectedDatabaseFailureAsADuplicate() {
        ScriptedProvider provider = new ScriptedProvider(List.of(), List.of(), List.of());
        provider.failInsert = true;

        List<String> result = service(provider).createPseudonyms(List.of(dto("failing-id", "TYPE", "failing-psn")), 7, true);

        assertEquals(List.of(PseudonymDBAccessService.INSERTION_ERROR), result);
        assertEquals(1, provider.insertCount);
    }

    @Test
    void fiveHundredRowsUseOneBulkInsert() {
        List<PseudonymDTO> input = new ArrayList<>();
        List<PseudonymRecord> inserted = new ArrayList<>();
        
        for (int i = 0; i < 500; i++) {
            String identifier = "id-" + i;
            String pseudonym = "psn-" + i;
            input.add(dto(identifier, "TYPE", pseudonym));
            inserted.add(record(identifier, "TYPE", 7, pseudonym));
        }
        
        ScriptedProvider provider = new ScriptedProvider(List.of(), inserted, List.of());

        List<String> result = service(provider).createPseudonyms(input, 7, true);

        assertEquals(500, result.size());
        assertTrue(result.stream().allMatch(PseudonymDBAccessService.INSERTION_SUCCESS::equals));
        assertEquals(1, provider.insertCount);
        assertEquals(1, provider.selectCount);
        assertTrue(provider.sql.stream().anyMatch(sql -> sql.contains("values") && sql.contains(",")));
    }

    /**
     * Creates the service with a jOOQ mock connection.
     *
     * @param provider the SQL result provider
     * @return the service under test
     */
    private PseudonymDBAccessService service(ScriptedProvider provider) {
        DSLContext dsl = DSL.using(new MockConnection(provider), SQLDialect.POSTGRES);
        PseudonymDBAccessService service = new PseudonymDBAccessService();
        ReflectionTestUtils.setField(service, "dsl", dsl);
        
        return service;
    }

    /**
     * Creates a mocked normalized pseudonym input.
     *
     * @param identifier the identifier value
     * @param idType the identifier type
     * @param pseudonym the pseudonym value
     * @return the mocked DTO
     */
    private PseudonymDTO dto(String identifier, String idType, String pseudonym) {
        PseudonymDTO dto = mock(PseudonymDTO.class);
        when(dto.getIdentifierItem()).thenReturn(IdentifierItem.builder().identifier(identifier).idType(idType).build());
        when(dto.getPsn()).thenReturn(pseudonym);
        when(dto.getValidFrom()).thenReturn(VALID_FROM);
        when(dto.getValidFromInherited()).thenReturn(true);
        when(dto.getValidTo()).thenReturn(VALID_TO);
        when(dto.getValidToInherited()).thenReturn(true);
        
        return dto;
    }

    /**
     * Creates a jOOQ record returned by a mocked database operation.
     *
     * @param identifier the identifier value
     * @param idType the identifier type
     * @param domainId the domain identifier
     * @param pseudonym the pseudonym value
     * @return a populated pseudonym record
     */
    private PseudonymRecord record(String identifier, String idType, int domainId, String pseudonym) {
        PseudonymRecord record = DSL.using(SQLDialect.POSTGRES).newRecord(PSEUDONYM);
        record.setIdentifier(identifier);
        record.setIdtype(idType);
        record.setDomainid(domainId);
        record.setPseudonym(pseudonym);
        record.setValidfrom(VALID_FROM);
        record.setValidfrominherited(true);
        record.setValidto(VALID_TO);
        record.setValidtoinherited(true);
        
        return record;
    }

    /**
     * Supplies deterministic jOOQ results and records SQL statements for persistence assertions.
     */
    private static final class ScriptedProvider implements MockDataProvider {
        /** Rows returned by the pre-insert set query. */
        private final List<PseudonymRecord> existing;

        /** Rows returned by the bulk insert's RETURNING clause. */
        private final List<PseudonymRecord> inserted;

        /** Rows returned by the post-conflict set query. */
        private final List<PseudonymRecord> resolved;

        /** SQL statements observed by this provider. */
        private final List<String> sql = new ArrayList<>();

        /** Number of set-based select statements observed. */
        private int selectCount;

        /** Number of bulk insert statements observed. */
        private int insertCount;

        /** Causes the insert operation to simulate an unrelated database failure. */
        private boolean failInsert;

        /**
         * Creates a provider with results for the pre-insert and conflict-resolution queries.
         *
         * @param existing rows returned by the pre-insert lookup
         * @param inserted rows returned by the bulk insert {@code RETURNING} clause
         * @param resolved rows returned by the post-conflict set-based lookup
         */
        private ScriptedProvider(List<PseudonymRecord> existing, List<PseudonymRecord> inserted, List<PseudonymRecord> resolved) {
            this.existing = existing;
            this.inserted = inserted;
            this.resolved = resolved;
        }

        @Override
        public MockResult[] execute(MockExecuteContext context) {
            String sqlText = context.sql().toLowerCase(Locale.ROOT);
            sql.add(sqlText);
            DSLContext dsl = DSL.using(SQLDialect.POSTGRES);
            
            if (sqlText.startsWith("select")) {
                List<PseudonymRecord> records = selectCount++ == 0 ? existing : resolved;
                Result<PseudonymRecord> result = dsl.newResult(PSEUDONYM);
                result.addAll(records);
                
                return new MockResult[] {new MockResult(records.size(), result)};
            }
            
            insertCount++;
            
            if (failInsert) {
                throw new IllegalStateException("simulated unrelated database failure");
            }
            
            Result<Record4<String, String, String, Integer>> result = dsl.newResult(
                    PSEUDONYM.IDENTIFIER, PSEUDONYM.IDTYPE, PSEUDONYM.PSEUDONYM_, PSEUDONYM.DOMAINID);
            
            for (PseudonymRecord record : inserted) {
                result.add(dsl.newRecord(PSEUDONYM.IDENTIFIER, PSEUDONYM.IDTYPE, PSEUDONYM.PSEUDONYM_, PSEUDONYM.DOMAINID)
                        .values(record.getIdentifier(), record.getIdtype(), record.getPseudonym(), record.getDomainid()));
            }
            
            return new MockResult[] {new MockResult(inserted.size(), result)};
        }
    }
}
