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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.jooq.generated.tables.pojos.Domain;

/**
 * Tests that the filling-rate check reads the domain's pseudonym counter, never fails
 * the request, and rate-limits its warning.
 */
class DomainFillingRateMonitorTest {

	private final DomainDBAccessService database = mock(DomainDBAccessService.class);
	private final DefaultProperties defaults = new DefaultProperties();
	private final MutableClock clock = new MutableClock();

	private DomainFillingRateMonitor monitor;
	private Domain domain;
	private Algorithm algorithm;

	@BeforeEach
	void setUp() {
		monitor = new DomainFillingRateMonitor(database, defaults, clock);

		domain = new Domain();
		domain.setId(7);
		domain.setName("test-domain");

		// Without an alphabet the ten digits are assumed: 10^3 possible pseudonyms; with T = 0.875 and 3 attempts
		// the threshold is 1000 * 0.125^(1/3) = 500
		algorithm = new Algorithm();
		algorithm.setPseudonymLength(3);
		algorithm.setRandomAlgorithmDesiredSuccessProbability(0.875d);
	}

	/** Every collision reads the current counter of the domain and warns when it is above the threshold. */
	@Test
	void readsCounterOnEveryCollision() {
		when(database.getAmountOfPseudonymsInDomain(7)).thenReturn(100L, 600L);

		monitor.checkAfterCollision(domain, algorithm);
		monitor.checkAfterCollision(domain, algorithm);

		verify(database, times(2)).getAmountOfPseudonymsInDomain(7);
		// The second check already warned, so a direct check within the hour stays silent
		assertFalse(monitor.warnIfFull(domain, algorithm, 600));
	}

	/** A failing database read is only logged and does not fail the request. */
	@Test
	void ignoresFailedRead() {
		when(database.getAmountOfPseudonymsInDomain(7)).thenThrow(new IllegalStateException("database unavailable"));

		assertDoesNotThrow(() -> monitor.checkAfterCollision(domain, algorithm));
	}

	/** Without a domain ID or algorithm nothing is read. */
	@Test
	void skipsIncompleteInput() {
		monitor.checkAfterCollision(new Domain(), algorithm);
		monitor.checkAfterCollision(domain, null);

		verifyNoInteractions(database);
	}

	/** Above the threshold the warning is logged at most once per domain per hour. */
	@Test
	void rateLimitsWarning() {
		assertTrue(monitor.warnIfFull(domain, algorithm, 600));
		assertFalse(monitor.warnIfFull(domain, algorithm, 600));

		clock.advance(Duration.ofMinutes(59));
		assertFalse(monitor.warnIfFull(domain, algorithm, 600));

		clock.advance(Duration.ofMinutes(1));
		assertTrue(monitor.warnIfFull(domain, algorithm, 600));
	}

	/** Below the threshold nothing is logged. */
	@Test
	void doesNotWarnBelowThreshold() {
		assertFalse(monitor.warnIfFull(domain, algorithm, 400));
	}

	/** A check digit that replaces the last character leaves one random character less. */
	@Test
	void subtractsCheckDigitThatReplacesLastCharacter() {
		// 10 digits and length 3, of which the check digit replaces one: k = 10^2 = 100, threshold 100 * 0.5 = 50
		algorithm.setAddCheckDigit(true);
		algorithm.setLengthIncludesCheckDigit(true);

		assertFalse(monitor.warnIfFull(domain, algorithm, 50));
		assertTrue(monitor.warnIfFull(domain, algorithm, 51));

		// An appended check digit keeps all three characters random: k = 1000, threshold 500
		Domain other = new Domain();
		other.setId(9);
		other.setName("other-domain");
		algorithm.setLengthIncludesCheckDigit(false);
		assertFalse(monitor.warnIfFull(other, algorithm, 51));
	}

	/** The number of possible pseudonyms uses the algorithm's alphabet size, not always ten symbols. */
	@Test
	void usesAlphabetSizeForPossiblePseudonyms() {
		// 2 symbols and length 3 give k = 2^3 = 8 possible pseudonyms; with T = 0.875 and 3 attempts the threshold is 8 * 0.5 = 4
		algorithm.setAlphabet("AB");

		assertFalse(monitor.warnIfFull(domain, algorithm, 4));
		assertTrue(monitor.warnIfFull(domain, algorithm, 5));

		// With 36 symbols, k = 36^3 = 46656 and the threshold is 23328; 600 pseudonyms are far below it
		Domain other = new Domain();
		other.setId(8);
		other.setName("other-domain");
		algorithm.setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
		assertFalse(monitor.warnIfFull(other, algorithm, 600));
	}

	/** Clock that tests can move forward. */
	private static final class MutableClock extends Clock {

		private Instant now = Instant.parse("2026-11-06T12:00:00Z");

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}
}
