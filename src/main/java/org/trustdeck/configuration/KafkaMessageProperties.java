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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Class that loads the Kafka message properties from the yml-file.
 * 
 * @deprecated The internal Kafka listener will be replaced by the dedicated Kafka ingester
 *             and will be removed in a future release.
 * 
 * @author Armin Müller
 */
@Deprecated(forRemoval = true)
@Data
@Configuration
@ConfigurationProperties(prefix = "app.kafka.message")
public class KafkaMessageProperties {
	
	/** The JSON property descriptor/name that indicates the identifier-value. */
	private String identifierDescriptor;
	
	/** The JSON property descriptor/name that indicates the idType-value. */
	private String idTypeDescriptor;
	
	/** The JSON property descriptor/name that indicates the psn-value. */
	private String psnDescriptor;
	
	/** The JSON property descriptor/name that indicates the source system or domain. */
	private String sourceSystemDescriptor;
	
	/** The JSON property descriptor/name that indicates the validity period's start value. */
	private String validFromDescriptor;
	
	/** The JSON property descriptor/name that indicates the validity period's end value. */
	private String validToDescriptor;
	
	/** The JSON property descriptor/name that indicates the username of the user that sent the message. */
	private String usernameDescriptor;
}
