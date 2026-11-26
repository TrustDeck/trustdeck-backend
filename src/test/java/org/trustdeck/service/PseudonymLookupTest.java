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
import static org.trustdeck.jooq.generated.Tables.PSEUDONYM;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.jooq.generated.tables.records.PseudonymRecord;
import org.trustdeck.utils.SpringBeanLocator;

/**
 * Tests that a list of pseudonyms is looked up with a single statement.
 */
class PseudonymLookupTest {

	private final PseudonymDBAccessService service = new PseudonymDBAccessService();

	private final Domain domain = new Domain().setId(7).setName("test-domain").setPrefix("TST-")
			.setValidfrom(LocalDateTime.of(2020, 1, 1, 0, 0)).setValidto(LocalDateTime.of(2050, 1, 1, 0, 0));

	/** Every statement sent to the mocked database, in lower case, and its bind values. */
	private final List<String> statements = new ArrayList<>();
	private final List<List<Object>> bindings = new ArrayList<>();

	/** The pseudonym values the mocked database returns for a select. */
	private List<String> rows = List.of();

	/** The DTOs resolve service beans on construction; a stub context is enough here. */
	@BeforeAll
	static void stubSpringContext() {
		new SpringBeanLocator().setApplicationContext(mock(ApplicationContext.class));
	}

	@BeforeEach
	void setUp() {
		DSLContext ctx = DSL.using(new MockConnection(context -> {
			String sql = context.sql().toLowerCase();
			statements.add(sql);
			bindings.add(Arrays.asList(context.bindings()));
			DSLContext create = DSL.using(SQLDialect.POSTGRES);
			Result<PseudonymRecord> result = create.newResult(PSEUDONYM);
			long id = 1;
			for (String psn : rows) {
				result.add(new PseudonymRecord().setId(id++).setIdentifier("id-" + psn).setIdtype("patient")
						.setPseudonym(psn).setDomainid(domain.getId()).setValidfrominherited(true).setValidtoinherited(true));
			}
			return new MockResult[] { new MockResult(result.size(), result) };
		}), SQLDialect.POSTGRES);
		ReflectionTestUtils.setField(service, "dsl", ctx);
	}

	/** All pseudonyms are resolved with one IN query restricted to the domain; the domain is set on every item. */
	@Test
	void looksUpAllPseudonymsWithOneQuery() {
		rows = List.of("TST-A", "TST-C");
		List<PseudonymDTO> found = service.getPseudonymsFromPsns(domain, new LinkedHashSet<>(List.of("TST-A", "TST-B", "TST-C")));

		assertEquals(List.of("TST-A", "TST-C"), found.stream().map(PseudonymDTO::getPsn).toList());
		assertEquals("id-TST-A", found.get(0).getIdentifierItem().getIdentifier());
		assertEquals("test-domain", found.get(0).getDomainName());

		assertEquals(1, statements.size(), statements.toString());
		String sql = statements.get(0);
		assertTrue(sql.contains("\"domainid\" = ?"), sql);
		assertTrue(sql.contains("\"pseudonym\" in ("), sql);
		assertTrue(bindings.get(0).containsAll(List.of("TST-A", "TST-B", "TST-C")), bindings.get(0).toString());
	}

	/** An empty list doesn't touch the database. */
	@Test
	void emptyListNeedsNoQuery() {
		assertTrue(service.getPseudonymsFromPsns(domain, List.of()).isEmpty());
		assertTrue(statements.isEmpty());
	}
}
