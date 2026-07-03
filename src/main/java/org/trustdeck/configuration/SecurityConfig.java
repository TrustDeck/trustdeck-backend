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
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.trustdeck.security.RequestResponseCachingFilter;
import org.trustdeck.security.authentication.JwtAuthConverter;
import org.trustdeck.security.authentication.handler.CustomAccessDeniedHandler;
import org.trustdeck.security.authentication.handler.CustomAuthenticationEntryPointHandler;

import java.util.Arrays;

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

	private final JwtAuthConverter jwtAuthConverter;

    /**
     * Defines CORS settings (cf. 
     * <a href="https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html#page-title">Spring: CORS</a>).
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedOrigins(Arrays.asList("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Chain for filtering secured requests.
     *
     * @param http the HTTP request object in a secured manner
     *
     * @return the security filter chain
     * @throws Exception forwarded {@code Exception}s from the configuration process
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
		              .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		              .sessionAuthenticationStrategy(sessionAuthenticationStrategy()))
		      .exceptionHandling(exceptions -> exceptions
		              .authenticationEntryPoint(authenticationEntryPoint())
		              .accessDeniedHandler(accessDeniedHandler()))
		      .oauth2ResourceServer(oauth2 -> oauth2
		              .jwt(jwt -> jwt
		                  .jwtAuthenticationConverter(jwtAuthConverter)))
		      .cors(cors -> cors.configurationSource(corsConfigurationSource()));
		
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
     * Overwrites the SessionAuthenticationStrategy object with a new RegisterSessionAuthenticationStrategy object.
     *
     * @return the new RegisterSessionAuthenticationStrategy
     */
    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        SessionRegistryImpl sr = new SessionRegistryImpl();
        ConcurrentSessionControlAuthenticationStrategy cscas = new ConcurrentSessionControlAuthenticationStrategy(sr);
        cscas.setMaximumSessions(-1);
        return cscas;
    }
}
