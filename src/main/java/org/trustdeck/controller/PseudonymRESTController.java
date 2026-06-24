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

package org.trustdeck.controller;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.*;
import org.trustdeck.algorithms.LuhnCheckDigit;
import org.trustdeck.algorithms.LuhnMod10CheckDigit;
import org.trustdeck.algorithms.LuhnMod16CheckDigit;
import org.trustdeck.algorithms.LuhnMod26CheckDigit;
import org.trustdeck.algorithms.LuhnMod32CheckDigit;
import org.trustdeck.algorithms.LuhnMod36CheckDigit;
import org.trustdeck.algorithms.PseudonymizationFactory;
import org.trustdeck.algorithms.Pseudonymizer;
import org.trustdeck.algorithms.RandomNumberPseudonymizer;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.dto.PseudonymUpdateDTO;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.security.audittrail.annotation.Audit;
import org.trustdeck.service.AuthorizationService;
import org.trustdeck.service.DomainDBAccessService;
import org.trustdeck.service.PseudonymDBAccessService;
import org.trustdeck.service.ResponseService;
import org.trustdeck.utils.Assertion;
import org.trustdeck.utils.Utility;
import org.trustdeck.utils.Utility.Pair;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents a REST-API controller encapsulating the requests for pseudonyms.
 * This REST-API offers full access to the data items.
 *
 * @author Armin Müller and Eric Wündisch
 */
@RestController
@EnableMethodSecurity
@Slf4j
@RequestMapping(value = "/api")
public class PseudonymRESTController {

    /** Enables the access to the domain specific database access methods. */
    @Autowired
    private DomainDBAccessService domainDBAccessService;

    /** Enables the access to the pseudonym specific database access methods. */
    @Autowired
    private PseudonymDBAccessService pseudonymDBAccessService;

    /** Enables services for better working with responses. */
    @Autowired
    private ResponseService responseService;

    /** Provides functionality to ensure proper rights and roles when accessing the endpoints. */
    @Autowired
    private AuthorizationService authorizationService;

    /** The default maximum allowed batch size. */
    private static final int DEFAULT_PSEUDONYM_BATCH_LENGTH = 50000;

    /** The default for regenerating the pseudonym on updates that affect the identifierItem or the domain. */
    private static final boolean DEFAULT_REGENERATE_PSEUDONYM = true;
    
    /** The default maximum number of pseudonyms found by searching the DB that are returned to the user. */
    private static final int MAX_NUMBER_OF_SEARCH_RESULTS = 30;
	
    /**
     * Method to check if the current domain's filling rate exceeds a point where the generation of unseen
     * pseudonyms is not anymore reasonably probable.
     * 
     * @param domain the domain to check
     */
	private void checkDomainFillingRate(Domain domain) {
		/* A high filling rate of the domain can result in too few possible pseudonyms left for generation.
		* We can calculate a threshold for which we can reasonably probable generate pseudonyms using this formula: 
		* 1-(1-((k-n)/k))^m > T => n < k*(1-T)^(1/m); with k being the number of theoretically possible pseudonyms 
		* (here number of alphabet-symbols a and psnLength characters, so k=a^psnLength); n being the number of 
		* reasonable probable pseudonyms; m being the number of retries; and T being the probability threshold which 
		* we deem to be reasonable probable (here: 99.999998% (probability of not getting struck by lightning), 
		* meaning that we want to successfully generate a pseudonym in a maximum of m=3 tries in 99.999998% of all 
		* cases).
		* --> Warn the user, if the domain is too full
		*/
		Double T = domain.getRandomalgorithmdesiredsuccessprobability();
		Double k = Math.pow(10.0d, (double) domain.getPseudonymlength());
		Double n = k * Math.pow((1.0d - T), (1.0d / (double) Pseudonymizer.DEFAULT_NUMBER_OF_RETRIES));
		
		Integer existingPseudonyms = domainDBAccessService.getAmountOfPseudonymsInDomain(domain.getName());
		existingPseudonyms = existingPseudonyms == null ? 0 : existingPseudonyms; // Ignore unsuccessful database retrieval
		
		if (existingPseudonyms > n) {
			// The domain reached it's filling point (~49%) at which the probability to generate a 
			// previously unseen pseudonym in m tries is no longer greater than 99.999998%.
			log.warn("The number of reasonably probable generated pseudonyms for this domain is exhausted. The probability "
					+ "to generate collisions will rise and at some point prevent the generation of non-colliding pseudonyms "
					+ "in a certain number of retries.");
		}
	}

