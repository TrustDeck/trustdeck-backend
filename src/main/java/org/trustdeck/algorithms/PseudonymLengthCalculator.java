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

/*
 * Minimal pseudonym length for random-character pseudonyms
 * ========================================================
 * Based on Müller et al., J Med Internet Res 2025;27:e71822, "Pseudonymization Algorithms".
 *
 * Notation
 *   q  maximum number of pseudonyms the domain must hold
 *   m  total number of attempts per new pseudonym
 *   a  alphabet size
 *   l  pseudonym length (random part only, without prefix, check digit or padding)
 *   k  size of the pseudonym space, k = a^l
 *   T  required probability that the whole domain can be filled without an error
 *
 * 1. The model
 *    With i pseudonyms already taken, one attempt hits a taken value with probability i/k,
 *    so all m attempts fail with probability (i/k)^m. The domain can be filled if
 *        P = prod_{i=0..q} (1 - (i/k)^m) > T                          (paper, eq. 3)
 *
 * 2. From a product to a sum
 *    Multiplying ~10^9 numbers just below 1 is slow and numerically fragile. Taking the
 *    logarithm turns the product into a sum; flipping the sign makes all terms positive:
 *        S(k) := sum_{i=1..q} -ln(1 - (i/k)^m)  <  tau := -ln T
 *    tau is the "error budget" (tau ~ 1 - T, e.g. 2e-8 for T = 0.99999998). It is computed
 *    as -log1p(-(1 - T)) to avoid rounding problems for T close to 1.
 *
 * 3. Only powers of a need to be tested
 *    A larger k makes every (i/k)^m smaller, so S(k) only decreases as k grows: if a length
 *    works, every longer length works too. Since k = a^l anyway, it suffices to find the
 *    smallest l for which k = a^l works. This equals the paper's l = ceil(log_a k_min).
 *
 * 4. A good starting guess
 *    (a) For tiny x, -ln(1 - x) ~ x, so  S(k) ~ (1^m + 2^m + ... + q^m) / k^m.
 *    (b) The sum of powers is a staircase of bars of width 1 and heights 1^m ... q^m that
 *        hugs the curve y = x^m, so it is about the area under that curve from 0 to q:
 *        q^(m+1) / (m+1).
 *    Together: S(k) ~ q^(m+1) / ((m+1) k^m). Setting this equal to tau and solving for k
 *    (in logarithms, to avoid overflowing q^(m+1)):
 *        ln k ~ ((m+1) ln q - ln((m+1) tau)) / m,      l ~ ln k / ln a
 *    Example: q = 10^9, m = 3, T = 0.99999998 gives k ~ 2.3e14, i.e. 15 digits for a = 10.
 *    The code starts at this guess and walks up or down to the exact minimal length.
 *
 * 5. Guaranteed bounds instead of approximations
 *    To decide exactly without adding up q terms, both approximations are replaced by
 *    inequalities that always hold, giving a lower bound L and an upper bound U with
 *    L <= S(k) <= U.
 *
 *    (a) The logarithm. Its series is -ln(1 - x) = x + x^2/2 + x^3/3 + x^4/4 + ...
 *        - Lower bound: all terms are positive, so it is at least x + x^2/2.
 *        - Upper bound: replacing the divisors 3, 4, 5, ... by 2 only makes the terms
 *          bigger: x + x^2/2 * (1 + x + x^2 + ...) = x + x^2 / (2 (1 - x)).
 *        So:  x + x^2/2  <=  -ln(1 - x)  <=  x + x^2 / (2 (1 - x)).
 *        With x_i = (i/k)^m, S(k) is bounded by sums of x_i and x_i^2. The largest x_i is
 *        xMax = (q/k)^m, which is used in the upper bound.
 *
 *    (b) Staircase vs. curve. For p >= 1, y = x^p bends upward (convex). Two facts follow:
 *        - Lower bound (trapezoids): on each stretch [i-1, i], draw the trapezoid whose two
 *          parallel sides are the curve heights (i-1)^p and i^p. Its area is
 *          ((i-1)^p + i^p) / 2. On an upward-bending curve, the straight line between two
 *          points lies ABOVE the curve, so every trapezoid has at least the area under the
 *          curve. Summing all trapezoids, every inner value 1^p ... (q-1)^p appears twice
 *          with weight 1/2 (fully counted), only the ends appear once:
 *              0^p/2 + 1^p + ... + (q-1)^p + q^p/2  =  (1^p + ... + q^p) - q^p/2
 *          Since this is >= q^(p+1)/(p+1):
 *              1^p + ... + q^p  >=  q^(p+1)/(p+1) + q^p/2
 *        - Upper bound (centred bars): centre each bar of height i^p on x = i, covering
 *          [i - 1/2, i + 1/2]. The curve dips below the bar on the left and rises above it
 *          on the right; since it bends upward, the rise wins, so the area under the curve
 *          is at least the bar's area. Summing:
 *              1^p + ... + q^p  <=  area from 1/2 to q + 1/2  <=  (q + 1/2)^(p+1) / (p+1)
 *        Check with p = 2, q = 4: 29.33 <= 30 <= 30.375.
 *        These are applied with p = m (for the x_i) and p = 2m (for the x_i^2 terms; there,
 *        the lower bound uses only the integral term, which is still valid).
 *
 *    Resulting bounds (written via r = q/k <= 1, i.e. q^(m+1)/k^m = q * r^m, so that
 *    nothing overflows):
 *        L = q^(m+1)/((m+1) k^m) + q^m/(2 k^m) + q^(2m+1)/(2 (2m+1) k^(2m))
 *        U = (q+1/2)^(m+1)/((m+1) k^m) + (q+1/2)^(2m+1)/((2m+1) k^(2m)) / (2 (1 - xMax))
 *
 * 6. Decision
 *    - U < tau   -> length works for sure.
 *    - L >= tau  -> length does not work for sure.
 *    - otherwise -> undecided (L and U differ only marginally, so this is rare and in tests
 *      occurred only for small q). Then the exact sum is computed if q <= 10^6 (~10 ms);
 *      for larger q the length is conservatively treated as not working. In that case the
 *      result can be one character longer than necessary, but never too short.
 *    A relative safety margin of 1e-12 guards against floating-point rounding.
 *
 * Complexity: O(1) per call (a few Math.pow/Math.log calls, ~0.2-0.3 microseconds);
 * worst case O(q) with q <= 10^6 for the rare exact fallback.
 */

