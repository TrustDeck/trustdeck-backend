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
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.trustdeck.service.PseudonymDBAccessService;
import org.trustdeck.service.ResponseService;
import org.trustdeck.utils.SpringBeanLocator;

/**
 * Tests that batch creation retries generated pseudonyms that collide, with the configured total number of
 * attempts, and never replaces supplied pseudonyms.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PseudonymControllerBatchRetryTest {

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

	@Spy
	private DefaultProperties defaults = new DefaultProperties();

	@InjectMocks
	private PseudonymController controller;

	private final Pseudonymizer pseudonymizer = mock(Pseudonymizer.class);
	private final Algorithm algorithm = new Algorithm().setId(1).setName("RANDOM_SYM").setSalt("salt").setAddCheckDigit(false);

	/** Pseudonyms that already exist in the domain. */
	private final Set<String> taken = new HashSet<>();

	/** The pseudonyms passed to each insert call. */
	private final List<List<String>> insertCalls = new ArrayList<>();

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
		when(responseService.unprocessableEntity(any())).thenReturn(ResponseEntity.unprocessableEntity().build());
		when(responseService.created(any(), any(List.class))).thenReturn(ResponseEntity.status(201).build());
		when(responseService.partialContent(any(), any(List.class))).thenReturn(ResponseEntity.status(206).build());

		// The database reports a pseudonym collision for every taken value and stores all others
		when(pseudonymDBAccessService.createPseudonyms(anyList(), anyInt(), anyBoolean())).thenAnswer(invocation -> {
			List<PseudonymDTO> pseudonyms = invocation.getArgument(0);
			List<String> values = pseudonyms.stream().map(PseudonymDTO::getPsn).toList();
			insertCalls.add(values);

			List<String> statuses = new ArrayList<>();
			for (String value : values) {
				statuses.add(taken.add(value) ? PseudonymDBAccessService.INSERTION_SUCCESS : PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM);
			}
			return statuses;
		});
	}

	/** Only the colliding generated pseudonym is regenerated; the supplied one keeps its value and stays rejected. */
	@Test
	void regeneratesOnlyCollidingGeneratedPseudonyms() {
		taken.add("TST-taken");
		taken.add("supplied-taken");
		when(pseudonymizer.pseudonymize(anyString(), anyString())).thenReturn("TST-taken", "TST-new-1", "TST-new-2");

		ResponseEntity<?> response = controller.createPseudonymBatch("test-domain", false,
				List.of(request("id-1", null), request("id-2", "supplied-taken"), request("id-3", null)), null);

		assertEquals(206, response.getStatusCode().value());
		assertEquals(List.of(List.of("TST-taken", "supplied-taken", "TST-new-1"), List.of("TST-new-2")), insertCalls);
	}

	/** A generated pseudonym gets the configured total number of attempts, the batch insert included. */
	@Test
	void stopsAfterConfiguredNumberOfAttempts() {
		taken.add("TST-taken");
		when(pseudonymizer.pseudonymize(anyString(), anyString())).thenReturn("TST-taken");

		ResponseEntity<?> response = controller.createPseudonymBatch("test-domain", false, List.of(request("id-1", null)), null);

		assertEquals(422, response.getStatusCode().value());
		assertEquals(defaults.getAlgorithm().getNumberOfAttempts(), insertCalls.size());
	}

	/** Without collisions, the batch is inserted once. */
	@Test
	void insertsOnceWithoutCollisions() {
		when(pseudonymizer.pseudonymize(anyString(), anyString())).thenReturn("TST-1", "TST-2");

		ResponseEntity<?> response = controller.createPseudonymBatch("test-domain", false, List.of(request("id-1", null), request("id-2", null)), null);

		assertEquals(201, response.getStatusCode().value());
		assertEquals(1, insertCalls.size());
	}

	/** Non-random algorithms are deterministic, so a collision is not retried. */
	@Test
	void doesNotRetryNonRandomAlgorithms() {
		algorithm.setName("MD5");
		taken.add("TST-taken");
		when(pseudonymizer.pseudonymize(anyString(), anyString())).thenReturn("TST-taken");

		controller.createPseudonymBatch("test-domain", false, List.of(request("id-1", null)), null);

		assertEquals(1, insertCalls.size());
	}

	private PseudonymDTO request(String identifier, String psn) {
		PseudonymDTO dto = new PseudonymDTO();
		dto.setIdentifierItem(IdentifierItem.builder().identifier(identifier).idType("patient_number").build());
		dto.setPsn(psn);
		return dto;
	}
}