    /**
     * This method creates new pseudonymization-pseudonyms in batches.
     * When an external created pseudonym is given, no new pseudonym
     * is created but the given one is stored.
     *
     * @param domainName (required) the name of the domain the pseudonyms should be in
     * @param omitPrefix (optional) determines whether or not the prefix should be added to the pseudonym
     * @param pseudonymDtoList (required) the list of necessary information, formatted as a JSON to match the pseudonymDto
     * @param responseContentType (optional) the response content type
     * @return	<li>a <b>201-CREATED</b> status and a list of the created
     * 				<b>pseudonym-objects</b> on success</li>
     * 			<li>a <b>404-NOT_FOUND</b> when the domain wasn't found</li>
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> when the insertion into
     * 				the database failed (e.g. due to an expired domain validity
     * 				period or a batch that exceeds the maximum allowed batch size).</li>
     * 			<li>a <b>500-INTERNAL_SERVER_ERROR</b> when the pseudonymization
     * 				failed</li>
     * 			<li>a <b>507-INSUFFICICENT_STORAGE</b> when the domain reached its 
     * 				filling point and we therefore only generated collisions.</li>
     */
    @PostMapping("/domains/{domainName}/pseudonyms/batch")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'pseudonym:create-batch')")
    @Audit
    public ResponseEntity<?> createPseudonymBatch(@PathVariable("domainName") String domainName,
                                               	  @RequestParam(name = "omitPrefix", required = false, defaultValue = "false") Boolean omitPrefix,
                                               	  @RequestBody List<PseudonymDTO> pseudonymDtoList,
                                               	  @RequestHeader(name = "accept", required = false) String responseContentType) {
        // Check that the batch size isn't too big.
        if (pseudonymDtoList.size() > DEFAULT_PSEUDONYM_BATCH_LENGTH) {
            // The batch size exceeded the limit. Return an error 422-UNPROCESSABLE_ENTITY.
            log.error("The given list of objects is too big. The maximum allowed batch size is: " + DEFAULT_PSEUDONYM_BATCH_LENGTH);
            return responseService.unprocessableEntity(responseContentType);
        }

        // Retrieve the domain the pseudonyms belong to
        Domain domain = domainDBAccessService.getDomainByName(domainName);

        if (domain == null) {
            // The domain wasn't found; return a 404-NOT_FOUND
            log.debug("The domain in which the pseudonyms should be created couldn't be found.");
            return responseService.notFound(responseContentType);
        }

        // Check if the domain still allows adding pseudonyms
        if (domain.getValidto().isBefore(LocalDateTime.now())) {
            // Expired validity period. No changes allowed. Return an error 422-UNPROCESSABLE_ENTITY.
            log.debug("The validity period of the domain has already expired. No changes allowed.");
            return responseService.unprocessableEntity(responseContentType);
        }

        // Transform the user-given inputs into a list of pseudonym objects.
        List<PseudonymDTO> pseudonyms = new ArrayList<>();
        for (PseudonymDTO pseudonymDTO : pseudonymDtoList) {
            // Start creating the pseudonym
            PseudonymDTO p = new PseudonymDTO();
            p.setIdentifierItem(pseudonymDTO.getIdentifierItem());
            p.setDomainName(domain.getName());

            // Pseudonymize the identifier and store it in the object
            String pseudonym = null;
            if (pseudonymDTO.getPsn() != null && !pseudonymDTO.getPsn().trim().equals("")) {
                // A pseudonym was already given --> store it instead of creating a new one if it's in the correct format
                String psn = pseudonymDTO.getPsn().trim();
                if (omitPrefix != null && omitPrefix) {
                    pseudonym = psn;
                } else {
                    pseudonym = psn.startsWith(domain.getPrefix()) ? psn : domain.getPrefix() + psn;
                }
            } else {
                // Generate a new pseudonym
                String prefix = (omitPrefix != null && omitPrefix) ? "" : domain.getPrefix(); // Omitting the prefix here shouldn't be the norm
                Pseudonymizer pseudonymizer = new PseudonymizationFactory().getPseudonymizer(domain);
                pseudonym = pseudonymizer.pseudonymize(pseudonymDTO.getIdentifierItem().getIdentifier() + pseudonymDTO.getIdentifierItem().getIdType() + domain.getSalt(), prefix);
                pseudonym = domain.getAddcheckdigit() ? pseudonymizer.addCheckDigit(pseudonym, domain.getLengthincludescheckdigit(), domain.getName(), prefix) : pseudonym;

                if (domain.getAlgorithm().toUpperCase().startsWith("RANDOM") && pseudonym.equals(RandomNumberPseudonymizer.DOMAIN_FULL)) {
                	// Pseudonymization failed due to too many collisions. The domain reached it's filling point (~49%) 
                	// at which the probability to generate a previously unseen pseudonym in 25 tries is no longer greater
                	// than 99.999998%.
                	log.warn("Couldn't generate a new pseudonym due to too many pseudonyms being already in the database. ");
                	return responseService.insufficientStorage(responseContentType);
                } else if (pseudonym == null && domain.getAlgorithm().toUpperCase().startsWith("RANDOM")) {
                	// Pseudonymization failed: probably no non-colliding pseudonym was found.
                	log.warn("Pseudonymization failed for identifier \"" + pseudonymDTO.getIdentifierItem().getIdentifier() + "\" and idType \"" + pseudonymDTO.getIdentifierItem().getIdType() + "\". "
                			+ "Probably due to collisions with other pseudonyms. Try a greater pseudonym-length.");
                	return responseService.unprocessableEntity(responseContentType);
            	} else if (pseudonym == null) {
                    // Pseudonymization failed. Return a 500-INTERNAL_SERVER_ERROR.
                    log.error("Pseudonymization failed for identifier \"" + pseudonymDTO.getIdentifierItem().getIdentifier() + "\" and idType \"" + pseudonymDTO.getIdentifierItem().getIdType() + "\".");
                    return responseService.internalServerError(responseContentType);
                }
            }
            p.setPsn(pseudonym);

            // Determine validFrom date if (not) given by the user
            if (pseudonymDTO.getValidFrom() != null) {
                if (domain.getEnforcestartdatevalidity()) {
                    // Ensure that the given start date isn't before the start date of the domain
                    p.setValidFrom(pseudonymDTO.getValidFrom().isAfter(domain.getValidfrom()) ? pseudonymDTO.getValidFrom() : domain.getValidfrom());
                    p.setValidFromInherited(!pseudonymDTO.getValidFrom().isAfter(domain.getValidfrom()));
                } else {
                    p.setValidFrom(pseudonymDTO.getValidFrom());
                    p.setValidFromInherited(false);
                }
            } else {
                // Nothing given by the user. Use domain information.
            	// If no start and end date is given, ensure that the used start date plus a given validityTime results in a 
            	// validity period that has not already ended (e.g. domain.start is 2 weeks ago, validityTime is 1 week 
            	// --> the calculated period would be in the past) 
            	if (pseudonymDTO.getValidTo() == null && pseudonymDTO.getValidityTime() != null 
            			&& Utility.plusValidityTime(domain.getValidfrom(), pseudonymDTO.getValidityTime()).isBefore(LocalDateTime.now())
            			&& LocalDateTime.now().isBefore(domain.getValidto())) {
            		log.debug("Using the domain validFrom-date plus the given validity time would result in an expired pseudonym. Using now() for the start instead.");
            		
            		p.setValidFrom(LocalDateTime.now());
            		p.setValidFromInherited(false);
            	}
            	
                p.setValidFrom(domain.getValidfrom());
                p.setValidFromInherited(true);
            }

            // Determine validTo date if (not) given by the user
            if (pseudonymDTO.getValidTo() != null) {
                // End date of validity period is given
                if (domain.getEnforceenddatevalidity()) {
                    // Ensure that the given end date isn't after the end date of the domain
                    p.setValidTo((pseudonymDTO.getValidTo().isBefore(domain.getValidto())) ? pseudonymDTO.getValidTo() : domain.getValidto());
                    p.setValidToInherited(!pseudonymDTO.getValidTo().isBefore(domain.getValidto()));
                } else {
                    p.setValidTo(pseudonymDTO.getValidTo());
                    p.setValidToInherited(false);
                }
            } else if (pseudonymDTO.getValidTo() == null && pseudonymDTO.getValidityTime() != null) {
                // A validity period was given
                String vTime = pseudonymDTO.getValidityTime();

                if (domain.getEnforceenddatevalidity()) {
                    // Ensure that the given validity period ends before the end date of the domain
                    p.setValidTo((Utility.plusValidityTime(p.getValidFrom(), vTime).isBefore(domain.getValidto())) ? Utility.plusValidityTime(p.getValidFrom(), vTime) : domain.getValidto());
                    p.setValidToInherited(!Utility.plusValidityTime(p.getValidFrom(), vTime).isBefore(domain.getValidto()));
                } else {
                    p.setValidTo(Utility.plusValidityTime(p.getValidFrom(), vTime));
                    p.setValidToInherited(false);
                }
            } else {
                // Nothing was given: use date from domain
                p.setValidTo(domain.getValidto());
                p.setValidToInherited(true);
            }

            // Add the newly created pseudonym to the list
            pseudonyms.add(p);
        }

        // Insert the list of pseudonyms in one batch
        List<String> result = pseudonymDBAccessService.createPseudonyms(pseudonyms, domain.getId(), domain.getMultiplepsnallowed());

        // Evaluate the result
        List<PseudonymDTO> pseudonymDTOs = new ArrayList<>();
        List<String> pseudonymDtoStrings = new ArrayList<>();
        int successful = 0;

        for (int i = 0; i < pseudonyms.size(); i++) {
        	if (result.get(i).equals(PseudonymDBAccessService.INSERTION_SUCCESS)) {
	            // Success. Return a status code 201-CREATED and the pseudonym as payload.
        		PseudonymDTO p = pseudonyms.get(i);
        		successful++;

                // Determine whether or not a reduced standard view or a complete view is requested
                if (!authorizationService.hasDomainPermission(domainName, "complete-view")) {
                    p.toReducedStandardView();
                }

                // Process the DTO depending on the response`s media type
                if (responseContentType != null && responseContentType.equals(MediaType.TEXT_PLAIN_VALUE)) {
                    pseudonymDtoStrings.add(p.toRepresentationString());
                } else {
                    pseudonymDTOs.add(p);
                }
            }
        }
        
        // Decide which status code to use
        if (result.contains(PseudonymDBAccessService.INSERTION_SUCCESS)) {
        	if (result.contains(PseudonymDBAccessService.INSERTION_DUPLICATE_IDENTIFIER)
        		|| result.contains(PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM)
        		|| result.contains(PseudonymDBAccessService.INSERTION_ERROR)) {
        		// Encountered some unprocessable entities
        		log.debug("Successfully inserted " + successful + " out of " + pseudonyms.size() + " pseudonyms.");
        		if (responseContentType != null && responseContentType.equals(MediaType.TEXT_PLAIN_VALUE)) {
                    return responseService.partialContent(responseContentType, pseudonymDtoStrings);
                } else {
                    return responseService.partialContent(responseContentType, pseudonymDTOs);
                }
        	} else {
	            // Only successful inserts
	            log.debug("Successfully inserted the batch of " + pseudonyms.size() + " pseudonyms.");
	
	            if (responseContentType != null && responseContentType.equals(MediaType.TEXT_PLAIN_VALUE)) {
	                return responseService.created(responseContentType, pseudonymDtoStrings);
	            } else {
	                return responseService.created(responseContentType, pseudonymDTOs);
	            }
        	}
        } else {
            // Nothing added. Return an error 422-UNPROCESSABLE_ENTITY.
            log.error("Insertion of a batch of pseudonyms failed.");
            return responseService.unprocessableEntity(responseContentType);
        }
    }

    /**
     * This method creates a new pseudonym.
     * When an external created pseudonym is given, no new pseudonym
     * is created but the given one is stored.
     * This method functions as a get-method if the pseudonym already exists.
     *
     * @param domainName (required) the name of the domain the pseudonym should be in
     * @param pseudonymDTO (required) the Pseudonym object
     * @param omitPrefix (optional) determines whether or not the prefix should be added to the pseudonym
     * @param responseContentType (optional) the response content type
     * @return	<li>a <b>200-OK</b> status and the <b>pseudonym</b> when the
     * 				requested insertion would be a duplicate</li>
     * 			<li>a <b>201-CREATED</b> status and the created pseudonym on
     * 				success</li>
     * 			<li>a <b>404-NOT_FOUND</b> when the domain wasn't found</li>
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> when the insertion into
     * 				the database failed (e.g. due to an expired domain validity
     * 				period).</li>
     * 			<li>a <b>500-INTERNAL_SERVER_ERROR</b> when the pseudonymization
     * 				failed</li>
     * 			<li>a <b>507-INSUFFICICENT_STORAGE</b> when the domain reached its 
     * 				filling point and we therefore only generated collisions.</li>
     */
    @PostMapping("/domains/{domainName}/pseudonyms")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'pseudonym:create')")
    @Audit
    public ResponseEntity<?> createPseudonym(@PathVariable("domainName") String domainName,
                                          	 @RequestBody PseudonymDTO pseudonymDTO,
                                          	 @RequestParam(name = "omitPrefix", required = false, defaultValue = "false") Boolean omitPrefix,
                                          	 @RequestHeader(name = "accept", required = false) String responseContentType) {

        if (!pseudonymDTO.validate() || !pseudonymDTO.isValidStandardView()) {
        	log.debug("The given PseudonymDTO was either invalid or had information that is not allowed in it.");
            return responseService.unprocessableEntity(responseContentType);
        }

        String identifier = pseudonymDTO.getIdentifierItem().getIdentifier();
        String idType = pseudonymDTO.getIdentifierItem().getIdType();
        String psn = pseudonymDTO.getPsn();
        Timestamp validFrom = pseudonymDTO.getValidFrom() != null ? Timestamp.valueOf(pseudonymDTO.getValidFrom()) : null;
        Timestamp validTo = pseudonymDTO.getValidTo() != null ? Timestamp.valueOf(pseudonymDTO.getValidTo()) : null;
        String validityTime = pseudonymDTO.getValidityTime();

        if (Assertion.assertNullAll(identifier, idType, psn, validFrom, validTo, validityTime, domainName)) {
            // An empty object was passed, so there is nothing to create.
            log.debug("The pseudonym DTO passed by the user was empty. Nothing to create.");
            return responseService.unprocessableEntity(responseContentType);
        }

        // Retrieve the domain the pseudonym belongs to
        Domain domain = domainDBAccessService.getDomainByName(domainName);

        if (domain == null) {
            // The domain wasn't found; return a 404-NOT_FOUND
            log.debug("The domain in which the pseudonym should be created couldn't be found.");
            return responseService.notFound(responseContentType);
        }

        // Check if the domain still allows adding pseudonyms
        if (domain.getValidto().isBefore(LocalDateTime.now())) {
            // Expired validity period. No changes allowed. Return an error 422-UNPROCESSABLE_ENTITY.
            log.debug("The validity period of the domain has already expired. No changes allowed.");
            return responseService.unprocessableEntity(responseContentType);
        }

        // Start creating the pseudonym
        PseudonymDTO p = new PseudonymDTO();
        p.setIdentifierItem(pseudonymDTO.getIdentifierItem());
        p.setDomainName(domainName);

        // Determine validFrom date if (not) given by the user
        if (validFrom != null) {
            if (domain.getEnforcestartdatevalidity()) {
                // Ensure that the given start date isn't before the start date of the domain
                p.setValidFrom((validFrom.after(Timestamp.valueOf(domain.getValidfrom()))) ? validFrom.toLocalDateTime() : domain.getValidfrom());
                p.setValidFromInherited(!validFrom.after(Timestamp.valueOf(domain.getValidfrom())));
            } else {
                p.setValidFrom(validFrom.toLocalDateTime());
                p.setValidFromInherited(false);
            }
        } else {
        	// If no start and end date is given, ensure that the used start date plus a given validityTime results in a 
        	// validity period that has not already ended (e.g. domain.start is 2 weeks ago, validityTime is 1 week 
        	// --> the calculated period would be in the past) 
        	if (validTo == null && validityTime != null && Utility.plusValidityTime(domain.getValidfrom(), validityTime).isBefore(LocalDateTime.now())
        			&& LocalDateTime.now().isBefore(domain.getValidto())) {
        		log.debug("Using the domain validFrom-date plus the given validity time would result in an expired pseudonym. Using now() for the start instead.");
        		
        		p.setValidFrom(LocalDateTime.now());
        		p.setValidFromInherited(false);
        	}
        	
            p.setValidFrom(domain.getValidfrom());
            p.setValidFromInherited(true);
        }

        // Determine validTo date if (not) given by the user
        if (validTo != null) {
            // End date of validity period is given
            if (domain.getEnforceenddatevalidity()) {
                // Ensure that the given end date isn't after the end date of the domain
                p.setValidTo((validTo.before(Timestamp.valueOf(domain.getValidto()))) ? validTo.toLocalDateTime() : domain.getValidto());
                p.setValidToInherited(!validTo.before(Timestamp.valueOf(domain.getValidto())));
            } else {
                p.setValidTo(validTo.toLocalDateTime());
                p.setValidToInherited(false);
            }
        } else if (validTo == null && validityTime != null) {
            // A validity period was given
            if (domain.getEnforceenddatevalidity()) {
                // Ensure that the given validity period ends before the end date of the domain
                p.setValidTo((Utility.plusValidityTime(p.getValidFrom(), validityTime).isBefore(domain.getValidto())) ? Utility.plusValidityTime(p.getValidFrom(), validityTime) : domain.getValidto());
                p.setValidToInherited(!Utility.plusValidityTime(p.getValidFrom(), validityTime).isBefore(domain.getValidto()));
            } else {
                p.setValidTo(Utility.plusValidityTime(p.getValidFrom(), validityTime));
                p.setValidToInherited(false);
            }
        } else {
            // Nothing was given: use date from domain
            p.setValidTo(domain.getValidto());
            p.setValidToInherited(true);
        }

        // Pseudonymize the identifier and store it in the object
        String pseudonym = null;
        if (psn != null && !psn.isBlank()) {
            // A pseudonym was already given --> store it instead of creating a new one if it's in the correct format
            if (omitPrefix != null && omitPrefix) {
                pseudonym = psn.trim();
            } else {
                pseudonym = psn.trim().startsWith(domain.getPrefix()) ? psn.trim() : domain.getPrefix() + psn.trim();
            }
        } else {
        	// Generate a new pseudonym
            pseudonym = pseudonymize(identifier, idType, domain, omitPrefix);
        }
        
        if (pseudonym == null) {
            // Pseudonymization failed. Return a 500-INTERNAL_SERVER_ERROR.
            log.error("Pseudonymization failed for identifier \"" + identifier + "\" and idType \"" + idType + "\".");
            return responseService.internalServerError(responseContentType);
        }
        
        p.setPsn(pseudonym);
        
        // Insert the pseudonym into the database
        String result = pseudonymDBAccessService.createPseudonyms(List.of(p), domain.getId(), domain.getMultiplepsnallowed()).getFirst();
        
        // If a random algorithm is used, check if we generated a duplicate. If so, retry.
        if (domain.getAlgorithm().toUpperCase().startsWith("RANDOM")) {
	        // Retry DEFAULT_NUMBER_OF_RETRIES - 1 times
        	for (int i = 1; i < Pseudonymizer.DEFAULT_NUMBER_OF_RETRIES; i++) {
	        	// Check if its actually a duplicate
        		if (result.equals(PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM)) {
					// Check the domain's filling rate
        			checkDomainFillingRate(domain);
	        		
	        		// Retry
	        		p.setPsn(pseudonymize(identifier, idType, domain, omitPrefix));
	        		result = pseudonymDBAccessService.createPseudonyms(List.of(p), domain.getId(), domain.getMultiplepsnallowed()).getFirst();
				} else {
					// Not a duplicate
					break;
				}
			}
        	
        	// Re-check if it's still a duplicate
        	if (result.equals(PseudonymDBAccessService.INSERTION_DUPLICATE_PSEUDONYM)) {
        		log.warn("Couldn't generate a new pseudonym due to collisions due to too many pseudonyms being already in the database.");
            	return responseService.insufficientStorage(responseContentType);
        	}
    	}

        // Evaluate the result
        if (result.equals(PseudonymDBAccessService.INSERTION_SUCCESS)) {
            // Success. Return a status code 201-CREATED and the pseudonym as payload.
            // Determine whether or not a reduced standard view or a complete view is requested
            if (!authorizationService.hasDomainPermission(domainName, "complete-view")) {
                p.toReducedStandardView();
            }

            log.debug("Successfully inserted a new pseudonym (" + pseudonym + ").");
            return responseService.created(responseContentType, p);
        } else if (result.equals(PseudonymDBAccessService.INSERTION_DUPLICATE_IDENTIFIER)) {
            // Nothing added since the entry is a duplicate. Return an 200-OK status.
        	List<PseudonymDTO> ps = pseudonymDBAccessService.getPseudonym(domainName, p.getIdentifierItem(), psn);
            if (ps == null || ps.isEmpty()) {
            	// Could not find the pseudonym object that was just created
            	log.error("Insertion of a new pseudonym failed.");
            	return responseService.unprocessableEntity(responseContentType);
            }
            
            // Determine whether or not a reduced standard view or a complete view is requested
            PseudonymDTO psnDTO = ps.getFirst();
            if (!authorizationService.hasDomainPermission(domainName, "complete-view")) {
                psnDTO.toReducedStandardView();
            }

            log.debug("The pseudonym requested to be inserted was skipped because it is already in the database.");
            return responseService.ok(responseContentType, psnDTO);
        } else {
            // Nothing added. Return an error 422-UNPROCESSABLE_ENTITY.
            log.error("Insertion of a new pseudonym failed.");
            return responseService.unprocessableEntity(responseContentType);
        }
    }

    /**
     * This method deletes all pseudonyms in the given domain.
     *
     * @param domainName (required) the name of the domain the pseudonyms are in
     * @param responseContentType (optional) the response content type
     * @return	<li>a <b>204-NO_CONTENT</b> status when the deletion was
     * 				successful</li>
     * 			<li>a <b>206-PARTIAL_CONTENT</b> status and a list of deletion 
     * 				results matching the input list</li>
     * 			<li>a <b>404-NOT_FOUND</b> when the given domain wasn't
     * 				found</li>
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the
     * 				pseudonyms could not be deleted</li>
     */
    @DeleteMapping("/domains/{domainName}/pseudonyms/batch")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'pseudonym:delete-batch')")
    @Audit
    public ResponseEntity<?> deletePseudonymBatch(@PathVariable("domainName") String domainName,
                                               	  @RequestHeader(name = "accept", required = false) String responseContentType) {
        // Retrieve the domain the pseudonyms belong to
        Domain d = domainDBAccessService.getDomainByName(domainName);
        if (d == null) {
            // The domain wasn't found; return a 404-NOT_FOUND
            log.debug("The domain where the pseudonyms should be searched couldn't be found.");
            return responseService.notFound(responseContentType);
        }

        // Retrieve the pseudonyms
        List<PseudonymDTO> pseudonyms = domainDBAccessService.getAllPseudonymsInDomain(domainName);

        // Check if anything was found
        if (pseudonyms == null) {
            // Something went wrong, return a 422-UNPROCESSABLE_ENTITY
            log.error("Retrieving the pseudonyms for the deletion in the domain \"" + domainName + "\" failed.");
            return responseService.unprocessableEntity(responseContentType);
        } else if (pseudonyms.size() == 0) {
            // Nothing was found. Return a 204-NO_CONTENT status.
            log.debug("No pseudonyms were found. Nothing to delete");
            return responseService.noContent(responseContentType);
        }

        // Perform deletion
        List<Boolean> results = pseudonymDBAccessService.deletePseudonyms(pseudonyms, d.getId());
        if (results != null && results.contains(true) && !results.contains(false)) {
            // Successfully deleted the batch, return a 204-NO_CONTENT.
            log.debug("Successfully deleted all pseudonyms from domain \"" + domainName + "\".");
            return responseService.noContent(responseContentType);
        } else if (results != null && results.contains(true) && results.contains(false)) {
        	// Partially successful deletion
        	log.debug("Partially deleted the batch of pseudonyms.");
        	return responseService.partialContent(responseContentType, results);
    	} else {
            // Deletion was unsuccessful, return a 422-UNPROCESSABLE_ENTITY
            log.error("Deletion of the pseudonyms in domain \"" + domainName + "\" was unsuccessful.");
            return responseService.unprocessableEntity(responseContentType);
        }
    }

    /**
     * This method deletes a pseudonym. It must be given an identifier and its
     * type or a pseudonym.
     *
     * @param domainName (required) the name of the domain the pseudonym is in
     * @param identifier (optional) the identifier to search for
     * @param idType (optional) the type of the identifier
     * @param psn (optional) the pseudonym to search for
     * @param responseContentType (optional) the response content type
     * @return	<li>a <b>204-NO_CONTENT</b> status when the deletion was
     * 				successful</li>
     * 			<li>a <b>400-BAD_REQUEST</b> when neither an identifier, nor
     * 				a pseudonym were given</li>
     * 			<li>a <b>404-NOT_FOUND</b> when no pseudonym was found for the
     * 				given identifier/pseudonym</li>
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the
     * 				pseudonym could not be deleted</li>
     */
    @DeleteMapping("/domains/{domainName}/pseudonyms")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'pseudonym:delete')")
    @Audit
    public ResponseEntity<?> deletePseudonym(@PathVariable("domainName") String domainName,
                                          	 @RequestParam(name = "id", required = false) String identifier,
                                          	 @RequestParam(name = "idType", required = false) String idType,
                                          	 @RequestParam(name = "psn", required = false) String psn,
                                          	 @RequestHeader(name = "accept", required = false) String responseContentType) {
    	// Retrieve domain
        Domain d = domainDBAccessService.getDomainByName(domainName);
        if (d == null) {
        	log.debug("The domain with the name \"" + domainName + "\" was not found.");
        	return responseService.notFound(responseContentType);
        }
    	
    	// Check if any pseudonym-identifying information was given and build the delete-object
        IdentifierItem idItem = IdentifierItem.builder().identifier(identifier).idType(idType).build();
        PseudonymDTO p;
        
        if (idItem.isNotNullNorEmpty() && Assertion.isNotNullOrEmpty(psn)) {
            // All necessary info was given
        	p = PseudonymDTO.builder().identifierItem(idItem).psn(psn).build();
        } else if (Assertion.assertNotNullAll(identifier, idType) && psn == null) {
            // Delete through identifier
            List<PseudonymDTO> pList = pseudonymDBAccessService.getPseudonym(domainName, idItem, null);

            // Check if a pseudonym with the given identifier exists
            if (pList == null || pList.size() == 0) {
                log.debug("There was no pseudonym found for the given identifier and idType.");
                return responseService.notFound(responseContentType);
            } else if (pList.size() > 1) {
            	log.debug("The deletion for the given identifier and idType would have affected more than one pseudonym which is not allowed.");
            	return responseService.unprocessableEntity(responseContentType);
            }

            // Build delete-object
            p = PseudonymDTO.builder().identifierItem(idItem).psn(pList.get(0).getPsn()).build();
        } else if (Assertion.assertNullAll(identifier, idType) && psn != null) {
            // Delete through pseudonym
            List<PseudonymDTO> pList = pseudonymDBAccessService.getPseudonym(domainName, null, psn);

            // Check if a pseudonym with the given psn exists
            if (pList == null || pList.size() == 0) {
                log.debug("There was no pseudonym found for the given psn.");
                return responseService.notFound(responseContentType);
            }

            // Build delete-object
            p = PseudonymDTO.builder().identifierItem(pList.get(0).getIdentifierItem()).psn(psn).build();
        } else {
            // Invalid configuration of parameters encountered, return an error 400-BAD_REQUEST
            log.debug("Invalid configuration of parameters. At least an id and idType or the psn is needed. Ideally all three.");
            return responseService.badRequest(responseContentType);
        }
        
        // Perform deletion and evaluate success
        if (pseudonymDBAccessService.deletePseudonyms(List.of(p), d.getId()).getFirst()) {
            // Successfully deleted a pseudonym, return a 204-NO_CONTENT
            log.debug("Successfully deleted the pseudonym.");
            return responseService.noContent(responseContentType);
        } else {
            // Deletion was unsuccessful, return a 422 unprocessable entity
            log.debug("The pseudonym could not be deleted.");
            return responseService.unprocessableEntity(responseContentType);
        }
    }
    
    /**
     * Method to search and link pseudonyms along the pseudonym-chain in the tree.
     * 
     * @param sourceDomain the starting domain for the search
     * @param targetDomain the target domain for the search
     * @param sourceIdentifier (optional) the identifier of the pseudonym to start the search from
     * @param sourceIdType (optional) the idType of the pseudonym to start the search from
     * @param sourcePsn (optional) the pseudonym of the pseudonym to start the search from
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>200-OK</b> status and the <b>list of linked pseudonyms</b> 
     * 				when linkable pseudonyms were found</li>
     * 			<li>a <b>403-FORBIDDEN</b> when the requester does not have 
     * 				the rights to access the source and/or target domain</li>
     * 			<li>a <b>404-NOT_FOUND</b> when no linkable pseudonyms were found for the
     * 				given parameters</li>
     */
    @GetMapping(value = "/domains/linked-pseudonyms", params = {"sourceDomain", "targetDomain"})
    @PreAuthorize("isAuthenticated()"
    		+ " and @auth.hasDomainPermission(#root, #sourceDomain, 'pseudonym:read')"
    		+ " and @auth.hasDomainPermission(#root, #sourceDomain, 'pseudonym:link')"
    		+ " and @auth.hasDomainPermission(#root, #targetDomain, 'pseudonym:read')"
    		+ " and @auth.hasDomainPermission(#root, #targetDomain, 'pseudonym:link')")
    @Audit
    public ResponseEntity<?> getLinkedPseudonyms(@RequestParam(name = "sourceDomain", required = true) String sourceDomain,
    										  	 @RequestParam(name = "targetDomain", required = true) String targetDomain,
    										  	 @RequestParam(name = "sourceIdentifier", required = false) String sourceIdentifier,
    										  	 @RequestParam(name = "sourceIdType", required = false) String sourceIdType,
    										  	 @RequestParam(name = "sourcePsn", required = false) String sourcePsn,
    										  	 @RequestHeader(name = "accept", required = false) String responseContentType) {

        // Try to find any pseudonyms that are connected in a pseudonym chain in the given domains
    	List<Pair<PseudonymDTO, PseudonymDTO>> pseudonyms = domainDBAccessService.getLinkedPseudonyms(sourceDomain,
                Assertion.isNotNullOrEmpty(sourceIdentifier) ? sourceIdentifier : null,
                Assertion.isNotNullOrEmpty(sourceIdType) ? sourceIdType : null,
                Assertion.isNotNullOrEmpty(sourcePsn) ? sourcePsn : null,
                targetDomain);
        
        // Check if anything was found
        if (pseudonyms == null || pseudonyms.size() == 0) {
        	log.debug("No linkable pseudonyms were found.");
        	return responseService.notFound(responseContentType);
        }

        // Convert the pair list
        boolean completeView = authorizationService.hasDomainPermission(sourceDomain, "complete-view")
        		&& authorizationService.hasDomainPermission(targetDomain, "complete-view");

        List<List<PseudonymDTO>> listOfPseudonymPairs = new ArrayList<>();
        for (Pair<PseudonymDTO, PseudonymDTO> pair : pseudonyms) {
            List<PseudonymDTO> pseudonymPair = new ArrayList<>();
            
            if (!completeView) {
            	pseudonymPair.add(pair.first().toReducedStandardView());
            	pseudonymPair.add(pair.second().toReducedStandardView());
            } else {
            	pseudonymPair.add(pair.first());
            	pseudonymPair.add(pair.second());
            }

            listOfPseudonymPairs.add(pseudonymPair);
        }

        log.debug("Successfully linked pseudonyms.");
        return responseService.ok(responseContentType, listOfPseudonymPairs);
    }

    /**
     * This method retrieves all pseudonyms stored in the given domain.
     *
     * @param domainName (required) the name of the domain the pseudonyms are in
     * @param responseContentType (optional) the response content type
     * @return	<li>a <b>200-OK</b> status and the <b>list of pseudonyms</b>
     * 				when successful</li>
     * 			<li>a <b>404-NOT_FOUND</b> when the given domain wasn't
     * 				found</li>
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the
     * 				pseudonyms could not be retrieved</li>
     */
    @GetMapping("/domains/{domainName}/pseudonyms/batch")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'pseudonym:read-batch')")
    @Audit
    public ResponseEntity<?> getPseudonymBatch(@PathVariable("domainName") String domainName,
                                               @RequestHeader(name = "accept", required = false) String responseContentType) {
        // Check that the batch size isn't too big.
        Integer count = domainDBAccessService.getAmountOfPseudonymsInDomain(domainName);
        if (count == null) {
            // An error during accessing the domain occurred. Maybe the domain is not in the database.
            log.error("Couldn't count the number of pseudonyms in the domain \"" + domainName + "\".");
            return responseService.unprocessableEntity(responseContentType);
        } else if (count == DomainDBAccessService.DOMAIN_NOT_FOUND) {
            // The domain wasn't found. Return an error 404-NOT_FOUND
            log.error("Couldn't find the domain \"" + domainName + "\".");
            return responseService.notFound(responseContentType);
        } else if (count > DEFAULT_PSEUDONYM_BATCH_LENGTH) {
            // The batch size exceeded the limit. Return an error 422-UNPROCESSABLE_ENTITY.
            log.error("The domain contains too many entries. The maximum allowed batch size is: " + DEFAULT_PSEUDONYM_BATCH_LENGTH);
            return responseService.unprocessableEntity(responseContentType);
        }

        // Retrieve the domain the pseudonyms belong to
        Domain d = domainDBAccessService.getDomainByName(domainName);
        if (d == null) {
            // The domain wasn't found; return a 404-NOT_FOUND
            log.debug("The domain where the pseudonyms should be searched couldn't be found.");
            return responseService.notFound(responseContentType);
        }

        // Retrieve the pseudonyms
        List<PseudonymDTO> pseudonyms = domainDBAccessService.getAllPseudonymsInDomain(domainName);

        // Check if anything was found
        if (pseudonyms == null) {
            // Something went wrong
            log.error("Retrieving the pseudonyms in the domain \"" + domainName + "\" failed.");
            return responseService.unprocessableEntity(responseContentType);
        } else if (pseudonyms.size() == 0) {
            // Nothing was found. Return a 200-OK status.
            log.debug("No pseudonyms were found.");
            return responseService.ok(responseContentType, Collections.emptyList());
        }

        // Transform result into the desired output format
        List<PseudonymDTO> resultAsJson = new ArrayList<>();
        List<String> resultAsString = new ArrayList<>();
        for (PseudonymDTO p : pseudonyms) {
        	// Determine whether or not a reduced standard view or a complete view is requested
            if (!authorizationService.hasDomainPermission(domainName, "complete-view")) {
                p = p.toReducedStandardView();
            }

            // Process the DTO depending on the response`s media type
            if (responseContentType != null && responseContentType.equals(MediaType.TEXT_PLAIN_VALUE)) {
                resultAsString.add(p.toRepresentationString());
            } else {
                resultAsJson.add(p);
            }
        }

        // Return result
        log.debug("Successfully retrieved all pseudonyms from domain \"" + domainName + "\".");

        if (responseContentType != null && responseContentType.equals(MediaType.TEXT_PLAIN_VALUE)) {
            return responseService.ok(responseContentType, resultAsString);
        } else {
            return responseService.ok(responseContentType, resultAsJson);
        }
    }

    /**
     * This method retrieves a pseudonym through its identifier (id &amp; idType).
     *
     * @param domainName (required) the name of the domain the pseudonym is in
     * @param identifier (required) the identifier to search for
     * @param idType (required) the type of the identifier
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>200-OK</b> status and the <b>pseudonym</b> when it was
     * 				found</li>
     * 			<li>a <b>404-NOT_FOUND</b> when no pseudonym was found for the
     * 				given identifier and idType</li>
     */
    @GetMapping(value = "/domains/{domainName}/pseudonyms", params = {"id", "idType"})
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'pseudonym:read')")
    @Audit
    public ResponseEntity<?> getPseudonymByIdentifier(@PathVariable("domainName") String domainName,
                                                   	  @RequestParam(name = "id", required = true) String identifier,
                                                   	  @RequestParam(name = "idType", required = true) String idType,
                                                   	  @RequestHeader(name = "accept", required = false) String responseContentType) {
        // Retrieve the domain the pseudonym belongs to
        Domain d = domainDBAccessService.getDomainByName(domainName);

        if (d == null) {
            // The domain wasn't found; return a 404-NOT_FOUND
            log.debug("The domain where the pseudonym should be searched couldn't be found.");
            return responseService.notFound(responseContentType);
        }

        // Retrieve the pseudonym
        IdentifierItem idItem = IdentifierItem.builder().identifier(identifier).idType(idType).build();
        List<PseudonymDTO> p = pseudonymDBAccessService.getPseudonym(domainName, idItem, null);
        if (p != null && !p.isEmpty()) {
            // Successfully retrieved pseudonym(s), return it to the user as well as a 200-OK
        	PseudonymDTO pseudonymDTO = null;
		
	        // Determine whether or not a reduced standard view or a complete view is requested
	        if (!authorizationService.hasDomainPermission(domainName, "complete-view")) {
	        	pseudonymDTO = p.getFirst().toReducedStandardView();
	        } else {
	            pseudonymDTO = p.getFirst();
	        }

            log.debug("Successfully retrieved the requested pseudonym.");
            return responseService.ok(responseContentType, pseudonymDTO);
        } else {
            // Nothing found, return a 404-NOT_FOUND
            return responseService.notFound(responseContentType);
        }
    }

    /**
     * This method retrieves a pseudonym through its psn.
     *
     * @param domainName (required) the name of the domain the pseudonym is in
     * @param psn (required) the pseudonym to search for
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>200-OK</b> status and the <b>pseudonym</b> when it was
     * 				found</li>
     * 			<li>a <b>404-NOT_FOUND</b> when no pseudonym was found for the
     * 				given pseudonym</li>
     */
    @GetMapping(value = "/domains/{domainName}/pseudonyms", params = "psn")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'pseudonym:read')")
    @Audit
    public ResponseEntity<?> getPseudonymByPsn(@PathVariable("domainName") String domainName,
                                               @RequestParam(name = "psn", required = true) String psn,
                                               @RequestHeader(name = "accept", required = false) String responseContentType) {
        // Retrieve the domain the pseudonym belongs to
        Domain d = domainDBAccessService.getDomainByName(domainName);

        if (d == null) {
            // The domain wasn't found; return a 404-NOT_FOUND
            log.debug("The domain where the pseudonym should be searched couldn't be found.");
            return responseService.notFound(responseContentType);
        }

        // Retrieve the pseudonym
        List<PseudonymDTO> p = pseudonymDBAccessService.getPseudonym(domainName, null, psn);
        if (p != null && !p.isEmpty()) {
            // Successfully retrieved pseudonym(s), return it to the user as well as a 200-OK
        	PseudonymDTO pseudonymDTO = null;
		
	        // Determine whether or not a reduced standard view or a complete view is requested
	        if (!authorizationService.hasDomainPermission(domainName, "complete-view")) {
	            pseudonymDTO = p.getFirst().toReducedStandardView();
	        } else {
	            pseudonymDTO = p.getFirst();
	        }

            log.debug("Successfully retrieved the requested pseudonym.");
            return responseService.ok(responseContentType, pseudonymDTO);
        } else {
            // Nothing found, return a 404-NOT_FOUND
            return responseService.notFound(responseContentType);
        }
    }
    
    /**
     * Method that encapsulates the pseudonym generation.
     * 
     * @param identifier the identifier that should be pseudonymized 
     * @param idType the identifier's type
     * @param domain the domain in which the identifier should be pseudonymized
     * @param omitPrefix determines whether or not the prefix should be added to the pseudonym
     * @return the generated pseudonym
     */
    public static String pseudonymize(String identifier, String idType, Domain domain, Boolean omitPrefix) {
        // Generate a new pseudonym
        String prefix = (omitPrefix != null && omitPrefix) ? "" : domain.getPrefix();
        
        Pseudonymizer pseudonymizer = new PseudonymizationFactory().getPseudonymizer(domain);
        String pseudonym = pseudonymizer.pseudonymize(identifier + idType + domain.getSalt(), prefix);
        return domain.getAddcheckdigit() ? pseudonymizer.addCheckDigit(pseudonym, domain.getLengthincludescheckdigit(), domain.getName(), prefix) : pseudonym;
    }

    /**
     * This method updates a list of pseudonyms in a batch.
     * Every list entry must contain all necessary information (identifier, idType,
     * pseudonym, validFrom, validFromInherited, validTo, validToInherited).
     *
     * @param domainName (required) the name of the domain the pseudonyms are in
     * @param pseudonymUpdateList (required) the list of necessary information, formatted as a JSON to match the pseudonymDto
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>200-OK</b> status on success</li>
     * 			<li>a <b>404-NOT_FOUND</b> status when the domain couldn't
     * 				be found</li>
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> when the update of the
     *	 			pseudonym failed</li>
     */
    @PutMapping("/domains/{domainName}/pseudonyms/batch")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'pseudonym:update-batch')")
    @Audit
    public ResponseEntity<?> updatePseudonymBatch(@PathVariable("domainName") String domainName,
                                               	  @RequestBody List<PseudonymUpdateDTO> pseudonymUpdateList,
                                               	  @RequestHeader(name = "accept", required = false) String responseContentType) {
        int ignored = 0;
        int updated = 0;
        List<PseudonymUpdateDTO> updateablePseudonyms = new ArrayList<>();

        // Retrieve the domain the pseudonyms belong to
        Domain d = domainDBAccessService.getDomainByName(domainName);

        if (d == null) {
            // The domain wasn't found; return a 404-NOT_FOUND
            log.debug("The domain to which the batch update is scoped to couldn't be found.");
            return responseService.notFound(responseContentType);
        }

        // Iterate over the pseudonym DTOs to create a list of updatable pseudonym-objects
        for (int i = 0; i < pseudonymUpdateList.size(); i++) {
        	PseudonymUpdateDTO p = pseudonymUpdateList.get(i);
        	p.setOldDomain(d);

            // Check if any of the identifying attributes of a pseudonym is missing
        	if (!p.hasIdentifyingInformation()) {
        		log.debug("The pseudonym with batch-number " + (i + 1) + " is missing its identifying attributes. This part of the batch request is ignored.");
                ignored++;

                // Early "break"
                continue;
        	} else {
        		// Everything that's needed is there, add the pseudonym object to a list of updatable objects
        		updateablePseudonyms.add(p);
        	}
        }

        // Update pseudonyms
        List<Boolean> updateSuccess = pseudonymDBAccessService.updatePseudonyms(updateablePseudonyms);
        if (updateSuccess != null && !updateSuccess.isEmpty()) {
        	// Success. Return a status code 200-OK and a list of the updated pseudonyms.
        	List<PseudonymDTO> updatedPseudonyms = new ArrayList<>();
        	for (int i = 0; i < updateablePseudonyms.size(); i++) {
        		if (updateSuccess.get(i) != null && updateSuccess.get(i)) {
        			// Decide on which identifier item to use: the new one or the old one 
        			IdentifierItem idItem = updateablePseudonyms.get(i).getNewIdentifierItem() != null ? updateablePseudonyms.get(i).getNewIdentifierItem() : updateablePseudonyms.get(i).getOldIdentifierItem();
        			String psn = updateablePseudonyms.get(i).getNewPsn() != null ? updateablePseudonyms.get(i).getNewPsn() : updateablePseudonyms.get(i).getOldPsn();
        			
        			PseudonymDTO updatedPseudonym = pseudonymDBAccessService.getPseudonym(domainName, idItem, psn).getFirst();
        			updatedPseudonyms.add(authorizationService.hasDomainPermission(domainName, "complete-view") ? updatedPseudonym : updatedPseudonym.toReducedStandardView());
        			updated++;
        		} else if (updateSuccess.get(i) != null && !updateSuccess.get(i)) {
        			// An update failed
        			ignored++;
        		}
        	}
        	
            log.debug("Successfully updated " + updated + " out of " + pseudonymUpdateList.size() + " pseudonym(s). " + ignored + (ignored == 1 ? " was" : " were") + " ignored.");
            return responseService.ok(responseContentType, updatedPseudonyms);
        } else {
            // Update failed. Return an error 422-UNPROCESSABLE_ENTITY.
            log.error("The requested update of the batch of pseudonyms failed.");
            return responseService.unprocessableEntity(responseContentType);
        }
    }

    /**
     * This method updates a pseudonym identified by either its id and idType or its psn.
     *
     * @param oldDomainName (required) the name of the domain the pseudonym should be in
     * @param pseudonymUpdateDTO (required) the update object containing the identifying and the new data
     * @param regeneratePseudonym (optional) a flag to keep or regenerate the psn when the identifierItem or the domain changed
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>200-OK</b> status and the updated pseudonym on success</li>
     * 			<li>a <b>400-BAD_REQUEST</b> when neither an identifier, nor
     * 				a pseudonym were given</li>
     * 			<li>a <b>404-NOT_FOUND</b> status when either the domain,
     * 				the pseudonym that should be updated, or the new domain
     * 				couldn't be found
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> when the update of the
     * 				pseudonym failed</li>
     */
    @PutMapping(value = "/domains/{domainName}/pseudonyms/complete")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #oldDomainName, 'pseudonym:update-complete')")
    @Audit
    public ResponseEntity<?> updatePseudonymComplete(@PathVariable("domainName") String oldDomainName,
                                                  	 @RequestBody PseudonymUpdateDTO pseudonymUpdateDTO,
                                                  	 @RequestParam(name = "regeneratePseudonym", required = false) Boolean regeneratePseudonym,
                                                  	 @RequestHeader(name = "accept", required = false) String responseContentType) {
        String newIdentifier = pseudonymUpdateDTO.getNewIdentifierItem() == null ? null : pseudonymUpdateDTO.getNewIdentifierItem().getIdentifier();
        String newIdType = pseudonymUpdateDTO.getNewIdentifierItem() == null ? null : pseudonymUpdateDTO.getNewIdentifierItem().getIdType();
        String newPsn = pseudonymUpdateDTO.getNewPsn();
        Timestamp validFrom = pseudonymUpdateDTO.getValidFrom() != null ? Timestamp.valueOf(pseudonymUpdateDTO.getValidFrom()) : null;
        Timestamp validTo = pseudonymUpdateDTO.getValidTo() != null ? Timestamp.valueOf(pseudonymUpdateDTO.getValidTo()) : null;
        String validityTime = pseudonymUpdateDTO.getValidityTime();
        String newDomainName = pseudonymUpdateDTO.getNewDomainName();
        regeneratePseudonym = regeneratePseudonym == null ? DEFAULT_REGENERATE_PSEUDONYM : regeneratePseudonym;
        
        if (Assertion.assertNullAll(newIdentifier, newIdType, newPsn, validFrom, validTo, validityTime, newDomainName)) {
            // An empty object was passed, so there is nothing to create.
            log.debug("The pseudonym DTO passed by the user was empty. Nothing to update.");
            return responseService.unprocessableEntity(responseContentType);
        }

        // Ensure that we can somehow identify the object that is to be updated (either have (id and idType) or a psn)
        if ((pseudonymUpdateDTO.getOldIdentifierItem() == null || pseudonymUpdateDTO.getOldIdentifierItem().getIdentifier() == null || pseudonymUpdateDTO.getOldIdentifierItem().getIdType() == null) && pseudonymUpdateDTO.getOldPsn() == null) {
        	log.debug("For identifying the pseudonym that should be updated, either (identifier and idType) or the psn (or both) is needed.");
            return responseService.unprocessableEntity(responseContentType);
        }

        // Retrieve the domain the pseudonym belongs to
        Domain oldDomain = domainDBAccessService.getDomainByName(oldDomainName);

        // Try to retrieve the updated domain (is null, when no new domain was given)
        Domain newDomain = domainDBAccessService.getDomainByName(newDomainName);

        // Retrieve the old pseudonym
        List<PseudonymDTO> oldPseudonymList = pseudonymDBAccessService.getPseudonym(oldDomainName, pseudonymUpdateDTO.getOldIdentifierItem(), pseudonymUpdateDTO.getOldPsn());
        if (oldPseudonymList == null || oldPseudonymList.size() == 0) {
        	// Couldn't find the pseudonym that should be updated. Return a 404-NOT_FOUND
            log.debug("The pseudonym that should be updated couldn't be found.");
            return responseService.notFound(responseContentType);
        } else if (oldPseudonymList.size() > 1) {
        	// Found too many pseudonyms.Return a 422-UNPROCESSABLE_ENTITY
        	log.debug("The update for the given identifier and idType would have affected more than one pseudonym. "
        			+ "Updating multiple pseudonyms at once is only permitted via the batch-endpoint.");
        	return responseService.unprocessableEntity(responseContentType);
        }

        PseudonymDTO oldPseudonym = oldPseudonymList.get(0);

        // Check if the retrieved values are null
        if (oldDomain == null) {
            // Couldn't find the domain. Return a 404-NOT_FOUND
            log.debug("The provided domain (\"" + oldDomainName + "\") couldn't be found.");
            return responseService.notFound(responseContentType);
        } else if (oldPseudonym == null) {
        	// Couldn't find the pseudonym that should be updated. Return a 404-NOT_FOUND
            log.debug("The pseudonym that should be updated couldn't be found.");
            return responseService.notFound(responseContentType);
        } else if (newDomainName != null && newDomain == null) {
            // A new domain name was provided but it couldn't be found. Return a 404-NOT_FOUND
            log.debug("The new domain for the pseudonym (\"" + newDomainName + "\") couldn't be found.");
            return responseService.notFound(responseContentType);
        }

        // Check if the requester has the permission for the newDomain if given
        if (newDomain != null && !authorizationService.hasGlobalPermission(newDomain.getName())) {
            log.error("The user requested to change the domain of a pseudonym to a domain the user has no rights for.");
            return responseService.forbidden(responseContentType);
        }

        // Determine which domain to use
        Domain d = (newDomain != null) ? newDomain : oldDomain;
        
        // Determine which identifierItem to use
        String id, idT;
        if (newIdentifier == null || newIdType == null) {
        	id = oldPseudonym.getIdentifierItem().getIdentifier();
        	idT = oldPseudonym.getIdentifierItem().getIdType();
        } else {
        	id = newIdentifier;
        	idT = newIdType;
        }
        IdentifierItem idItem = IdentifierItem.builder().identifier(id).idType(idT).build();
        
        // Determine which psn to use
        String p;
        if ((!idItem.equals(oldPseudonym.getIdentifierItem()) || !d.getName().equals(oldDomain.getName())) && regeneratePseudonym) {
        	// New identifier and idType or new domain --> regenerate a pseudonym
        	p = pseudonymize(idItem.getIdentifier(), idItem.getIdType(), d, false);
        } else {
        	// Keep the old pseudonym
        	p = oldPseudonym.getPsn();
        }

        // Create new pseudonym
        PseudonymUpdateDTO updateDTO = new PseudonymUpdateDTO();
        updateDTO.setOldIdentifierItem(oldPseudonym.getIdentifierItem());
        updateDTO.setOldPsn(oldPseudonym.getPsn());
        updateDTO.setOldDomain(oldDomain);
        updateDTO.setNewIdentifierItem(idItem);
        updateDTO.setNewPsn(p);
        updateDTO.setNewDomain(d);
        updateDTO.setNewDomainName(d.getName());

        // Determine the validity of the validFrom date if given by the user
        if (validFrom != null) {
            if (d.getEnforcestartdatevalidity()) {
                // Ensure that the given start date isn't before the start date of the domain
            	updateDTO.setValidFrom(validFrom.after(Timestamp.valueOf(d.getValidfrom())) ? validFrom.toLocalDateTime() : d.getValidfrom());
            	updateDTO.setValidFromInherited(!validFrom.after(Timestamp.valueOf(d.getValidfrom())));
            } else {
            	updateDTO.setValidFrom(validFrom.toLocalDateTime());
            	updateDTO.setValidFromInherited(false);
            }
        } else {
            // Set the old value in the new pseudonym again to easier determine/calculate a potential validTo value
        	updateDTO.setValidFrom(oldPseudonym.getValidFrom());
        }

        // Determine the validity of the validTo date if given by the user
        if (validTo != null) {
            // End date of validity period is given
            if (d.getEnforceenddatevalidity()) {
                // Ensure that the given end date isn't after the end date of the domain
            	updateDTO.setValidTo(validTo.before(Timestamp.valueOf(d.getValidto())) ? validTo.toLocalDateTime() : d.getValidto());
            	updateDTO.setValidToInherited(!validTo.before(Timestamp.valueOf(d.getValidto())));
            } else {
            	updateDTO.setValidTo(validTo.toLocalDateTime());
                updateDTO.setValidToInherited(false);
            }
        } else if (validTo == null && validityTime != null) {
            // A validity period was given
            if (d.getEnforceenddatevalidity()) {
                // Ensure that the given validity period ends before the end date of the domain
                updateDTO.setValidTo(Utility.plusValidityTime(updateDTO.getValidFrom(), validityTime).isBefore(d.getValidto()) ? Utility.plusValidityTime(updateDTO.getValidFrom(), validityTime) : d.getValidto());
                updateDTO.setValidToInherited(!Utility.plusValidityTime(updateDTO.getValidFrom(), validityTime).isBefore(d.getValidto()));
            } else {
                updateDTO.setValidTo(Utility.plusValidityTime(updateDTO.getValidFrom(), validityTime));
                updateDTO.setValidToInherited(false);
            }
        }

        // Update pseudonym
        if (pseudonymDBAccessService.updatePseudonyms(List.of(updateDTO)).getFirst()) {
            // Update successful. Retrieve the updated pseudonym to show it to the user.
            PseudonymDTO updatedPseudonym = pseudonymDBAccessService.getPseudonym(updateDTO.getNewDomainName(), updateDTO.getNewIdentifierItem(), updateDTO.getNewPsn()).get(0);

            // Determine whether or not a reduced standard view or a complete view is requested
            if (!authorizationService.hasDomainPermission(updateDTO.getNewDomainName(), "complete-view")) {
            	updatedPseudonym.toReducedStandardView();
            }

            // Success. Return a status code 200 and the pseudonym as payload.
            log.debug("Successfully updated a pseudonym.");
            return responseService.ok(responseContentType, updatedPseudonym);
        } else {
            // Update failed. Return an error 422-UNPROCESSABLE_ENTITY.
            log.error("The requested update of a pseudonym failed.");
            return responseService.unprocessableEntity(responseContentType);
        }
    }

    /**
     * This method updates a pseudonym with a reduced set of updatable attributes.
     * The pseudonym is identified by its id and idType.
     *
     * @param domainName (required) the name of the domain the pseudonym should be in
     * @param pseudonymDTO (required) the pseudonym object
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>200-OK</b> status and the <b>pseudonym</b> on
     * 				success</li>
     * 			<li>a <b>400-BAD_REQUEST</b> when neither an identifier, nor
     * 				a pseudonym were given</li>
     * 			<li>a <b>404-NOT_FOUND</b> status when either the domain or
     * 				the pseudonym that should be updated couldn't be found
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> when the update of the
     * 				pseudonym failed</li>
     */
    @PutMapping("/domains/{domainName}/pseudonyms")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'pseudonym:update')")
    @Audit
    public ResponseEntity<?> updatePseudonym(@PathVariable("domainName") String domainName,
                                          	 @RequestBody PseudonymUpdateDTO pseudonymDTO,
                                          	 @RequestHeader(name = "accept", required = false) String responseContentType) {
        Timestamp validFrom = pseudonymDTO.getValidFrom() != null ? Timestamp.valueOf(pseudonymDTO.getValidFrom()) : null;
        Timestamp validTo = pseudonymDTO.getValidTo() != null ? Timestamp.valueOf(pseudonymDTO.getValidTo()) : null;
        String validityTime = pseudonymDTO.getValidityTime();

        if (Assertion.assertNullAll(validFrom, validTo, validityTime)) {
            // An empty object was passed, so there is nothing to update.
            log.debug("The pseudonym DTO passed by the user was empty. Nothing to update.");
            return responseService.unprocessableEntity(responseContentType);
        }

        // Retrieve the domain the pseudonym belongs to
        Domain d = domainDBAccessService.getDomainByName(domainName);

        // Retrieve the old pseudonym
        List<PseudonymDTO> oldPseudonymList = pseudonymDBAccessService.getPseudonym(domainName, pseudonymDTO.getOldIdentifierItem(), pseudonymDTO.getOldPsn());
        if (oldPseudonymList == null || oldPseudonymList.size() == 0) {
        	// Couldn't find the pseudonym that should be updated. Return a 404-NOT_FOUND
            log.debug("The pseudonym that should be updated couldn't be found.");
            return responseService.notFound(responseContentType);
        } else if (oldPseudonymList.size() > 1) {
        	// Found too many pseudonyms, return a 422-UNPROCESSABLE_ENTITY
        	log.debug("The update for the given identifier and idType would have affected more than one pseudonym. "
        			+ "Updating multiple pseudonyms at once is only permitted via the batch-endpoint.");
        	return responseService.unprocessableEntity(responseContentType);
        }
        
		PseudonymDTO oldPseudonym = oldPseudonymList.get(0);

        // Check if the retrieved values are null
        if (d == null) {
            // Couldn't find the domain. Return a 404-NOT_FOUND
            log.debug("The provided domain (\"" + domainName + "\") couldn't be found.");
            return responseService.notFound(responseContentType);
        } else if (oldPseudonym == null) {
            // Couldn't find the pseudonym that should be updated. Return a 404-NOT_FOUND
            log.debug("The pseudonym that should be updated couldn't be found.");
            return responseService.notFound(responseContentType);
        }

        // Create new pseudonym
        PseudonymUpdateDTO updateDTO = new PseudonymUpdateDTO();
        updateDTO.setOldIdentifierItem(oldPseudonym.getIdentifierItem());
        updateDTO.setOldPsn(oldPseudonym.getPsn());
        updateDTO.setOldDomain(d);
        updateDTO.setNewIdentifierItem(oldPseudonym.getIdentifierItem());
        updateDTO.setNewPsn(oldPseudonym.getPsn());
        updateDTO.setNewDomain(d);
        updateDTO.setNewDomainName(d.getName());

        // Determine the validity of the validFrom date if given by the user
        if (validFrom != null) {
            if (d.getEnforcestartdatevalidity()) {
                // Ensure that the given start date isn't before the start date of the domain
                updateDTO.setValidFrom(validFrom.after(Timestamp.valueOf(d.getValidfrom())) ? validFrom.toLocalDateTime() : d.getValidfrom());
                updateDTO.setValidFromInherited(!validFrom.after(Timestamp.valueOf(d.getValidfrom())));
            } else {
                updateDTO.setValidFrom(validFrom.toLocalDateTime());
                updateDTO.setValidFromInherited(false);
            }
        } else {
            // Set the old value in the new pseudonym again to easier determine/calculate a potential validTo value
            updateDTO.setValidFrom(oldPseudonym.getValidFrom());
        }

        // Determine the validity of the validTo date if given by the user
        if (validTo != null) {
            // End date of validity period is given
        	
        	if (validityTime != null && validityTime != null) {
        		log.debug("A validity time period was given in addition to an end date. The end date is preferred so that the given validity time is ignored.");
        	}
        	
            if (d.getEnforceenddatevalidity()) {
                // Ensure that the given end date isn't after the end date of the domain
                updateDTO.setValidTo(validTo.before(Timestamp.valueOf(d.getValidto())) ? validTo.toLocalDateTime() : d.getValidto());
                updateDTO.setValidToInherited(!validTo.before(Timestamp.valueOf(d.getValidto())));
            } else {
                updateDTO.setValidTo(validTo.toLocalDateTime());
                updateDTO.setValidToInherited(false);
            }
        } else if (validTo == null && validityTime != null) {
            // A validity period was given
            if (d.getEnforceenddatevalidity()) {
                // Ensure that the given validity period ends before the end date of the domain
                updateDTO.setValidTo(Utility.plusValidityTime(updateDTO.getValidFrom(), validityTime).isBefore(d.getValidto()) ? Utility.plusValidityTime(updateDTO.getValidFrom(), validityTime) : d.getValidto());
                updateDTO.setValidToInherited(!Utility.plusValidityTime(updateDTO.getValidFrom(), validityTime).isBefore(d.getValidto()));
            } else {
                updateDTO.setValidTo(Utility.plusValidityTime(updateDTO.getValidFrom(), validityTime));
                updateDTO.setValidToInherited(false);
            }
        }

        // Update pseudonym
        if (pseudonymDBAccessService.updatePseudonyms(List.of(updateDTO)).getFirst()) {
            // Update successful, retrieve the updated pseudonym to show it to the user
            PseudonymDTO updatedPseudonym = pseudonymDBAccessService.getPseudonym(d.getName(), oldPseudonym.getIdentifierItem(), oldPseudonym.getPsn()).get(0);

            // Determine whether or not a reduced standard view or a complete view is requested
            if (!authorizationService.hasDomainPermission(domainName, "complete-view")) {
                updatedPseudonym.toReducedStandardView();
            }

            // Success. Return a status code 200 and the pseudonym as payload.
            log.debug("Successfully updated a pseudonym.");
            return responseService.ok(responseContentType, updatedPseudonym);
        } else {
            // Update failed. Return an error 422-UNPROCESSABLE_ENTITY.
            log.error("The requested update of a pseudonym failed.");
            return responseService.unprocessableEntity(responseContentType);
        }
    }
    
    /**
     * This method provides an endpoint to validate pseudonyms.
     *
     * @return 	<li>a <b>200-OK</b> status and the result of the validity-
     * 				check for the given pseudonym</li>
     * 			<li>a <b>404-NOT_FOUND</b> status when either the domain the
     * 				pseudonym is on or the pseudonym that should be validated 
     * 				couldn't be found</li>
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> status if there is 
     * 				no check digit according to the domain</li>
     */
    @GetMapping("/domains/{domainName}/pseudonyms/validation")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'pseudonym:read')")
    @Audit
    public ResponseEntity<?> validatePseudonym(@PathVariable("domainName") String domainName,
								               @RequestParam(name = "psn", required = true) String psn,
								               @RequestHeader(name = "accept", required = false) String responseContentType) {
    	// Retrieve domain
    	Domain d = domainDBAccessService.getDomainByName(domainName);
    	
    	if (d == null) {
    		log.debug("Could not find the domain in which the validation should be performed.");
    		return responseService.notFound(responseContentType);
    	}
    	
    	if (!d.getAddcheckdigit()) {
    		// There is no check digit to validate
    		log.debug("The domain is configured so that there is no check digit added. Therefore, no validation can be performed.");
    		return responseService.unprocessableEntity(responseContentType);
    	}
    	
    	// Decide the character space for the check digit validation depending on the used pseudonymization algorithm
		LuhnCheckDigit luhn;
		
		switch (d.getAlgorithm().toUpperCase()) {
	        case "CONSECUTIVE":
	        case "RANDOM":
	        case "RANDOM_NUM": {
	        	// Used alphabet: "0123456789"
	        	luhn = new LuhnMod10CheckDigit();
	        	break;
	        }
			case "MD5":
	        case "SHA1":
	        case "SHA2": 
	        case "SHA3":
	        case "BLAKE3":
	        case "XXHASH": {
	        	// Used alphabet: "0123456789ABCDEF"
	        	luhn = new LuhnMod16CheckDigit();
	        	break;
	        }
	        case "RANDOM_LET": {
	        	// Used alphabet: "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
	        	luhn = new LuhnMod26CheckDigit();
	        	break;
	        }
	        case "RANDOM_SYM_BIOS": {
	        	// Used alphabet: "ACDEFGHJKLMNPQRTUVWXYZ0123456789"
	        	luhn = new LuhnMod32CheckDigit();
	        	break;
	        }
	        case "RANDOM_SYM": {
	        	// Used alphabet: "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	        	luhn = new LuhnMod36CheckDigit();
	        	break;
	        }
			default:
				throw new IllegalArgumentException("Unexpected algorithm: " + d.getAlgorithm());
		}
    	
		// Perform validation
		Boolean valid = luhn.validateCheckDigit(psn, d.getPrefix());
    	
    	if (valid == null) {
    		log.debug("The validation was aborted since a character that is not part of the allowed alphabet was encountered.");
    		return responseService.badRequest(responseContentType);
    	}
    	
        return responseService.ok(responseContentType, valid.toString());
    }
    
    /**
	 * Endpoint to search for pseudonyms.
	 * Supports multi-word searches.
	 * 
	 * @param domainName the name of the domain to which the request is scoped to
	 * @param query the search string that should be looked up 
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with the list of matching pseudonyms on 
	 * 		   success</li>
     *         <li>a <b>206-PARTIAL_CONTENT</b> status with a truncated result set when 
     *         more than the maximum number of allowed results were found</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the domain cannot be found, or 
     *         when no pseudonyms match the query</li>
     *         <li>a <b>410-GONE</b> status when the request is not during the domain's 
     *         validity period</li>
	 */
	@GetMapping(value = "/domains/{domainName}/pseudonyms", params = {"query"})
	@PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'pseudonym:search')")
	@Audit
	public ResponseEntity<?> searchPseudonyms(@PathVariable("domainName") String domainName,
											  @RequestParam(name = "query", required = true) String query,
											  @RequestHeader(name = "accept", required = false) String responseContentType) {
		// Check if the domain exists and is still active
        Domain domain = domainDBAccessService.getDomainByName(domainName);
        if (domain == null) {
            log.debug("The domain where the pseudonym should be searched couldn't be found.");
            return responseService.notFound(responseContentType);
        }
		
		if (domain.getValidfrom().isAfter(LocalDateTime.now()) || domain.getValidto().isBefore(LocalDateTime.now())) {
			log.debug("The domain's validity period has not yet started or has already ended, so that no pseudonyms can be retrieved from it.");
			return responseService.gone(responseContentType);
		}
		
		// Retrieve pseudonyms from the database
		List<PseudonymDTO> pseudonyms = pseudonymDBAccessService.searchPseudonyms(query, domain.getId());
		
		// Evaluate result
		if (pseudonyms == null || pseudonyms.size() == 0) {
			log.debug("No pseudonyms for the given query string were found.");
			return responseService.ok(responseContentType, List.of());
		} else if (pseudonyms.size() > MAX_NUMBER_OF_SEARCH_RESULTS) {
			log.debug("Successfully queried the database and found more than " + MAX_NUMBER_OF_SEARCH_RESULTS + " pseudonyms, so the result list was truncated.");
			return responseService.partialContent(responseContentType, pseudonyms.subList(0, MAX_NUMBER_OF_SEARCH_RESULTS));
		} else {
			log.debug("Successfully found " + pseudonyms.size() + " pseudonyms.");
			return responseService.ok(responseContentType, pseudonyms);
		}
	}
}
