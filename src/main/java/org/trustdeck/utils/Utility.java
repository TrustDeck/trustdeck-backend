/*
 * Trust Deck Services
 * Copyright 2022-2026 Armin Müller and Eric Wündisch
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

package org.trustdeck.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class offers a variety of utilities.
 *
 * @author Armin Müller and Eric Wündisch
 */
@Slf4j
@Component
public class Utility {
    
    /** The default alphabet (A-Z0-9) used for the pseudonymization process. */
    private static final String DEFAULT_PSEUDONYMIZATION_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    /** A flexible date-time-formatter that accepts a multitude of input variants. */
    private static final DateTimeFormatter FLEXIBLE_DATE_TIME_FORMATTER = buildFormatter();
	
	/** Regex to match time and unit of a time-period-string. */
	private static final Pattern DURATION_PATTERN = Pattern.compile("^\\s*([+-]?\\d+)\\s*([a-zA-Z]+)\\s*\\.?\\s*$");
	
	/**
	 * This method processes the given validity period String, computes
	 * the encoded time and adds it to the initial time point.
	 * 
	 * @param validityTime the String containing the validity time
	 * @return the computed end time or {@code null} if something went wrong
	 */
	public static LocalDateTime plusValidityTime(LocalDateTime initialDateTime, String validityPeriod) {
		if (Assertion.isNullOrEmpty(validityPeriod) || initialDateTime == null) {
			log.debug("The given initialDateTime or the validityPeriod were null/empty. Using now() instead.");
			return null;
		}
		
		// Normalize the given string
		String vTime = validityPeriod.trim().toLowerCase();
		
		// Match it against the regex
		Matcher m = DURATION_PATTERN.matcher(vTime);
	    if (!m.matches()) {
	    	log.debug("The given validityPeriod was not properly formatted.");
	    	return null;
	    }

	    long time;
	    try {
	        time = Long.parseLong(m.group(1));
	    } catch (NumberFormatException e) {
	        return null;
	    }

	    String unit = m.group(2).toLowerCase();
		
	    // Add the time depending on the given unit
	    switch (unit) {
	        // Years
	        case "y":
	        case "yr":
        	case "yrs":
    		case "year":
    		case "years":
    			return safePlus(() -> initialDateTime.plusYears(time));

	        // Months
	        case "mo":
			case "mos":
			case "mon":
			case "mons":
			case "mth":
			case "mths":
			case "month":
			case "months":
	            return safePlus(() -> initialDateTime.plusMonths(time));

	        // Weeks
	        case "w":
			case "wk":
			case "wks":
			case "week":
			case "weeks":
	            return safePlus(() -> initialDateTime.plusWeeks(time));

	        // Days
	        case "d":
			case "day":
			case "days":
	            return safePlus(() -> initialDateTime.plusDays(time));

	        // Hours
	        case "h":
			case "hr":
			case "hrs":
			case "hour":
			case "hours":
	            return safePlus(() -> initialDateTime.plusHours(time));

	        // Minutes
	        case "m":
			case "min":
			case "mins":
			case "minute":
			case "minutes":
	            return safePlus(() -> initialDateTime.plusMinutes(time));

	        // Seconds
	        case "s":
			case "sec":
			case "secs":
			case "second":
			case "seconds":
	            return safePlus(() -> initialDateTime.plusSeconds(time));

	        // Milliseconds
	        case "ms":
			case "msec":
			case "msecs":
			case "millis":
			case "millisecond":
			case "milliseconds":
	            return safePlus(() -> initialDateTime.plusNanos(1000L * time));

	        // Nanoseconds
	        case "ns":
			case "nsec":
			case "nsecs":
			case "nanosec":
			case "nanosecs":
			case "nanosecond":
			case "nanoseconds":
			case "nano":
			case "nanos":
	            return safePlus(() -> initialDateTime.plusNanos(time));

	         // If this point is reached, no validity time could be recognized
	        default:
	            return null;
	    }
	}
	
	/**
	 * Helper method to safely add a time amount to a given base time.
	 * Handles possible exceptions.
	 * 
	 * @param op the plusXXX() method-call to add a time period to a time point
	 * @return the result of the addition or {@code null}, when a DateTimeException was thrown
	 */
	private static LocalDateTime safePlus(Supplier<LocalDateTime> op) {
	    try {
	        return op.get();
	    } catch (DateTimeException ex) {
	        return null;
	    }
	}
	
