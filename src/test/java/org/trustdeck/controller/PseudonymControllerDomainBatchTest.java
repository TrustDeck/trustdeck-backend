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

package org.trustdeck.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.dto.CursorPageDTO;
import org.trustdeck.dto.DomainDTO;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.service.AuthorizationService;
import org.trustdeck.service.DomainDBAccessService;
import org.trustdeck.service.PseudonymDBAccessService;
import org.trustdeck.service.ResponseService;
import org.trustdeck.utils.SpringBeanLocator;

/**
 * Tests the paged reading and the deletion of all pseudonyms of a domain on the controller level:
 * parameter checks, status codes, and the reduced standard view.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PseudonymControllerDomainBatchTest {

	@Mock
	private DomainDBAccessService domainDBAccessService;

	@Mock
	private PseudonymDBAccessService pseudonymDBAccessService;

	@Mock
	private ResponseService responseService;

	@Mock
	private AuthorizationService authorizationService;

	@Spy
	private DefaultProperties defaults = new DefaultProperties();

	@InjectMocks
	private PseudonymController controller;

	private final Domain domain = new Domain().setId(7).setName("test-domain").setPrefix("TST-")
			.setValidfrom(LocalDateTime.now().minusYears(1)).setValidto(LocalDateTime.now().plusYears(10));

	/** The DTOs resolve service beans on construction; a stub context is enough here. */
	@BeforeAll
	static void stubSpringContext() {
		new SpringBeanLocator().setApplicationContext(mock(ApplicationContext.class));
	}

	@BeforeEach
	void setUp() {
		when(domainDBAccessService.getDomainByName("test-domain")).thenReturn(domain);
		when(responseService.badRequest(any())).thenReturn(ResponseEntity.badRequest().build());
		when(responseService.notFound(any())).thenReturn(ResponseEntity.notFound().build());
		when(responseService.unprocessableEntity(any())).thenReturn(ResponseEntity.unprocessableEntity().build());
		when(responseService.noContent(any())).thenReturn(ResponseEntity.noContent().build());
		when(responseService.ok(anyString(), any(CursorPageDTO.class))).thenAnswer(invocation -> ResponseEntity.ok(invocation.getArgument(1)));
	}

	/** Without parameters, the first page is read with the configured default size. */
	@Test
	void readsFirstPageWithDefaultSize() {
		when(pseudonymDBAccessService.getPseudonymPage(eq(domain), isNull(), anyInt())).thenReturn(new CursorPageDTO<>(new ArrayList<>(), null));

		assertEquals(200, controller.getPseudonymBatch("test-domain", null, null, null).getStatusCode().value());
		verify(pseudonymDBAccessService).getPseudonymPage(domain, null, defaults.getPseudonym().getBatchPageSize());
	}

	/** The cursor is decoded and the page continues behind its pseudonym value. */
	@Test
	void continuesBehindCursor() {
		when(pseudonymDBAccessService.getPseudonymPage(eq(domain), anyString(), anyInt())).thenReturn(new CursorPageDTO<>(new ArrayList<>(), null));

		controller.getPseudonymBatch("test-domain", CursorPageDTO.encodeCursor("TST-0042"), 50, null);
		verify(pseudonymDBAccessService).getPseudonymPage(domain, "TST-0042", 50);
	}

	/** Page sizes outside 1 to batch length and cursors not created by the backend are rejected. */
	@Test
	void rejectsInvalidParameters() {
		int max = defaults.getPseudonym().getBatchLength();
		assertEquals(400, controller.getPseudonymBatch("test-domain", null, 0, null).getStatusCode().value());
		assertEquals(400, controller.getPseudonymBatch("test-domain", null, max + 1, null).getStatusCode().value());
		assertEquals(400, controller.getPseudonymBatch("test-domain", "TST-0042", 10, null).getStatusCode().value());
		verify(pseudonymDBAccessService, never()).getPseudonymPage(any(), any(), anyInt());

		when(pseudonymDBAccessService.getPseudonymPage(any(), any(), anyInt())).thenReturn(new CursorPageDTO<>(new ArrayList<>(), null));
		assertEquals(200, controller.getPseudonymBatch("test-domain", null, max, null).getStatusCode().value());
	}

	/** An unknown domain gives 404, a failed read 422. */
	@Test
	void reportsMissingDomainAndFailedRead() {
		assertEquals(404, controller.getPseudonymBatch("unknown", null, null, null).getStatusCode().value());
		assertEquals(422, controller.getPseudonymBatch("test-domain", null, null, null).getStatusCode().value());
	}

	/** Without the permission for the complete view, the items are reduced to the standard view. */
	@Test
	void reducesItemsWithoutCompleteView() {
		PseudonymDTO item = new PseudonymDTO();
		item.setPsn("TST-0001");
		item.setValidFromInherited(true);
		item.setDomain(new DomainDTO());
		when(pseudonymDBAccessService.getPseudonymPage(any(), any(), anyInt())).thenReturn(new CursorPageDTO<>(List.of(item), "next"));
		when(authorizationService.hasDomainPermission("test-domain", "complete-view")).thenReturn(false);

		controller.getPseudonymBatch("test-domain", null, null, null);
		assertNull(item.getValidFromInherited());
		assertNull(item.getDomain());

		item.setDomain(new DomainDTO());
		when(authorizationService.hasDomainPermission("test-domain", "complete-view")).thenReturn(true);
		controller.getPseudonymBatch("test-domain", null, null, null);
		assertNotNull(item.getDomain());
	}

	/** Deleting all pseudonyms answers 204 (also for an empty domain), 404, or 422 and never 206. */
	@Test
	void deleteAnswersOnlyNoContentNotFoundOrUnprocessable() {
		when(pseudonymDBAccessService.deleteAllPseudonymsInDomain(7)).thenReturn(1_000_000, 0, -1);

		assertEquals(204, controller.deletePseudonymBatch("test-domain", null).getStatusCode().value());
		assertEquals(204, controller.deletePseudonymBatch("test-domain", null).getStatusCode().value());
		assertEquals(422, controller.deletePseudonymBatch("test-domain", null).getStatusCode().value());
		assertEquals(404, controller.deletePseudonymBatch("unknown", null).getStatusCode().value());
	}
}
