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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trustdeck.dto.DomainDTO;
import org.trustdeck.dto.DomainTreeDTO;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.security.audittrail.annotation.Audit;
import org.trustdeck.service.AuthorizationService;
import org.trustdeck.service.DomainDBAccessService;
import org.trustdeck.service.ResponseService;
import org.trustdeck.utils.Assertion;
import org.trustdeck.utils.Utility;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class encapsulates the requests for domains in a controller for the REST-API.
 * This REST-API offers full access to the data items.
 *
 * @author Armin Müller and Eric Wündisch
 */
@RestController
@EnableMethodSecurity
@Slf4j
@RequestMapping(value = "/api")
public class DomainRESTController {

	/** The default value for adding a check digit to the pseudonym. */
	public static final boolean DEFAULT_ADD_CHECK_DIGIT = true;
	
	/** The default value for allowing multiple pseudonyms per id&idType pair. */
	public static final boolean DEFAULT_ALLOW_MULTIPLE_PSN = false;
	
    /** The default value for enforcing the validTo date correctness. */
	public static final boolean DEFAULT_ENFORCE_END_DATE_VALIDITY = true;

    /** The default value for enforcing the validFrom date correctness. */
	public static final boolean DEFAULT_ENFORCE_START_DATE_VALIDITY = true;
    
    /** The default value for determining if the check digit should be included in the defined pseudonym length. */
	public static final boolean DEFAULT_LENGTH_INCLUDES_CHECK_DIGIT = false;
    
	/** Determines the default number of retries when a generated random number is already in use. */
	private static final int DEFAULT_NUMBER_OF_RETRIES = 3;

    /** The default character used for padding pseudonyms if necessary. */
	public static final char DEFAULT_PADDING_CHARACTER = '0';

    /** The default value for performing a recursive update of possible child domains. */
    private static final boolean DEFAULT_PERFORM_RECURSIVE_CHANGES = true;

    /** The default pseudonymization algorithm. */
    public static final String DEFAULT_PSEUDONYMIZATION_ALGO = "RANDOM_LET";
    
    /** The default alphabet (A-Z0-9) used for the pseudonymization process. */
    public static final String DEFAULT_PSEUDONYMIZATION_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** The default pseudonym length. */
    public static final int DEFAULT_PSEUDONYM_LENGTH = 16;
    
    /** The default number of pseudonyms a user wants to be able to create. */
    public static final long DEFAULT_RANDOM_ALGORITHM_DESIRED_SIZE = 100000000L;
    
    /** The default success probability with which a user wants to create a new pseudonym. */
    public static final double DEFAULT_RANDOM_ALGORITHM_DESIRED_SUCCESS_PROBABILITY = 0.99999998d;

    /** The default length of a salt. */
    public static final int DEFAULT_SALT_LENGTH = 32;

    /** The default validity time in seconds. */
    public static final String DEFAULT_VALIDITY_TIME = "30 years";
    
    /** The maximum length for the salt. */
    private static final int MAXIMUM_SALT_LENGTH = 256;

    /** The minimum length for pseudonyms. */
    private static final int MINIMUM_PSEUDONYM_LENGTH = 2;
    
    /** The minimum length for the salt. */
    private static final int MINIMUM_SALT_LENGTH = 8;
    
    /** The maximum number of domains that are returned in a search. */
    private static final int MAX_NUMBER_OF_SEARCH_RESULTS = 20;

    /** Enables the access to the domain specific database access methods. */
    @Autowired
    private DomainDBAccessService domainDBAccessService;

    /** Enables services for better working with responses. */
    @Autowired
    private ResponseService responseService;

    /** Provides functionality to ensure proper rights and roles when accessing the endpoints. */
    @Autowired
    private AuthorizationService authorizationService;

