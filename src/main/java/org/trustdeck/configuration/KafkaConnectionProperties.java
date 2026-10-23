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
 * Class that loads the Kafka properties from the yml-file.
 * 
 * @deprecated The internal Kafka listener will be replaced by the dedicated Kafka ingester
 *             and will be removed in a future release.
 * 
 * @author Armin Müller
 */
@Deprecated(forRemoval = true)
@Data
@Configuration
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaConnectionProperties {
	
	/** The flag determining whether the Kafka consumer should be activated/used or not. */
	private boolean enabled;
	
	/** A comma-separated list of Kafka bootstrap servers in the format host:port. */
	private String bootstrapServers;
	
	/** The name of the Kafka topic. */
	private String topic;
	
	/** The group ID where this consumer is in. */
	private String groupId;
	
	/** The username used for authentication against Kafka. */
	private String username;
	
	/** The user's password. */
	private String password;
	
	/** The protocol used for securing communication channels between the consumer and Kafka servers (default: SASL_SSL). */
	private String securityProtocol = "SASL_SSL";
	
	/** The authentication mechanism used in SASL (default: PLAIN). */
	private String saslMechanism = "PLAIN";
	
	/** The truststore location for the Kafka connection. Contains the certs needed to trust to access the topic. */
	private String trustStoreLocation;

	/** The password for the Kafka truststore. */
	private String trustStorePassword;

	/** The type of the Kafka truststore. */
	private String trustStoreType = "JKS";
}
