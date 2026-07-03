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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

/**
 * Used to control incoming requests without an access token.
 * 
 * @author Eric Wündisch and Armin Müller
 */
@Slf4j
public class CustomAuthenticationEntryPointHandler implements AuthenticationEntryPoint {

    /**
     * Listen for incoming request and resolve them if no access token is given.
     *
     * @param httpServletRequest the HttpServletRequest object
     * @param httpServletResponse the HttpServletResponse object
     * @param authenticationException the AuthenticationException object
     */
    @Override
    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException authenticationException) {
    	String method = httpServletRequest.getMethod();
        String uri = getRequestUriWithQueryString(httpServletRequest);
        String username = getRequesterName(httpServletRequest);
        String ip = getClientIpAddress(httpServletRequest);

        log.debug("Request without an access token encountered. Request was denied with status code 400-BAD_REQUEST.");
    	log.trace("Causing request: [" + method + "] " + uri + "\nCausing user: " + username + ", IP: " + ip);
        
        if (!httpServletResponse.isCommitted() ) {
	        // Set a 400-BAD_REQUEST status.
	        httpServletResponse.setStatus(HttpStatus.BAD_REQUEST.value());
	        
	        // Try to add an accompanying text.
            PrintWriter writer;
            try {
				writer = httpServletResponse.getWriter();
	            writer.print(HttpStatus.BAD_REQUEST.name().toUpperCase());
			} catch (IOException e) {
				// Not really critical since a HTTP status was successfully set already.
				log.trace("Couldn't add a text to the 400-BAD_REQUEST state: " + e.getMessage());
			}
        }
    }
    
    /**
     * Builds the request URI including the query string, if one is present.
     * 
     * The returned value contains the servlet request URI and appends the raw query
     * string separated by a question mark. If the request has no query string, only
     * the URI is returned.
     *
     * @param request the HTTP servlet request
     * @return the request URI, optionally followed by the query string
     */
    private String getRequestUriWithQueryString(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();

        if (queryString == null || queryString.isBlank()) {
            return uri;
        }

        return uri + "?" + queryString;
    }

    /**
     * Resolves the requester name from the current request or security context.
     * <p>
     * The method first checks the servlet request principal. If no principal is
     * available, it falls back to the current Spring Security authentication. If no
     * authenticated requester can be resolved, {@code anonymous} is returned.
     *
     * @param request the HTTP servlet request
     * @return the resolved requester name, or {@code anonymous} if unavailable
     */
    private String getRequesterName(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();

        if (principal != null && principal.getName() != null) {
            return principal.getName();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }

        return "anonymous";
    }

    /**
     * Resolves the client IP address for the current request.
     * 
     * If reverse-proxy headers are present, the method first uses the first value
     * from {@code X-Forwarded-For}, then {@code X-Real-IP}. If neither header is
     * available, it falls back to the remote address reported by the servlet
     * request.
     * 
     * Proxy headers should only be trusted when the application is deployed behind
     * a trusted reverse proxy that controls or sanitizes these headers.
     *
     * @param request the HTTP servlet request
     * @return the resolved client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");

        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}