    /**
     * Method to create a new domain. Creates the record inside the
     * domain table.
     *
     * @param domainDTO (required) the domain object
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>201-CREATED</b> status and the location to the
     * 				domain inside the response header on success</li>
     * 			<li>a <b>400-BAD_REQUEST</b> when both, the super-domain
     * 				name and the super-domain ID were given</li>
     * 			<li>a <b>404-NOT_FOUND</b> when the provided parent 
     * 				domain could not be found</li>
     * 			<li>a <b>406-NOT_ACCEPTABLE</b> status when the domain
     * 				name is violating the URI-validity</li>
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> when the addition 
     * 				of the domain meta-information failed or when a check
     * 				digit should be calculated and the provided alphabet 
     * 				length is not an even number</li>
     */
    @PostMapping("/domains/complete")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'domain:create-complete')")
    @Audit
    public ResponseEntity<?> createDomainComplete(@RequestBody DomainDTO domainDTO,
                                                  @RequestHeader(name = "accept", required = false) String responseContentType) {

        String domainName = domainDTO.getName();
        String domainPrefix = domainDTO.getPrefix();
        Timestamp validFrom = domainDTO.getValidFrom() != null ? Timestamp.valueOf(domainDTO.getValidFrom()) : null;
        Timestamp validTo = domainDTO.getValidTo() != null ? Timestamp.valueOf(domainDTO.getValidTo()) : null;
        String validityTime = domainDTO.getValidityTime();
        Boolean enforceStartDateValidity = domainDTO.getEnforceStartDateValidity();
        Boolean enforceEndDateValidity = domainDTO.getEnforceEndDateValidity();
        String algorithm = domainDTO.getAlgorithm();
        String alphabet = Utility.generateAlphabet(domainDTO.getAlgorithm(), domainDTO.getAlphabet());
        Long randomAlgorithmDesiredSize = domainDTO.getRandomAlgorithmDesiredSize();
        Double randomAlgorithmDesiredSuccessProbability = domainDTO.getRandomAlgorithmDesiredSuccessProbability();
        Long consecVal = domainDTO.getConsecutiveValueCounter();
        Boolean multiplePsnAllowed = domainDTO.getMultiplePsnAllowed();
        Integer psnLength = domainDTO.getPseudonymLength();
        Character paddingChar = domainDTO.getPaddingCharacter();
        Boolean addCheckDigit = domainDTO.getAddCheckDigit();
        Boolean lengthIncludesCheckDigit = domainDTO.getLengthIncludesCheckDigit();
        String salt = domainDTO.getSalt();
        Integer saltLength = domainDTO.getSaltLength();
        String description = domainDTO.getDescription();
        String superDomainName = domainDTO.getSuperDomainName();

        if (Assertion.assertNullAll(domainName, domainPrefix, validFrom, validTo, validityTime, enforceStartDateValidity,
                enforceEndDateValidity, algorithm, alphabet, randomAlgorithmDesiredSize, randomAlgorithmDesiredSuccessProbability, 
                consecVal, multiplePsnAllowed, psnLength, paddingChar, addCheckDigit, lengthIncludesCheckDigit, salt, 
                saltLength, description, superDomainName)) {
            // An empty object was passed, so there is nothing to create.
            log.debug("The domain DTO passed by the user was empty. Nothing to create.");
            return responseService.unprocessableEntity(responseContentType);
        }

        // Check if the name is valid in an URI. If not, tell the user and abort
        URI location;
        try {
            location = new URI("/api/pseudonymization/domain?name=" + domainName);
        } catch (URISyntaxException e) {
            log.debug("The domain name is not suitable to be used in a URI. Please choose another name.");
            return responseService.notAcceptable(responseContentType);
        }

        // Get parent information if provided
        Domain parent = null;
        if (superDomainName != null) {
            parent = domainDBAccessService.getDomainByName(superDomainName);

            // Parent domain name was provided but not found, return a 404-NOT_FOUND 
            if (parent == null) {
                log.debug("Parent domain name was provided but not found.");
                return responseService.notFound(responseContentType);
            }
        }
        
        // Ensure a proper salt length
        saltLength = (saltLength != null && saltLength >= MINIMUM_SALT_LENGTH && saltLength <= MAXIMUM_SALT_LENGTH) ? saltLength : DEFAULT_SALT_LENGTH;

        // Create a salt if not already given
        if (salt == null || salt.trim().length() < MINIMUM_SALT_LENGTH || salt.trim().length() > MAXIMUM_SALT_LENGTH) {
        	salt = generateSalt(saltLength);
        }
        
        // Sanitize the alphabet
 		if (alphabet.contains(";")) {
 			log.error("The alphabet provided for the pseudonymization in domain contained a semicolon (\";\"), which is not allowed.");
 			return responseService.unprocessableEntity(responseContentType);
 		}
		
		// Ensure that the alphabet has a valid length, when a check digit is requested
		if (addCheckDigit != null && addCheckDigit && (alphabet.length() % 2) == 1) {
			log.warn("The alphabet provided for the pseudonymization in domain does not have an even length, "
					+ "which is necessary for the requested check-digit-calculation. The last character was "
					+ "therefore removed.");
			alphabet = alphabet.substring(0, alphabet.length() - 1);
		}
        
        // Ensure a valid desired success probability, if the selected algorithm is of the RANDOM-family
        if (algorithm != null && algorithm.trim().toUpperCase().startsWith("RANDOM") && randomAlgorithmDesiredSuccessProbability != null) {
        	if (randomAlgorithmDesiredSuccessProbability > 1.0d) {
        		// The success probability was probably provided as a number between 0 and 100, so we transform it into [0,1] 
        		randomAlgorithmDesiredSuccessProbability /= 100.0d;
        	}
        	
        	if (randomAlgorithmDesiredSuccessProbability >= 1.0d) {
        		// The provided number is still too big, switch to default value
        		log.warn("The provided success probability for the pseudonymization process was too big. Switching to default value.");
        		randomAlgorithmDesiredSuccessProbability = DEFAULT_RANDOM_ALGORITHM_DESIRED_SUCCESS_PROBABILITY;
        	} else if (randomAlgorithmDesiredSuccessProbability <= 0.0d) {
	    		// The provided number is zero or negative, switch to default value
	    		log.warn("The provided success probability for the pseudonymization process was too small. Switching to default value.");
	    		randomAlgorithmDesiredSuccessProbability = DEFAULT_RANDOM_ALGORITHM_DESIRED_SUCCESS_PROBABILITY;
        	}
        } else if (algorithm != null && algorithm.toUpperCase().startsWith("RANDOM") && randomAlgorithmDesiredSuccessProbability == null) {
        	// Use default
        	randomAlgorithmDesiredSuccessProbability = DEFAULT_RANDOM_ALGORITHM_DESIRED_SUCCESS_PROBABILITY;
        } else {
        	// Discard any provided information
        	randomAlgorithmDesiredSuccessProbability = null;
        }
        
        // Ensure a proper desired domain size
        if (algorithm != null && algorithm.trim().toUpperCase().startsWith("RANDOM") && randomAlgorithmDesiredSize == null) {
        	randomAlgorithmDesiredSize = DEFAULT_RANDOM_ALGORITHM_DESIRED_SIZE;
        }
        
        // Ensure that the check digit algorithm can work if it is requested
        if (addCheckDigit != null && addCheckDigit && (alphabet.length() % 2) != 0) {
        	// The calculation of a check digit was requested. For the algorithm that calculates the check digit to 
        	// work, the alphabet length must be an even number.
        	log.debug("The calculation of a check digit was requested. For the algorithm that calculates the check digit "
        			+ "to work, the alphabet length must be an even number.");
            return responseService.unprocessableEntity(responseContentType);
        }

        // Ensure that a valid counter for the consecutive numbering pseudonymization algorithm is present
        if (consecVal == null || consecVal <= 0) {
            consecVal = 1L;
        }

        // Ensure that the pseudonyms are long enough and not null
        if (algorithm != null && algorithm.trim().toUpperCase().startsWith("RANDOM")) {
        	int neededPsnLength = calculatePseudonymLength(randomAlgorithmDesiredSize, randomAlgorithmDesiredSuccessProbability, alphabet);
        	if (psnLength != null && neededPsnLength > psnLength) {
        		log.debug("The pseudonym length needed to store the desired number of pseudonyms is smaller than the provided pseudonym length. The calculated size will be used.");
        		psnLength = neededPsnLength;
        	} else if (psnLength != null && neededPsnLength < psnLength) {
        		// A length longer than the needed one was given
        		// Nothing to do
        	} else {
        		psnLength = neededPsnLength;
        	}
        } else if (randomAlgorithmDesiredSize != null && randomAlgorithmDesiredSuccessProbability != null) {
        	// Calculate minimal needed length for all non-random algorithms
        	psnLength = calculatePseudonymLength(randomAlgorithmDesiredSize, randomAlgorithmDesiredSuccessProbability, alphabet);
        }
        
        // If no info about the desired size was given and no length was provided, use the default
        if (psnLength == null) {
            psnLength = DEFAULT_PSEUDONYM_LENGTH;
        }
        
        // Ensure that the pseudonyms are never too short
        if (psnLength < MINIMUM_PSEUDONYM_LENGTH) {
            log.warn("The requested length for pseudonyms in the domain \"" + domainName + "\" was too short "
                    + "and therefore set to the minimum length (" + MINIMUM_PSEUDONYM_LENGTH + ").");
            psnLength = MINIMUM_PSEUDONYM_LENGTH;
        }

        // Gather the information needed for a new domain object
        Domain domain = new Domain();

        // Start creating a new domain object
        domain.setName(domainName);
        domain.setPrefix(domainPrefix);

        domain.setConsecutivevaluecounter(consecVal);
        
        domain.setDescription(description);
        
        domain.setSalt(salt);
        domain.setSaltlength(salt.length());

        if (parent != null) {
            // Determine validFrom date if not given by the user
            domain.setValidfrom((validFrom != null) ? validFrom.toLocalDateTime() : parent.getValidfrom());
            domain.setValidfrominherited(validFrom == null);

            // Determine validTo date
            LocalDateTime vTo;
            Boolean vToInh = false;
            if (validTo != null) {
                vTo = validTo.toLocalDateTime();
            } else if (validTo == null && validityTime != null) {
                vTo = Utility.plusValidityTime(domain.getValidfrom(), validityTime);
            } else {
                vTo = parent.getValidto();
                vToInh = true;
            }

            domain.setValidto(vTo);
            domain.setValidtoinherited(vToInh);

            // Determine if the validFrom date correctness should be enforced
            domain.setEnforcestartdatevalidity((enforceStartDateValidity != null) ? enforceStartDateValidity : parent.getEnforcestartdatevalidity());
            domain.setEnforcestartdatevalidityinherited(enforceStartDateValidity == null);

            // Determine if the validTo date correctness should be enforced
            domain.setEnforceenddatevalidity((enforceEndDateValidity != null) ? enforceEndDateValidity : parent.getEnforceenddatevalidity());
            domain.setEnforceenddatevalidityinherited(enforceEndDateValidity == null);

            // Determine pseudonymization algorithm
            domain.setAlgorithm((algorithm != null) ? algorithm : parent.getAlgorithm());
            domain.setAlgorithminherited(algorithm == null);
            
            // Determine the alphabet used for pseudonymization
            domain.setAlphabet((alphabet != null) ? alphabet : parent.getAlphabet());
            domain.setAlphabetinherited(alphabet == null);
            
            // Determine the number of pseudonyms the user wants to create
            domain.setRandomalgorithmdesiredsize((randomAlgorithmDesiredSize != null) ? randomAlgorithmDesiredSize : parent.getRandomalgorithmdesiredsize());
            domain.setRandomalgorithmdesiredsizeinherited(randomAlgorithmDesiredSize == null);
            
            // Determine the minimum success probability for the generation of new pseudonyms
            domain.setRandomalgorithmdesiredsuccessprobability((randomAlgorithmDesiredSuccessProbability != null) ? randomAlgorithmDesiredSuccessProbability : parent.getRandomalgorithmdesiredsuccessprobability());
            domain.setRandomalgorithmdesiredsuccessprobabilityinherited(randomAlgorithmDesiredSuccessProbability == null);
            
            // Determine if multiple pseudonyms per identifier&idType combination are allowed
            domain.setMultiplepsnallowed((multiplePsnAllowed != null) ? multiplePsnAllowed : parent.getMultiplepsnallowed());
            domain.setMultiplepsnallowedinherited(multiplePsnAllowed == null);
            
            // Determine pseudonym length
            domain.setPseudonymlength((psnLength != null) ? psnLength : parent.getPseudonymlength());
            domain.setPseudonymlengthinherited(psnLength == null);

            // Determine padding character
            domain.setPaddingcharacter((paddingChar != null) ? paddingChar.toString() : parent.getPaddingcharacter());
            domain.setPaddingcharacterinherited(paddingChar == null);

            // Determine if a check digit should be created
            domain.setAddcheckdigit((addCheckDigit != null) ? addCheckDigit : parent.getAddcheckdigit());
            domain.setAddcheckdigitinherited(addCheckDigit == null);

            // Determine if the check digit should be included in the length of the pseudonym
            domain.setLengthincludescheckdigit((lengthIncludesCheckDigit != null) ? lengthIncludesCheckDigit : parent.getLengthincludescheckdigit());
            domain.setLengthincludescheckdigitinherited(lengthIncludesCheckDigit == null);
            
            domain.setSuperdomainid(parent.getId());
        } else {
            // No parent information provided

            // Check if either validTo or validityTime is given
            if (validTo == null && validityTime == null) {
                // Both values are missing; inform user and use default
                log.debug("For the domain \"" + domainName + "\" there was neither an end date nor a validity period given."
                        + " The default of " + DEFAULT_VALIDITY_TIME + " is used.");
                validityTime = DEFAULT_VALIDITY_TIME;
            }

            // Determine validFrom date if not given by the user
            domain.setValidfrom((validFrom != null) ? validFrom.toLocalDateTime() : LocalDateTime.now());
            domain.setValidfrominherited(false);

            // Determine expiration date
            domain.setValidto((validTo != null) ? validTo.toLocalDateTime() : Utility.plusValidityTime(domain.getValidfrom(), validityTime));
            domain.setValidtoinherited(false);

            // Determine if the validFrom date correctness should be enforced
            domain.setEnforcestartdatevalidity((enforceStartDateValidity != null) ? enforceStartDateValidity : DEFAULT_ENFORCE_START_DATE_VALIDITY);
            domain.setEnforcestartdatevalidityinherited(false);

            // Determine if the validTo date correctness should be enforced
            domain.setEnforceenddatevalidity((enforceEndDateValidity != null) ? enforceEndDateValidity : DEFAULT_ENFORCE_END_DATE_VALIDITY);
            domain.setEnforceenddatevalidityinherited(false);

            // Determine pseudonymization algorithm
            domain.setAlgorithm((algorithm != null) ? algorithm : DEFAULT_PSEUDONYMIZATION_ALGO);
            domain.setAlgorithminherited(false);
            
            // Determine the alphabet used for pseudonymization
            domain.setAlphabet((alphabet != null) ? alphabet : DEFAULT_PSEUDONYMIZATION_ALPHABET);
            domain.setAlphabetinherited(false);
            
            // Determine the number of pseudonyms the user wants to create
            domain.setRandomalgorithmdesiredsize((randomAlgorithmDesiredSize != null) ? randomAlgorithmDesiredSize : DEFAULT_RANDOM_ALGORITHM_DESIRED_SIZE);
            domain.setRandomalgorithmdesiredsizeinherited(false);
            
            // Determine the minimum success probability for the generation of new pseudonyms
            domain.setRandomalgorithmdesiredsuccessprobability((randomAlgorithmDesiredSuccessProbability != null) ? randomAlgorithmDesiredSuccessProbability : DEFAULT_RANDOM_ALGORITHM_DESIRED_SUCCESS_PROBABILITY);
            domain.setRandomalgorithmdesiredsuccessprobabilityinherited(false);

            // Determine if multiple pseudonyms per identifier&idType combination are allowed
            domain.setMultiplepsnallowed((multiplePsnAllowed != null) ? multiplePsnAllowed : DEFAULT_ALLOW_MULTIPLE_PSN);
            domain.setMultiplepsnallowedinherited(false);
            
            // Determine pseudonym length
            domain.setPseudonymlength((psnLength != null) ? psnLength : DEFAULT_PSEUDONYM_LENGTH);
            domain.setPseudonymlengthinherited(false);

            // Determine padding character
            domain.setPaddingcharacter((paddingChar != null) ? paddingChar.toString() : String.valueOf(DEFAULT_PADDING_CHARACTER));
            domain.setPaddingcharacterinherited(false);

            // Determine if a check digit should be created
            domain.setAddcheckdigit((addCheckDigit != null) ? addCheckDigit : DEFAULT_ADD_CHECK_DIGIT);
            domain.setAddcheckdigitinherited(false);

            // Determine if the check digit should be included in the length of the pseudonym
            domain.setLengthincludescheckdigit((lengthIncludesCheckDigit != null) ? lengthIncludesCheckDigit : DEFAULT_LENGTH_INCLUDES_CHECK_DIGIT);
            domain.setLengthincludescheckdigitinherited(false);
            
            domain.setSuperdomainid(0);
        }

        // Insert into database
        String result = domainDBAccessService.insertDomain(domain);
        
        if (result.equals(DomainDBAccessService.INSERTION_SUCCESS)) {
            // Return a 201-CREATED status and the location to the domain inside the response header.
        	DomainDTO createdDomDTO = new DomainDTO().assignPojoValues(domainDBAccessService.getDomainByName(domain.getName()));
            if (!authorizationService.hasDomainPermission(createdDomDTO.getName(), "complete-view")) {
            	createdDomDTO = createdDomDTO.toReducedStandardView();
            }
            
            log.info("Successfully created the domain \"" + domainName + "\".");
            return responseService.created(responseContentType, location, createdDomDTO);
        } else if (result.equals(DomainDBAccessService.INSERTION_DUPLICATE)) {
            // Nothing added since the entry is a duplicate. Return an 200-OK status.
        	DomainDTO existingDomDTO = new DomainDTO().assignPojoValues(domainDBAccessService.getDomainByName(domain.getName()));
            if (!authorizationService.hasDomainPermission(existingDomDTO.getName(), "complete-view")) {
            	existingDomDTO = existingDomDTO.toReducedStandardView();
            }
            
            log.info("The domain requested to be inserted was skipped because it is already in the database.");
            return responseService.ok(responseContentType, existingDomDTO);
        } else {
            // Creating the domain failed. Return an error 422-UNPROCESSABLE_ENTITY.
            log.error("Adding the domain \"" + domainName + "\" was unsuccessful.");
            return responseService.unprocessableEntity(responseContentType);
        }
    }

    /**
     * Method to create a new domain with a reduced set of attributes.
     * Creates the record inside the domain table.
     *
     * @param domainDTO (required) the domain object
     * @param responseContentType(optional) the response content type
     * @return 	<li>a <b>200-OK</b> status when the domain was already
     * 				in the database</li>
     * 			<li>a <b>201-CREATED</b> status and the location to the
     * 				domain inside the response header on success</li>
     * 			<li>a <b>404-NOT_FOUND</b> when the provided parent 
     * 				domain could not be found</li>
     * 			<li>a <b>406-NOT_ACCEPTABLE</b> status when the domain
     * 				name is violating the URI-validity</li>
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> when the addition
     * 				of the domain failed or when the DTO was invalid</li>
     */
    @PostMapping("/domains")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'domain:create')")
    @Audit
    public ResponseEntity<?> createDomain(@RequestBody DomainDTO domainDTO,
                                          @RequestHeader(name = "accept", required = false) String responseContentType) {

        if (!domainDTO.validate() || !domainDTO.isValidStandardView()) {
        	log.debug("The given domain DTO is invalid, due to " + (!domainDTO.validate() ? "missing mandatory fields." : "having non-standard information."));
        	return responseService.unprocessableEntity(responseContentType);
        }

        String domainName = domainDTO.getName();
        String domainPrefix = domainDTO.getPrefix();
        Timestamp validFrom = domainDTO.getValidFrom() != null ? Timestamp.valueOf(domainDTO.getValidFrom()) : null;
        Timestamp validTo = domainDTO.getValidTo() != null ? Timestamp.valueOf(domainDTO.getValidTo()) : null;
        String validityTime = domainDTO.getValidityTime();
        String algorithm = domainDTO.getAlgorithm();
        String alphabet = Utility.generateAlphabet(algorithm, null);
        Boolean multiplePsnAllowed = domainDTO.getMultiplePsnAllowed();
        String description = domainDTO.getDescription();
        String superDomainName = domainDTO.getSuperDomainName();
        
        if (Assertion.assertNullAll(domainName, domainPrefix, validFrom, validTo, validityTime, algorithm, 
        		multiplePsnAllowed, description, superDomainName)) {
            // An empty object was passed, so there is nothing to create.
            log.debug("The domain DTO passed by the user was empty. Nothing to create.");
            return responseService.unprocessableEntity(responseContentType);
        }

        if (domainName == null || domainPrefix == null) {
        	// These two attributes must be given
        	log.debug("No name and/or no prefix were provided for the domain.");
        	return responseService.unprocessableEntity(responseContentType);
        }

        // Check if the name is valid in an URI. If not, tell the user and abort
        URI location;
        try {
            location = new URI("/api/pseudonymization/domain?name=" + domainName);
        } catch (URISyntaxException e) {
            log.debug("The domain name is not suitable to be used in a URI. Please choose another name.");
            return responseService.notAcceptable(responseContentType);
        }

        // Get parent information if provided
        Domain parent = null;
        if (superDomainName != null) {
            parent = domainDBAccessService.getDomainByName(superDomainName);

            // Parent domain name was provided but not found, return a 404-NOT_FOUND 
            if (parent == null) {
                log.debug("Parent domain name was provided but nothing was found.");
                return responseService.notFound(responseContentType);
            }
        }
        
        // Create a salt
        String salt = generateSalt(DEFAULT_SALT_LENGTH);

        // Ensure that a valid counter for the consecutive numbering pseudonymization algorithm is present
        long consecVal = 1L;

        // Gather the information needed for a new domain object
        Domain domain = new Domain();

        // Start creating the new domain object
        domain.setName(domainName);
        domain.setPrefix(domainPrefix);
        
        domain.setConsecutivevaluecounter(consecVal);
        
        domain.setDescription(description);
        
        domain.setSalt(salt);
        domain.setSaltlength(salt.length());

        if (parent != null) {
            // Determine validFrom date if not given by the user
            domain.setValidfrom((validFrom != null) ? validFrom.toLocalDateTime() : parent.getValidfrom());
            domain.setValidfrominherited(validFrom == null);

            // Determine validTo date
            LocalDateTime vTo;
            Boolean vToInh = false;
            if (validTo != null) {
                vTo = validTo.toLocalDateTime();
            } else if (validTo == null && validityTime != null) {
                vTo = Utility.plusValidityTime(domain.getValidfrom(), validityTime);
            } else {
                vTo = parent.getValidto();
                vToInh = true;
            }

            domain.setValidto(vTo);
            domain.setValidtoinherited(vToInh);

            // Determine if the validFrom date correctness should be enforced
            domain.setEnforcestartdatevalidity(parent.getEnforcestartdatevalidity());
            domain.setEnforcestartdatevalidityinherited(true);

            // Determine if the validTo date correctness should be enforced
            domain.setEnforceenddatevalidity(parent.getEnforceenddatevalidity());
            domain.setEnforceenddatevalidityinherited(true);

            // Determine pseudonymization algorithm
            domain.setAlgorithm((algorithm != null) ? algorithm : parent.getAlgorithm());
            domain.setAlgorithminherited(algorithm == null);
            
            // Determine the alphabet used for pseudonymization
            domain.setAlphabet((alphabet != null) ? alphabet : parent.getAlphabet());
            domain.setAlphabetinherited(true);
            
            // Determine the number of pseudonyms the user wants to create
            domain.setRandomalgorithmdesiredsize(parent.getRandomalgorithmdesiredsize());
            domain.setRandomalgorithmdesiredsizeinherited(true);
            
            // Determine the minimum success probability for the generation of new pseudonyms
            domain.setRandomalgorithmdesiredsuccessprobability(parent.getRandomalgorithmdesiredsuccessprobability());
            domain.setRandomalgorithmdesiredsuccessprobabilityinherited(true);
            
            domain.setMultiplepsnallowed((multiplePsnAllowed != null) ? multiplePsnAllowed : parent.getMultiplepsnallowed());
            domain.setMultiplepsnallowedinherited(multiplePsnAllowed == null);
            
            // Determine pseudonym length
            domain.setPseudonymlength(parent.getPseudonymlength());
            domain.setPseudonymlengthinherited(true);

            // Determine padding character
            domain.setPaddingcharacter(parent.getPaddingcharacter());
            domain.setPaddingcharacterinherited(true);

            // Determine if a check digit should be created
            domain.setAddcheckdigit(parent.getAddcheckdigit());
            domain.setAddcheckdigitinherited(true);

            // Determine if the check digit should be included in the length of the pseudonym
            domain.setLengthincludescheckdigit(parent.getLengthincludescheckdigit());
            domain.setLengthincludescheckdigitinherited(true);
            
            domain.setSuperdomainid(parent.getId());
        } else {
            // No parent information provided

            // Check if either validTo or validityTime is given
            if (validTo == null && validityTime == null) {
                // Both values are missing; inform user and use default
                log.debug("For the domain \"" + domainName + "\" there was neither an end date nor a validity period given."
                        + " The default of " + DEFAULT_VALIDITY_TIME + " is used.");
                validityTime = DEFAULT_VALIDITY_TIME;
            }

            // Determine validFrom date if not given by the user
            domain.setValidfrom((validFrom != null) ? validFrom.toLocalDateTime() : LocalDateTime.now());
            domain.setValidfrominherited(false);

            // Determine expiration date
            domain.setValidto((validTo != null) ? validTo.toLocalDateTime() : Utility.plusValidityTime(domain.getValidfrom(), validityTime));
            domain.setValidtoinherited(false);

            // Determine if the validFrom date correctness should be enforced
            domain.setEnforcestartdatevalidity(DEFAULT_ENFORCE_START_DATE_VALIDITY);
            domain.setEnforcestartdatevalidityinherited(false);

            // Determine if the validTo date correctness should be enforced
            domain.setEnforceenddatevalidity(DEFAULT_ENFORCE_END_DATE_VALIDITY);
            domain.setEnforceenddatevalidityinherited(false);

            // Determine pseudonymization algorithm
            domain.setAlgorithm((algorithm != null) ? algorithm : DEFAULT_PSEUDONYMIZATION_ALGO);
            domain.setAlgorithminherited(false);
            
            // Determine the alphabet used for pseudonymization
            domain.setAlphabet((alphabet != null) ? alphabet : DEFAULT_PSEUDONYMIZATION_ALPHABET);
            domain.setAlphabetinherited(false);
            
            // Determine the number of pseudonyms the user wants to create
            domain.setRandomalgorithmdesiredsize(DEFAULT_RANDOM_ALGORITHM_DESIRED_SIZE);
            domain.setRandomalgorithmdesiredsizeinherited(false);
            
            // Determine the minimum success probability for the generation of new pseudonyms
            domain.setRandomalgorithmdesiredsuccessprobability(DEFAULT_RANDOM_ALGORITHM_DESIRED_SUCCESS_PROBABILITY);
            domain.setRandomalgorithmdesiredsuccessprobabilityinherited(false);
            
            domain.setMultiplepsnallowed((multiplePsnAllowed != null) ? multiplePsnAllowed : DEFAULT_ALLOW_MULTIPLE_PSN);
            domain.setMultiplepsnallowedinherited(false);
            
            // Determine pseudonym length
            domain.setPseudonymlength(DEFAULT_PSEUDONYM_LENGTH);
            domain.setPseudonymlengthinherited(false);

            // Determine padding character
            domain.setPaddingcharacter(String.valueOf(DEFAULT_PADDING_CHARACTER));
            domain.setPaddingcharacterinherited(false);

            // Determine if a check digit should be created
            domain.setAddcheckdigit(DEFAULT_ADD_CHECK_DIGIT);
            domain.setAddcheckdigitinherited(false);

            // Determine if the check digit should be included in the length of the pseudonym
            domain.setLengthincludescheckdigit(DEFAULT_LENGTH_INCLUDES_CHECK_DIGIT);
            domain.setLengthincludescheckdigitinherited(false);
            
            domain.setSuperdomainid(0);
        }

        // Insert into database
        String result = domainDBAccessService.insertDomain(domain);
        
        if (result.equals(DomainDBAccessService.INSERTION_SUCCESS)) {
            // Return a 201-CREATED status and the location to the domain inside the response header.
            DomainDTO createdDomDTO = new DomainDTO().assignPojoValues(domainDBAccessService.getDomainByName(domain.getName()));
            if (!authorizationService.hasDomainPermission(domain.getName(), "complete-view")) {
            	createdDomDTO = createdDomDTO.toReducedStandardView();
            }
            
            log.info("Successfully created the domain \"" + domainName + "\".");
            return responseService.created(responseContentType, location, createdDomDTO);
        } else if (result.equals(DomainDBAccessService.INSERTION_DUPLICATE)) {
            // Nothing added since the entry is a duplicate. Return an 200-OK status.
        	DomainDTO existingDomDTO = new DomainDTO().assignPojoValues(domainDBAccessService.getDomainByName(domain.getName()));
            if (!authorizationService.hasDomainPermission(domain.getName(), "complete-view")) {
            	existingDomDTO = existingDomDTO.toReducedStandardView();
            }
            
            log.info("The domain requested to be inserted was skipped because it is already in the database.");
            return responseService.ok(responseContentType, existingDomDTO);
        } else {
            // Creating the domain failed. Return an error 422-UNPROCESSABLE_ENTITY.
            log.error("Adding the domain \"" + domainName + "\" was unsuccessful.");
            return responseService.unprocessableEntity(responseContentType);
        }
    }

    /**
     * This method deletes a domain. A domain name must be provided.
     *
     * @param domainName (required) the name of the domain that should be deleted
     * @param performRecursiveChanges (optional) specifies whether or not changes should be cascaded to sub-domains
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>204-NO_CONTENT</b> status when the deletion was
     * 				successful</li>
     * 			<li>a <b>404-NOT_FOUND</b> when no domain was found for the given name</li>
     * 			<li>a <b>500-INTERNAL_SERVER_ERROR</b> status when the domain
     * 				could not be deleted</li>
     */
    @DeleteMapping("/domains")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'domain:delete')")
    @Audit
    public ResponseEntity<?> deleteDomain(@RequestParam(name = "name", required = true) String domainName,
                                          @RequestParam(name = "recursive", required = false) Boolean performRecursiveChanges,
                                          @RequestHeader(name = "accept", required = false) String responseContentType) {
        // Get domain
        Domain domain = domainDBAccessService.getDomainByName(domainName);

        // Check if a domain was found. If not, return a 404-NOT_FOUND
        if (domain == null) {
            log.debug("The domain that should be deleted couldn't be found.");
            return responseService.notFound(responseContentType);
        }

        // Check if all necessary values are given. If not, use defaults.
        if (performRecursiveChanges == null) {
            performRecursiveChanges = DEFAULT_PERFORM_RECURSIVE_CHANGES;
        }

        // Perform deletion
        if (domainDBAccessService.deleteDomain(domain.getName(), performRecursiveChanges)) {
            // Successfully deleted a domain, return a 204-NO_CONTENT
            log.info("Successfully deleted the domain \"" + domain.getName() + "\".");
            return responseService.noContent(responseContentType);
        } else {
            // Deletion was unsuccessful, return a 500-INTERNAL_SERVER_ERROR
            log.error("The domain \"" + domain.getName() + "\" could not be deleted.");
            return responseService.internalServerError(responseContentType);
        }
    }
    
    /**
     * This method allows to read only a single attribute of a domain and nothing else.
     * 
     * @param domainName (required) the name of the domain
     * @param attributeName (required) the name of the attribute that should be retrieved from the domain
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>200-OK</b> status and the <b>attribute</b> when
     * 				it was found</li>
     * 			<li>a <b>403-FORBIDDEN</b> when the rights for accessing 
     * 				the attribute were not found in the token</li>
     * 			<li>a <b>404-NOT_FOUND</b> when no domain was found for
     * 				the given name or the when given attribute name 
     * 				wasn't found</li>
     */
    @GetMapping("/domains/{domainName}/{attribute}")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'domain:read')")
    @Audit
    public ResponseEntity<?> getDomainAttribute(@PathVariable("domainName") String domainName,
    											@PathVariable("attribute") String attributeName,
    		                                    @RequestHeader(name = "accept", required = false) String responseContentType) {
    	// Check if the user has the rights to access the requested attribute
    	boolean canSeeComplete = authorizationService.hasGlobalPermission("complete-view");
    	
    	if (!"name, prefix, validfrom, validto, multiplepsnallowed, description".contains(attributeName.trim().toLowerCase()) && !canSeeComplete) {
    		log.debug("The user is trying to read protected attributes without the necessary permission.");
    		return responseService.forbidden(responseContentType);
    	}
    	
    	String attribute;
    	Domain domain = domainDBAccessService.getDomainByName(domainName);
    	
    	// Check if the domain was found
    	if (domain == null) {
    		return responseService.notFound(responseContentType);
    	}
    	
    	// Get required attribute
    	switch (attributeName.trim().toLowerCase()) {
		case "id": {
			attribute = domain.getId().toString();
			break;
		} case "name": {
			attribute = domain.getName();
			break;
		} case "prefix": {
			attribute = domain.getPrefix();
			break;
		} case "validfrom": {
			attribute = domain.getValidfrom().toString();
			break;
		} case "validfrominherited": {
			attribute = domain.getValidfrominherited().toString();
			break;
		} case "validto": {
			attribute = domain.getValidto().toString();
			break;
		} case "validtoinherited": {
			attribute = domain.getValidtoinherited().toString();
			break;
		} case "enforcestartdatevalidity": {
			attribute = domain.getEnforcestartdatevalidity().toString();
			break;
		} case "enforcestartdatevalidityinherited": {
			attribute = domain.getEnforcestartdatevalidityinherited().toString();
			break;
		} case "enforceenddatevalidity": {
			attribute = domain.getEnforceenddatevalidity().toString();
			break;
		} case "enforceenddatevalidityinherited": {
			attribute = domain.getEnforceenddatevalidityinherited().toString();
			break;
		} case "algorithm": {
			attribute = domain.getAlgorithm();
			break;
		} case "algorithminherited": {
			attribute = domain.getAlgorithminherited().toString();
			break;
		} case "alphabet": {
			attribute = domain.getAlphabet();
			break;
		} case "alphabetinherited": {
			attribute = domain.getAlphabetinherited().toString();
			break;
		} case "randomalgorithmdesiredsize": {
			attribute = domain.getRandomalgorithmdesiredsize().toString();
			break;
		} case "randomalgorithmdesiredsizeinherited": {
			attribute = domain.getRandomalgorithmdesiredsizeinherited().toString();
			break;
		} case "randomalgorithmdesiredsuccessprobability": {
			attribute = domain.getRandomalgorithmdesiredsuccessprobability().toString();
			break;
		} case "randomalgorithmdesiredsuccessprobabilityinherited": {
			attribute = domain.getRandomalgorithmdesiredsuccessprobabilityinherited().toString();
			break;
		} case "multiplepsnallowed": {
			attribute = domain.getMultiplepsnallowed().toString();
			break;
		} case "multiplepsnallowedinherited": {
			attribute = domain.getMultiplepsnallowedinherited().toString();
			break;
		} case "consecutivevaluecounter": {
			attribute = domain.getConsecutivevaluecounter().toString();
			break;
		} case "pseudonymlength": {
			attribute = domain.getPseudonymlength().toString();
			break;
		} case "pseudonymlengthinherited": {
			attribute = domain.getPseudonymlengthinherited().toString();
			break;
		} case "paddingcharacter": {
			attribute = domain.getPaddingcharacter();
			break;
		} case "paddingcharacterinherited": {
			attribute = domain.getPaddingcharacterinherited().toString();
			break;
		} case "addcheckdigit": {
			attribute = domain.getAddcheckdigit().toString();
			break;
		} case "addcheckdigitinherited": {
			attribute = domain.getAddcheckdigitinherited().toString();
			break;
		} case "lengthincludescheckdigit": {
			attribute = domain.getLengthincludescheckdigit().toString();
			break;
		} case "lengthincludescheckdigitinherited": {
			attribute = domain.getLengthincludescheckdigitinherited().toString();
			break;
		} case "salt": {
			attribute = domain.getSalt();
			break;
		} case "saltlength": {
			attribute = domain.getSaltlength().toString();
			break;
		} case "description": {
			attribute = domain.getDescription();
			break;
		} case "superdomainid": {
			attribute = domain.getSuperdomainid().toString();
			break;
		} default:
			log.debug("The requested attribute was not found.");
			return responseService.notFound(responseContentType);
		}
    	
    	return responseService.ok(responseContentType, attribute);
    }

    /**
     * This method retrieves a domain through its name.
     *
     * @param domainName (required) the name of the domain to search for
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>200-OK</b> status and the <b>domain</b> when
     * 				it was found</li>
     * 			<li>a <b>404-NOT_FOUND</b> when no domain was found for
     * 				the given name</li>
     */
    @GetMapping("/domains/{domainName}")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'domain:read')")
    @Audit
    public ResponseEntity<?> getDomain(@PathVariable(name = "domainName", required = true) String domainName,
                                       @RequestHeader(name = "accept", required = false) String responseContentType) {
        // Get domain
        Domain domain = domainDBAccessService.getDomainByName(domainName);

        if (domain != null) {
            // Successfully retrieved a domain, return it to the user
            DomainDTO domainDTO = new DomainDTO().assignPojoValues(domain);

            // Determine whether or not a reduced standard view or a complete view is requested
            if (!authorizationService.hasDomainPermission(domain.getName(), "complete-view")) {
                domainDTO = domainDTO.toReducedStandardView();
            }

            log.debug("Successfully retrieved the domain \"" + domainName + "\".");
            return responseService.ok(responseContentType, domainDTO);
        } else {
            // Nothing found, return a 404-NOT_FOUND
            log.debug("No domain with the name \"" + domainName + "\" was found.");
            return responseService.notFound(responseContentType);
        }
    }
    
    /**
     * This method returns all domains from the database in a minimal version.
     *
     * @param domainName the domain's name
     * @param responseContentType (optional) the response content type
     * @return <li>a <b>200-OK</b> status and the <b>list of domains</b> 
     * 		   when the query was successful</li>
     * 		   <li>a <b>404-NOT_FOUND</b> status, when the given domain
     * 		   could not be found</li>
     */
    @GetMapping("/domains/{domainName}/subtree")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'domain:read-subtree')")
    @Audit
    public ResponseEntity<?> getDomainSubtree(@PathVariable("domainName") String domainName,
    										  @RequestHeader(name = "accept", required = false) String responseContentType) {
        List<DomainDTO> domains = domainDBAccessService.getSubtreeFromDomainName(domainName);
        
        if (domains == null || domains.size() == 0) {
        	log.debug("The subtree search did not find anything.");
        	return responseService.notFound(responseContentType);
        }

        // Determine whether or not a reduced standard view or a complete view is requested
        boolean canSeeComplete = authorizationService.hasGlobalPermission("complete-view");

        // Create a list of domains
        for (int i = 0; i < domains.size(); i++) {
        	if (!canSeeComplete) {
        		domains.get(i).toReducedStandardView();
        	}
        }
        
        // Build the DTO with inlined children object
        DomainTreeDTO tree = buildDomainTree(domains, domainName);
        if (tree == null) {
        	// Fallback if building fails --> use list-view
            log.warn("Buidling the domain subtree failed.");
            return responseService.ok(responseContentType, domains);
        }

        log.trace("Succesfully retrieved the subtree for domain \"" + domainName + "\".");
        return responseService.ok(responseContentType, tree);
    }

    /**
     * This method returns all domains from the database in a list of trees with each domains children inlined.
     *
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>200-OK</b> status and the <b>list of domain 
     * 			trees</b> when the query was successful</li>
     */
    @GetMapping(value = "/domains/hierarchy")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'domain:list-all')")
    @Audit
    public ResponseEntity<?> listDomainHierarchy(@RequestHeader(name = "accept", required = false) String responseContentType) {
        // Determine whether or not a reduced standard view or a complete view is requested
        boolean canSeeComplete = authorizationService.hasGlobalPermission("complete-view");
        
        List<DomainDTO> domains = domainDBAccessService.listDomains();
        
        if (domains == null || domains.size() == 0) {
        	log.debug("The domain search did not find anything.");
        	return responseService.notFound(responseContentType);
        }

        // Create a list of domains
        for (int i = 0; i < domains.size(); i++) {
        	if (!canSeeComplete) {
        		domains.get(i).toReducedStandardView();
        	}
        }
        
        // Build the DTO with inlined children object
        List<DomainTreeDTO> trees = buildDomainTree(domains);
        if (trees == null) {
        	// Fallback if building fails --> use list-view
            log.warn("Buidling the domain subtree failed.");
            domains.sort(Comparator.comparing(DomainDTO::getName));
            return responseService.ok(responseContentType, domains);
        }

        log.trace("Succesfully retrieved the trees for the stored domains.");
        trees.sort(Comparator.comparing(d -> d.getDomain().getName()));
        return responseService.ok(responseContentType, trees);
    }

    /**
     * Method to update a domain.
     *
     * @param oldDomainName (required) the name of the domain that is to be updated
     * @param performRecursiveChanges (required) specifies whether or not changes should be cascaded to sub-domains
     * @param domainDTO (required) the domain object
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>200-OK</b> status when the update was successful</li>
     * 			<li>a <b>404-NOT_FOUND</b> when the domain that should be
     * 				updated couldn't be found</li>
     * 			<li>a <b>406-NOT_ACCEPTABLE</b> status when the new domain name
     * 				is violating the URI-validity</li>
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> when updating the domain
     * 				failed</li>
     */
    @PutMapping("/domains/complete")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #oldDomainName, 'domain:update-complete')")
    @Audit
    public ResponseEntity<?> updateDomainComplete(@RequestParam(name = "name", required = true) String oldDomainName,
                                                  @RequestParam(name = "recursive", required = true) Boolean performRecursiveChanges,
                                                  @RequestBody DomainDTO domainDTO,
                                                  @RequestHeader(name = "accept", required = false) String responseContentType) {
        String newDomainName = domainDTO.getName();
        String prefix = domainDTO.getPrefix();
        Timestamp validFrom = domainDTO.getValidFrom() != null ? Timestamp.valueOf(domainDTO.getValidFrom()) : null;
        Timestamp validTo = domainDTO.getValidTo() != null ? Timestamp.valueOf(domainDTO.getValidTo()) : null;
        String validityTime = domainDTO.getValidityTime();
        Boolean enforceStartDateValidity = domainDTO.getEnforceStartDateValidity();
        Boolean enforceEndDateValidity = domainDTO.getEnforceEndDateValidity();
        String algorithm = domainDTO.getAlgorithm();
        String alphabet = Utility.generateAlphabet(algorithm, null);
        Long randomAlgorithmDesiredSize = domainDTO.getRandomAlgorithmDesiredSize();
        Double randomAlgorithmDesiredSuccessProbability = domainDTO.getRandomAlgorithmDesiredSuccessProbability();
        Boolean multiplePsnAllowed = domainDTO.getMultiplePsnAllowed();
        Long consecVal = domainDTO.getConsecutiveValueCounter();
        Integer psnLength = domainDTO.getPseudonymLength();
        Character paddingChar = domainDTO.getPaddingCharacter();
        Boolean addCheckDigit = domainDTO.getAddCheckDigit();
        Boolean lengthIncludesCheckDigit = domainDTO.getLengthIncludesCheckDigit();
        String salt = domainDTO.getSalt();
        Integer saltLength = domainDTO.getSaltLength();
        String description = domainDTO.getDescription();
        
        if (domainDBAccessService.getAmountOfPseudonymsInDomain(oldDomainName) > 0) {
        	log.warn("Changes to the domain configuration can introduce inconsistencies when creating further pseudonyms.");
        }

        if (salt != null && !this.validateSalt(salt, false)) {
        	// A non-valid salt value was encountered. Return a 400-BAD_REQUEST.
            return responseService.badRequest(responseContentType);
        }

        if (Assertion.assertNullAll(newDomainName, prefix, validFrom, validTo, validityTime, enforceStartDateValidity,
                enforceEndDateValidity, algorithm, alphabet, randomAlgorithmDesiredSize, 
                randomAlgorithmDesiredSuccessProbability, multiplePsnAllowed, consecVal, psnLength, paddingChar, 
                addCheckDigit, lengthIncludesCheckDigit, salt, saltLength, description)) {
            // An empty object was passed, so there is nothing to update.
            log.debug("The domain DTO passed by the user was empty. Nothing to update.");
            return responseService.unprocessableEntity(responseContentType);
        }

        // Get old domain object
        Domain old = domainDBAccessService.getDomainByName(oldDomainName);

        // Ensure that the old domain was found
        if (old == null) {
            // The old domain wasn't found
            log.error("The domain that should be updated wasn't found.");
            return responseService.notFound(responseContentType);
        }
        
        // Check if the new name is already in use
        String domainName;
        if (newDomainName != null && !newDomainName.isBlank()) {
        	Domain d = domainDBAccessService.getDomainByName(newDomainName);
        	domainName = d != null ? old.getName() : newDomainName.trim();
        } else {
        	domainName = old.getName();
        }

        // Check if the (new) name is valid in an URI. If not, tell the user and abort
        try {
            @SuppressWarnings("unused")
            URI location = new URI("/api/pseudonymization/domain?name=" + domainName);
        } catch (URISyntaxException e) {
            log.debug("The new domain name is not suitable to be used in a URI. Please choose another name.");
            return responseService.notAcceptable(responseContentType);
        }
        
        // Ensure a valid desired success probability, if the selected algorithm is of the RANDOM-family
        if (algorithm != null && algorithm.toUpperCase().startsWith("RANDOM") && randomAlgorithmDesiredSuccessProbability != null) {
        	
        	if (randomAlgorithmDesiredSuccessProbability > 1.0d) {
        		// The success probability was probably provided as a number between 0 and 100, so we transform it into [0,1] 
        		randomAlgorithmDesiredSuccessProbability /= 100.0d;
        	}
        	
        	if (randomAlgorithmDesiredSuccessProbability >= 1.0d) {
        		// The provided number is still too big, switch to default value
        		log.warn("The provided success probability for the pseudonymization process was too big. Will not be updated");
        		randomAlgorithmDesiredSuccessProbability = null;
        	} else if (randomAlgorithmDesiredSuccessProbability <= 0.0d) {
	    		// The provided number is zero or negative, switch to default value
	    		log.warn("The provided success probability for the pseudonymization process was too small. Will not be updated.");
	    		randomAlgorithmDesiredSuccessProbability = null;
	        }
        } else {
        	// Do not update and discard any provided information
        	randomAlgorithmDesiredSuccessProbability = null;
        }
        
        // Determine validTo date
        LocalDateTime vTo = null;
        if (validTo != null) {
            vTo = validTo.toLocalDateTime();
        } else if (validTo == null && validityTime != null) {
        	if (validFrom == null) {
        		vTo = Utility.plusValidityTime(old.getValidfrom(), validityTime);
        	} else {
        		vTo = Utility.plusValidityTime(validFrom.toLocalDateTime(), validityTime);
        	}
        }

        // Create the updated domain object
        Domain updated = new Domain();
        updated.setName(domainName);
        updated.setPrefix((prefix != null && !prefix.trim().equals("")) ? prefix.trim() : null);
        updated.setValidfrom((validFrom != null) ? validFrom.toLocalDateTime() : null);
        updated.setValidfrominherited((validFrom != null) ? false : null);
        updated.setValidto(vTo);
        updated.setValidtoinherited((vTo != null) ? false : null);
        updated.setEnforcestartdatevalidity(enforceStartDateValidity);
        updated.setEnforcestartdatevalidityinherited((enforceStartDateValidity != null) ? false : null);
        updated.setEnforceenddatevalidity(enforceEndDateValidity);
        updated.setEnforceenddatevalidityinherited((enforceEndDateValidity != null) ? false : null);
        updated.setAlgorithm((algorithm != null && !algorithm.trim().equals("")) ? algorithm.trim() : null);
        updated.setAlgorithminherited((algorithm != null && !algorithm.trim().equals("")) ? false : null);
        updated.setAlphabet((alphabet != null && !alphabet.trim().equals("")) ? alphabet.trim() : null);
        updated.setAlphabetinherited((alphabet != null) ? false : null);
        updated.setRandomalgorithmdesiredsize((randomAlgorithmDesiredSize != null) ? randomAlgorithmDesiredSize : null);
        updated.setRandomalgorithmdesiredsizeinherited((randomAlgorithmDesiredSize != null) ? false : null);
        updated.setRandomalgorithmdesiredsuccessprobability((randomAlgorithmDesiredSuccessProbability != null) ? randomAlgorithmDesiredSuccessProbability : null);
        updated.setRandomalgorithmdesiredsuccessprobabilityinherited((randomAlgorithmDesiredSuccessProbability != null) ? false : null);
        updated.setMultiplepsnallowed(multiplePsnAllowed);
        updated.setMultiplepsnallowedinherited((multiplePsnAllowed != null) ? false : null);
        updated.setConsecutivevaluecounter((consecVal != null && consecVal > 0L) ? consecVal : null);
        updated.setPseudonymlength((psnLength != null && psnLength > 0) ? psnLength : null);
        updated.setPseudonymlengthinherited((psnLength != null && psnLength > 0) ? false : null);
        updated.setPaddingcharacter((paddingChar != null && !Character.isWhitespace(paddingChar)) ? paddingChar.toString() : null);
        updated.setPaddingcharacterinherited((paddingChar != null && !Character.isWhitespace(paddingChar)) ? false : null);
        updated.setAddcheckdigit(addCheckDigit);
        updated.setAddcheckdigitinherited((addCheckDigit != null) ? false : null);
        updated.setLengthincludescheckdigit(lengthIncludesCheckDigit);
        updated.setLengthincludescheckdigitinherited((lengthIncludesCheckDigit != null) ? false : null);
        updated.setSalt(salt);
        updated.setSaltlength((saltLength != null && saltLength >= MINIMUM_SALT_LENGTH && saltLength <= MAXIMUM_SALT_LENGTH) ? saltLength : null);
        updated.setDescription(description);

        // Execute update
        Domain updatedDomain = domainDBAccessService.updateDomain(old, updated, performRecursiveChanges);
        if (updatedDomain != null) {
            // Success. Return a 200-OK status.
        	DomainDTO updatedDomDTO = new DomainDTO().assignPojoValues(updatedDomain);
            if (!authorizationService.hasDomainPermission(updatedDomain.getName(), "complete-view")) {
            	updatedDomDTO = updatedDomDTO.toReducedStandardView();
            }
            
            log.info("Successfully updated the domain \"" + domainName + "\".");
            return responseService.ok(responseContentType, updatedDomDTO);
        } else {
            // Updating the meta-information failed. Return an error 422-UNPROCESSABLE_ENTITY.
            log.error("Updating the domain \"" + domainName + "\" was unsuccessful.");
            return responseService.unprocessableEntity(responseContentType);
        }
    }

    /**
     * Method to update a domain with a reduced set of updatable attributes.
     * The attributes newPrefix, algorithm, consecVal, psnLength, and paddingChar are
     * only updatable when the domain is still empty.
     *
     * @param oldDomainName (required) the name of the domain that is to be updated
     * @param domainDTO (required) The domain object
     * @param responseContentType (optional) the response content type
     * @return 	<li> a <b>200-OK</b> status when the update was successful</li>
     * 			<li> a <b>404-NOT_FOUND</b> when the domain that should be
     * 				updated couldn't be found</li>
     * 			<li> a <b>422-UNPROCESSABLE_ENTITY</b> when updating the domain
     * 				failed</li>
     */
    @PutMapping("/domains")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #oldDomainName, 'domain:update')")
    @Audit
    public ResponseEntity<?> updateDomain(@RequestParam(name = "name", required = true) String oldDomainName,
                                          @RequestBody DomainDTO domainDTO,
                                          @RequestHeader(name = "accept", required = false) String responseContentType) {
        // Get old domain object
        Domain old = domainDBAccessService.getDomainByName(oldDomainName);

        String newName = domainDTO.getName();
        String prefix = domainDTO.getPrefix();
        Timestamp validFrom = domainDTO.getValidFrom() != null ? Timestamp.valueOf(domainDTO.getValidFrom()) : null;
        Timestamp validTo = domainDTO.getValidTo() != null ? Timestamp.valueOf(domainDTO.getValidTo()) : null;
        String validityTime = domainDTO.getValidityTime();
        String algorithm = domainDTO.getAlgorithm();
        Boolean multiplePsnAllowed = domainDTO.getMultiplePsnAllowed();
        String description = domainDTO.getDescription();

        if (Assertion.assertNullAll(newName, prefix, validFrom, validTo, validityTime, algorithm, multiplePsnAllowed, description)) {
            // An empty object was passed, so there is nothing to update.
            log.debug("The domain DTO passed by the user was empty. Nothing to update.");
            return responseService.unprocessableEntity(responseContentType);
        }

        // Ensure that the old domain was found
        if (old == null) {
            // The old domain wasn't found
            log.error("The domain that should be updated wasn't found.");
            return responseService.notFound(responseContentType);
        }
        
        // Check if the new name is already in use
        String domainName;
        if (newName != null && !newName.isBlank()) {
        	Domain d = domainDBAccessService.getDomainByName(newName);
        	domainName = d == null ? old.getName() : newName.trim();
        } else {
        	domainName = old.getName();
        }

        // Check if the (new) name is valid in an URI. If not, tell the user and abort
        try {
            @SuppressWarnings("unused")
            URI location = new URI("/api/pseudonymization/domain?name=" + domainName);
        } catch (URISyntaxException e) {
            log.debug("The new domain name is not suitable to be used in a URI. Please choose another name.");
            return responseService.notAcceptable(responseContentType);
        }

        // Determine validTo date
        LocalDateTime vTo = null;
        if (validTo != null) {
            vTo = validTo.toLocalDateTime();
        } else if (validTo == null && validityTime != null) {
        	if (validFrom == null) {
        		vTo = Utility.plusValidityTime(old.getValidfrom(), validityTime);
        	} else {
        		vTo = Utility.plusValidityTime(validFrom.toLocalDateTime(), validityTime);
        	}
        }

        // Create the updated domain object
        Domain updated = new Domain();
        updated.setName(domainName);
        updated.setValidfrom((validFrom != null) ? validFrom.toLocalDateTime() : null);
        updated.setValidfrominherited((validFrom != null) ? false : null);
        updated.setValidto(vTo);
        updated.setValidtoinherited((vTo != null) ? false : null);
        updated.setDescription(description);

        // Allow the following changes only when the domain does not contain any records yet
        if (domainDBAccessService.getAmountOfPseudonymsInDomain(old.getName()) == 0) {
            updated.setPrefix((prefix != null && !prefix.trim().equals("")) ? prefix.trim() : null);
            updated.setAlgorithm((algorithm != null && !algorithm.trim().equals("")) ? algorithm : null);
            updated.setAlgorithminherited((algorithm != null && !algorithm.trim().equals("")) ? false : null);
            updated.setMultiplepsnallowed(multiplePsnAllowed);
            updated.setMultiplepsnallowedinherited((multiplePsnAllowed != null) ? false : null);
        } else {
            log.info("Since the domain \"" + domainName + "\" isn't empty, updates of the prefix, the algorithm, "
                    + "the consecutive value, the pseudonym-length, or the padding character can't be processed and are ignored.");
        }

        // All other domain attributes are null and are therefore correctly left as they are

        // Execute update
        Domain updatedDomain = domainDBAccessService.updateDomain(old, updated, DEFAULT_PERFORM_RECURSIVE_CHANGES);
        if (updatedDomain != null) {
            // Success. Return a 200-OK status.
        	DomainDTO updatedDomDTO = new DomainDTO().assignPojoValues(updatedDomain);
            if (!authorizationService.hasDomainPermission(updatedDomDTO.getName(), "complete-view")) {
            	updatedDomDTO = updatedDomDTO.toReducedStandardView();
            }
            
            log.info("Successfully updated the domain \"" + updatedDomain.getName() + "\".");
            return responseService.ok(responseContentType, updatedDomDTO);
        } else {
            // Updating the meta-information failed. Return an error 422-UNPROCESSABLE_ENTITY.
            log.error("Updating the domain \"" + old.getName() + "\" was unsuccessful.");
            return responseService.unprocessableEntity(responseContentType);
        }
    }

    /**
     * This method updates the salt variable of a domain.
     *
     * @param domainName (required) the name of the domain for which the salt value should be updated
     * @param newSalt (required) the new salt value
     * @param allowEmpty (optional) determines whether or not the given salt is allowed to be empty
     * @param responseContentType (optional) the response content type
     * @return 	<li>a <b>200-OK</b> status when the update was successful</li>
     * 			<li>a <b>400-BAD_REQUEST</b> when the given salt value was
     * 				empty</li>
     * 			<li>a <b>404-NOT_FOUND</b> when no domain was found for the
     * 				given name</li>
     * 			<li>a <b>422-UNPROCESSABLE_ENTITY</b> when updating the salt
     * 				value failed</li>
     */
    @PutMapping("/domains/{domainName}/salt")
    @PreAuthorize("isAuthenticated() and @auth.hasDomainPermission(#root, #domainName, 'domain:update-salt')")
    @Audit
    public ResponseEntity<?> updateSalt(@PathVariable("domainName") String domainName,
                                        @RequestParam(name = "salt", required = true) String newSalt,
                                        @RequestParam(name = "allowEmpty", required = false, defaultValue = "false") Boolean allowEmpty,
                                        @RequestHeader(name = "accept", required = false) String responseContentType) {

    	// Validate the given salt value
        if (!this.validateSalt(newSalt, allowEmpty)) {
        	// A non-valid salt value was encountered. Return a 400-BAD_REQUEST.
            return responseService.badRequest(responseContentType);
        }

        // Retrieve old domain
        Domain old = domainDBAccessService.getDomainByName(domainName);

        // Check if the retrieved domain is null
        if (old == null) {
            // Couldn't find the domain that should be updated. Return a 404-NOT_FOUND.
            log.info("The domain for which the updated salt-value was given, couldn't be found.");
            return responseService.notFound(responseContentType);
        }

        // Create update-domain
        Domain updated = new Domain();
        updated.setSalt(newSalt);
        updated.setSaltlength((allowEmpty && newSalt.isBlank()) ? 0 : newSalt.length());

        // Execute update
        Domain updatedDomain = domainDBAccessService.updateDomain(old, updated, false);
        if (updatedDomain != null) {
            // Success. Return a 200-OK status.
        	DomainDTO updatedDomDTO = new DomainDTO().assignPojoValues(updatedDomain);
            if (!authorizationService.hasDomainPermission(updatedDomDTO.getName(), "complete-view")) {
            	updatedDomDTO = updatedDomDTO.toReducedStandardView();
            }
            
            log.info("Successfully updated the salt for the domain \"" + domainName + "\". It is now \"" + newSalt + "\".");
            return responseService.ok(responseContentType, updatedDomDTO);
        } else {
            // Updating the salt failed. Return an error 422-UNPROCESSABLE_ENTITY.
            log.error("Updating the salt for the domain \"" + domainName + "\" was unsuccessful.");
            return responseService.unprocessableEntity(responseContentType);
        }
    }
    
    /**
     * This method searches for domains by a free-text query.
     * 
     * The controller only returns domains for which the current user has read access.
     * If the user does not have the necessary permission to see the complete domain
     * configuration, the returned domain is reduced to its standard view.
     *
     * @param query (required) the search query used to find matching domains
     * @param responseContentType (optional) the response content type
     * @return  <li>a <b>200-OK</b> status and a possibly empty <b>list of domains</b>
     *              when the search was processed successfully</li>
     *          <li>a <b>400-BAD_REQUEST</b> status when the search query is empty</li>
     */
    @GetMapping("/domains")
    @PreAuthorize("isAuthenticated() and @auth.hasGlobalPermission(#root, 'domain:search')")
    @Audit
    public ResponseEntity<?> searchDomains(@RequestParam(name = "query", required = true) String query,
                                           @RequestHeader(name = "accept", required = false) String responseContentType) {
        List<DomainDTO> foundDomains = domainDBAccessService.searchDomains(query);
        List<DomainDTO> visibleDomains = new ArrayList<>();
        boolean canListAllDomains = authorizationService.hasGlobalPermission("domain:list-all");

        if (foundDomains == null) {
        	log.trace("The domain search failed.");
        	return responseService.internalServerError(responseContentType);
        } else if (foundDomains.size() == 0 || foundDomains.isEmpty()) {
        	log.trace("No domains were found for the given query.");
        	return responseService.ok(responseContentType, List.of());
        }
        
        // Iterate over all found domains and create a list of domains of those the user is allowed to view
        for (DomainDTO domainDTO : foundDomains) {
            String domainName = domainDTO.getName();

            // Check if the user is allowed to either read all domains or this one in particular
            if (!canListAllDomains && !authorizationService.hasDomainPermission(domainName, "domain:read")) {
            	// The user is not allowed to read this domain
                continue;
            }

            // Check if the user is allowed to read the full object or only a reduced version
            if (!authorizationService.hasDomainPermission(domainName, "complete-view")) {
                domainDTO = domainDTO.toReducedStandardView();
            }

            // Add the domain to list of allowed ones
            visibleDomains.add(domainDTO);

            // Early break if the list of search results should become too long
            if (visibleDomains.size() >= MAX_NUMBER_OF_SEARCH_RESULTS) {
                break;
            }
        }

        log.debug("Domain search for query \"" + query + "\" returned " + visibleDomains.size() + " result(s).");
        return responseService.ok(responseContentType, visibleDomains);
    }
	
	/**
	 * Calculate the length of pseudonyms needed so that the desired number of pseudonyms can be stored.
	 * 
	 * @param desiredSize the desired number of pseudonyms that can be generated for the domain in question
	 * @param desiredSuccessProbability the desired probability of successful pseudonym generation
	 * @param alphabet the alphabet used for generating the pseudonyms
	 * @return the minimal length the pseudonyms must have so that the desired amount can be generated
	 */
	private int calculatePseudonymLength(Long desiredSize, Double desiredSuccessProbability, String alphabet) {
		// Collect variables
		int m = DEFAULT_NUMBER_OF_RETRIES;
		double T = desiredSuccessProbability;
		long n = desiredSize;
		
		// Calculate the amount of possible pseudonyms needed so that the desired amount of pseudonyms will be 
		// reasonably probable created: (1-(1-((k-n)/k))^m)>T -->
		// k > n/((1-T)^(1/m)) --> k = ⌈n/((1-T)^(1/m))⌉
		double k = Math.ceil(n/Math.pow((1.0-T), (1.0/m)));
		
		// Calculate length of the pseudonyms: k = alphabet.length^psn_length
		double psnLength = Math.ceil(Math.log(k)/Math.log(alphabet.length()));
		
		return (int) psnLength;
	}
	
	/**
	 * Method to generate a random salt value of a given length.
	 * 
	 * @param saltLength the desired length of the salt value
	 * @return a randomly generated string of the desired length
	 */
	public static String generateSalt(int saltLength) {
		String saltAlphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_+";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(saltLength);

        for (int i = 0; i < saltLength; i++) {
            sb.append(saltAlphabet.charAt(rnd.nextInt(saltAlphabet.length())));
        }

        return sb.toString();
	}

    /**
     * Used to validate the salt value given during an update request.
     *
     * @param salt the salt as a string
     * @param allowEmpty this flag indicates whether an empty salt value is okay
     * @return {@code true} if the input is a valid salt value, {@code false} if not.
     */
    private boolean validateSalt(String salt, Boolean allowEmpty) {
        if (salt.isBlank() && !allowEmpty) {
            // The salt was empty when a non-empty salt was expected.
            log.debug("The new salt given by the user was empty.");
            return false;
        }

        if (salt.length() < MINIMUM_SALT_LENGTH && !allowEmpty) {
            // The new salt is too short.
            log.debug("The new salt given by the user was too short. It should be at least " + MINIMUM_SALT_LENGTH + " characters long.");
            return false;
        }

        if (salt.length() > MAXIMUM_SALT_LENGTH) {
            // The new salt is too long.
            log.debug("The new salt given by the user was too long. It should be no more than " + MAXIMUM_SALT_LENGTH + " characters long.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Helper method to transform a list of domain DTOs into a DTO-with-children-structure.
     * Start from a given domain and only build its subtree.
     * 
     * @param domains the list that should be transformed
     * @param rootDomainName the name of the domain that acts as the root of this (sub-)tree
     * @return a DTO that has its children as an inlined object
     */
    private DomainTreeDTO buildDomainTree(List<DomainDTO> domains, String rootDomainName) {
        if (domains == null || domains.isEmpty()) {
            return null;
        }

        // Create a node for each domain
        Map<String, DomainTreeDTO> nodesByName = new HashMap<>();
        for (DomainDTO dto : domains) {
            nodesByName.put(dto.getName(), DomainTreeDTO.builder().domain(dto).build());
        }

        // Find the root node (i.e. the one matching the given root domain name)
        DomainTreeDTO root = nodesByName.values().stream()
                .filter(node -> node.getDomain().getName() != null && node.getDomain().getName().equalsIgnoreCase(rootDomainName))
                .findFirst().orElse(null);

        if (root == null) {
            return null;
        }

        // Insert child-domains into their parents using superDomainName
        for (DomainDTO dto : domains) {
            DomainTreeDTO node = nodesByName.get(dto.getName());

            // Skip root
            if (node == root) {
                continue;
            }

            // Check if the current domain is a child of one of the others
            if (dto.getSuperDomainName() != null) {
                // Current domain has a parent, search it in the given list
            	DomainTreeDTO parent = nodesByName.get(dto.getSuperDomainName());
                if (parent != null) {
                	if (parent.getChildren() == null) {
                		parent.setChildren(new ArrayList<>());
                	}
                    parent.getChildren().add(node);
                }
            }
        }

        return root;
    }
    
    /**
     * Helper method to transform a list of domain DTOs into a DTO-with-children-structure.
     * Keep all parent-less domains as their own subtrees.
     * 
     * @param domains the list that should be transformed
     * @return a List of DTOs where each has its children as an inlined object
     */
    private List<DomainTreeDTO> buildDomainTree(List<DomainDTO> domains) {
        if (domains == null || domains.isEmpty()) {
            return null;
        }

        // Create a node for each domain
        Map<String, DomainTreeDTO> nodesByName = new HashMap<>();
        for (DomainDTO dto : domains) {
            nodesByName.put(dto.getName(), DomainTreeDTO.builder().domain(dto).build());
        }

        // Start by assuming every node is a root, we’ll remove children later
        Set<DomainTreeDTO> roots = new HashSet<>(nodesByName.values());

        // Insert child-domains into their parents using superDomainName
        for (DomainDTO dto : domains) {
            String parentName = dto.getSuperDomainName();

            // Domain has no parent, nothing to insert
            if (parentName == null || parentName.isBlank()) {
                continue;
            }

            // Domain has a parent --> insert it into its parent
            DomainTreeDTO child = nodesByName.get(dto.getName());
            DomainTreeDTO parent = nodesByName.get(parentName);

            // If the parent is in the list, then attach the child and remove it from roots
            if (parent != null) {
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                
                parent.getChildren().add(child);
                roots.remove(child);
            }
        }

        return new ArrayList<>(roots);
    }
}
