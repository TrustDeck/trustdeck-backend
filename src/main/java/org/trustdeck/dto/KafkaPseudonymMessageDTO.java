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

package org.trustdeck.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.HashMap;

/**
 * Data transfer object for the exchange of Kafka messages.
 *
 * @deprecated The internal Kafka listener will be replaced by the dedicated Kafka ingester
 *             and will be removed in a future release.
 * 
 * @author Armin Müller
 */
@Deprecated(forRemoval = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaPseudonymMessageDTO {
	
	/** The identifying value for which a pseudonym will be created. */
	private String identifier;

	/** The pseudonym for the identifying value, either given or created. */
	private String psn;

	/** The type of the identifier (e.g., case number, patient number). */
	private String idType;

	/** The source system (e.g., patient information system). */
	private String sourceDomain;

	/** The validity period's start value for this pseudonym object. */
	private String validFrom;

	/** The validity period's end value for this pseudonym object. */
	private String validTo;
	
	/** The username of the user that sent the message. */
	private String username;

	/** Used as a catch-all object for all JSON fields during deserialization. */
	@JsonIgnore
	@Builder.Default
	private Map<String, Object> rawDTOValues = new HashMap<>();

	/**
	 * Helper method that collects all key-value pairs during the deserialization 
	 * and stores it in the catch-all map. The correct mapping from the map 
	 * to the DTO's attributes needs to done via the 
	 * {@link org.trustdeck.kafka.KafkaPseudonymMessageDTOMapper}.
	 * 
	 * @param name the JSON attribute's name
	 * @param value the JSON attribute's value
	 */
	@JsonAnySetter
	public void storeRawValues(String name, Object value) {
		rawDTOValues.put(name, value);
	}
}
