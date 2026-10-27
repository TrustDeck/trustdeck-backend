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
