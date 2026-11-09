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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.jooq.generated.tables.pojos.Domain;

/**
 * Tests that the filling-rate check never blocks on the database, counts each
 * domain at most once per interval, and rate-limits its warning.
 */
class DomainFillingRateMonitorTest {

	private final DomainDBAccessService database = mock(DomainDBAccessService.class);
	private final DefaultProperties defaults = new DefaultProperties();
	private final MutableClock clock = new MutableClock();

	/** Background tasks are collected and run explicitly, so tests control when a count happens. */
	private final List<Runnable> background = new ArrayList<>();

	private DomainFillingRateMonitor monitor;
	private Domain domain;
	private Algorithm algorithm;

	@BeforeEach
	void setUp() {
		defaults.getAlgorithm().setFillingRateCheckInterval(Duration.ofMinutes(10));
		monitor = new DomainFillingRateMonitor(database, defaults, background::add, clock);

		domain = new Domain();
		domain.setId(7);
		domain.setName("test-domain");

		// Without an alphabet the ten digits are assumed: 10^3 possible pseudonyms; with T = 0.875 and 3 retries
		// the threshold is 1000 * 0.125^(1/3) = 500
		algorithm = new Algorithm();
		algorithm.setPseudonymLength(3);
		algorithm.setRandomAlgorithmDesiredSuccessProbability(0.875d);
	}

	/** The first collision only schedules the count; the request thread does not touch the database. */
	@Test
	void firstCollisionCountsInBackground() {
		when(database.getAmountOfPseudonymsInDomain("test-domain")).thenReturn(100);

		monitor.checkAfterCollision(domain, algorithm);

		verify(database, never()).getAmountOfPseudonymsInDomain("test-domain");
		assertEquals(1, background.size());

		background.getFirst().run();
		verify(database).getAmountOfPseudonymsInDomain("test-domain");
	}

	/** Within the interval the cached count is used without another database count. */
	@Test
	void reusesCachedCountWithinInterval() {
		when(database.getAmountOfPseudonymsInDomain("test-domain")).thenReturn(100);
		monitor.checkAfterCollision(domain, algorithm);
		background.removeFirst().run();

		clock.advance(Duration.ofMinutes(9));
		monitor.checkAfterCollision(domain, algorithm);

		assertTrue(background.isEmpty());
		verify(database, times(1)).getAmountOfPseudonymsInDomain("test-domain");
	}

	/** Collisions while a count is running do not start a second count for the same domain. */
	@Test
	void startsOnlyOneCountPerDomainAtATime() {
		monitor.checkAfterCollision(domain, algorithm);
		monitor.checkAfterCollision(domain, algorithm);
		monitor.checkAfterCollision(domain, algorithm);

		assertEquals(1, background.size());
	}

	/** An outdated count triggers a recount in the background. */
	@Test
	void recountsAfterInterval() {
		when(database.getAmountOfPseudonymsInDomain("test-domain")).thenReturn(100, 200);
		monitor.checkAfterCollision(domain, algorithm);
		background.removeFirst().run();

		clock.advance(Duration.ofMinutes(10));
		monitor.checkAfterCollision(domain, algorithm);

		assertEquals(1, background.size());
		background.removeFirst().run();
		verify(database, times(2)).getAmountOfPseudonymsInDomain("test-domain");
	}

	/** A failed count does not block later attempts. */
	@Test
	void retriesAfterFailedCount() {
		when(database.getAmountOfPseudonymsInDomain("test-domain")).thenReturn(null, 100);
		monitor.checkAfterCollision(domain, algorithm);
		background.removeFirst().run();

		monitor.checkAfterCollision(domain, algorithm);

		assertEquals(1, background.size());
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

	/** The number of possible pseudonyms uses the algorithm's alphabet size, not always ten symbols. */
	@Test
	void usesAlphabetSizeForPossiblePseudonyms() {
		// 2 symbols and length 3 give k = 2^3 = 8 possible pseudonyms; with T = 0.875 and 3 retries the threshold is 8 * 0.5 = 4
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