	/**
	 * Method to generate the alphabet depending on the given algorithm.
	 * 
	 * @param algorithm the user-given algorithm
	 * @param alphabet the alphabet provided by the user, if available
	 * @return the alphabet that matches the algorithm as a String
	 */
	public static String generateAlphabet(String algorithm, String alphabet) {
		// The possible alphabets
		String HEXADECIMAL_ALPHABET = "ABCDEF0123456789";
		String NUMBERS_ONLY_ALPHABET = "0123456789";
		String LETTERS_ONLY_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String LETTERS_AND_NUMBERS_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		String LETTERS_AND_NUMBERS_WITHOUT_BIOS_ALPHABET = "ACDEFGHJKLMNPQRTUVWXYZ0123456789";
		
		// If nothing was provided, return the default alphabet
		if (algorithm == null) {
			return DEFAULT_PSEUDONYMIZATION_ALPHABET;
		}
		
		// Generate alphabet depending on the used algorithm
		switch (algorithm.trim().toUpperCase()) {
	        case "MD5":
	        case "SHA1":
	        case "SHA2":
	        case "SHA3":
	        case "BLAKE3":
	        case "XXHASH": {
	        	return HEXADECIMAL_ALPHABET;
	        }
	        case "CONSECUTIVE":
	        case "RANDOM_NUM": {
	        	return NUMBERS_ONLY_ALPHABET;
	        }
	        case "RANDOM": {
	        	// If "RANDOM" was selected, use the user-provided alphabet or A-Z0-9 if nothing was provided
	        	return (alphabet != null && !alphabet.isBlank()) ? alphabet : LETTERS_AND_NUMBERS_ALPHABET;
	        }
	        case "RANDOM_HEX": {
	        	return HEXADECIMAL_ALPHABET;
	        }
	        case "RANDOM_LET": {
	        	return LETTERS_ONLY_ALPHABET;
	        }
	        case "RANDOM_SYM": {
	        	return LETTERS_AND_NUMBERS_ALPHABET;
	        }
	        case "RANDOM_SYM_BIOS": {
	        	return LETTERS_AND_NUMBERS_WITHOUT_BIOS_ALPHABET;
	        }
	        default: {
	            // Unrecognized algorithm
	            log.debug("The pseudonymization algorithm that was requested (" + algorithm + ") wasn't recognized.");
	            return DEFAULT_PSEUDONYMIZATION_ALPHABET;
	        }
		}
	}

    /**
     * Method to build a flexible date time formatter that accepts date-times such as:
     * <li>2025-12-31</li>
     * <li>2025-12-31 12:00</li>
     * <li>2025-12-31 12:00:30</li>
     * <li>2025-12-31T12:00</li>
     * <li>2025-12-31T12:00:30</li>
     * <li>... with offsets: Z, +02:00, etc.</li>
     * 
     * @return
     */
	private static DateTimeFormatter buildFormatter() {
		return new DateTimeFormatterBuilder()
		        .parseCaseInsensitive()
		        .append(DateTimeFormatter.ISO_LOCAL_DATE)
		        // Time with 'T' separator
		        .optionalStart()
		            .appendLiteral('T')
		            .append(DateTimeFormatter.ISO_LOCAL_TIME)
		        .optionalEnd()
		        // Time with space separator
		        .optionalStart()
		            .appendLiteral(' ')
		            .append(DateTimeFormatter.ISO_LOCAL_TIME)
		        .optionalEnd()
		        // Optional offset (Z or +HH:mm) after the time
		        .optionalStart()
		            .appendOffset("+HH:MM", "Z")
		        .optionalEnd()
		        .toFormatter(Locale.ROOT)
		        .withResolverStyle(ResolverStyle.SMART);
	}
	
	/**
	 * Method to parse a string in an unknown date-time format into
	 * the OffsetDateTime format.
	 * 
	 * @param dateTime the date time String
	 * @return the given date-time as an instance of OffsetDateTime, or {@code null} if no known format was detected
	 */
	public static OffsetDateTime parseDateTimeString(String dateTime) {
		return parseDateTimeString(dateTime, ZoneId.systemDefault());
	}

