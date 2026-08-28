/*
 * Trust Deck Services
 * Copyright 2023-2024 Armin Müller and Eric Wündisch
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

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;

import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.service.AlgorithmDBService;

/**
 * This class provides a pseudonymization by assigning random numbers as pseudonyms.
 * 
 * @author Armin Müller
 *
 */
@Slf4j
public class RandomNumberPseudonymizer extends Pseudonymizer {
	
	/**
	 * Basic constructor.
	 * All necessary variables are directly retrieved from the algorithm object.
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param algorithm the algorithm object
	 */
	public RandomNumberPseudonymizer(boolean paddingWanted, Algorithm algorithm, DefaultProperties defaults, AlgorithmDBService algorithmDBService) {
		super(paddingWanted, algorithm, defaults, algorithmDBService);
	}
	
	/**
	 * Creates a random number pseudonym from the given identifier.
	 */
	@Override
	public String pseudonymize(String identifier, String domainPrefix) {
		SecureRandom rnd = new SecureRandom(identifier.getBytes());
		
		// Generate a random number
		String pseudonym = "";

		// Due to a Long being only 64 bit, this won't produce anything longer than a string with 19 numbers.
		// Append multiple random values to get the desired length.
		int psnLength = getPseudonymValueLength();
		while (psnLength > 0) {
			Double upperBound = psnLength >= 19 ? Long.MAX_VALUE : Math.pow(10, psnLength);
			String p = String.valueOf(rnd.nextLong(upperBound.longValue()));
			pseudonym += p;
			psnLength -= p.length();
		}
		
		return domainPrefix + correctPseudonymLength(pseudonym);
	}
}
