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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.trustdeck.jooq.generated.Tables.DOMAIN;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.jooq.generated.tables.records.DomainRecord;

/**
 * Tests that domain updates and deletions change the pseudonyms of a domain with set-based statements
 * instead of loading every pseudonym into memory.
 */
@ExtendWith(MockitoExtension.class)
class DomainBulkPseudonymStatementsTest {

	@Mock
	private PermissionDBService permissionDBService;

	@InjectMocks
	private DomainDBAccessService service;

	/** Every statement sent to the mocked database, in lower case. */
	private final List<String> statements = new ArrayList<>();

	/** Whether the domain holds records, as answered to an existence check. */
	private boolean domainHasRecords;

	private DSLContext ctx;

	private final Domain domain = new Domain().setId(7).setName("test-domain").setPrefix("TST-")
			.setValidfrom(LocalDateTime.of(2020, 1, 1, 0, 0)).setValidto(LocalDateTime.of(2050, 1, 1, 0, 0));

	@BeforeEach
	void setUp() {
		ctx = DSL.using(new MockConnection(context -> {
			String sql = context.sql().toLowerCase();
			statements.add(sql);

			DSLContext create = DSL.using(SQLDialect.POSTGRES);
			if (sql.startsWith("select exists")) {
				Field<Boolean> exists = DSL.field("exists", Boolean.class);
				Result<Record1<Boolean>> result = create.newResult(exists);
				result.add(create.newRecord(exists).values(domainHasRecords));
				return new MockResult[] { new MockResult(1, result) };
			}
			if (sql.startsWith("select")) {
				Result<DomainRecord> result = create.newResult(DOMAIN);
				result.add(new DomainRecord(domain));
				return new MockResult[] { new MockResult(1, result) };
			}
			// Updates and deletions: the pseudonym statements affect many rows, the domain statements one
			return new MockResult[] { new MockResult(sql.contains("\"pseudonym\"") ? 1_000_000 : 1) };
		}), SQLDialect.POSTGRES);
		ReflectionTestUtils.setField(service, "dsl", ctx);
	}

	/** Without a change of validity, the pseudonyms aren't touched at all. */
	@Test
	void leavesPseudonymsAloneWithoutValidityChange() {
		service.updateInheritedPseudonymValidity(ctx, domain, new Domain().setMultiplepsnallowed(true));
		service.updateInheritedPseudonymValidity(ctx, domain, new Domain().setValidfrom(domain.getValidfrom()).setValidto(domain.getValidto()));

		assertTrue(statements.isEmpty(), statements.toString());
	}

	/** Each changed validity field is passed on with one statement, only to the pseudonyms that inherit it. */
	@Test
	void passesChangedValidityOnToInheritingPseudonymsOnly() {
		service.updateInheritedPseudonymValidity(ctx, domain, new Domain().setValidfrom(LocalDateTime.of(2021, 1, 1, 0, 0)));

		assertEquals(1, statements.size(), statements.toString());
		assertUpdate(statements.get(0), "\"validfrom\"", "\"validfrominherited\"");

		statements.clear();
		service.updateInheritedPseudonymValidity(ctx, domain, new Domain().setValidfrom(LocalDateTime.of(2021, 1, 1, 0, 0))
				.setValidto(LocalDateTime.of(2060, 1, 1, 0, 0)));

		assertEquals(2, statements.size(), statements.toString());
		assertUpdate(statements.get(0), "\"validfrom\"", "\"validfrominherited\"");
		assertUpdate(statements.get(1), "\"validto\"", "\"validtoinherited\"");
	}

	/** A domain update that only changes a setting issues no pseudonym statement and never reads the pseudonyms. */
	@Test
	void updatesSettingWithoutReadingPseudonyms() {
		Domain updated = service.updateDomain(domain, new Domain().setMultiplepsnallowed(true), false);

		assertEquals(domain.getName(), updated.getName());
		assertTrue(statements.stream().noneMatch(s -> s.contains("\"pseudonym\"")), statements.toString());
	}

	/** Deleting a domain removes its pseudonyms with a single statement, whatever their number. */
	@Test
	void deletesPseudonymsWithSingleStatement() {
		when(permissionDBService.removePermissionsForResource(anyString(), anyInt())).thenReturn(true);

		assertTrue(service.deleteDomain(domain.getName(), false));

		List<String> pseudonymStatements = statements.stream().filter(s -> s.contains("\"pseudonym\"")).toList();
		assertEquals(1, pseudonymStatements.size(), statements.toString());
		assertTrue(pseudonymStatements.get(0).startsWith("delete from \"public\".\"pseudonym\""), pseudonymStatements.get(0));
		assertTrue(pseudonymStatements.get(0).contains("\"domainid\" = ?"), pseudonymStatements.get(0));
	}

	/** The emptiness check asks the database for the existence of a record instead of counting them. */
	@Test
	void checksForRecordsWithoutCounting() {
		domainHasRecords = true;
		assertTrue(service.hasPseudonyms(domain.getName()));

		domainHasRecords = false;
		assertFalse(service.hasPseudonyms(domain.getName()));

		assertTrue(statements.stream().noneMatch(s -> s.contains("count(")), statements.toString());
		assertTrue(statements.stream().anyMatch(s -> s.startsWith("select exists")), statements.toString());
	}

	private static void assertUpdate(String sql, String column, String inheritedColumn) {
		assertTrue(sql.startsWith("update \"public\".\"pseudonym\" set " + column + " = "), sql);
		assertTrue(sql.contains("\"domainid\" = ?"), sql);
		assertTrue(sql.contains(inheritedColumn), sql);
	}
}
