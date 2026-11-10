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

package org.trustdeck.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Tests the minimal pseudonym length against Table 1 of Müller et al., J Med Internet Res 2025;27:e71822,
 * and against an exact reference calculation.
 */
class PseudonymLengthCalculatorTest {

	/** Success probability used in Table 1 of the paper. */
	private static final double TABLE_SUCCESS_PROBABILITY = 0.99999998d;

	/** Table 1, rows q = 10^1 ... 10^9 for T = 0.99999998. */
	@Test
	void matchesTableOneOfThePaper() {
		assertTableRow(10, 3, 4, 6, 7, 8, 10, 11, 12, 14, 15);
		assertTableRow(10, 5, 3, 4, 5, 7, 8, 9, 10, 11, 13);
		assertTableRow(10, 10, 2, 3, 4, 6, 7, 8, 9, 10, 11);
		assertTableRow(36, 3, 3, 4, 5, 5, 6, 7, 8, 9, 10);
		assertTableRow(36, 5, 2, 3, 4, 4, 5, 6, 7, 8, 8);
		assertTableRow(36, 10, 2, 2, 3, 4, 4, 5, 6, 7, 7);
	}

	/** Random parameters give exactly the same length as the exact reference calculation. */
	@Test
	void matchesExactReferenceForRandomParameters() {
		Random random = new Random(20250521L);
		List<String> mismatches = new ArrayList<>();

		for (int run = 0; run < 5_000; run++) {
			// q and 1-T are drawn log-uniformly, so that small and large values are covered equally
			long q = Math.max(1L, Math.min(100_000L, Math.round(Math.pow(10.0d, random.nextDouble() * 5.0d))));
			int m = 1 + random.nextInt(15);
			int a = 2 + random.nextInt(61);
			double failureProbability = Math.pow(10.0d, -15.0d + random.nextDouble() * 14.0d);

			int expected = exactReference(q, m, a, failureProbability);
			int actual = PseudonymLengthCalculator.minimalLength(q, m, a, failureProbability);
			if (expected != actual) {
				mismatches.add("q=" + q + ", m=" + m + ", a=" + a + ", 1-T=" + failureProbability + ": expected " + expected + ", got " + actual);
			}
		}

		assertTrue(mismatches.isEmpty(), mismatches.size() + " mismatches, e.g. " + mismatches.subList(0, Math.min(5, mismatches.size())));
	}

	/** A single pseudonym, a single attempt, and a binary alphabet are valid boundary values. */
	@Test
	void handlesSmallestValidParameters() {
		// q = 1, m = 1, a = 2, 1-T = 0.5: k = 2 gives exactly -ln(1/2) = tau, which is not enough; k = 4 is
		assertEquals(2, PseudonymLengthCalculator.minimalLength(1L, 1, 2, 0.5d));
		assertEquals(exactReference(1L, 3, 36, 1e-8d), PseudonymLengthCalculator.minimalLength(1L, 3, 36, 1e-8d));
		assertEquals(exactReference(1_000L, 1, 36, 1e-6d), PseudonymLengthCalculator.minimalLength(1_000L, 1, 36, 1e-6d));
		assertEquals(exactReference(1_000L, 3, 2, 1e-6d), PseudonymLengthCalculator.minimalLength(1_000L, 3, 2, 1e-6d));
	}

	/** The largest possible q gives a finite, plausible length instead of overflowing. */
	@Test
	void handlesLargestNumberOfPseudonyms() {
		int length = PseudonymLengthCalculator.minimalLength(Long.MAX_VALUE, 3, 36, 2e-8d);

		// The space must be larger than q, and close to the starting guess of the paper's approximation
		double lnK = (4 * Math.log(Long.MAX_VALUE) - Math.log(4 * -Math.log1p(-2e-8d))) / 3;
		int guess = (int) Math.ceil(lnK / Math.log(36));
		assertTrue(Math.pow(36, length) > Long.MAX_VALUE);
		assertTrue(length >= guess && length <= guess + 1, "length " + length + ", guess " + guess);

		// Also for a binary alphabet with a single attempt
		int binary = PseudonymLengthCalculator.minimalLength(Long.MAX_VALUE, 1, 2, 1e-15d);
		assertTrue(binary > 63 && binary < 300, "length " + binary);
	}

	/** Parameters without a meaningful result are rejected. */
	@Test
	void rejectsInvalidParameters() {
		assertThrows(IllegalArgumentException.class, () -> PseudonymLengthCalculator.minimalLength(0L, 3, 36, 1e-8d));
		assertThrows(IllegalArgumentException.class, () -> PseudonymLengthCalculator.minimalLength(-1L, 3, 36, 1e-8d));
		assertThrows(IllegalArgumentException.class, () -> PseudonymLengthCalculator.minimalLength(1_000L, 0, 36, 1e-8d));
		assertThrows(IllegalArgumentException.class, () -> PseudonymLengthCalculator.minimalLength(1_000L, 3, 1, 1e-8d));
		assertThrows(IllegalArgumentException.class, () -> PseudonymLengthCalculator.minimalLength(1_000L, 3, 36, 0.0d));
		assertThrows(IllegalArgumentException.class, () -> PseudonymLengthCalculator.minimalLength(1_000L, 3, 36, 1.0d));
		assertThrows(IllegalArgumentException.class, () -> PseudonymLengthCalculator.minimalLength(1_000L, 3, 36, -0.1d));
		assertThrows(IllegalArgumentException.class, () -> PseudonymLengthCalculator.minimalLength(1_000L, 3, 36, Double.NaN));
	}

	private void assertTableRow(int a, int m, int... expectedLengths) {
		for (int exponent = 1; exponent <= expectedLengths.length; exponent++) {
			long q = (long) Math.pow(10, exponent);
			assertEquals(expectedLengths[exponent - 1], PseudonymLengthCalculator.minimalLength(q, m, a, 1.0d - TABLE_SUCCESS_PROBABILITY),
					"a=" + a + ", m=" + m + ", q=10^" + exponent);
		}
	}

	/**
	 * Exact reference: for l = 1, 2, ..., take k = a^l and return the first l with k > q and
	 * sum_{i=1..q} -log1p(-(i/k)^m) < -log1p(-(1-T)).
	 *
	 * Two shortcuts keep it fast without changing the result: all terms are positive, so the summation stops
	 * once the sum reaches the budget, and a length is skipped when its largest term alone already does.
	 */
	private static int exactReference(long q, int m, int a, double failureProbability) {
		double tau = -Math.log1p(-failureProbability);
		for (int l = 1; ; l++) {
			double k = Math.pow(a, l);
			if (!(k > q) || -Math.log1p(-Math.pow(q / k, m)) >= tau) {
				continue;
			}

			double sum = 0.0d;
			for (long i = 1; i <= q && sum < tau; i++) {
				sum += -Math.log1p(-Math.pow(i / k, m));
			}

			if (sum < tau) {
				return l;
			}
		}
	}
}