/**
 * Calculates the minimal length of the random part of random-character pseudonyms,
 * following the method described above.
 *
 * @author Armin Müller
 */
public final class PseudonymLengthCalculator {

	/** Upper limit for the calculated length; reaching it means no feasible length exists. */
	private static final int MAX_LENGTH = 1000;

	/** Largest number of pseudonyms for which an undecided length is checked with the exact sum. */
	private static final long EXACT_FALLBACK_LIMIT = 1_000_000L;

	/** Utility class; not instantiable. */
	private PseudonymLengthCalculator() {
	}

	/**
	 * Calculates the minimal length of the random part of the pseudonyms, so that a domain can hold
	 * the desired number of pseudonyms with the required probability.
	 *
	 * @param q the maximum number of pseudonyms the domain must hold, at least 1
	 * @param m the total number of attempts per new pseudonym, at least 1
	 * @param a the alphabet size, at least 2
	 * @param failureProbability the accepted probability 1 - T that filling the domain fails, strictly between 0 and 1
	 * @return the minimal number of random characters (without prefix, check digit or padding)
	 * @throws IllegalArgumentException if a parameter is outside its valid range
	 * @throws IllegalStateException if no length up to the internal maximum is feasible
	 */
	public static int minimalLength(long q, int m, int a, double failureProbability) {
		if (q < 1 || m < 1 || a < 2 || !(failureProbability > 0 && failureProbability < 1)) {
			throw new IllegalArgumentException("Invalid parameters for the pseudonym length calculation (q=" + q + ", m=" + m
					+ ", a=" + a + ", failure probability=" + failureProbability + ").");
		}

		double tau = -Math.log1p(-failureProbability);
		double lnK = ((m + 1) * Math.log(q) - Math.log((m + 1) * tau)) / m;
		int l = Math.max(1, Math.min(MAX_LENGTH, (int) Math.floor(lnK / Math.log(a))));

		if (feasible(q, m, a, l, tau)) {
			while (l > 1 && feasible(q, m, a, l - 1, tau)) {
				l--;
			}
		} else {
			do {
				if (++l > MAX_LENGTH) {
					throw new IllegalStateException("No feasible pseudonym length up to " + MAX_LENGTH + " characters.");
				}
			} while (!feasible(q, m, a, l, tau));
		}

		return l;
	}

