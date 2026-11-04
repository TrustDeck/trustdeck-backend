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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.utils.SpringBeanLocator;

/**
 * Tests the request validation of {@link PseudonymDTO}.
 */
class PseudonymDTOTest {

    /** The DTO resolves a service bean on construction; a stub context is enough for validation. */
    @BeforeAll
    static void stubSpringContext() {
        new SpringBeanLocator().setApplicationContext(mock(ApplicationContext.class));
    }

    /** A body without an identifier item, such as {@code {}}, is invalid instead of throwing. */
    @Test
    void missingIdentifierItemIsInvalid() {
        assertFalse(new PseudonymDTO().validate());
    }

    /** Blank identifier fields are invalid. */
    @Test
    void incompleteIdentifierItemIsInvalid() {
        PseudonymDTO dto = new PseudonymDTO();
        dto.setIdentifierItem(IdentifierItem.builder().identifier("id-1").idType(" ").build());

        assertFalse(dto.validate());
    }

    /** A complete identifier item is valid. */
    @Test
    void completeIdentifierItemIsValid() {
        PseudonymDTO dto = new PseudonymDTO();
        dto.setIdentifierItem(IdentifierItem.builder().identifier("id-1").idType("patient_number").build());

        assertTrue(dto.validate());
    }
}
