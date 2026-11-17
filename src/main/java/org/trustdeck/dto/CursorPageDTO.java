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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One page of a complete list, read page by page with a cursor.
 * 
 * Use it when a program reads a whole list (e.g. all pseudonyms of a domain). Instead of a page
 * number, the cursor points directly behind the last item of the previous page. Every page is
 * therefore read in the same time, however far into the list it is, and items added or removed in
 * the meantime do not shift the following pages. There is no total and no jumping to a page: the
 * pages are read one after another until {@link #nextCursor} is {@code null}.
 * 
 * For interactive searches with page numbers and a match count, use {@link SearchResultPageDTO} instead.
 * 
 * The cursor is an opaque token: callers pass it back unchanged and must not rely on its content,
 * so that the sort key behind it can change without changing the API.
 * 
 * @param <T> the type of the items on the page
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageDTO<T> {

	/** Marks the format of the cursor content, so that a different sort key can be introduced later. */
	private static final String CURSOR_PREFIX = "psn:";

	/** The items on this page. */
	private List<T> items;

	/** The cursor for the next page, or {@code null} when this is the last page. */
	private String nextCursor;

	/**
	 * Creates the cursor that continues behind the given pseudonym value.
	 *
	 * @param lastPseudonym the pseudonym value of the last item on a page
	 * @return the opaque cursor
	 */
	public static String encodeCursor(String lastPseudonym) {
		return Base64.getUrlEncoder().withoutPadding()
				.encodeToString((CURSOR_PREFIX + lastPseudonym).getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Reads the pseudonym value a cursor continues behind.
	 *
	 * @param cursor the opaque cursor from a previous page
	 * @return the pseudonym value, or {@code null} when the cursor is not valid
	 */
	public static String decodeCursor(String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}

		try {
			String content = new String(Base64.getUrlDecoder().decode(cursor.trim()), StandardCharsets.UTF_8);
			return content.startsWith(CURSOR_PREFIX) ? content.substring(CURSOR_PREFIX.length()) : null;
		} catch (IllegalArgumentException e) {
			// Not valid base64url
			return null;
		}
	}
}