	/**
	 * Decides whether the pseudonym length l is long enough.
	 *
	 * @param q the maximum number of pseudonyms
	 * @param m the total number of attempts per new pseudonym
	 * @param a the alphabet size
	 * @param l the pseudonym length to check
	 * @param tau the error budget -ln T
	 * @return {@code true} if the length works, {@code false} if it does not or cannot be decided
	 */
	private static boolean feasible(long q, int m, int a, int l, double tau) {
		double k = Math.pow(a, l);
		if (k <= q) {
			return false;
		}

		int d = boundsDecision(q, m, k, tau);
		if (d != 0) {
			return d > 0;
		}

		return q <= EXACT_FALLBACK_LIMIT && feasibleExact(q, m, k, tau);
	}

	/**
	 * Decides the length with the guaranteed lower and upper bounds of S(k).
	 *
	 * @param q the maximum number of pseudonyms
	 * @param m the total number of attempts per new pseudonym
	 * @param k the size of the pseudonym space
	 * @param tau the error budget -ln T
	 * @return {@code 1} if the length works for sure, {@code -1} if it does not work for sure, {@code 0} if undecided
	 */
	private static int boundsDecision(long q, int m, double k, double tau) {
		double qd = q;
		double xMax = Math.pow(qd / k, m);
		if (xMax >= 1.0) {
			return -1;
		}

		double rh = (qd + 0.5) / k;
		double sumXLo = qd * xMax / (m + 1) + xMax / 2.0;
		double sumX2Lo = qd * xMax * xMax / (2 * m + 1);
		double sumXHi = (qd + 0.5) * Math.pow(rh, m) / (m + 1);
		double sumX2Hi = (qd + 0.5) * Math.pow(rh, 2.0 * m) / (2 * m + 1);
		double lower = sumXLo + sumX2Lo / 2.0;
		double upper = sumXHi + sumX2Hi / (2.0 * (1.0 - xMax));

		if (upper * (1 + 1e-12) < tau) {
			return 1;
		}

		if (lower * (1 - 1e-12) >= tau) {
			return -1;
		}

		return 0;
	}

	/**
	 * Decides the length with the exact sum S(k), using Kahan summation and stopping as soon as the
	 * error budget is exceeded.
	 *
	 * @param q the maximum number of pseudonyms
	 * @param m the total number of attempts per new pseudonym
	 * @param k the size of the pseudonym space
	 * @param tau the error budget -ln T
	 * @return {@code true} if the length works, {@code false} otherwise
	 */
	private static boolean feasibleExact(long q, int m, double k, double tau) {
		double sum = 0.0, c = 0.0;
		for (long i = q; i >= 1; i--) {
			double x = Math.pow(i / k, m);
			if (x >= 1.0) {
				return false;
			}

			double y = -Math.log1p(-x) - c;
			double t = sum + y;
			c = (t - sum) - y;
			sum = t;
			if (sum >= tau) {
				return false;
			}
		}

		return sum < tau;
	}
}
