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

package org.trustdeck.controller;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.trustdeck.algorithms.Pseudonymizer;
import org.trustdeck.dto.KafkaPseudonymMessageDTO;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.kafka.KafkaPseudonymMessageParser;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.security.audittrail.annotation.Audit;
import org.trustdeck.service.DomainDBAccessService;
import org.trustdeck.service.PseudonymDBAccessService;
import org.trustdeck.utils.Assertion;
import org.trustdeck.utils.Utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This class provides a listener / consumer for a Kafka topic in 
 * which there are messages with pseudonymization requests.
 * The listener is only active when {@code app.kafka.enabled=true}
 * in the application.yaml.
 * 
 * @author Armin Müller
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class PseudonymKafkaListener {
	
	/** Gives access to the message parser so we can transform a Kafka message into a processable DTO. */
	@Autowired
	private KafkaPseudonymMessageParser kafkaParser;

	/** Provides access to the database methods for pseudonym handling. */
	@Autowired
	private PseudonymDBAccessService pdba;

	/** Provides access to the database methods for domain handling. */
	@Autowired
	private DomainDBAccessService ddba;
	
	/**
	 * The method that consumes Kafka messages and processes the content.
	 * Currently, all pseudonymization results are acknowledged, even errors. 
	 * In the future, pushing unprocessable/error messages into a dead letter 
	 * topic (DLT) could be beneficial.
	 * The message processing is not yet part of the audit trail.
	 * 
	 * @param payload the Kafka message
	 * @param ack handle for acknowledging the processing of the message
	 */
	@KafkaListener(topics = "${app.kafka.topic}", containerFactory = "kafkaListenerContainerFactory")
	@Audit
	public void onMessage(Message<String> message, Acknowledgment ack) {
		log.trace("Processing Kafka pseudonymization message ...");
		
		// Get payload from the message
		String payload = message.getPayload();
		
		// Parse JSON into DTO
		KafkaPseudonymMessageDTO kafkaDto;
		try {
			kafkaDto = kafkaParser.parse(payload);
		} catch (JsonProcessingException e) {
			// Log the exception and ack so we don't block the partition forever
			log.warn("Parsing the Kafka message JSON failed, so it was skipped. Payload=" 
					+ Utility.truncateWhenTooLong(payload, 2000), e);
			
			ack.acknowledge();
			return;
		}

		// Check that the required fields are populated
		if (kafkaDto.getIdentifier().isBlank() || kafkaDto.getIdType().isBlank() || kafkaDto.getSourceDomain().isBlank()) {
			log.warn("The Kafka message is missing required fields, so it was skipped. identifier='" + kafkaDto.getIdentifier()
				+ "', idType='" + kafkaDto.getIdType() + "', sourceDomain='" + kafkaDto.getSourceDomain() 
				+ "', rawValues='" + kafkaDto.getRawDTOValues() + "'.");

			ack.acknowledge();
			return;
		}
		
		// Retrieve the domain the pseudonym belongs to
        Domain domain = ddba.getDomainByName(kafkaDto.getSourceDomain());
        if (domain == null) {
            log.trace("The domain in which the pseudonym from the Kafka message should be created couldn't be found. "
            		+ "It will now be created.");
            LocalDateTime validFrom = Utility.parseDateTimeString(kafkaDto.getValidFrom()).toLocalDateTime();
            
            Domain d = new Domain();
            d.setName(kafkaDto.getSourceDomain());
            d.setPrefix(d.getName().substring(0, 2).toUpperCase() + "-");
            d.setValidfrom((validFrom == null || validFrom.isAfter(LocalDateTime.now())) ? LocalDateTime.now() : validFrom);
            d.setValidfrominherited(false);
            d.setValidto(Utility.plusValidityTime(d.getValidfrom(), DomainController.DEFAULT_VALIDITY_TIME));
            d.setValidtoinherited(false);
            d.setEnforcestartdatevalidity(DomainController.DEFAULT_ENFORCE_START_DATE_VALIDITY);
            d.setEnforcestartdatevalidityinherited(false);
            d.setEnforceenddatevalidity(DomainController.DEFAULT_ENFORCE_END_DATE_VALIDITY);
            d.setEnforceenddatevalidityinherited(false);
            d.setAlgorithm(DomainController.DEFAULT_PSEUDONYMIZATION_ALGO);
            d.setAlgorithminherited(false);
            d.setAlphabet(DomainController.DEFAULT_PSEUDONYMIZATION_ALPHABET);
            d.setAlphabetinherited(false);
            d.setRandomalgorithmdesiredsize(DomainController.DEFAULT_RANDOM_ALGORITHM_DESIRED_SIZE);
            d.setRandomalgorithmdesiredsizeinherited(false);
            d.setRandomalgorithmdesiredsuccessprobability(DomainController.DEFAULT_RANDOM_ALGORITHM_DESIRED_SUCCESS_PROBABILITY);
            d.setRandomalgorithmdesiredsuccessprobabilityinherited(false);
            d.setConsecutivevaluecounter(0L);
            d.setMultiplepsnallowed(DomainController.DEFAULT_ALLOW_MULTIPLE_PSN);
            d.setMultiplepsnallowedinherited(false);
            d.setPseudonymlength(DomainController.DEFAULT_PSEUDONYM_LENGTH);
            d.setPseudonymlengthinherited(false);
            d.setPaddingcharacter(String.valueOf(DomainController.DEFAULT_PADDING_CHARACTER));
            d.setPaddingcharacterinherited(false);
            d.setAddcheckdigit(DomainController.DEFAULT_ADD_CHECK_DIGIT);
            d.setAddcheckdigitinherited(false);
            d.setLengthincludescheckdigit(DomainController.DEFAULT_LENGTH_INCLUDES_CHECK_DIGIT);
            d.setLengthincludescheckdigitinherited(false);
            d.setSalt(DomainController.generateSalt(DomainController.DEFAULT_SALT_LENGTH));
            d.setSaltlength(d.getSalt().length());
            d.setDescription("Autogenerated domain to store pseudonym objects obtained from Kafka.");
            
            if (ddba.insertDomain(d).equalsIgnoreCase(DomainDBAccessService.INSERTION_SUCCESS)) {
            	domain = ddba.getDomainByName(d.getName());
            } else {
            	log.debug("Could not create a domain for the Kafka pseudonym objects.");

                ack.acknowledge();
    			return;
            }
        }

        // Check if the domain still allows adding pseudonyms
        if (domain.getValidto().isBefore(LocalDateTime.now())) {
            // Expired validity period. No changes allowed
            log.debug("The validity period of the domain has already expired. No changes allowed.");

            ack.acknowledge();
			return;
        }
		
		// Start creating PseudonymDTO
		IdentifierItem ii = IdentifierItem.builder()
				.identifier(kafkaDto.getIdentifier())
				.idType(kafkaDto.getIdType())
				.build();
		
		PseudonymDTO dto = PseudonymDTO.builder()
				.identifierItem(ii)
				.domainName(domain.getName())
				.build();

		// Determine validFrom date if (not) given by the user
		LocalDateTime validFrom = Utility.parseDateTimeString(kafkaDto.getValidFrom()).toLocalDateTime();
        if (validFrom != null) {
            if (domain.getEnforcestartdatevalidity()) {
                // Ensure that the given start date isn't before the start date of the domain
                dto.setValidFrom((validFrom.isAfter(domain.getValidfrom())) ? validFrom : domain.getValidfrom());
                dto.setValidFromInherited(!validFrom.isAfter(domain.getValidfrom()));
            } else {
                dto.setValidFrom(validFrom);
                dto.setValidFromInherited(false);
            }
        } else {
            dto.setValidFrom(domain.getValidfrom());
            dto.setValidFromInherited(true);
        }

        // Determine validTo date if (not) given by the user
		OffsetDateTime parsed = Utility.parseDateTimeString(kafkaDto.getValidTo());
		LocalDateTime validTo = parsed == null ? null : parsed.toLocalDateTime();
        if (validTo != null) {
            // End date of validity period is given
            if (domain.getEnforceenddatevalidity()) {
                // Ensure that the given end date isn't after the end date of the domain
                dto.setValidTo((validTo.isBefore(domain.getValidto())) ? validTo : domain.getValidto());
                dto.setValidToInherited(!validTo.isBefore(domain.getValidto()));
            } else {
                dto.setValidTo(validTo);
                dto.setValidToInherited(false);
            }
        } else {
            // Nothing was given: use date from domain
            dto.setValidTo(domain.getValidto());
            dto.setValidToInherited(true);
        }

        // Pseudonymize the identifier and store it in the object
        String pseudonym = null;
        String psn = kafkaDto.getPsn();
        if (Assertion.isNotNullOrEmpty(kafkaDto.getPsn())) {
            // A pseudonym was already given --> store it instead of creating a new one
            pseudonym = psn.trim();
        } else {
        	// Generate a new pseudonym
            pseudonym = PseudonymController.pseudonymize(ii.getIdentifier(), ii.getIdType(), domain, false);
        }
        
        if (pseudonym == null) {
            // Pseudonymization failed
            log.error("Pseudonymization failed for Kafka message for identifier \"" + ii.getIdentifier() 
            	+ "\" and idType \"" + ii.getIdType() + "\".");

            ack.acknowledge();
			return;
        }
        
        dto.setPsn(pseudonym);
        
        // Insert the pseudonym into the database
        String result = pdba.createPseudonyms(List.of(dto), domain.getId(), domain.getMultiplepsnallowed()).getFirst();
		
        // If a random algorithm is used, check if we generated a duplicate. If so, retry.
        if (domain.getAlgorithm().toUpperCase().startsWith("RANDOM")) {
	        // Retry DEFAULT_NUMBER_OF_RETRIES - 1 times
        	for (int i = 1; i < Pseudonymizer.DEFAULT_NUMBER_OF_RETRIES; i++) {
	        	// Check if its actually a duplicate
        		if (result.equals(PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM)) {
	        		// Retry
				dto.setPsn(PseudonymController.pseudonymize(ii.getIdentifier(), ii.getIdType(), domain, false));
	        		result = pdba.createPseudonyms(List.of(dto), domain.getId(), domain.getMultiplepsnallowed()).getFirst();
				} else {
					// Not a duplicate
					break;
				}
			}
        	
        	// Re-check if it's still a duplicate
        	if (result.equals(PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM)) {
        		log.warn("Couldn't generate a new pseudonym due to collisions due to too many pseudonyms being "
        				+ "already in the database (identifier=\"" + ii.getIdentifier() + "\", idType=\"" + ii.getIdType() 
        				+ "\").");
        		
        		// Note: we could omit acknowledging here, so Kafka would retry, but that could also lead to a loop
                ack.acknowledge();
    			return;
        	}
    	}

        // Evaluate the result
        if (result.equals(PseudonymDBAccessService.INSERTION_SUCCESS)) {
            // Success
            log.debug("Successfully inserted a new pseudonym (" + pseudonym + ") from the processed Kafka message.");
            ack.acknowledge();
			return;
        } else if (result.equals(PseudonymDBAccessService.INSERTION_DUPLICATE_IDENTIFIER)) {
            // Nothing added since the entry is a duplicate
            log.debug("The pseudonym requested to be inserted was skipped because it is already in the database.");
            ack.acknowledge();
			return;
        } else {
            // Nothing added
            log.error("Insertion of a new pseudonym failed.");

    		// Note: we could omit acknowledging here, so Kafka would retry, but that could also lead to a loop
            ack.acknowledge();
			return;
        }
	}
}
