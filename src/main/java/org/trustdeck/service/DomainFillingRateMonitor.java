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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.jooq.generated.tables.pojos.Domain;

/**
 * Warns when a domain with a random algorithm approaches the filling point at
 * which generating an unused pseudonym within the configured retries is no
 * longer reasonably probable.
 *
 * The warning needs the number of pseudonyms in the domain, which is a full
 * count over a potentially very large table. Requests therefore never wait for
 * it: counts are cached per domain for
 * {@code app.defaults.algorithm.filling-rate-check-interval}, a missing or
 * outdated count is refreshed in the background by a single thread, and at most
 * one count per domain runs at a time. The warning itself is logged at most once
 * per domain per {@link #WARNING_INTERVAL}.
 *
 * @author Armin Müller
 */
@Slf4j
@Service
public class DomainFillingRateMonitor {

	/** Minimum time between two filling-rate warnings for the same domain. */
	static final Duration WARNING_INTERVAL = Duration.ofHours(1);

	/** Counts the pseudonyms of a domain. */
	private final DomainDBAccessService domainDBAccessService;

	/** Provides the number of retries and the cache interval. */
	private final DefaultProperties defaults;

	/** Runs the counts outside the request thread. */
	private final Executor executor;

	/** Time source, replaceable in tests. */
	private final Clock clock;

	/** Last known pseudonym count per domain ID. */
	private final Map<Integer, CachedCount> counts = new ConcurrentHashMap<>();

	/** Domain IDs whose count is currently running. */
	private final Set<Integer> counting = ConcurrentHashMap.newKeySet();

	/** Time of the last warning per domain ID. */
	private final Map<Integer, Instant> lastWarnings = new ConcurrentHashMap<>();

	/**
	 * Creates the monitor with its own single background thread.
	 *
	 * @param domainDBAccessService service used to count the pseudonyms of a domain
	 * @param defaults default properties containing the retries and the cache interval
	 */
	@Autowired
	public DomainFillingRateMonitor(DomainDBAccessService domainDBAccessService, DefaultProperties defaults) {
		this(domainDBAccessService, defaults, Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "domain-filling-rate");
			thread.setDaemon(true);
			return thread;
		}), Clock.systemUTC());
	}

	/**
	 * Creates the monitor with an explicit executor and clock.
	 *
	 * @param domainDBAccessService service used to count the pseudonyms of a domain
	 * @param defaults default properties containing the retries and the cache interval
	 * @param executor executor that runs the counts
	 * @param clock time source
	 */
	DomainFillingRateMonitor(DomainDBAccessService domainDBAccessService, DefaultProperties defaults, Executor executor, Clock clock) {
		this.domainDBAccessService = domainDBAccessService;
		this.defaults = defaults;
		this.executor = executor;
		this.clock = clock;
	}

	/**
	 * Checks the filling rate after a generated pseudonym collided with an existing one.
	 *
	 * Uses the cached count when it is recent enough. Otherwise a background count
	 * is started (unless one is already running for the domain) and an outdated
	 * cached count, if any, is used in the meantime. The method never blocks on the
	 * database.
	 *
	 * @param domain domain in which the collision occurred
	 * @param algorithm the domain's algorithm
	 */
	public void checkAfterCollision(Domain domain, Algorithm algorithm) {
		if (domain == null || domain.getId() == null || algorithm == null) {
			return;
		}

		CachedCount cached = counts.get(domain.getId());
		Instant now = clock.instant();
		if (cached == null || !cached.measuredAt().plus(defaults.getAlgorithm().getFillingRateCheckInterval()).isAfter(now)) {
			refresh(domain, algorithm);
		}

		if (cached != null) {
			warnIfFull(domain, algorithm, cached.count());
		}
	}

	/**
	 * Starts a background count for a domain unless one is already running.
	 *
	 * @param domain domain to count
	 * @param algorithm the domain's algorithm, used to evaluate the fresh count
	 */
	private void refresh(Domain domain, Algorithm algorithm) {
		Integer id = domain.getId();
		if (!counting.add(id)) {
			return;
		}

		try {
			executor.execute(() -> {
				try {
					Integer count = domainDBAccessService.getAmountOfPseudonymsInDomain(domain.getName());
					if (count != null && count >= 0) {
						counts.put(id, new CachedCount(count, clock.instant()));
						warnIfFull(domain, algorithm, count);
					}
				} catch (RuntimeException exception) {
					log.debug("Counting the pseudonyms of domain \"" + domain.getName() + "\" failed: " + exception.getMessage());
				} finally {
					counting.remove(id);
				}
			});
		} catch (RejectedExecutionException exception) {
			// Shutting down; the next collision will try again
			counting.remove(id);
		}
	}

	/**
	 * Logs the filling-rate warning if the count exceeds the domain's threshold and
	 * the last warning for the domain is older than {@link #WARNING_INTERVAL}.
	 *
	 * A high filling rate leaves too few unused pseudonyms for generation. The
	 * threshold follows from 1-(1-((k-n)/k))^m > T, i.e. n < k*(1-T)^(1/m), with
	 * k = 10^pseudonymLength theoretically possible pseudonyms, n the number of
	 * pseudonyms that can still be generated with reasonable probability, m the
	 * number of retries, and T the desired success probability.
	 *
	 * @param domain domain to evaluate
	 * @param algorithm the domain's algorithm
	 * @param count number of pseudonyms in the domain
	 * @return {@code true} if the warning was logged
	 */
	boolean warnIfFull(Domain domain, Algorithm algorithm, int count) {
		Double successProbability = algorithm.getRandomAlgorithmDesiredSuccessProbability();
		Integer length = algorithm.getPseudonymLength();
		if (successProbability == null || length == null) {
			return false;
		}

		double possible = Math.pow(10.0d, length);
		double threshold = possible * Math.pow(1.0d - successProbability, 1.0d / defaults.getAlgorithm().getNumberOfRetries());
		if (count <= threshold) {
			return false;
		}

		Instant now = clock.instant();
		boolean[] due = {false};
		lastWarnings.compute(domain.getId(), (id, last) -> {
			if (last == null || !last.plus(WARNING_INTERVAL).isAfter(now)) {
				due[0] = true;
				return now;
			}

			return last;
		});

		if (due[0]) {
			// The domain reached its filling point (~49%) at which the probability to generate a
			// previously unseen pseudonym in m tries is no longer greater than the desired probability.
			log.warn("The number of reasonably probable generated pseudonyms for domain \"" + domain.getName() + "\" is exhausted. "
					+ "The probability to generate collisions will rise and at some point prevent the generation of "
					+ "non-colliding pseudonyms in a certain number of retries.");
		}

		return due[0];
	}

	/** Stops the background thread when the application shuts down. */
	@PreDestroy
	void shutdown() {
		if (executor instanceof ExecutorService service) {
			service.shutdownNow();
		}
	}

	/**
	 * A pseudonym count and the time it was measured.
	 *
	 * @param count number of pseudonyms in the domain
	 * @param measuredAt time of the count
	 */
	private record CachedCount(int count, Instant measuredAt) { }
}
