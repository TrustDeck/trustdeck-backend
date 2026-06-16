/*
 * Trust Deck Services
 * Copyright 2024 Armin Müller and Eric Wündisch
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

import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * This class is used to define settings for swagger.
 *
 * @author Armin Müller
 */
@Configuration
public class SwaggerConfig {

	/**
	 * This method sets basic information items that are shown in the API documentation.
	 * 
	 * @return an openAPI object containing some meta information
	 */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("TrustDeck API")
                .version("v2.0.1")
                .description("Scalable trusted third party services for registration and pseudonymization of biomedical data."));
    }
}