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

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.trustdeck.jooq.generated.Routines;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Folds the logged changes of the pseudonym and entity counts into the counter tables.
 *
 * Database triggers append every change of the number of pseudonyms per domain and of entities
 * per entity type to log tables, so that concurrent writers never wait for each other. Reads add
 * the log to the folded counts and are therefore always exact; folding only keeps the log short.
 * The database function skips the fold when another session is already folding, so several
 * backend instances can run this job.
 *
 * @author Armin Müller
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CountFoldService {

	/** References the TrustDeck database. */
	private final DSLContext dsl;

	/**
	 * Moves the logged count changes into the counter tables. A failed fold is only logged;
	 * the changes stay in the log for the next run.
	 */
	@Scheduled(fixedDelayString = "${statistics.count-fold-interval:30s}", initialDelayString = "${statistics.count-fold-interval:30s}")
	public void foldCountDeltas() {
		try {
			Routines.foldCountDeltas(dsl.configuration());
		} catch (DataAccessException e) {
			log.warn("Folding the logged pseudonym and entity count changes failed; retrying with the next run: " + e.getMessage());
		}
	}
}
