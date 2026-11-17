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

package org.trustdeck.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One page of a search result, addressed by its page number.
 * 
 * Use it for interactive searches, where people page through the matches, jump to a page, and want
 * to know roughly how many matches there are. The matches are counted only up to the configured
 * count limit ({@code pseudonym.search-count-limit}), so that a search in a domain with millions of
 * pseudonyms does not have to count them all; {@link #numberOfMatchesExceedsSearchLimit} says
 * whether the real number is higher than {@link #total}. Deep pages get slower, because the
 * database has to skip all earlier matches, which is acceptable for searches but not for reading
 * a whole list.
 * 
 * To read a complete list (e.g. all pseudonyms of a domain), use {@link CursorPageDTO} instead.
 * 
 * @param <T> the type of the items on the page
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultPageDTO<T> {

	/** The items on this page. */
	private List<T> items;

	/** The zero-based index of this page. */
	private int page;

	/** The requested maximum number of items per page. */
	private int size;

	/** The number of matching items, counted at most up to the count limit. */
	private long total;

	/**
	 * Whether more items match than the count limit, so that counting stopped and {@link #total} is
	 * lower than the real number of matches.
	 */
	private boolean numberOfMatchesExceedsSearchLimit;
}
