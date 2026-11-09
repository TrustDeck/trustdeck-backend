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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;

/**
 * Tests the calculation of the shortest pseudonym length for random algorithms and how a requested
 * length is applied.
 */
class AlgorithmPseudonymLengthTest {

	private AlgorithmDBService service;

	@BeforeEach
	void setUp() {
		service = new AlgorithmDBService();
		ReflectionTestUtils.setField(service, "dsl", DSL.using(SQLDialect.POSTGRES));
		ReflectionTestUtils.setField(service, "defaults", new DefaultProperties());
	}

	/** Known results: k = n/((1-T)^(1/m)), length = ceil(log(k)/log(alphabet size)). */
	@Test
	void calculatesShortestLength() {
		// 10^9 pseudonyms, T = 0.999999998, 36 symbols, 3 retries: k ≈ 7.94 * 10^11, log36(k) ≈ 7.65
		assertEquals(8, AlgorithmDBService.calculatePseudonymLength(1_000_000_000L, 0.999999998d, 36, 3));
		// 10^8 pseudonyms, T = 0.99999998, 26 symbols: k ≈ 3.68 * 10^10, log26(k) ≈ 7.47
		assertEquals(8, AlgorithmDBService.calculatePseudonymLength(100_000_000L, 0.99999998d, 26, 3));
		// Digits only need more characters for the same number of pseudonyms
		assertEquals(12, AlgorithmDBService.calculatePseudonymLength(1_000_000_000L, 0.999999998d, 10, 3));
	}

	/** At an exact power of the alphabet size, rounding must not add a character. */
	@Test
	void doesNotOvershootAtExactPowers() {
		// One retry and T = 0.5 give k = 2n = 36^4 exactly, which needs exactly 4 characters
		assertEquals(4, AlgorithmDBService.calculatePseudonymLength(839_808L, 0.5d, 36, 1));
		// One pseudonym more needs a fifth character
		assertEquals(5, AlgorithmDBService.calculatePseudonymLength(839_809L, 0.5d, 36, 1));
	}

	/** Extreme parameters stay finite and plausible instead of overflowing. */
	@Test
	void handlesExtremeParameters() {
		int length = AlgorithmDBService.calculatePseudonymLength(Long.MAX_VALUE, 1.0d - 1e-15d, 2, 1);

		assertTrue(length > 63 && length < 120, "length " + length);
	}

	/** Parameters without a meaningful result are rejected. */
	@Test
	void rejectsInvalidParameters() {
		assertThrows(IllegalArgumentException.class, () -> AlgorithmDBService.calculatePseudonymLength(0L, 0.9d, 36, 3));
		assertThrows(IllegalArgumentException.class, () -> AlgorithmDBService.calculatePseudonymLength(1000L, 1.0d, 36, 3));
		assertThrows(IllegalArgumentException.class, () -> AlgorithmDBService.calculatePseudonymLength(1000L, 0.0d, 36, 3));
		assertThrows(IllegalArgumentException.class, () -> AlgorithmDBService.calculatePseudonymLength(1000L, 0.9d, 1, 3));
		assertThrows(IllegalArgumentException.class, () -> AlgorithmDBService.calculatePseudonymLength(1000L, 0.9d, 36, 0));
	}

	/** Without a requested length, a random algorithm gets the calculated minimum. */
	@Test
	void usesMinimumWithoutRequestedLength() {
		assertEquals(8, normalizedLength(randomAlgorithm(null, false, false)));
	}

	/** When the check digit replaces the last character, one more character is needed. */
	@Test
	void addsOneCharacterWhenCheckDigitReplacesLastCharacter() {
		assertEquals(9, normalizedLength(randomAlgorithm(null, true, true)));
		// An appended check digit does not reduce the random part
		assertEquals(8, normalizedLength(randomAlgorithm(null, true, false)));
	}

	/** A requested length that reaches the minimum is kept, a shorter one is raised to the minimum. */
	@Test
	void appliesRequestedLength() {
		assertEquals(20, normalizedLength(randomAlgorithm(20, false, false)));
		assertEquals(8, normalizedLength(randomAlgorithm(8, false, false)));
		assertEquals(8, normalizedLength(randomAlgorithm(5, false, false)));
		assertEquals(9, normalizedLength(randomAlgorithm(8, true, true)));
	}

	/** Non-random algorithms keep their length rules: the requested length, or the default. */
	@Test
	void leavesNonRandomAlgorithmsUnchanged() {
		Algorithm md5 = new Algorithm().setName("MD5").setSaltLength(32).setPseudonymLength(32);
		assertEquals(32, normalizedLength(md5));

		Algorithm withoutLength = new Algorithm().setName("MD5").setSaltLength(32);
		assertEquals(new DefaultProperties().getAlgorithm().getPseudonymLength(), normalizedLength(withoutLength));
	}

	/** A success probability of 1 cannot be reached; it falls back to the default instead of failing. */
	@Test
	void replacesUnreachableSuccessProbability() {
		Algorithm algorithm = randomAlgorithm(null, false, false).setRandomAlgorithmDesiredSuccessProbability(1.0d);

		assertEquals(new DefaultProperties().getAlgorithm().getRandomDesiredSuccessProbability(),
				service.normalizeAlgorithm(algorithm).getRandomAlgorithmDesiredSuccessProbability());
	}

	private int normalizedLength(Algorithm algorithm) {
		return service.normalizeAlgorithm(algorithm).getPseudonymLength();
	}

	/** A RANDOM_SYM algorithm (36 symbols) for 10^9 pseudonyms with T = 0.999999998, which needs 8 random characters. */
	private Algorithm randomAlgorithm(Integer requestedLength, boolean addCheckDigit, boolean lengthIncludesCheckDigit) {
		return new Algorithm().setName("RANDOM_SYM").setSaltLength(32)
				.setRandomAlgorithmDesiredSize(1_000_000_000L).setRandomAlgorithmDesiredSuccessProbability(0.999999998d)
				.setAddCheckDigit(addCheckDigit).setLengthIncludesCheckDigit(lengthIncludesCheckDigit)
				.setPseudonymLength(requestedLength);
	}
}
