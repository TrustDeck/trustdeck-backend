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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;

/**
 * Tests that a child domain inheriting an updated algorithm gets its own copy with a new salt
 * instead of sharing the parent's algorithm record.
 */
@ExtendWith(MockitoExtension.class)
class DomainInheritedAlgorithmTest {

	@Mock
	private AlgorithmDBService algorithmDBService;

	@InjectMocks
	private DomainDBAccessService service;

	/** The copy keeps the configuration, including the length, but drops the ID and the salt. */
	@Test
	void createsCopyWithOwnSalt() {
		Algorithm parent = new Algorithm().setId(5).setName("RANDOM_SYM").setAlphabet("ABC").setRandomAlgorithmDesiredSize(1000L)
				.setRandomAlgorithmDesiredSuccessProbability(0.99d).setPseudonymLength(12).setAddCheckDigit(true)
				.setLengthIncludesCheckDigit(true).setPaddingCharacter("0").setSalt("parent-salt").setSaltLength(32);
		when(algorithmDBService.getAlgorithmByID(5)).thenReturn(parent);
		when(algorithmDBService.createOrGetAlgorithm(any())).thenReturn(6);

		assertEquals(6, service.createInheritedAlgorithm(5));

		ArgumentCaptor<Algorithm> captor = ArgumentCaptor.forClass(Algorithm.class);
		verify(algorithmDBService).createOrGetAlgorithm(captor.capture());
		Algorithm child = captor.getValue();
		assertNull(child.getId());
		assertNull(child.getSalt());
		assertEquals(parent.getSaltLength(), child.getSaltLength());
		assertEquals(parent.getPseudonymLength(), child.getPseudonymLength());
		assertEquals(parent.getRandomAlgorithmDesiredSize(), child.getRandomAlgorithmDesiredSize());
		assertEquals(parent.getAlphabet(), child.getAlphabet());
		// The parent's record itself is left untouched
		assertEquals("parent-salt", parent.getSalt());
	}

	/** A missing parent algorithm creates nothing, so the caller can abort the update. */
	@Test
	void returnsNullWithoutParentAlgorithm() {
		assertNull(service.createInheritedAlgorithm(5));
		verify(algorithmDBService, never()).createOrGetAlgorithm(any());
	}
}
