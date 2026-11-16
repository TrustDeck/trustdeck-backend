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
