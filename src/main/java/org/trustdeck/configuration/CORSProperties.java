/*
 * Trust Deck Services
 * Copyright 2026 Armin Müller and Eric Wündisch
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

package org.trustdeck.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * Configuration properties for Cross-Origin Resource Sharing.
 *
 * CORS only controls which browser origins may call the TrustDeck API.
 * It does not replace authentication or authorization.
 *
 * @author Armin Müller
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "app.cors")
public class CORSProperties {

    /** 
     * Exact browser origins allowed to call the API (each value must contain only scheme, host and optional port, for
     * example {@code https://trustdeck.example.org}. Paths and trailing slashes are not allowed.
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /** HTTP methods accepted for cross-origin API requests. */
    @NotEmpty
    private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE"));

    /** Request headers accepted for cross-origin API requests. */
    @NotEmpty
    private List<String> allowedHeaders = new ArrayList<>(List.of("Authorization", "Content-Type", "Accept", "Origin"));

    /**
     * Whether browser credentials such as cookies may be included.
     *
     * <p>This should remain {@code false} while TrustDeck uses stateless
     * bearer-token authentication.</p>
     */
    private boolean allowCredentials = false;
}
