/*
 * Trust Deck Services
 * Copyright 2026 Armin Müller
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

package org.trustdeck.model;

import java.util.Map;

/**
 * The order of a paged entity search.
 * 
 * @param key what the entities are sorted by
 * @param attribute the attribute to sort by when the key is {@link Key#ATTRIBUTE}, {@code null} otherwise
 * @param descending whether the order is descending (ignored for {@link Key#RELEVANCE}, which
 *        always puts the best matches first)
 * @author Armin Müller
 */
public record EntitySort(Key key, SortableAttribute attribute, boolean descending) {
	
	/** The prefix of a sort parameter naming an attribute (e.g. {@code attribute:address.city}). */
	public static final String ATTRIBUTE_PREFIX = "attribute:";
	
	/**
	 * What the entities are sorted by.
	 */
	public enum Key {
		/** How well the entities match the query, best first. */
		RELEVANCE,
		/** The entities' TrustDeck IDs. */
		TRUSTDECK_ID,
		/** The entities' creation time. */
		CREATED_AT,
		/** The entities' last update time. */
		UPDATED_AT,
		/** An attribute of the entities' data. */
		ATTRIBUTE
	}
	
	/**
	 * Parses the sort and direction parameters of a paged entity search.
	 * 
	 * Without a sort parameter, searches for everything ("*") are sorted by the update time,
	 * newest first, and all other searches by relevance. A search for everything has no relevance
	 * either and is also sorted by the update time, newest first.
	 * 
	 * @param sort the sort parameter: {@code relevance}, {@code trustDeckId}, {@code createdAt},
	 *        {@code updatedAt}, or {@code attribute:<path>}; may be {@code null}
	 * @param direction the direction parameter: {@code asc} or {@code desc}; may be {@code null}
	 * @param matchAll whether the search is for everything
	 * @param attributes the sortable attributes of the entity type, keyed by their path
	 * @return the parsed order
	 * @throws IllegalArgumentException when a parameter is unknown or names an unknown attribute
	 */
	public static EntitySort parse(String sort, String direction, boolean matchAll, Map<String, SortableAttribute> attributes) {
		Boolean descending = null;
		if (direction != null) {
			if ("asc".equalsIgnoreCase(direction)) {
				descending = false;
			} else if ("desc".equalsIgnoreCase(direction)) {
				descending = true;
			} else {
				throw new IllegalArgumentException("Unknown sort direction \"" + direction + "\".");
			}
		}
		
		if (sort == null || sort.isBlank()) {
			return matchAll ? new EntitySort(Key.UPDATED_AT, null, descending == null || descending) : new EntitySort(Key.RELEVANCE, null, false);
		} else if ("relevance".equalsIgnoreCase(sort)) {
			return matchAll ? new EntitySort(Key.UPDATED_AT, null, true) : new EntitySort(Key.RELEVANCE, null, false);
		} else if ("trustDeckId".equalsIgnoreCase(sort)) {
			return new EntitySort(Key.TRUSTDECK_ID, null, Boolean.TRUE.equals(descending));
		} else if ("createdAt".equalsIgnoreCase(sort)) {
			return new EntitySort(Key.CREATED_AT, null, Boolean.TRUE.equals(descending));
		} else if ("updatedAt".equalsIgnoreCase(sort)) {
			return new EntitySort(Key.UPDATED_AT, null, Boolean.TRUE.equals(descending));
		} else if (sort.startsWith(ATTRIBUTE_PREFIX)) {
			SortableAttribute attribute = attributes.get(sort.substring(ATTRIBUTE_PREFIX.length()));
			if (attribute == null) {
				throw new IllegalArgumentException("Unknown attribute in \"" + sort + "\".");
			}
			
			return new EntitySort(Key.ATTRIBUTE, attribute, Boolean.TRUE.equals(descending));
		}
		
		throw new IllegalArgumentException("Unknown sort \"" + sort + "\".");
	}
}
