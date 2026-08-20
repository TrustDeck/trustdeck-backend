/*
 * Trust Deck Services
 * Copyright 2022-2024 Armin Müller and Eric Wündisch
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

import org.apache.commons.codec.digest.DigestUtils;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.service.AlgorithmDBService;

import lombok.extern.slf4j.Slf4j;

/**
 * This class provides a pseudonymization by assigning sha3-hash-values as pseudonyms
 * 
 * @author Armin Müller
 *
 */
@Slf4j
public class SHA3Pseudonymizer extends Pseudonymizer {
	
	/**
	 * Basic constructor.
	 * All necessary variables are directly retrieved from the algorithm object.
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param algorithm the algorithm object
	 */
	public SHA3Pseudonymizer(boolean paddingWanted, Algorithm algorithm, DefaultProperties defaults, AlgorithmDBService algorithmDBService) {
		super(paddingWanted, algorithm, defaults, algorithmDBService);
	}
	
	/**
	 * Creates a sha3 hash pseudonym from the given identifier.
	 */
	@Override
	public String pseudonymize(String identifier, String domainPrefix) {
		// Retrieve counter if needed. Immediately update it and write it back
		Long counter = null;
		if (isMultiplePsnAllowed()) {
			counter = getAdbs().getAlgorithmByID(getAlgorithmID()).getConsecutiveValueCounter();
			setCurrentValue(counter == null ? 1L : counter + 1L);
			persist();
		}
		
		// Include counter into the identifier when the domain allows multiple psn for each identifier
		String text = isMultiplePsnAllowed() ? identifier + counter.toString() : identifier;
		
		// Use SHA3 hashes because they're of equal length (512 bits = 128 chars)
		String hash = DigestUtils.sha3_512Hex(text.trim()).toUpperCase();
		
		// Warn if the desired pseudonym-length is shorter than the sha3Hex
		if (getPseudonymValueLength() < 128) {
			log.debug("The requested length (" + getPseudonymValueLength() + ") for the pseudonyms is "
					+ "shorter than the output of the hashing algorithm (128).");
		}

		return domainPrefix + correctPseudonymLength(hash);
	}
}
