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
 * so that the sort key behind it can change without changing the API. Opaque does not mean secret:
 * the cursor is only base64url-encoded, so anyone can read and build one. That is intended. It only
 * contains the last pseudonym value of the previous page, which the caller already received with
 * that page, and a self-made cursor only chooses where to start within pseudonyms the caller is
 * allowed to read anyway. It must therefore never contain secrets, such as a domain's salt.
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

	/**
	 * Longest accepted cursor, so that oversized input is rejected before it is decoded.
	 * 
	 * TrustDeck sets no explicit maximum length for pseudonyms (the column is unbounded text, and
	 * generated pseudonyms may have up to 1,000 random characters plus prefix and check digit). The
	 * effective limit is the database: a pseudonym is part of two unique B-tree indexes, whose entries
	 * may be at most 2,704 bytes. A cursor therefore holds at most 4 + 2,704 = 2,708 bytes, which
	 * base64url encodes into 3,611 characters. 4,096 leaves room on top.
	 */
	static final int MAX_CURSOR_LENGTH = 4096;

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
	 * @return the pseudonym value, or {@code null} when the cursor is not valid or too long
	 */
	public static String decodeCursor(String cursor) {
		if (cursor == null || cursor.isBlank() || cursor.length() > MAX_CURSOR_LENGTH) {
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
