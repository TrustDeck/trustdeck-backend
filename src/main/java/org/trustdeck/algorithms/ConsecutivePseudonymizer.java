/*
 * Trust Deck Services
 * Copyright 2021-2024 Armin Müller and Eric Wündisch
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

import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.service.AlgorithmDBService;

import lombok.extern.slf4j.Slf4j;

/**
 * This class provides a pseudonymization by assigning consecutive numbers as pseudonyms.
 * <b>This class is not thread safe.</b>
 * 
 * @author Armin Müller
 *
 */
@Slf4j
public class ConsecutivePseudonymizer extends Pseudonymizer {
	
	/** Stores a user-given value to start the counting from. */
	private Long startValue;
	
	/**
	 * Basic constructor.
	 * All necessary variables are directly retrieved from the algorithm object.
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param algorithm the algorithm object
	 */
	public ConsecutivePseudonymizer(boolean paddingWanted, Algorithm algorithm, DefaultProperties defaults, AlgorithmDBService algorithmDBService) {
		super(paddingWanted, algorithm, defaults, algorithmDBService);
		this.startValue = null;
	}

	/**
	 * Creates a consecutive pseudonym. The identifier, however, is not actively used here.
	 */
	@Override
	public String pseudonymize(String identifier, String domainPrefix) {
		// Retrieve counter or use user-given start value if given; immediately update it and write it back
		Long counter;
		if (startValue != null && startValue > 0) {
			counter = startValue;
		} else {
			counter = getAdbs().getAlgorithmByID(getAlgorithmID()).getConsecutiveValueCounter();
		}
		
		setCurrentValue(counter == null ? 1L : counter + 1L);
		if (!persist()) {
			log.error("Couldn't persist the current consecutive value in the database and it may not have been updated.");
		}
		
		// Pseudonymize
		String pseudonym = correctPseudonymLength(String.valueOf(counter == null ? 1L : counter + 1L));
		
		return domainPrefix + pseudonym;
	}
}
