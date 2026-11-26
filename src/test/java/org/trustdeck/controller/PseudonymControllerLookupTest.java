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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.dto.DomainDTO;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.service.AuthorizationService;
import org.trustdeck.service.DomainDBAccessService;
import org.trustdeck.service.PseudonymDBAccessService;
import org.trustdeck.service.ResponseService;
import org.trustdeck.utils.SpringBeanLocator;

/**
 * Tests the lookup of multiple pseudonyms through their psn values on the controller level:
 * input cleaning, status codes, and the reduced standard view.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PseudonymControllerLookupTest {

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

	// A domain whose validity has expired: reading must still work
	private final Domain domain = new Domain().setId(7).setName("test-domain").setPrefix("TST-")
			.setValidfrom(LocalDateTime.now().minusYears(10)).setValidto(LocalDateTime.now().minusYears(1));

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
		when(responseService.ok(anyString(), any(List.class))).thenAnswer(invocation -> ResponseEntity.ok(invocation.getArgument(1)));
	}

	/** The pseudonyms are trimmed, blank values and duplicates dropped, and all are resolved in one call. */
	@Test
	@SuppressWarnings("unchecked")
	void cleansInputAndQueriesOnce() {
		when(pseudonymDBAccessService.getPseudonymsFromPsns(eq(domain), any())).thenReturn(new ArrayList<>());

		List<String> psns = Arrays.asList(" TST-0001 ", "TST-0002", "", "   ", null, "TST-0001");
		assertEquals(200, controller.lookupPseudonyms("test-domain", psns, null).getStatusCode().value());

		ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
		verify(pseudonymDBAccessService).getPseudonymsFromPsns(eq(domain), captor.capture());
		assertEquals(List.of("TST-0001", "TST-0002"), new ArrayList<>(captor.getValue()));
	}

	/** A missing body gives 400, an oversized list 422, both without querying the database. */
	@Test
	void rejectsMissingAndOversizedLists() {
		assertEquals(400, controller.lookupPseudonyms("test-domain", null, null).getStatusCode().value());

		List<String> tooMany = Collections.nCopies(defaults.getPseudonym().getBatchLength() + 1, "TST-0001");
		assertEquals(422, controller.lookupPseudonyms("test-domain", tooMany, null).getStatusCode().value());
		verify(pseudonymDBAccessService, never()).getPseudonymsFromPsns(any(), any());
	}

	/** An unknown domain gives 404, a failed lookup 422. */
	@Test
	void reportsMissingDomainAndFailedLookup() {
		assertEquals(404, controller.lookupPseudonyms("unknown", List.of("TST-0001"), null).getStatusCode().value());
		when(pseudonymDBAccessService.getPseudonymsFromPsns(eq(domain), any())).thenReturn(null);
		assertEquals(422, controller.lookupPseudonyms("test-domain", List.of("TST-0001"), null).getStatusCode().value());
	}

	/** Found pseudonyms are returned; without the complete view they are reduced to the standard view. */
	@Test
	@SuppressWarnings("unchecked")
	void returnsFoundPseudonymsInTheRightView() {
		PseudonymDTO item = new PseudonymDTO();
		item.setPsn("TST-0001");
		item.setValidFromInherited(true);
		item.setDomain(new DomainDTO());
		when(pseudonymDBAccessService.getPseudonymsFromPsns(eq(domain), any())).thenReturn(new ArrayList<>(List.of(item)));
		when(authorizationService.hasDomainPermission("test-domain", "complete-view")).thenReturn(false);

		ResponseEntity<?> response = controller.lookupPseudonyms("test-domain", List.of("TST-0001", "TST-9999"), null);
		assertEquals(200, response.getStatusCode().value());
		assertEquals(1, ((List<PseudonymDTO>) response.getBody()).size());
		assertNull(item.getValidFromInherited());
		assertNull(item.getDomain());

		item.setDomain(new DomainDTO());
		when(authorizationService.hasDomainPermission("test-domain", "complete-view")).thenReturn(true);
		controller.lookupPseudonyms("test-domain", List.of("TST-0001"), null);
		assertNotNull(item.getDomain());
	}
}
