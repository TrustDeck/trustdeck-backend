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

package org.trustdeck.kafka;

import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.trustdeck.configuration.KafkaMessageProperties;
import org.trustdeck.dto.KafkaPseudonymMessageDTO;

import lombok.RequiredArgsConstructor;

/**
 * A mapper to fill the attributes {@link KafkaPseudonymMessageDTO} from the 
 * DTO's catch-all map filled during JSON deserialization.
 * 
 * @deprecated The internal Kafka listener will be replaced by the dedicated Kafka ingester
 *             and will be removed in a future release.
 * 
 * @author Armin Müller
 */
@Deprecated(forRemoval = true)
@Component
@RequiredArgsConstructor
public class KafkaPseudonymMessageDTOMapper {

	/** Provides the mapping names/descriptors for the attributes. */
	private final KafkaMessageProperties kmp;

	/**
	 * Actual mapping function. Uses the names from the properties object 
	 * to search the catch-all map and finally fills the DTO attributes 
	 * accordingly. This allows to have different names for the same attribute
	 * in different settings (e.g., in one setup the identifier might be called
	 * 'id', in another it might be called 'original-value', as defined in the 
	 * application.yml).
	 * 
	 * @param dto the DTO for which the attributes should be set with values from its catch-all map
	 * @return the DTO with properly set attributes
	 */
	public KafkaPseudonymMessageDTO fillDTOAttributeFields(KafkaPseudonymMessageDTO dto) {
		// Define the function to retrieve the value from the catch-all map; ensure proper null-handling
		Function<String, String> get = (field) -> {
			Object v = dto.getRawDTOValues().get(field);
			return v == null ? null : String.valueOf(v);
		};

		// Retrieve and map/assign the values to the proper attributes
		dto.setIdentifier(get.apply(kmp.getIdentifierDescriptor()));
		dto.setIdType(get.apply(kmp.getIdTypeDescriptor()));
		dto.setPsn(get.apply(kmp.getPsnDescriptor()));
		dto.setSourceDomain(get.apply(kmp.getSourceSystemDescriptor()));
		dto.setValidFrom(get.apply(kmp.getValidFromDescriptor()));
		dto.setValidTo(get.apply(kmp.getValidToDescriptor()));
		dto.setUsername(get.apply(kmp.getUsernameDescriptor()));

		return dto;
	}
}
