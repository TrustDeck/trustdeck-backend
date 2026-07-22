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

import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.utils.Assertion;

import lombok.extern.slf4j.Slf4j;

/**
 * This is a factory class for creating a pseudonymizer based on the desired algorithm.
 * 
 * @author Armin Müller
 */
@Slf4j
public class PseudonymizationFactory {
	
	/**
	 * Method for automatically creating the correct pseudonymizer 
	 * depending on the desired algorithm.
	 * 
	 * @param algorithm the algorithm with which the inputs should be pseudonymized
	 * @return the pre-configured pseudonymizer so that the pseudonymization step itself is easier to accomplish 
	 */
	public static Pseudonymizer getPseudonymizer(Algorithm algorithm) {
		// Select the desired pseudonymization algorithm and create a pseudonymizer
        switch (algorithm.getName().toUpperCase()) {
            case "MD5": {
                return new MD5Pseudonymizer(true, algorithm);
            }
            case "SHA1": {
                return new SHA1Pseudonymizer(true, algorithm);
            }
            case "SHA2": {
            	return new SHA2Pseudonymizer(true, algorithm);
            }
            case "SHA3": {
            	return new SHA3Pseudonymizer(true, algorithm);
            }
            case "BLAKE3": {
            	return new BLAKE3Pseudonymizer(true, algorithm);
            }
            case "CONSECUTIVE": {
            	return new ConsecutivePseudonymizer(true, algorithm);
            }
            case "RANDOM_NUM": {
            	return new RandomNumberPseudonymizer(true, algorithm);
            }
            case "RANDOM": 
            case "RANDOM_HEX": 
            case "RANDOM_LET": 
            case "RANDOM_SYM": 
            case "RANDOM_SYM_BIOS": {
            	return new RandomAlphabetPseudonymizer(true, algorithm);
            }
            case "XXHASH": {
            	return new XxHashPseudonymizer(true, algorithm);
            }
            default: {
                // Unrecognized algorithm. Use default.
                log.warn("The pseudonymization algorithm that was requested (" + algorithm.getName() + ") wasn't recognized. Using random letters (A-Z) instead.");
                algorithm.setName("RANDOM_LET");
                algorithm.setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                return new RandomAlphabetPseudonymizer(true, algorithm);
            }
        }
	}
	
	/**
	 * Method for automatically creating the correct pseudonymizer 
	 * depending on the desired algorithm.
	 * 
	 * @param domain the domain under which the pseudonyms should be stored in
	 * @return the pre-configured pseudonymizer so that the pseudonymization step itself is easier to accomplish 
	 */
	public static Pseudonymizer getPseudonymizer(Domain domain) {
		// Check if any algorithm was provided
		if (!Assertion.isNotNullOrEmpty(domain.getAlgorithm())) {
			log.error("There was no valid pseudonymization algorithm provided.");
			return null;
		}

        // Select the desired pseudonymization algorithm and create a pseudonymizer
        switch (domain.getAlgorithm().toUpperCase()) {
            case "MD5": {
                return new MD5Pseudonymizer(true, domain);
            }
            case "SHA1": {
                return new SHA1Pseudonymizer(true, domain);
            }
            case "SHA2": {
            	return new SHA2Pseudonymizer(true, domain);
            }
            case "SHA3": {
            	return new SHA3Pseudonymizer(true, domain);
            }
            case "BLAKE3": {
            	return new BLAKE3Pseudonymizer(true, domain);
            }
            case "CONSECUTIVE": {
            	return new ConsecutivePseudonymizer(true, domain);
            }
            case "RANDOM_NUM": {
            	return new RandomNumberPseudonymizer(true, domain);
            }
            case "RANDOM": 
            case "RANDOM_HEX": 
            case "RANDOM_LET": 
            case "RANDOM_SYM": 
            case "RANDOM_SYM_BIOS": {
            	return new RandomAlphabetPseudonymizer(true, domain);
            }
            case "XXHASH": {
            	return new XxHashPseudonymizer(true, domain);
            }
            default: {
                // Unrecognized algorithm. Use default.
                log.warn("The pseudonymization algorithm that was requested (" + domain.getAlgorithm() + ") wasn't recognized. Using random letters (A-Z) instead.");
                domain.setAlgorithm("RANDOM_LET");
                domain.setAlphabet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
                return new RandomAlphabetPseudonymizer(true, domain);
            }
        }
	}
}
