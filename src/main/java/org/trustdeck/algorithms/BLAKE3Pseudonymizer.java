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

import org.apache.commons.codec.binary.Hex;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.jooq.generated.tables.pojos.Domain;

import io.lktk.NativeBLAKE3;
import io.lktk.NativeBLAKE3Util.InvalidNativeOutput;
import lombok.extern.slf4j.Slf4j;

/**
 * This class provides a pseudonymization by assigning blake3-hash-values as pseudonyms.
 * 
 * @author Armin Müller
 *
 */
@Slf4j
public class BLAKE3Pseudonymizer extends Pseudonymizer {
	
	/**
	 * Basic constructor.
	 * Padding is turned <b>off</b>.
	 * (The pseudonym value length is automatically set to the default, as well as the padding character.)
	 */
	public BLAKE3Pseudonymizer() {
		super(false, Pseudonymizer.DEFAULT_VALUE_LENGTH, Pseudonymizer.DEFAULT_PADDING_CHAR, null);
	}
	
	/**
	 * Basic constructor.
	 * Padding is turned <b>off</b>.
	 * (The pseudonym value length is automatically set to the default, as well as the padding character.)
	 * 
	 * @param domainName the name of the domain to which the record belongs to
	 */
	public BLAKE3Pseudonymizer(String domainName) {
		super(false, Pseudonymizer.DEFAULT_VALUE_LENGTH, Pseudonymizer.DEFAULT_PADDING_CHAR, domainName);
	}

	/**
	 * A constructor that allows to set whether or not the created pseudonyms should be padded 
	 * as well as the desired pseudonym value length and the character used for padding.
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param pseudonymValueLength the desired length of the pseudonym value
	 * @param paddingChar the character that should be used
	 * @param domainName the name of the domain to which the record belongs to
	 */
	public BLAKE3Pseudonymizer(boolean paddingWanted, int pseudonymValueLength, char paddingChar, String domainName) {
		super(paddingWanted, pseudonymValueLength, paddingChar, domainName);
	}
	
	/**
	 * Basic constructor.
	 * All necessary variables are directly retrieved from the domain object.
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param domain the domain object
	 */
	public BLAKE3Pseudonymizer(boolean paddingWanted, Domain domain) {
		super(paddingWanted, domain);
	}
	
	/**
	 * Basic constructor.
	 * All necessary variables are directly retrieved from the algorithm object.
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param algorithm the algorithm object
	 */
	public BLAKE3Pseudonymizer(boolean paddingWanted, Algorithm algorithm) {
		super(paddingWanted, algorithm);
	}
	
	/**
	 * Creates a blake3 hash pseudonym from the given identifier.
	 */
	@Override
	public String pseudonymize(String identifier, String domainPrefix) {
		// Initialize the BLAKE3 hasher using a JNI
		NativeBLAKE3 hasher = new NativeBLAKE3();
		hasher.initDefault();
		
		// Retrieve counter if needed. Immediately update it and write it back
		Long counter = null;
		if (isMultiplePsnAllowed()) {
			counter = isAlgorithmObjectBased() ? getAdbs().getAlgorithmByID(getAlgorithmID()).getConsecutiveValueCounter() : getAdbs().getAlgorithmByID(getDdba().getDomainByName(getDomainName()).getAlgorithmId()).getConsecutiveValueCounter();
			setCurrentValue(counter == null ? 1L : counter + 1L);
			persist();
		}
		
		// Include counter into the identifier when the domain allows multiple psn for each identifier
		String text = isMultiplePsnAllowed() ? identifier + counter.toString() : identifier;
		
		// "Read" identifier
		hasher.update(text.getBytes());

		// Finalize the hash. BLAKE3 output length defaults to 256 bits (= 64 chars). Convert to String.
		String hash = null;
		try {
			hash = Hex.encodeHexString(hasher.getOutput()).toUpperCase();
		} catch (InvalidNativeOutput e) {
			log.error("Couldn't hash the identifier using the BLAKE3 hash function: " + e.getMessage());
		}
		  
		// Hasher should be treated as a resource since there is an equivalent object allocated in memory in c.
		hasher.close();
		
		// Warn if the desired pseudonym-length is shorter than the blake3Hex
		if (getPseudonymValueLength() < 64) {
			log.debug("The requested length (" + getPseudonymValueLength() + ") for the pseudonyms is "
					+ "shorter than the output of the hashing algorithm (64).");
		}
		
		return domainPrefix + correctPseudonymLength(hash);
	}
}
