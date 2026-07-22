/*
 * Trust Deck Services
 * Copyright 2022-2026 Armin Müller and Eric Wündisch
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.trustdeck.security.RequestResponseCachingFilter;
import org.trustdeck.security.authentication.JwtAuthConverter;
import org.trustdeck.security.authentication.handler.CustomAccessDeniedHandler;
import org.trustdeck.security.authentication.handler.CustomAuthenticationEntryPointHandler;

import java.net.URI;
import java.util.List;

/**
 * This class is used to define security settings for keycloak and other custom security options.
 *
 * @author Armin Müller and Eric Wündisch
 */
@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

	/** Connects the converter that handles authentication from the Java web token. */
	private final JwtAuthConverter jwtAuthConverter;
	
	/** Object storing the CORS properties. */
	private final CORSProperties corsProperties;

	/**
	 * Defines the CORS policy for the TrustDeck REST API.
	 *
	 * @return the configured CORS configuration source
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
	    CorsConfiguration configuration = new CorsConfiguration();
	    
	    List<String> allowedOrigins = cleanValues(corsProperties.getAllowedOrigins());
	    allowedOrigins.forEach(SecurityConfig::validateOrigin);

	    // An empty list intentionally denies all cross-origin browser requests.
	    configuration.setAllowedOrigins(allowedOrigins);
	    configuration.setAllowedMethods(cleanValues(corsProperties.getAllowedMethods()));
	    configuration.setAllowedHeaders(cleanValues(corsProperties.getAllowedHeaders()));
	    configuration.setAllowCredentials(corsProperties.isAllowCredentials());

	    // Only the actual TrustDeck REST API needs this policy
	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    source.registerCorsConfiguration("/api/**", configuration);

	    if (allowedOrigins.isEmpty()) {
	        log.info("No CORS origins configured. Cross-origin browser requests to the TrustDeck API will be rejected.");
	    } else {
	        log.debug("Configured CORS for " + allowedOrigins.size() + " exact browser origin(s).");
	    }

	    return source;
	}

    /**
     * Chain for filtering secured requests.
     *
     * @param http the HTTP request object in a secured manner
     * @param corsConfigurationSource the CORS configuration; is injected by Spring from the above method
     *
     * @return the security filter chain
     * @throws Exception forwarded {@code Exception}s from the configuration process
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        log.debug("Creating default security filter chain ...");
        
    	http.addFilterAfter(new RequestResponseCachingFilter(), BasicAuthenticationFilter.class)
		      .csrf(csrf -> csrf.disable()) // CSRF will be disabled since this API will mainly be used by services not browsers or individual users
		      .authorizeHttpRequests(auth -> auth
		    		// Permit all requests to Swagger UI and API documentation
		    		.requestMatchers(
		                      "/v3/api-docs/**",
		                      "/swagger-ui.html",
		                      "/swagger-ui/**"
		                  ).permitAll()
		    		// Recommended for browser-based clients: allow CORS preflight requests
		            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
		            // Allow health-checks
		            .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
		            // Optional: allow the actuator and health info endpoint
		            // .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
		            // Everything else must be authenticated
		            .anyRequest().authenticated())
		      .sessionManagement(session -> session
		              .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		      .exceptionHandling(exceptions -> exceptions
		              .authenticationEntryPoint(authenticationEntryPoint())
		              .accessDeniedHandler(accessDeniedHandler()))
		      .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)))
		      .cors(cors -> cors.configurationSource(corsConfigurationSource));
		
		  return http.build();
    }

    /**
     * Overwrites the AuthenticationEntryPoint with a new CustomAuthenticationEntryPointHandler.
     *
     * @return the authentication entry-point handler
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new CustomAuthenticationEntryPointHandler();
    }

    /**
     * Overwrites the AccessDeniedHandler with a new CustomAccessDeniedHandler.
     *
     * @return the overwritten access denied handler
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }
    
    /**
     * Removes blank and duplicate configuration entries.
     *
     * @param values the configured values
     * @return cleaned values
     */
    private static List<String> cleanValues(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream().filter(value -> value != null && !value.isBlank()).map(String::strip).distinct().toList();
    }

    /**
     * Validates that a configured value is an exact HTTP or HTTPS origin.
     *
     * @param origin the configured origin
     */
    private static void validateOrigin(String origin) {
        if (origin.contains("*")) {
            log.warn("Wildcard CORS origins should only be used during development.");
        }

        final URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid CORS origin: " + origin, exception);
        }

        boolean validScheme = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
        boolean hasHost = uri.getHost() != null && !uri.getHost().isBlank();
        boolean hasNoPath = uri.getRawPath() == null || uri.getRawPath().isEmpty();
        boolean hasNoAdditionalParts = uri.getRawUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null;

        if (!validScheme || !hasHost || !hasNoPath || !hasNoAdditionalParts) {
            throw new IllegalStateException("CORS origin must contain only scheme, host and optional port, without a trailing slash: " + origin);
        }
    }
}
