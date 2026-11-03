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

package org.trustdeck.security.authentication.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class CustomAuthenticationEntryPointHandlerTest {

    @Test
    void respondsWithUnauthorizedAndBearerChallenge() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/domains");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CustomAuthenticationEntryPointHandler().commence(request, response, new InsufficientAuthenticationException("missing token"));

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        assertEquals("UNAUTHORIZED", response.getContentAsString());
    }
}
