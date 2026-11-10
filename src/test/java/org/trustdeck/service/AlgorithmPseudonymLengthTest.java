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

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;

/**
 * Tests how the minimal pseudonym length is applied to random algorithms: the calculated minimum, the check
 * digit, and a requested length. The calculation itself is tested in {@code PseudonymLengthCalculatorTest}.
 */
class AlgorithmPseudonymLengthTest {

	private AlgorithmDBService service;

	@BeforeEach
	void setUp() {
		service = new AlgorithmDBService();
		ReflectionTestUtils.setField(service, "dsl", DSL.using(SQLDialect.POSTGRES));
		ReflectionTestUtils.setField(service, "defaults", new DefaultProperties());
	}

	/** Without a requested length, a random algorithm gets the calculated minimum. */
	@Test
	void usesMinimumWithoutRequestedLength() {
		assertEquals(10, normalizedLength(randomAlgorithm(null, false, false)));
	}

	/** When the check digit replaces the last character, one more character is needed. */
	@Test
	void addsOneCharacterWhenCheckDigitReplacesLastCharacter() {
		assertEquals(11, normalizedLength(randomAlgorithm(null, true, true)));
		// An appended check digit does not reduce the random part
		assertEquals(10, normalizedLength(randomAlgorithm(null, true, false)));
	}

	/** A requested length that reaches the minimum is kept, a shorter one is raised to the minimum. */
	@Test
	void appliesRequestedLength() {
		assertEquals(20, normalizedLength(randomAlgorithm(20, false, false)));
		assertEquals(10, normalizedLength(randomAlgorithm(10, false, false)));
		assertEquals(10, normalizedLength(randomAlgorithm(5, false, false)));
		assertEquals(11, normalizedLength(randomAlgorithm(10, true, true)));
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

	/** A single pseudonym is a valid desired size and is not replaced by the default. */
	@Test
	void keepsDesiredSizeOfOne() {
		Algorithm algorithm = randomAlgorithm(null, false, false).setRandomAlgorithmDesiredSize(1L);

		assertEquals(1L, service.normalizeAlgorithm(algorithm).getRandomAlgorithmDesiredSize());
	}

	/** The configured number of attempts enters the calculation (paper, Table 1: m = 5 needs 8 characters). */
	@Test
	void usesConfiguredNumberOfAttempts() {
		DefaultProperties defaults = new DefaultProperties();
		defaults.getAlgorithm().setNumberOfAttempts(5);
		ReflectionTestUtils.setField(service, "defaults", defaults);

		assertEquals(8, normalizedLength(randomAlgorithm(null, false, false)));
	}

	/** At least one attempt per pseudonym is required. */
	@Test
	void rejectsNumberOfAttemptsBelowOne() {
		assertThrows(IllegalArgumentException.class, () -> new DefaultProperties().getAlgorithm().setNumberOfAttempts(0));
	}

	private int normalizedLength(Algorithm algorithm) {
		return service.normalizeAlgorithm(algorithm).getPseudonymLength();
	}

	/** A RANDOM_SYM algorithm (36 symbols) for 10^9 pseudonyms with T = 0.99999998, which needs 10 random characters (paper, Table 1). */
	private Algorithm randomAlgorithm(Integer requestedLength, boolean addCheckDigit, boolean lengthIncludesCheckDigit) {
		return new Algorithm().setName("RANDOM_SYM").setSaltLength(32)
				.setRandomAlgorithmDesiredSize(1_000_000_000L).setRandomAlgorithmDesiredSuccessProbability(0.99999998d)
				.setAddCheckDigit(addCheckDigit).setLengthIncludesCheckDigit(lengthIncludesCheckDigit)
				.setPseudonymLength(requestedLength);
	}
}
