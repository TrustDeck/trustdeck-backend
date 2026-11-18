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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

/**
 * Tests the opaque cursor of pages that are read with a cursor.
 */
class CursorPageDTOTest {

	/** A cursor leads back to exactly the pseudonym value it was created from, special characters included. */
	@Test
	void decodesWhatWasEncoded() {
		for (String psn : new String[] { "TST-0001", "ÄÖÜ-ß-€", "with space/slash+plus=", "a_b%c\\d" }) {
			assertEquals(psn, CursorPageDTO.decodeCursor(CursorPageDTO.encodeCursor(psn)));
		}
	}

	/** The cursor doesn't show the pseudonym value in plain text and is safe to use in a URL. */
	@Test
	void isOpaqueAndUrlSafe() {
		String cursor = CursorPageDTO.encodeCursor("TST-0001/+?");
		assertFalse(cursor.contains("TST"));
		assertEquals(cursor, cursor.replaceAll("[^A-Za-z0-9_-]", ""));
	}

	/** A cursor behind the longest pseudonym the database can store is accepted; longer input is rejected unread. */
	@Test
	void limitsCursorLength() {
		// 2,704 bytes is the largest B-tree index entry, so no stored pseudonym can be longer
		String longest = "P".repeat(2_704);
		String cursor = CursorPageDTO.encodeCursor(longest);
		assertEquals(3_611, cursor.length());
		assertEquals(longest, CursorPageDTO.decodeCursor(cursor));

		// Multi-byte characters take more bytes and still fit
		String umlauts = "Ä".repeat(1_352);
		assertEquals(umlauts, CursorPageDTO.decodeCursor(CursorPageDTO.encodeCursor(umlauts)));

		assertNull(CursorPageDTO.decodeCursor(CursorPageDTO.encodeCursor("P".repeat(4_000))));
		assertNull(CursorPageDTO.decodeCursor("A".repeat(CursorPageDTO.MAX_CURSOR_LENGTH + 1)));
	}

	/** Tokens that weren't created by the backend are rejected. */
	@Test
	void rejectsInvalidCursors() {
		assertNull(CursorPageDTO.decodeCursor(null));
		assertNull(CursorPageDTO.decodeCursor(""));
		assertNull(CursorPageDTO.decodeCursor("   "));
		assertNull(CursorPageDTO.decodeCursor("not base64 at all!"));
		assertNull(CursorPageDTO.decodeCursor("TST-0001"));
		// Valid base64url, but without the format marker
		assertNull(CursorPageDTO.decodeCursor(Base64.getUrlEncoder().encodeToString("TST-0001".getBytes(StandardCharsets.UTF_8))));
	}
}
