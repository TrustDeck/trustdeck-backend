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
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.service.AlgorithmDBService;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * An abstract class for different pseudonymization algorithms
 * 
 * @author Armin Müller
 *
 */
@Slf4j
@Getter
@Setter
public abstract class Pseudonymizer {
	
	/** The database controller for the algorithm-object (used to retrieve and store the counter). */
	@Setter(AccessLevel.NONE)
	private AlgorithmDBService adbs;
	
	/** The ID of the algorithm used for creating identifiers during registration. */
	private Integer algorithmID;
	
	/** The name of the algorithm used for creating identifiers during registration. */
	private String algorithmName;
	
	/** The alphabet to use for the pseudonymization. */
	private String alphabet;
	
	/** The currently used counting value for the pseudonyms. */
	@Getter(AccessLevel.NONE)
	private Long currentValue;
	
	/** String to determine that a domain reaches its filling point at which new pseudonyms are not reasonably probable generated anymore. */
	public static final String DOMAIN_FULL = "The domain reached its filling point.";
	
	/** Indicates whether the domain allows multiple pseudonyms for each identifier. */
	private boolean multiplePsnAllowed;
	
	/** The user-given number of retries. */
	private int numberOfRetries;
	
	/** The character used for padding the pseudonyms. */
	private char paddingChar;
	
	/** Shows whether or not the created pseudonyms should be padded to a certain length. */
	private boolean paddingWanted;
	
	/** Indicates how long the pseudonym should be (not considering the domain prefix and the hyphen). */
	private int pseudonymValueLength;

	/** String to determine that a pseudonymization process failed. */
	public static final String PSEUDONYMIZATION_FAILED = "Pseudonymization failed.";

	/**
	 * A constructor that allows to set whether or not the created pseudonyms should be padded.
	 * All other information will be directly retrieved from the provided domain object.
	 * 
	 * @param paddingWanted whether or not the pseudonyms should be padded to a certain length
	 * @param algorithm the algorithm object containing the necessary configuration information
	 */
	public Pseudonymizer(boolean paddingWanted, Algorithm algorithm, DefaultProperties defaults, AlgorithmDBService algorithmDBService) {
		this.adbs = algorithmDBService;
		this.algorithmID = algorithm.getId();
		this.algorithmName = algorithm.getName();
		this.alphabet = algorithm.getAlphabet();
		this.currentValue = algorithm.getConsecutiveValueCounter();
		this.multiplePsnAllowed = false;
		this.numberOfRetries = defaults.getAlgorithm().getNumberOfRetries();
		this.paddingChar = algorithm.getPaddingCharacter().charAt(0);
		this.paddingWanted = paddingWanted;
		this.pseudonymValueLength = algorithm.getPseudonymLength();
	}
	
	/**
	 * Abstract method for the pseudonymization of an identifier.
	 * 
	 * @param identifier the value to be pseudonymized
	 * @param domainPrefix the prefix for the domain
	 * @return a pseudonym in the form of domainPrefix-pseudonymValue (e.g. TST-a1cd75fe29c449248cabfae)
	 */
	public abstract String pseudonymize(String identifier, String domainPrefix);
	
	/**
	 * Corrects the length of the generated pseudonym.
	 * 
	 * @param pseudonym the pseudonym to work with
	 * @return the length-corrected pseudonym
	 */
	public String correctPseudonymLength(String pseudonym) {
		if (pseudonym.length() < this.pseudonymValueLength) {
			// Pseudonym needs padding
			if (paddingWanted) {
				return addPadding(pseudonym, this.pseudonymValueLength, this.paddingChar);
			} else {
				log.debug("The pseudonym is shorter than the requested pseudonym value length "
						+ "but padding was turned off. Returning a non-padded pseudonym.");
				return pseudonym;
			}
		} else if (pseudonym.length() > this.pseudonymValueLength) {
			// Pseudonym needs to be shorter but cutting the end of the string off 
			// might produce collisions.
			log.debug("Shortening the pseudonym value length might lead to collisions.");
			return pseudonym.substring(0, this.pseudonymValueLength);
		} else {
			// Pseudonym has already the correct length
			return pseudonym;
		}
	}

