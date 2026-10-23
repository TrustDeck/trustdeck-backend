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

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Class that provides the configuration for the Kafka consumer.
 * 
 * @deprecated The internal Kafka listener will be replaced by the dedicated Kafka ingester
 *             and will be removed in a future release.
 * 
 * @author Armin Müller
 */
@Deprecated(forRemoval = true)
@Configuration
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaConsumerConfig {
	
	/**
	 * Provides the factory bean that creates the Kafka consumer using the 
	 * properties loaded from the application.yml.
	 * 
	 * @param p the Kafka properties from the application.yml, injected by Spring
	 * @return the configured Kafka consumer factory bean
	 */
	@Bean
	public ConsumerFactory<String, String> consumerFactory(KafkaConnectionProperties p) {
		// Add all properties to a hashmap
		Map<String, Object> props = new HashMap<>();

		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, p.getBootstrapServers());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, p.getGroupId());
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, p.getSecurityProtocol());
		props.put(SaslConfigs.SASL_MECHANISM, p.getSaslMechanism());
		// Format authentication credentials properly in the Java Authentication and Authorization Service (JAAS)-style
		String jaas = String.format("org.apache.kafka.common.security.plain.PlainLoginModule required "
				+ "username=\"%s\" password=\"%s\";", p.getUsername(), p.getPassword());
		props.put(SaslConfigs.SASL_JAAS_CONFIG, jaas);
		props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, p.getTrustStoreLocation());
		props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, p.getTrustStorePassword());
		props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, p.getTrustStoreType());

		return new DefaultKafkaConsumerFactory<>(props);
	}

	/**
	 * Provides the factory bean that creates the listener container (the background 
	 * components that manage Kafka consumer threads, polling, and calling the 
	 * listener method).
	 * 
	 * @param consumerFactory the consumer factory bean, injected by Spring
	 * @return the configured factory bean
	 */
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
		factory.setConsumerFactory(consumerFactory);

		// Listener methods must explicitly acknowledge a record (or batch) after the processing was successful
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

		// Set how many consumer threads Spring starts per listener
		factory.setConcurrency(1);

		return factory;
	}
}
