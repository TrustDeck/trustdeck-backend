/*
 * Trust Deck Services
 * Copyright 2024 Armin Müller and Eric Wündisch
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

import lombok.Getter;

/**
 * Abstract class that provides the functionalities for calculating a check digit for an arbitrary list of characters 
 * as long as the length of this list is divisible by two.
 * 
 * @author Armin Müller
 */
public abstract class LuhnCheckDigit {

	/** The list of allowed characters for the algorithm. The check digit will also be any of those chars. */
	@Getter
	private String allowedCharacters;
	
	/** Represents the modulo-n for this instance. */
	@Getter
	private int mod;
	
	/** Represents the status of the standardization when the given prefix was not found in the input. */
	private static final String PREFIX_NOT_FOUND = "PREFIX_NOT_FOUND";
	
	/**
	 * Basic constructor. A list of allowed characters needs to be provided.
	 * The list's length must be divisible by two.
	 * 
	 * @param allowedCharacters the characters that are allowed in the input and in the check digit
	 */
	public LuhnCheckDigit(String allowedCharacters) {
		this.allowedCharacters = allowedCharacters;
		this.mod = allowedCharacters.length();
	}
	
	/**
	 * A method to standardize the input strings (e.g. removes surrounding white spaces and the domain prefix, turns input into upper case, ...).
	 * 
	 * @param input the unsanitized input string
	 * @param domainPrefix the prefix of the domain where the input is in, including hyphens
	 * @return the sanitized input string
	 */
	public String standardizeInput(String input, String domainPrefix) {
		// Check if anything was provided
		if (input == null) {
			return null;
		}
		
		// If no prefix is provided, set it to the empty string so that the substring-method doesn't fail
		if (domainPrefix == null) {
			domainPrefix = "";
		}
		
		// Remove surrounding white spaces before looking for the prefix
		String trimmedInput = input.trim();
		
		// Remove the domain's prefix if it's there
		String temp = trimmedInput.startsWith(domainPrefix) ? trimmedInput.substring(domainPrefix.length()) : PREFIX_NOT_FOUND;
		
		// Remove white spaces and capitalize
		return temp.trim().toUpperCase();
	}
	
	/**
	 * Method to compute the check digit for the given input.
	 * 
	 * @param input the unsanitized input string
	 * @param domainPrefix the prefix of the domain where the input should be in, including hyphens
	 * @return the check digit for the provided input, {@code null} when a character was encountered
	 * 			that is not part of the list of allowed characters
	 */
	public Character computeCheckDigit(String input, String domainPrefix) {
		int factor = 2;
		int sum = 0;
		
		String standardizedInput = standardizeInput(input, domainPrefix);
		char[] inputChars = standardizedInput.equals(PREFIX_NOT_FOUND) ? input.trim().toUpperCase().toCharArray() : standardizedInput.toCharArray();
		char[] allowedChars = getAllowedCharacters().toCharArray();
		
		// Iterate over the input. Starting from the right and working leftwards 
		// is easier since the initial "factor" will always be "2".
		for (int i = inputChars.length - 1; i >= 0; i--) {
			int index = -1;
			
			// Get index of the current character in the array of allowed characters
			for (int j = 0; j < allowedChars.length; j++) {
				if (allowedChars[j] == inputChars[i]) {
					index = j;
				}
			}
			
			// If the provided character was not found in the allowed characters, return null
			if (index == -1) {
				return null;
			}
			
			// Calculate the addend
			int addend = factor * index;
	 
			// Alternate the factor that each index is multiplied by
			factor = (factor == 2) ? 1 : 2;
	 
			// Sum the digits as expressed in base "n"
			sum += (addend / mod) + (addend % mod);
		}
	 
		// Calculate the number that must be added to the "sum" to make it divisible by "n"
		int checkIndex = mod - (sum % mod);
		checkIndex %= mod;
		
		return allowedChars[checkIndex];
	}
	
	/**
	 * Method to validate the check digit from the given input.
	 * 
	 * @param input the unsanitized input string
	 * @param domainPrefix the prefix of the domain where the input is in, including hyphens
	 * @return {@code true} when the check digit is valid, {@code false} when it is invalid, 
	 * 			and {@code null} when there was a character in the input that is not part of 
	 * 			the allowed characters.
	 */
	public Boolean validateCheckDigit(String input, String domainPrefix) {
		int factor = 1;
		int sum = 0;

		String standardizedInput = standardizeInput(input, domainPrefix);
		if (standardizedInput.equals(PREFIX_NOT_FOUND)) {
			return false;
		}
		
		char[] inputChars = standardizedInput.toCharArray();
		char[] allowedChars = getAllowedCharacters().toCharArray();

		// Iterate over the input. Starting from the right and working leftwards 
		// is easier since the initial "factor" will always be "2".
		for (int i = inputChars.length-1; i >= 0; i--) {
			int index = -1;
			
			// Get index of the current character in the array of allowed characters
			for (int j = 0; j < allowedChars.length; j++) {
				if (allowedChars[j] == inputChars[i]) {
					index = j;
				}
			}
			
			// If the provided character was not found in the allowed characters, return null
			if (index == -1) {
				return null;
			}
			
			// Calculate the addend
			int addend = factor * index;
			
			// Alternate the factor that each index is multiplied by
			factor = (factor == 2) ? 1 : 2;
		 
			// Sum the digits as expressed in base "n"
			sum += (addend / mod) + (addend % mod);
		}
		
		// If there is a remainder when calculating modulo, then the check digit does not fit to the input
		return (sum % mod == 0);
	}
}
