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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.jooq.generated.tables.pojos.Domain;

/**
 * Warns when a domain with a random algorithm approaches the filling point at
 * which generating an unused pseudonym within the configured attempts is no
 * longer reasonably probable.
 *
 * The number of pseudonyms in the domain is read from the counter that database
 * triggers keep up to date, a single-row lookup. The warning is logged at most
 * once per domain per {@link #WARNING_INTERVAL}.
 *
 * @author Armin Müller
 */
@Slf4j
@Service
public class DomainFillingRateMonitor {

	/** Minimum time between two filling-rate warnings for the same domain. */
	static final Duration WARNING_INTERVAL = Duration.ofHours(1);

	/** Reads the number of pseudonyms in a domain. */
	private final DomainDBAccessService domainDBAccessService;

	/** Provides the number of attempts. */
	private final DefaultProperties defaults;

	/** Time source, replaceable in tests. */
	private final Clock clock;

	/** Time of the last warning per domain ID. */
	private final Map<Integer, Instant> lastWarnings = new ConcurrentHashMap<>();

	/**
	 * Creates the monitor.
	 *
	 * @param domainDBAccessService service used to read the number of pseudonyms in a domain
	 * @param defaults default properties containing the number of attempts
	 */
	@Autowired
	public DomainFillingRateMonitor(DomainDBAccessService domainDBAccessService, DefaultProperties defaults) {
		this(domainDBAccessService, defaults, Clock.systemUTC());
	}

	/**
	 * Creates the monitor with an explicit clock.
	 *
	 * @param domainDBAccessService service used to read the number of pseudonyms in a domain
	 * @param defaults default properties containing the number of attempts
	 * @param clock time source
	 */
	DomainFillingRateMonitor(DomainDBAccessService domainDBAccessService, DefaultProperties defaults, Clock clock) {
		this.domainDBAccessService = domainDBAccessService;
		this.defaults = defaults;
		this.clock = clock;
	}

	/**
	 * Checks the filling rate after a generated pseudonym collided with an existing one.
	 * A failed check is only logged, so that it never fails the request.
	 *
	 * @param domain domain in which the collision occurred
	 * @param algorithm the domain's algorithm
	 */
	public void checkAfterCollision(Domain domain, Algorithm algorithm) {
		if (domain == null || domain.getId() == null || algorithm == null) {
			return;
		}

		try {
			warnIfFull(domain, algorithm, domainDBAccessService.getAmountOfPseudonymsInDomain(domain.getId()));
		} catch (RuntimeException exception) {
			log.debug("Checking the filling rate of domain \"" + domain.getName() + "\" failed: " + exception.getMessage());
		}
	}

	/**
	 * Logs the filling-rate warning if the count exceeds the domain's threshold and
	 * the last warning for the domain is older than {@link #WARNING_INTERVAL}.
	 *
	 * @param domain domain to evaluate
	 * @param algorithm the domain's algorithm
	 * @param count number of pseudonyms in the domain
	 * @return {@code true} if the warning was logged
	 */
	boolean warnIfFull(Domain domain, Algorithm algorithm, long count) {
		if (algorithm.getRandomAlgorithmDesiredSuccessProbability() == null || algorithm.getPseudonymLength() == null) {
			return false;
		}

		/* A high filling rate of the domain can result in too few possible pseudonyms left for generation.
		* We can calculate a threshold for which we can reasonably probable generate pseudonyms using this formula: 
		* 1-(1-((k-n)/k))^m > T => n < k*(1-T)^(1/m); with k being the number of theoretically possible pseudonyms 
		* (here number of alphabet-symbols a and psnLength characters, so k=a^psnLength); n being the number of 
		* reasonable probable pseudonyms; m being the number of attempts; and T being the probability threshold which 
		* we deem to be reasonable probable (here: 99.999998% (probability of not getting struck by lightning), 
		* meaning that we want to successfully generate a pseudonym in a maximum of m=3 tries in 99.999998% of all 
		* cases).
		* --> Warn the user, if the domain is too full
		*/
		// Every algorithm stores its alphabet (see Utility.generateAlphabet); fall back to the ten digits if it is missing
		int a = algorithm.getAlphabet() == null || algorithm.getAlphabet().isEmpty() ? 10 : algorithm.getAlphabet().length();

		// When the check digit replaces the last character, that character is not random and does not add pseudonyms
		boolean checkDigitReplacesLastCharacter = Boolean.TRUE.equals(algorithm.getAddCheckDigit())
				&& Boolean.TRUE.equals(algorithm.getLengthIncludesCheckDigit());
		int psnLength = algorithm.getPseudonymLength() - (checkDigitReplacesLastCharacter ? 1 : 0);

		Double T = algorithm.getRandomAlgorithmDesiredSuccessProbability();
		Double k = Math.pow((double) a, (double) psnLength);
		Double n = k * Math.pow((1.0d - T), (1.0d / (double) defaults.getAlgorithm().getNumberOfAttempts()));

		if (!(count > n)) {
			return false;
		}

		// Warn at most once per interval. compute() decides atomically per domain, so the decision has to be made
		// inside its lambda. A plain "boolean due" cannot be used for the result: Java lambdas may only read local
		// variables that are effectively final, i.e. never reassigned, so "due = true" inside the lambda does not
		// compile. The AtomicBoolean reference stays final while its value can change, which lets the lambda
		// report the decision back. (A one-element boolean array would work the same way but reads less clearly.)
		Instant now = clock.instant();
		AtomicBoolean due = new AtomicBoolean(false);
		lastWarnings.compute(domain.getId(), (id, last) -> {
			if (last == null || !last.plus(WARNING_INTERVAL).isAfter(now)) {
				due.set(true);
				return now;
			}

			return last;
		});

		if (due.get()) {
			// The domain reached it's filling point (~49%) at which the probability to generate a 
			// previously unseen pseudonym in m tries is no longer greater than 99.999998%.
			log.warn("The number of reasonably probable generated pseudonyms for domain \"" + domain.getName() + "\" is exhausted. "
					+ "The probability to generate collisions will rise and at some point prevent the generation of "
					+ "non-colliding pseudonyms in a certain number of attempts.");
		}

		return due.get();
	}
}
