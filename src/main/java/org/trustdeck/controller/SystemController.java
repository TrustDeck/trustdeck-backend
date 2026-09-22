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

package org.trustdeck.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trustdeck.security.audittrail.annotation.Audit;
import org.trustdeck.service.ResponseService;
import org.trustdeck.service.SystemStatisticsService;

/**
 * This class offers REST API endpoints for system information.
 *
 * @author Armin Müller
 */
@RestController
@EnableMethodSecurity
@Slf4j
@RequestMapping(value = "/api")
public class SystemController {

    /** Enables services for standardized responses. */
    @Autowired
    private ResponseService responseService;

    /** Enables access to system statistics. */
    @Autowired
    private SystemStatisticsService systemStatisticsService;

    /**
     * Retrieves current system statistics.
     *
     * @param responseContentType the requested response content type
     * @return the system statistics or an internal server error (500)
     */
    @GetMapping("/system/statistics")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'system:statistics')")
    @Audit
    public ResponseEntity<?> getSystemStatistics(@RequestHeader(name = "accept", required = false) String responseContentType) {
        try {
            return responseService.ok(responseContentType, systemStatisticsService.getStatistics());
        } catch (Exception e) {
            log.error("Retrieving system statistics failed.", e);
            return responseService.internalServerError(responseContentType);
        }
    }
}
