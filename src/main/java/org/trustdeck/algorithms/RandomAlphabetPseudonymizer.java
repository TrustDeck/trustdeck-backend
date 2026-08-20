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
 * This class provides a pseudonymization by assigning random letters as pseudonyms.
 * 
 * @author Armin Müller
 *
 */
@Slf4j
public class RandomAlphabetPseudonymizer extends Pseudonymizer {
	
	/**
	 * Basic constructor.
	 * Number of retries is set to default, all other variables are directly retrieved from the algorithm object.
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param algorithm the algorithm object
	 */
	public RandomAlphabetPseudonymizer(boolean paddingWanted, Algorithm algorithm, DefaultProperties defaults, AlgorithmDBService algorithmDBService) {
		super(paddingWanted, algorithm, defaults, algorithmDBService);
	}
	
	/**
	 * Creates a random character pseudonym for the given identifier.
	 * The identifier is not actively used in this method.
	 */
	@Override
	public String pseudonymize(String identifier, String domainPrefix) {
		String pseudonym = getRandomString(getPseudonymValueLength(), getAlphabet());
		
		// Check if successful
		if (!pseudonym.equals(PSEUDONYMIZATION_FAILED)) {
			return domainPrefix + pseudonym;
		}
		
		log.warn("Random-Alphabet-Pseudonymizer: pseudonym generation failed!");
		return null;
	}
	
	/**
	 * Method to generate a random string of a desired length using the alphabet provided for this instance.
	 * 
	 * @param length the desired length of the random character string
	 * @return a string containing a random sequence of characters from this instance's alphabet
	 */
	private String getRandomString(int length, String alphabet) {
		SecureRandom rnd = new SecureRandom();
		StringBuilder sb = new StringBuilder();
		
		if (alphabet == null || alphabet.isBlank()) {
			return PSEUDONYMIZATION_FAILED;
		}
			
		// Generate random string
		for (int j = 0; j < length; j++) {
            // Pick random characters from the provided alphabet and append
            sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }
		
		return sb.toString();
	}
}
