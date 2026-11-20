/*
 * Trust Deck Services
 * Copyright 2024-2025 Armin Müller and Eric Wündisch
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

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller class to offer an interface to interact with basic API endpoints.
 * 
 * @author Armin Müller
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    /** Build metadata with the version from the pom.xml; absent when the application was not built by Maven. */
    private final ObjectProvider<BuildProperties> buildProperties;

    /**
     * Constructor.
     *
     * @param buildProperties the optional build metadata
     */
    public ApiController(ObjectProvider<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    /**
     * This method functions as a health-check endpoint.
     *
     * @return a <b>200-OK</b> status with a bit of info.
     */
    @GetMapping("/health")
    public ResponseEntity<?> ping() {
    	return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "trustdeck-backend",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    /**
     * Returns the version of this backend, so that clients can show which version they are
     * connected to. Every authenticated user may read it; no further permission is needed.
     *
     * @return a <b>200-OK</b> status with the version ("unknown" without build metadata)
     */
    @GetMapping("/version")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> version() {
        BuildProperties build = buildProperties.getIfAvailable();
        return ResponseEntity.ok(Map.of("version", build == null || build.getVersion() == null ? "unknown" : build.getVersion()));
    }
}