	/**
	 * Method to add a check digit (or check character) to a pseudonym.
	 * 
	 * @param pseudonym the pseudonym for which a checksum should be generated
	 * @param addInsteadOfLastChar decides whether the checksum should be included in the 
	 * 			predefined length of the pseudonym or if it should be an additional character
	 * @param domainName the name of this pseudonym's domain
	 * @param domainPrefix the prefix for this pseudonym's domain (including the hyphen, if existing)
	 * @return the pseudonym including the checksum
	 */
	public String addCheckDigit(String pseudonym, boolean addInsteadOfLastChar, String domainName, String domainPrefix) {
		String algo = algorithmName.toUpperCase();
		
		// Shorten pseudonym when the last character should be overwritten. Exception: For
		// the consecutive numbering, we don't want to cut anything from the pseudonym away.
		String psn = (addInsteadOfLastChar && !algo.equals("CONSECUTIVE")) ? pseudonym.substring(0, pseudonym.length() - 1) : pseudonym;
		
		// Decide the character space for the check digit depending on the used pseudonymization algorithm
		LuhnCheckDigit luhn;
		
		switch (algo) {
	        case "CONSECUTIVE":
	        case "RANDOM": {
	        	luhn = new LuhnModNCheckDigit(alphabet);
	        	break;
	        }
	        case "RANDOM_HEX": {
	        	// Used alphabet: "ABCDEF0123456789"
	        	luhn = new LuhnMod16CheckDigit();
	        	break;
	        }
	        case "RANDOM_NUM": {
	        	// Used alphabet: "0123456789"
	        	luhn = new LuhnMod10CheckDigit();
	        	break;
	        }
			case "MD5":
	        case "SHA1":
	        case "SHA2": 
	        case "SHA3":
	        case "BLAKE3":
	        case "XXHASH": {
	        	// Used alphabet: "ABCDEF0123456789"
	        	luhn = new LuhnMod16CheckDigit();
	        	break;
	        }
	        case "RANDOM_LET": {
	        	// Used alphabet: "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
	        	luhn = new LuhnMod26CheckDigit();
	        	break;
	        }
	        case "RANDOM_SYM_BIOS": {
	        	// Used alphabet: "ACDEFGHJKLMNPQRTUVWXYZ0123456789"
	        	luhn = new LuhnMod32CheckDigit();
	        	break;
	        }
	        case "RANDOM_SYM": {
	        	// Used alphabet: "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	        	luhn = new LuhnMod36CheckDigit();
	        	break;
	        }
			default:
				throw new IllegalArgumentException("Unexpected algorithm: " + algo);
		}
		
		// Generate check digit
		Character checkDigit = luhn.computeCheckDigit(psn, domainPrefix);
		
		// If successful, return the pseudonym with the attached check digit. If not, return the original pseudonym.
		return (checkDigit != null) ? psn + checkDigit : pseudonym;
	}
	
	/**
	 * A method to add padding to a given pseudonym.
	 * The padding will be added at the start of the string.
	 * If the input is longer than the defined length after padding,
	 * no padding (and no shortening) is performed.
	 * 
	 * @param pseudonym the pseudonym that should be padded
	 * @param lengthAfterPadding how long the output string should be
	 * @param paddingChar the character that is used to pad the string
	 * @return the padded pseudonym
	 */
	public String addPadding(String pseudonym, int lengthAfterPadding, char paddingChar) {
		String paddedPseudonym = pseudonym;
		
		for (int i = pseudonym.length(); i < lengthAfterPadding; i++) {
			paddedPseudonym = paddingChar + paddedPseudonym;
		}
		
		return paddedPseudonym;
	}
	
	/**
	 * Method to persist the consecutive value in the database.
	 * 
	 * @return {@code true} if the counter was successfully stored, {@code false} otherwise
	 */
	public boolean persist() {
		return algorithmID != null && adbs.updateCounter(currentValue, algorithmID);
	}
}