	/**
	 * Parses a date-time using the supplied zone when the input has no offset.
	 *
	 * @param dateTime the date-time text to parse
	 * @param defaultZone the zone to apply to a timezone-less input (can be empty --> uses system timezone)
	 * @return the parsed date-time, or {@code null} when no supported format matches
	 */
	public static OffsetDateTime parseDateTimeString(String dateTime, ZoneId defaultZone) {
        // Check if the input is given
		if (Assertion.isNullOrEmpty(dateTime)) {
         	return null;
        }
		
		// Use the caller-provided zone when the input has no explicit offset.
        ZoneId timeZone = defaultZone != null ? defaultZone : ZoneId.systemDefault();
        
        // Try best-fit parsing: OffsetDateTime, ZonedDateTime, LocalDateTime, LocalDate
        try {
            TemporalAccessor ta = FLEXIBLE_DATE_TIME_FORMATTER.parseBest(
            		dateTime.trim(),
                    OffsetDateTime::from,
                    ZonedDateTime::from,
                    LocalDateTime::from,
                    LocalDate::from
            );

            if (ta instanceof OffsetDateTime odt) {
            	log.trace("Given input date time is of type " + OffsetDateTime.class.getName() + ": " + odt.toString());
                return odt;
            } else if (ta instanceof ZonedDateTime zdt) {
            	log.trace("Given input date time is of type " + ZonedDateTime.class.getName() + ": " + zdt.toString());
                return zdt.toOffsetDateTime();
            } else if (ta instanceof LocalDateTime ldt) {
            	log.trace("Given input date time is of type " + LocalDateTime.class.getName() + ": " + ldt.toString());
                return ldt.atZone(timeZone).toOffsetDateTime();
            } else if (ta instanceof LocalDate ld) {
            	log.trace("Given input date time is of type " + LocalDate.class.getName() + ": " + ld.toString());
                return ld.atStartOfDay(timeZone).toOffsetDateTime();
            } else {
            	log.trace("Could not detect type of the given input string.");
            	return null;
            }
        } catch (DateTimeParseException e) {
            log.debug("No valid parser option for the given date \"" + dateTime + "\" was found.");
            return null;
        }
    }

	/**
	 * Parses an API validity timestamp into the UTC value used by legacy database columns.
	 * 
	 * @param dateTime the String to parse
	 * @return the given date time in UTC
	 */
	public static LocalDateTime parseUtcDateTime(String dateTime) {
		OffsetDateTime parsed = parseDateTimeString(dateTime, ZoneOffset.UTC);
		return parsed == null ? null : parsed.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
	}

	/**
	 * Formats a legacy UTC database value as an offset-aware API timestamp.
	 * 
	 * @param dateTime the date time value from the database
	 * @return the date time as a string that includes the timezone-offset
	 */
	public static String formatUtcDateTime(LocalDateTime dateTime) {
		return dateTime == null ? null : dateTime.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
	
	/**
	 * A method to truncate strings when they're getting too long.
	 * A "...(truncated)" will be added when it was shortened.
	 * 
	 * @param s the string to check
	 * @param maxLength the maximum allowed length for the given string
	 * @return the original string if it's shorter than the maxLength or the truncated string
	 */
	public static String truncateWhenTooLong(String s, int maxLength) {
		if (s == null || s.isBlank()) {
			return null;
		}
		
		return s.length() <= maxLength ? s : s.substring(0, maxLength) + "...(truncated)";
	}
	
	/**
	 * Provides access to the current request.
	 * 
	 * @return the request that is currently active
	 */
	public static HttpServletRequest getCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        
        return null;
    }
	
	/**
     * Checks whether a request is a multipart request.
     *
     * @param request the current request
     * @return {@code true} if the request is multipart, otherwise {@code false}
     */
    public static boolean isMultipartRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }
	
	/**
	 * A data type that lets you store two data items that belong together.
	 * 
	 * @param <A> the first item, of type A
	 * @param <B> the second item, of type B
	 */
	public record Pair<A, B>(A first, B second) {}
}
