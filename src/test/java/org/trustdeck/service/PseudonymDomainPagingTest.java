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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.trustdeck.jooq.generated.Tables.PSEUDONYM;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.trustdeck.dto.CursorPageDTO;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.jooq.generated.tables.records.PseudonymRecord;
import org.trustdeck.utils.SpringBeanLocator;

/**
 * Tests that the pseudonyms of a domain are read page by page behind a cursor and deleted with a single
 * statement, so that neither loads a whole domain into memory.
 */
class PseudonymDomainPagingTest {

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

			if (sql.startsWith("select")) {
				DSLContext create = DSL.using(SQLDialect.POSTGRES);
				Result<PseudonymRecord> result = create.newResult(PSEUDONYM);
				long id = 1;
				for (String psn : rows) {
					result.add(new PseudonymRecord().setId(id++).setIdentifier("id-" + psn).setIdtype("patient")
							.setPseudonym(psn).setDomainid(domain.getId()).setValidfrominherited(true).setValidtoinherited(true));
				}
				return new MockResult[] { new MockResult(result.size(), result) };
			}
			return new MockResult[] { new MockResult(1_000_000) };
		}), SQLDialect.POSTGRES);
		ReflectionTestUtils.setField(service, "dsl", ctx);
	}

	/** The first page reads one row more than requested and returns a cursor behind its last pseudonym. */
	@Test
	void firstPageReturnsCursorWhenMoreFollow() {
		rows = List.of("TST-A", "TST-B", "TST-C");

		CursorPageDTO<PseudonymDTO> page = service.getPseudonymPage(domain, null, 2);

		assertEquals(List.of("TST-A", "TST-B"), page.getItems().stream().map(PseudonymDTO::getPsn).toList());
		assertEquals("TST-B", CursorPageDTO.decodeCursor(page.getNextCursor()));

		assertEquals(1, statements.size(), statements.toString());
		String sql = statements.get(0);
		assertTrue(sql.contains("\"domainid\" = ?"), sql);
		assertTrue(!sql.contains("\"pseudonym\" > ?"), sql);
		assertTrue(sql.contains("order by \"public\".\"pseudonym\".\"pseudonym\" asc"), sql);
		assertTrue(sql.contains("fetch next ? rows only"), sql);
		assertTrue(bindings.get(0).contains(3L) || bindings.get(0).contains(3), bindings.get(0).toString());
	}

	/** A following page continues behind the cursor's pseudonym; the last page has no cursor. */
	@Test
	void nextPageContinuesBehindCursor() {
		rows = List.of("TST-C");

		CursorPageDTO<PseudonymDTO> page = service.getPseudonymPage(domain, "TST-B", 2);

		assertEquals(List.of("TST-C"), page.getItems().stream().map(PseudonymDTO::getPsn).toList());
		assertNull(page.getNextCursor());
		assertTrue(statements.get(0).contains("\"pseudonym\" > ?"), statements.get(0));
		assertTrue(bindings.get(0).contains("TST-B"), bindings.get(0).toString());
	}

	/** The domain is resolved once per page, not once per pseudonym, so the page needs a single query. */
	@Test
	void resolvesDomainOncePerPage() {
		rows = List.of("TST-A", "TST-B", "TST-C");

		CursorPageDTO<PseudonymDTO> page = service.getPseudonymPage(domain, null, 10);

		assertEquals(1, statements.size(), statements.toString());
		assertEquals(3, page.getItems().size());
		page.getItems().forEach(item -> assertEquals("test-domain", item.getDomainName()));
		assertSame(page.getItems().get(0).getDomain(), page.getItems().get(2).getDomain());
	}

	/** An empty domain gives an empty last page. */
	@Test
	void emptyDomainGivesEmptyLastPage() {
		CursorPageDTO<PseudonymDTO> page = service.getPseudonymPage(domain, null, 10);

		assertTrue(page.getItems().isEmpty());
		assertNull(page.getNextCursor());
	}

	/** All pseudonyms of a domain are deleted with one statement, whatever their number. */
	@Test
	void deletesAllPseudonymsWithSingleStatement() {
		assertEquals(1_000_000, service.deleteAllPseudonymsInDomain(domain.getId()));

		assertEquals(1, statements.size(), statements.toString());
		assertTrue(statements.get(0).startsWith("delete from \"public\".\"pseudonym\" where \"public\".\"pseudonym\".\"domainid\" = ?"),
				statements.get(0));
		assertEquals(List.of(7), bindings.get(0));
	}
}
