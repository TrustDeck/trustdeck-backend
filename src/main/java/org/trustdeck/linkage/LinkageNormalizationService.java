/*
 * Trust Deck Services
 * Copyright 2026 Armin Müller and Eric Wündisch
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

package org.trustdeck.linkage;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.commons.codec.language.ColognePhonetic;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * This service provides normalization and encoding methods for record linkage values.
 * It can be used to transform raw entity field values into normalized 
 * representations and linkage tokens that are suitable for candidate generation and 
 * candidate scoring.
 * 
 * @author Armin Müller
 */
@Service
@Slf4j
public class LinkageNormalizationService {

	/** The Cologne phonetic encoder used for phonetic token generation. */
	private final ColognePhonetic colognePhonetic = new ColognePhonetic();

	/** The Double Metaphone phonetic encoder used for phonetic token generation. */
	private final DoubleMetaphone doubleMetaphone = new DoubleMetaphone();

	/**
	 * Normalizes a given value according to the provided list of normalization
	 * rules. The rules are applied in the order in which they are provided.
	 * 
	 * @param value the raw value that should be normalized
	 * @param normalizers the list of normalization rules to apply
	 * @return the normalized value, or {@code null} if the input was {@code null} or the result is blank
	 */
	public String normalize(String value, List<String> normalizers) {
		if (value == null) {
			return null;
		}

		// Apply normalization rules
		String s = value;
		for (String rule : normalizers) {
			switch (rule.trim().toLowerCase()) {
				case "trim" -> s = s.trim();
				case "lower" -> s = s.toLowerCase(Locale.ROOT);
				case "collapsewhitespace" -> s = s.replaceAll("\\s+", " ");
				case "removepunctuation" -> s = s.replaceAll("[\\p{Punct}]", "");
				case "digitsonly" -> s = s.replaceAll("[^0-9]", "");
				case "umlautfold" -> {
					s = s.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
					s = s.replace("Ä", "Ae").replace("Ö", "Oe").replace("Ü", "Ue");
				}
				// Transforms accented characters into their base letter plus diacritic marks,
				// then remove the diacritic marks (the replace-part); e.g. "Müller" becomes "Muller"
				case "asciifold" -> s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
				default -> {
					log.debug("The normalizer rule \"" + rule + "\" is unkown and was therefore ignored.");
				}
			}
		}

		return s.isBlank() ? null : s;
	}

	/**
	 * Encodes a normalized value using the requested phonetic encoding method. 
	 * The result can be used as a linkage token during record linkage.
	 * 
	 * @param value the normalized value that should be encoded
	 * @param phoneticEncoder the name of the phonetic encoder to use
	 * @return the encoded value, or {@code null} if the input was blank or the encoder was not recognized
	 */
	public String phoneticEncode(String value, String phoneticEncoder) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return switch (phoneticEncoder.trim().toLowerCase()) {
			case "cologne" -> colognePhonetic.encode(value); // For German datasets
			case "doublemetaphone" -> doubleMetaphone.encode(value); // For English datasets
			// Maybe adding something language-agnostic would be good? E.g. trigrams?
			default -> null;
		};
	}

	/**
	 * Normalizes a date value to its ISO-8601 (e.g. '2026-12-25') representation.
	 * 
	 * @param value the raw date value that should be normalized
	 * @return the normalized date string, or {@code null} if the input was blank
	 */
	public String normalizeDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}

		return LocalDate.parse(value, DateTimeFormatter.ISO_DATE).toString();
	}

	/**
	 * Extracts the year component from a normalized ISO date.
	 * 
	 * @param isoDate the normalized ISO date string
	 * @return the year component as a string
	 */
	public String yearFromDate(String isoDate) {
		return String.valueOf(LocalDate.parse(isoDate).getYear());
	}

	/**
	 * Extracts the year-month component from a normalized ISO date.
	 * 
	 * @param isoDate the normalized ISO date string
	 * @return the year-month component in the format YYYY-MM
	 */
	public String yearMonthFromDate(String isoDate) {
		LocalDate d = LocalDate.parse(isoDate);
		return "%04d-%02d".formatted(d.getYear(), d.getMonthValue());
	}
}
