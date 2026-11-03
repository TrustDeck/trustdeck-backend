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

import org.springframework.stereotype.Service;
import org.trustdeck.dto.KafkaPseudonymMessageDTO;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * A parser for Kafka messages for pseudonymization.
 * This parser parses a given JSON string into a {@link KafkaPseudonymMessageDTO}.
 * 
 * @author Armin Müller
 */
@Service
@RequiredArgsConstructor
public class KafkaPseudonymMessageParser {

	/** The mapper used to transform the given String into the DTO, leading to the filling of the catch-all map in the DTO. */
	private final ObjectMapper objectMapper;

	/** The mapper used to retrieve the DTO attributes from the catch-all map depending on their name (as described in the application.yml). */
	private final KafkaPseudonymMessageDTOMapper dtoMapper;

	/**
	 * Parsing method that encapsulates the creation and two-step mapping of Kafka messages into the DTO.
	 * This allows a variable/per-setup naming of the attributes in the original Kafka messages.
	 * 
	 * @param json the original Kafka message as a JSON string
	 * @return the DTO with properly filled attribute fields
	 * @throws JacksonException when reading the original Kafka JSON message failed
	 */
	public KafkaPseudonymMessageDTO parse(String json) throws JacksonException {
		// Initial mapping from JSON string into DTO --> fills the catch-all map
		KafkaPseudonymMessageDTO dto = objectMapper.readValue(json, KafkaPseudonymMessageDTO.class);
		
		// Second mapping: from the catch-all map into the DTO's attribute fields
		return dtoMapper.fillDTOAttributeFields(dto);
	}
}
