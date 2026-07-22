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
import org.trustdeck.jooq.generated.tables.pojos.Domain;

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
	 * Basic constructor. Initializes the values needed for pseudonymization.
	 * The consecutive value used for pseudonymization is retrieved from the domain in the database.
	 * Padding is turned <b>off</b>.
	 * (The pseudonym value length is automatically set to the default, as well as the padding character.)
	 * 
	 * @param domainName the name of the domain to which the record belongs to
	 */
	public ConsecutivePseudonymizer(String domainName) {
		super(false, Pseudonymizer.DEFAULT_VALUE_LENGTH, Pseudonymizer.DEFAULT_PADDING_CHAR, domainName);
		this.startValue = null;
	}
	
	/**
	 * A constructor where everything padding-related can be set manually.
	 * The consecutive value used for pseudonymization is retrieved from the domain in the database.
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param pseudonymValueLength the desired length of the pseudonym value
	 * @param paddingChar the character that should be used for padding
	 * @param domainName the name of the domain to which the record belongs to
	 */
	public ConsecutivePseudonymizer(boolean paddingWanted, int pseudonymValueLength, char paddingChar, String domainName) {
		super(paddingWanted, pseudonymValueLength, paddingChar, domainName);
		this.startValue = null;
	}
	
	/**
	 * A constructor where everything padding-related as well as the start value 
	 * for the consecutive numbering can be set manually. <b>USE WITH CAUTION.</b>
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param pseudonymValueLength the desired length of the pseudonym value
	 * @param paddingChar the character that should be used for padding
	 * @param startValue the value for the first pseudonym
	 * @param domainName the name of the domain to which the record belongs to
	 */
	public ConsecutivePseudonymizer(boolean paddingWanted, int pseudonymValueLength, char paddingChar, long startValue, String domainName) {
		super(paddingWanted, pseudonymValueLength, paddingChar, domainName);
		this.startValue = startValue;
	}
	
	/**
	 * Basic constructor.
	 * All necessary variables are directly retrieved from the domain object.
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param domain the domain object
	 */
	public ConsecutivePseudonymizer(boolean paddingWanted, Domain domain) {
		super(paddingWanted, domain);
		this.startValue = null;
	}
	
	/**
	 * Basic constructor.
	 * All necessary variables are directly retrieved from the algorithm object.
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param algorithm the algorithm object
	 */
	public ConsecutivePseudonymizer(boolean paddingWanted, Algorithm algorithm) {
		super(paddingWanted, algorithm);
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
			counter = isAlgorithmObjectBased() ? getAdbs().getAlgorithmByID(getAlgorithmID()).getConsecutiveValueCounter() : getAdbs().getAlgorithmByID(getDdba().getDomainByName(getDomainName()).getAlgorithmId()).getConsecutiveValueCounter();
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
