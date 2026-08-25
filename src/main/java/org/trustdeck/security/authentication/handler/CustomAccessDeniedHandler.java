/*
 * Trust Deck Services
 * Copyright 2022-2024 Armin Müller and Eric Wündisch
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Handles access denied states and answers with a 403-FORBIDDEN status.
 * 
 * @author Eric Wündisch and Armin Müller
 */
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * Custom handler method that creates a 403-FORBIDDEN status response  
     * if the token is valid but does not meet the permissions.
     *
     * @param request the request object
     * @param response the response object
     * @param accessDeniedException the AccessDeniedException as object
     */
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        log.debug("Access denied for [" + request.getMethod() + "] " + request.getRequestURI() + ".");

        if (!response.isCommitted()) {
        	// Set a 403-FORBIDDEN status
            response.setStatus(HttpStatus.FORBIDDEN.value());
            
            // Try to add an accompanying text
            PrintWriter writer;
            try {
				writer = response.getWriter();
	            writer.print(HttpStatus.FORBIDDEN.name().toUpperCase());
			} catch (IOException e) {
				// Not really critical since a HTTP status was successfully set already.
				log.trace("Couldn't add a text to the 403-FORBIDDEN state: " + e.getMessage());
			}
        }
    }
}
