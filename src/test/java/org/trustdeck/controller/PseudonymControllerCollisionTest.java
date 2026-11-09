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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import org.trustdeck.algorithms.PseudonymizationFactory;
import org.trustdeck.algorithms.Pseudonymizer;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.service.AlgorithmDBService;
import org.trustdeck.service.AuthorizationService;
import org.trustdeck.service.DomainDBAccessService;
import org.trustdeck.service.DomainFillingRateMonitor;
import org.trustdeck.service.PseudonymDBAccessService;
import org.trustdeck.service.ResponseService;
import org.trustdeck.utils.SpringBeanLocator;

/**
 * Tests how single pseudonym creation handles a pseudonym that already exists in a
 * domain with a random algorithm: generated pseudonyms are regenerated, supplied
 * pseudonyms are never replaced.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PseudonymControllerCollisionTest {

	@Mock
	private DomainDBAccessService domainDBAccessService;

	@Mock
	private PseudonymDBAccessService pseudonymDBAccessService;

	@Mock
	private ResponseService responseService;

	@Mock
	private AuthorizationService authorizationService;

	@Mock
	private AlgorithmDBService algorithmDBService;

	@Mock
	private PseudonymizationFactory pseudonymizationFactory;

	@Mock
	private DomainFillingRateMonitor fillingRateMonitor;

	@Spy
	private DefaultProperties defaults = new DefaultProperties();

	@InjectMocks
	private PseudonymController controller;

	private final Pseudonymizer pseudonymizer = mock(Pseudonymizer.class);
	private final Algorithm algorithm = new Algorithm().setId(1).setName("RANDOM_SYM").setSalt("salt").setAddCheckDigit(false);

	/** The DTOs resolve a service bean on construction; a stub context is enough here. */
	@BeforeAll
	static void stubSpringContext() {
		new SpringBeanLocator().setApplicationContext(mock(ApplicationContext.class));
	}

	@BeforeEach
	void setUp() {
		Domain domain = new Domain().setId(7).setName("test-domain").setPrefix("TST-").setAlgorithmId(1)
				.setValidfrom(LocalDateTime.now().minusYears(1)).setValidto(LocalDateTime.now().plusYears(10))
				.setEnforcestartdatevalidity(true).setEnforceenddatevalidity(true).setMultiplepsnallowed(false);
		when(domainDBAccessService.getDomainByName("test-domain")).thenReturn(domain);
		when(algorithmDBService.getAlgorithmByID(1)).thenReturn(algorithm);
		when(pseudonymizationFactory.getPseudonymizer(algorithm)).thenReturn(pseudonymizer);
		when(pseudonymizer.pseudonymize(anyString(), anyString())).thenReturn("TST-1", "TST-2", "TST-3");
		when(responseService.unprocessableEntity(any())).thenReturn(ResponseEntity.unprocessableEntity().build());
		when(responseService.created(any(), any(PseudonymDTO.class))).thenReturn(ResponseEntity.status(201).build());
		when(responseService.insufficientStorage(any())).thenReturn(ResponseEntity.status(507).build());
	}

	/**
	 * A supplied pseudonym that already belongs to another identifier is a conflict.
	 * It must neither be replaced by a random pseudonym nor trigger the filling-rate check.
	 */
	@Test
	void suppliedPseudonymCollisionIsRejectedWithoutRegeneration() {
		when(pseudonymDBAccessService.createPseudonyms(anyList(), anyInt(), anyBoolean()))
				.thenReturn(List.of(PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM));

		ResponseEntity<?> response = controller.createPseudonym("test-domain", request("supplied-psn"), false, null);

		assertEquals(422, response.getStatusCode().value());
		verify(pseudonymDBAccessService, times(1)).createPseudonyms(anyList(), anyInt(), anyBoolean());
		verify(pseudonymizationFactory, never()).getPseudonymizer(any());
		verify(fillingRateMonitor, never()).checkAfterCollision(any(), any());
	}

	/** A generated pseudonym that collides is regenerated; the filling rate is checked once per request. */
	@Test
	void generatedPseudonymCollisionIsRetried() {
		when(pseudonymDBAccessService.createPseudonyms(anyList(), anyInt(), anyBoolean()))
				.thenReturn(List.of(PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM),
						List.of(PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM),
						List.of(PseudonymDBAccessService.INSERTION_SUCCESS));

		ResponseEntity<?> response = controller.createPseudonym("test-domain", request(null), false, null);

		assertEquals(201, response.getStatusCode().value());
		verify(pseudonymDBAccessService, times(3)).createPseudonyms(anyList(), anyInt(), anyBoolean());
		verify(pseudonymizer, times(3)).pseudonymize(anyString(), anyString());
		verify(fillingRateMonitor, times(1)).checkAfterCollision(any(), any());
	}

	/** Exhausted retries for a generated pseudonym still answer 507, as before. */
	@Test
	void exhaustedRetriesForGeneratedPseudonymReportInsufficientStorage() {
		when(pseudonymDBAccessService.createPseudonyms(anyList(), anyInt(), anyBoolean()))
				.thenReturn(List.of(PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM));

		ResponseEntity<?> response = controller.createPseudonym("test-domain", request(null), false, null);

		assertEquals(507, response.getStatusCode().value());
		verify(pseudonymDBAccessService, times(defaults.getAlgorithm().getNumberOfRetries()))
				.createPseudonyms(anyList(), anyInt(), anyBoolean());
		verify(fillingRateMonitor, times(1)).checkAfterCollision(any(), any());
	}

	private PseudonymDTO request(String psn) {
		PseudonymDTO dto = new PseudonymDTO();
		dto.setIdentifierItem(IdentifierItem.builder().identifier("0012345678").idType("patient_number").build());
		dto.setPsn(psn);
		return dto;
	}
}
