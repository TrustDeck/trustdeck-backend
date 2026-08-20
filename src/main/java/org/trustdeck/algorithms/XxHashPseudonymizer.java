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
import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.service.AlgorithmDBService;
import org.trustdeck.utils.Assertion;

import java.nio.ByteBuffer;

/**
 * This class provides a pseudonymization by assigning xxHash64-values as pseudonyms.
 * 
 * @author Armin Müller
 *
 */
@Slf4j
public class XxHashPseudonymizer extends Pseudonymizer {
	
	/**
	 * Basic constructor.
	 * All necessary variables are directly retrieved from the algorithm object.
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param algorithm the algorithm object
	 */
	public XxHashPseudonymizer(boolean paddingWanted, Algorithm algorithm, DefaultProperties defaults, AlgorithmDBService algorithmDBService) {
		super(paddingWanted, algorithm, defaults, algorithmDBService);
	}
	
	/**
	 * Creates a xxHash64 hash pseudonym from the given identifier.
	 */
	@Override
	public String pseudonymize(String identifier, String domainPrefix) {
		// Initialize the xxHash64 hasher
		XXHashFactory factory = XXHashFactory.fastestInstance();
		XXHash64 xxHash64 = factory.hash64();
		
		// Retrieve counter if needed. Immediately update it and write it back
		Long counter = null;
		if (isMultiplePsnAllowed()) {
			counter = getAdbs().getAlgorithmByID(getAlgorithmID()).getConsecutiveValueCounter();
			setCurrentValue(counter == null ? 1L : counter + 1L);
			persist();
		}
		
		// Include counter into the identifier when the domain allows multiple psn for each identifier
		String text = isMultiplePsnAllowed() ? identifier + counter.toString() : identifier;

		// Get salt and transform it into a seed (non-numeric String to Long)
		String salt = getAdbs().getAlgorithmByID(getAlgorithmID()).getSalt();
		long seed = Assertion.isNotNullOrEmpty(salt) ? ByteBuffer.wrap(DigestUtils.sha256(salt)).getLong() : 0L;

		// Transform the identifier into a ByteBuffer and hash it
		ByteBuffer textBuffer = ByteBuffer.wrap(text.getBytes());
		String hash = Long.toHexString(xxHash64.hash(textBuffer, seed));
		
		// Since the algorithm outputs numbers, leading zeros will not be reflected in the output. We will now add them again if necessary.
		if (hash.length() < 16) {
			hash = addPadding(hash, 16, '0');
		}
		
		// Warn if the desired pseudonym-length is shorter than the xxHash output
		if (getPseudonymValueLength() < 16) {
			log.debug("The requested length (" + getPseudonymValueLength() + ") for the pseudonyms is "
					+ "shorter than the output of the hashing algorithm (16).");
		}
		
		return domainPrefix + correctPseudonymLength(hash);
	}
}
