/*
 * Trust Deck Services
 * Copyright 2022-2025 Armin Müller and Eric Wündisch
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

package org.trustdeck.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * The REST controller that handles calls/forwards to /error
 */
@Slf4j
@RestController
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

	/**
	 * Error handling method.
	 * 
	 * @param request the request object, injected by Spring Boot
     * @return	<li>a <b>500-INTERNAL_SERVER_ERROR</b> status or the 
     * 			status code of the request that caused the error.</li>
	 */
    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        // Retrieve the HTTP error status code
        Object statusCodeObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        // Retrieve the HTTP error status code
        if (statusCodeObj != null) {
            try {
                int statusCode = Integer.parseInt(statusCodeObj.toString());
                status = HttpStatus.valueOf(statusCode);
            } catch (Exception ex) {
                // Fallback to INTERNAL_SERVER_ERROR if parsing fails
            }
        }

		// Try to retrieve and log the root cause of the error
		Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
		if (throwable != null) {
			log.trace("Request failed with exception: ", throwable);
		} else {
			log.trace("Error occurred but no exception object was found in the request.");
		}

        // Build a response map with error details
        Map<String, Object> errorAttributes = new HashMap<>();
        errorAttributes.put("timestamp", LocalDateTime.now());
        errorAttributes.put("status", status.value());
        errorAttributes.put("error", status.getReasonPhrase());
        errorAttributes.put("message", "An unexpected error occurred.");
		errorAttributes.put("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));

        log.debug("Handled error request: " + errorAttributes.toString());
        return new ResponseEntity<>(errorAttributes, status);
    }
}
