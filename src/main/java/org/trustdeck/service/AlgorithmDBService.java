/*
 * Trust Deck Services
 * Copyright 2024-2025 Armin Müller
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

package org.trustdeck.service;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.MappingException;
import org.jooq.exception.TooManyRowsException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.trustdeck.exception.UnexpectedResultSizeException;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import org.trustdeck.jooq.generated.tables.records.AlgorithmRecord;
import org.trustdeck.utils.Assertion;
import org.trustdeck.utils.Utility;

import lombok.extern.slf4j.Slf4j;

import static org.trustdeck.jooq.generated.Tables.ALGORITHM;
import static org.trustdeck.jooq.generated.Tables.DOMAIN;
import static org.trustdeck.jooq.generated.Keys.ALGORITHM_CONFIGURATION_KEY;

import java.security.SecureRandom;
import java.util.List;

/**
 * This class encapsulates the database access for algorithm objects.
 */
@Slf4j
@Service
public class AlgorithmDBService {
    
	/** References a jOOQ configuration object that configures jOOQ's behavior when executing queries. */
    @Autowired
	private DSLContext dsl;

    /** Enables access to default values. */
    @Autowired
    private DefaultProperties defaults;

    /** The minimum length a salt value given by the user is allowed to be. */
	private static final int MINIMUM_SALT_LENGTH = 4;

    /** The maximum length a salt value given by the user is allowed to be. */
	private static final int MAXIMUM_SALT_LENGTH = 256;
	
	/**
	 * Calculate the length the pseudonyms need to be so that the desired number of pseudonyms can be stored.
	 * 
	 * @param desiredSize the desired number of pseudonyms that can be generated for the domain in question
	 * @param desiredSuccessProbability the desired probability of successful pseudonym generation
	 * @param alphabet the alphabet used for generating the pseudonyms
	 * @return the minimal length the pseudonyms must have so that the desired amount can be generated
	 */
	private int calculatePseudonymLength(Long desiredSize, Double desiredSuccessProbability, String alphabet) {
		// Collect variables
		int m = defaults.getAlgorithm().getNumberOfRetries();
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
     * Creates an algorithm object in the database.
     * 
     * @param algorithm the algorithm POJO to store
     * @return the ID of the newly created algorithm, or {@code null} if an error occurred.
     */
    @Transactional
    public Integer createAlgorithm(Algorithm algorithm) {
		AlgorithmRecord algoRecord = normalizeAlgorithm(algorithm);
		Integer existingId = getAlgorithmIdIfExistsInDatabase(algoRecord);
		if (existingId != null) {
			log.debug("Reusing an existing algorithm with name \"" + algoRecord.getName() + "\".");
			return existingId;
		}

		AlgorithmRecord created = dsl.insertInto(ALGORITHM)
				.set(algoRecord)
				.onConflictOnConstraint(ALGORITHM_CONFIGURATION_KEY)
				.doNothing()
				.returning(ALGORITHM.ID)
				.fetchOne();
		if (created != null) {
			log.debug("Created a new algorithm with name \"" + algoRecord.getName() + "\".");
			return created.getId();
		}

		// Another transaction inserted this exact unique configuration after our lookup
		existingId = getAlgorithmIdIfExistsInDatabase(algoRecord);
		if (existingId != null) {
			log.debug("Reusing an existing algorithm with name " + algoRecord.getName() + " after a concurrent insert.");
			return existingId;
		}

		throw new DataAccessException("Algorithm insert conflicted but the matching algorithm could not be retrieved.");
	}

    /**
     * Normalizes an algorithm configuration into a database record.
     * Missing or invalid values are replaced with configured defaults. Invalid
     * salts are replaced with a newly generated salt. For random algorithms, the
     * pseudonym length is calculated from the normalized random-generation
     * configuration unless a longer explicit length was supplied.
     *
     * @param algorithm the requested algorithm configuration
     * @return a fully populated algorithm record ready for lookup or persistence
     */
	private AlgorithmRecord normalizeAlgorithm(Algorithm algorithm) {
		int saltLength = (algorithm.getSaltLength() >= MINIMUM_SALT_LENGTH && algorithm.getSaltLength() <= MAXIMUM_SALT_LENGTH) ? algorithm.getSaltLength() : defaults.getAlgorithm().getSaltLength();
		
		AlgorithmRecord algoRecord = dsl.newRecord(ALGORITHM);
        algoRecord.setName(algorithm.getName() != null ? algorithm.getName() : defaults.getAlgorithm().getName());
        algoRecord.setAlphabet(Utility.generateAlphabet(algoRecord.getName(), algorithm.getAlphabet()));
		algoRecord.setRandomAlgorithmDesiredSize(algorithm.getRandomAlgorithmDesiredSize() != null && algorithm.getRandomAlgorithmDesiredSize() > 1 ? algorithm.getRandomAlgorithmDesiredSize() : defaults.getAlgorithm().getRandomDesiredSize());
        algoRecord.setRandomAlgorithmDesiredSuccessProbability(algorithm.getRandomAlgorithmDesiredSuccessProbability() != null && algorithm.getRandomAlgorithmDesiredSuccessProbability() > 0 ? algorithm.getRandomAlgorithmDesiredSuccessProbability() : defaults.getAlgorithm().getRandomDesiredSuccessProbability());
        algoRecord.setConsecutiveValueCounter(algorithm.getConsecutiveValueCounter() != null && algorithm.getConsecutiveValueCounter() > 0 ? algorithm.getConsecutiveValueCounter() : defaults.getAlgorithm().getConsecutiveValueCounter());
		algoRecord.setPseudonymLength(algorithm.getPseudonymLength() != null && algorithm.getPseudonymLength() >= 4  ? algorithm.getPseudonymLength() : defaults.getAlgorithm().getPseudonymLength());
		algoRecord.setPaddingCharacter(algorithm.getPaddingCharacter() != null ? algorithm.getPaddingCharacter() : defaults.getAlgorithm().getPaddingCharacter());
		algoRecord.setAddCheckDigit(algorithm.getAddCheckDigit() != null ? algorithm.getAddCheckDigit() : defaults.getAlgorithm().isAddCheckDigit());
		algoRecord.setLengthIncludesCheckDigit(algorithm.getLengthIncludesCheckDigit() != null ? algorithm.getLengthIncludesCheckDigit() : defaults.getAlgorithm().isLengthIncludesCheckDigit());
		algoRecord.setSalt(sanitizeOrGenerateSalt(algorithm.getSalt(), saltLength));
		algoRecord.setSaltLength(saltLength);
		
		// Calculate pseudonym length, if a randomness algorithm is used
		if (algoRecord.getName().trim().toUpperCase().startsWith("RANDOM")) {
			// Check if the parameters for the algorithm are the default ones 
			if (algoRecord.getRandomAlgorithmDesiredSize() == defaults.getAlgorithm().getRandomDesiredSize()
					&& algoRecord.getRandomAlgorithmDesiredSuccessProbability() == defaults.getAlgorithm().getRandomDesiredSuccessProbability()
					&& defaults.getAlgorithm().getRandomAlphabet().equals(algoRecord.getAlphabet())) {
				// Defaults are used --> use default length
				algoRecord.setPseudonymLength(defaults.getAlgorithm().getRandomPseudonymLength());
			} else {
				// Not all parameters are defaults --> calculate the length
				int calculatedLength = calculatePseudonymLength(algoRecord.getRandomAlgorithmDesiredSize(), algoRecord.getRandomAlgorithmDesiredSuccessProbability(), algoRecord.getAlphabet());
				
				// Use the calculated length if it is longer than the user-given one
				if (algorithm.getPseudonymLength() != null && calculatedLength < algorithm.getPseudonymLength()) {
					algoRecord.setPseudonymLength(algorithm.getPseudonymLength());
				} else {
					log.debug("Used automatically calculated pseudonym length.");
					algoRecord.setPseudonymLength(calculatedLength);
				}
			}
		}

		return algoRecord;
    }
    
	/**
     * Creates an algorithm object in the database if it does not already exist.
     * 
     * @param algorithm the algorithm POJO to store
     * @return the ID of the (already) created algorithm, or {@code null} if an error occurred.
     */
    @Transactional
    public Integer createOrGetAlgorithm(Algorithm algorithm) {
        return createAlgorithm(algorithm);
    }
    
    /**
     * Deletes the algorithm object based on its unique ID.
     * 
     * @param ID the ID corresponding to the algorithm object of interest
     * @return {@code true} if the deletion was successful, {@code false} otherwise
     * @throws UnexpectedResultSizeException whenever the deletion would not exactly affect one entry
     */
    @Transactional
    public boolean deleteAlgorithm(int ID) throws UnexpectedResultSizeException {
    	if (ID <= 0) {
    		log.debug("Cannot delete the algorithm from the database with an ID <= 0 (given ID: " + ID + ").");
    		return false;
    	}
    	
    	// Check if any person object still uses this algorithm object
    	if (isAlgorithmInUse(ID)) { 
    		log.debug("The algorithm is still in use and is therefore not deleted.");
    		return false;
    	}
    	
    	int deletedEntries = dsl.deleteFrom(ALGORITHM).where(ALGORITHM.ID.eq(ID)).execute();
    	if (deletedEntries != 1) {
    		// Deletion would not affect exactly one entry -> abort
    		log.debug("Deletion of algorithm with ID " + ID + " would not affect exactly one entry. Aborting.");
    		throw new UnexpectedResultSizeException(1, deletedEntries);
    	}
    	
    	log.debug("Successfully deleted algorithm with ID: " + ID);
    	return true;
    }
    
    /**
     * Retrieves the algorithm object based on its unique ID.
     * 
     * @param ID the ID corresponding to the algorithm object of interest
     * @return the algorithm POJO, or {@code null} if nothing could be found
     */
    @Transactional
    public Algorithm getAlgorithmByID(int ID) {
    	if (ID <= 0) {
    		log.debug("Cannot retrieve the algorithm from the database with an ID <= 0 (given ID: " + ID + ").");
    		return null;
    	}
    	
    	Algorithm algo = null;
        try {
	        // Execute the query
	        algo = dsl.selectFrom(ALGORITHM)
	                  .where(ALGORITHM.ID.eq(ID))
	                  .fetchOneInto(Algorithm.class);
        } catch (TooManyRowsException e) {
        	log.debug("Found more than one algorithm.");
    	} catch (MappingException f) {
        	log.debug("Could not map the algorithm search result into the Algorithm-POJO.");
        } catch (DataAccessException g) {
        	log.debug("Searching for the algorithm in the database failed: " + g.getMessage());
        }
        
        // Check if the search was successful
        if (algo == null) {
        	log.debug("No single algorithm could be found with the given ID.");
        }
    	
    	return algo;
    }
    
    /**
     * Method to retrieve an algorithm-object from the database.
     * If there is more than one result, this method will return 
     * an arbitrary one that fits the search criteria.
     * 
     * @param name the name of the algorithm
     * @param alphabet the alphabet used in the algorithm
     * @param randomAlgoDesiredSize the desired number of possible pseudonyms in the output space of a randomness-based algorithm
     * @param randomAlgoDesiredSuccessProbability the desired success probability for a randomness-based algorithm
     * @param pseudonymLength the length of the pseudonyms
     * @param paddingChar the character used to pad short pseudonyms to the desired length
     * @param addCheckDigit whether or not to add a check digit to the pseudonym
     * @param lengthIncludesCheckDigit whether or not the desired length includes the check digit
     * @param salt the salt used for pseudonymization
     * @param saltLength the length of the salt value
     * @return the algorithm object that was found when searching for the given attributes, or {@code null} when nothing was found
     */
    @Transactional
    public Algorithm getAlgorithmByValues(String name, String alphabet, Long randomAlgoDesiredSize, 
    		Double randomAlgoDesiredSuccessProbability, Integer pseudonymLength, String paddingChar, 
    		Boolean addCheckDigit, Boolean lengthIncludesCheckDigit, String salt, Integer saltLength) {
    	// Build the query based on the given non-null attributes
        Condition condition = DSL.trueCondition();
        if (name != null) {
            condition = condition.and(ALGORITHM.NAME.eq(name));
        }
        if (alphabet != null) {
            condition = condition.and(ALGORITHM.ALPHABET.eq(alphabet));
        }
        if (randomAlgoDesiredSize != null && randomAlgoDesiredSize > 1) {
            condition = condition.and(ALGORITHM.RANDOM_ALGORITHM_DESIRED_SIZE.eq(randomAlgoDesiredSize));
        }
        if (randomAlgoDesiredSuccessProbability != null && randomAlgoDesiredSuccessProbability > 0) {
            condition = condition.and(ALGORITHM.RANDOM_ALGORITHM_DESIRED_SUCCESS_PROBABILITY.eq(randomAlgoDesiredSuccessProbability));
        }
        if (pseudonymLength != null && pseudonymLength > 0) {
            condition = condition.and(ALGORITHM.PSEUDONYM_LENGTH.eq(pseudonymLength));
        }
        if (paddingChar != null && !paddingChar.isBlank()) {
            condition = condition.and(ALGORITHM.PADDING_CHARACTER.eq(paddingChar));
        }
        if (addCheckDigit != null) {
            condition = condition.and(ALGORITHM.ADD_CHECK_DIGIT.eq(addCheckDigit));
        }
        if (lengthIncludesCheckDigit != null) {
            condition = condition.and(ALGORITHM.LENGTH_INCLUDES_CHECK_DIGIT.eq(lengthIncludesCheckDigit));
        }
        if (salt != null && !salt.isBlank()) {
            condition = condition.and(ALGORITHM.SALT.eq(salt));
        }
        if (saltLength != null && saltLength > 0) {
            condition = condition.and(ALGORITHM.SALT_LENGTH.eq(saltLength));
        }

        List<Algorithm> algos = null;
        try {
	        // Execute the query
	        algos = dsl.selectFrom(ALGORITHM)
	                  .where(condition)
	                  .fetchInto(Algorithm.class);
        } catch (MappingException e) {
        	log.debug("Could not map the algorithm search result into the Algorithm-POJO.");
        } catch (DataAccessException f) {
        	log.debug("Searching for the algorithm in the database failed: " + f.getMessage());
        }
        
        // Check if the search was successful
        if (algos == null || algos.size() == 0) {
        	log.debug("No algorithm could be found with the given set of attributes.");
        	return null;
        } else if (algos.size() > 1) {
        	log.debug("More than one algorithm object was found. The first result will be used.");
        }
        
        return algos.getFirst();
    }
    
    /**
     * Check if the algorithm is already in the database.
     * 
     * @param algorithm the algorithm object to check
     * @return the id of the algorithm in the database that is equivalent to the given one, or
     * {@code null} if nothing was found or an error occurred.
     */
    @Transactional
    private Integer getAlgorithmIdIfExistsInDatabase(AlgorithmRecord algorithm) {
        return dsl.select(ALGORITHM.ID)
                .from(ALGORITHM)
                .where(ALGORITHM.NAME.eq(algorithm.getName()))
                .and(ALGORITHM.ALPHABET.eq(algorithm.getAlphabet()))
                .and(ALGORITHM.RANDOM_ALGORITHM_DESIRED_SIZE.eq(algorithm.getRandomAlgorithmDesiredSize()))
                .and(ALGORITHM.RANDOM_ALGORITHM_DESIRED_SUCCESS_PROBABILITY.eq(algorithm.getRandomAlgorithmDesiredSuccessProbability()))
                .and(ALGORITHM.PSEUDONYM_LENGTH.eq(algorithm.getPseudonymLength()))
                .and(ALGORITHM.PADDING_CHARACTER.eq(algorithm.getPaddingCharacter()))
                .and(ALGORITHM.ADD_CHECK_DIGIT.eq(algorithm.getAddCheckDigit()))
                .and(ALGORITHM.LENGTH_INCLUDES_CHECK_DIGIT.eq(algorithm.getLengthIncludesCheckDigit()))
                .and(ALGORITHM.SALT.eq(algorithm.getSalt()))
                .and(ALGORITHM.SALT_LENGTH.eq(algorithm.getSaltLength()))
                .fetchOne(ALGORITHM.ID);
    }
    
    /**
     * Helper method that checks if an algorithm is still used anywhere 
     * by searching for references to its ID.
     * 
     * @param algorithmID the algorithm's ID
     * @return {@code true} when the algorithm ID is referenced anywhere, {@code false} otherwise
     */
    @Transactional
    public boolean isAlgorithmInUse(int algorithmID) {
		try {
			return dsl.fetchExists(dsl.selectOne().from(DOMAIN).where(DOMAIN.ALGORITHM_ID.eq(algorithmID)));
		} catch (DataAccessException e) {
			log.debug("Searching for algorithm references in the database failed.", e);
			return false;
		}
    }
    
    /**
     * Helper method that determines if the given salt value is valid or not.
     * @param salt the salt value to check
     * @return {@code true} if the salt value is not empty and within the right length-constraints, {@code false} otherwise
     */
    private boolean isSaltValueValid(String salt) {
    	return Assertion.isNotNullOrEmpty(salt) && salt.length() >= MINIMUM_SALT_LENGTH && salt.length() <= MAXIMUM_SALT_LENGTH;
    }
    
    /**
     * Returns a valid salt value by either returning the user-given one if it's valid or by generating a new one.
     * 
     * @param salt the user-given salt or {@code null} if not given
     * @param saltLength the length for the salt value
     * @return a valid salt value
     */
    private String sanitizeOrGenerateSalt(String salt, int saltLength) {
    	// Create a salt if not already given, or if its not a valid value
        if (!isSaltValueValid(salt)) {
        	String saltAlphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_+";
	        SecureRandom rnd = new SecureRandom();
	        StringBuilder sb = new StringBuilder(saltLength);
	
	        for (int i = 0; i < saltLength; i++) {
	            sb.append(saltAlphabet.charAt(rnd.nextInt(saltAlphabet.length())));
	        }
	        
	        log.debug("The given salt was null, too long, or too short. A new one was generated.");
	        return sb.toString();
        } else {
        	return salt;
        }
    }
    
	/**
     * Updates an algorithm object in the database.
     * Null values in the updateAlgorithm object indicate that this specific
     * attribute is to be kept as before.
     * 
     * @param oldAlgorithm the old algorithm POJO that should be updated
     * @param updatedAlgorithm the algorithm POJO that contains all the updated values
     * @return the ID of the updated algorithm, or {@code null} if an error occurred
     */
    @Transactional
    public Integer updateAlgorithm(Algorithm oldAlgorithm, Algorithm updatedAlgorithm) {
		// Fetch old algorithm object
    	AlgorithmRecord algorithmRecord = null;
    	try {
			algorithmRecord = dsl.fetchOne(ALGORITHM, ALGORITHM.ID.eq(oldAlgorithm.getId()));
		} catch (DataAccessException e) {
			log.debug("Fetching the algorithm record that should be updated failed (ID: " + oldAlgorithm.getId() + ").");
			return null;
		}
		
		// Check if the old record was found
		if (algorithmRecord == null) {
			log.debug("The algorithm object that should be updated was not found (ID: " + oldAlgorithm.getId() + ").");
			return null;
		}
		
		// Sanitize the given values and update the attributes
        algorithmRecord.setName(updatedAlgorithm.getName() != null && !updatedAlgorithm.getName().isBlank() ? updatedAlgorithm.getName() : oldAlgorithm.getName());
        algorithmRecord.setAlphabet(updatedAlgorithm.getAlphabet() != null && !updatedAlgorithm.getName().isBlank() ? updatedAlgorithm.getAlphabet() : oldAlgorithm.getAlphabet());
		algorithmRecord.setRandomAlgorithmDesiredSize(updatedAlgorithm.getRandomAlgorithmDesiredSize() != null && updatedAlgorithm.getRandomAlgorithmDesiredSize() >= 1 ? updatedAlgorithm.getRandomAlgorithmDesiredSize() : oldAlgorithm.getRandomAlgorithmDesiredSize());
        algorithmRecord.setRandomAlgorithmDesiredSuccessProbability(updatedAlgorithm.getRandomAlgorithmDesiredSuccessProbability() != null && updatedAlgorithm.getRandomAlgorithmDesiredSuccessProbability() > 0 ? updatedAlgorithm.getRandomAlgorithmDesiredSuccessProbability() : oldAlgorithm.getRandomAlgorithmDesiredSuccessProbability());
        algorithmRecord.setConsecutiveValueCounter(updatedAlgorithm.getConsecutiveValueCounter() != null && updatedAlgorithm.getConsecutiveValueCounter() >= 1 ? updatedAlgorithm.getConsecutiveValueCounter() : oldAlgorithm.getConsecutiveValueCounter());
		algorithmRecord.setPseudonymLength(updatedAlgorithm.getPseudonymLength() != null && updatedAlgorithm.getPseudonymLength() >= 1 ? updatedAlgorithm.getPseudonymLength() : oldAlgorithm.getPseudonymLength());
		algorithmRecord.setPaddingCharacter(updatedAlgorithm.getPaddingCharacter() != null && !updatedAlgorithm.getPaddingCharacter().isBlank() ? updatedAlgorithm.getPaddingCharacter() : oldAlgorithm.getPaddingCharacter());
		algorithmRecord.setAddCheckDigit(updatedAlgorithm.getAddCheckDigit() != null ? updatedAlgorithm.getAddCheckDigit() : oldAlgorithm.getAddCheckDigit());
		algorithmRecord.setLengthIncludesCheckDigit(updatedAlgorithm.getLengthIncludesCheckDigit() != null ? updatedAlgorithm.getLengthIncludesCheckDigit() : oldAlgorithm.getLengthIncludesCheckDigit());
		algorithmRecord.setSalt(isSaltValueValid(updatedAlgorithm.getSalt()) ? updatedAlgorithm.getSalt() : oldAlgorithm.getSalt());
		algorithmRecord.setSaltLength((updatedAlgorithm.getSaltLength() != null && updatedAlgorithm.getSaltLength() >= MINIMUM_SALT_LENGTH && updatedAlgorithm.getSaltLength() <= MAXIMUM_SALT_LENGTH) ? updatedAlgorithm.getSaltLength() : oldAlgorithm.getSaltLength());
	
    	// Store and determine success
        int wasStored = algorithmRecord.update();
        log.debug("Updating the algorithm object \"" + algorithmRecord.getName() + "\" (ID: " + algorithmRecord.getId() + ") " + ((wasStored == 1) ? "succeeded." : "failed."));
        
        // Return the algorithm ID when successful
        return wasStored == 1 ? algorithmRecord.getId() : null;
    }
    
    /**
     * Method to update only the counter value of an algorithm.
     * 
     * @param counter the new counter value
     * @param algorithmID the ID of the algorithm-object for which the counter should be updated
     * @return {@code true}, when the update was successful, {@code false} otherwise.
     */
    @Transactional
    public boolean updateCounter(Long counter, int algorithmID) {
    	// Fetch the existing algorithm record
        AlgorithmRecord algorithmRecord;
        try {
            algorithmRecord = dsl.fetchOne(ALGORITHM, ALGORITHM.ID.eq(algorithmID));
        } catch (DataAccessException e) {
            log.debug("Retrieving the algorithm record failed (ID: " + algorithmID + ").");
            return false;
        }

        // Check if the algorithm record was found
        if (algorithmRecord == null) {
            log.debug("The algorithm record was not found (ID: " + algorithmID + ").");
            return false;
        }

        // Validate and update consecutive value counter
        if (counter != null && counter >= 1) {
            algorithmRecord.setConsecutiveValueCounter(counter);
        } else {
            log.debug("Invalid consecutive value counter provided: " + counter);
            return false;
        }

        // Store and determine success
        int wasStored = algorithmRecord.update();
        log.debug("Updating consecutive value counter for algorithm \"" + algorithmRecord.getName() + "\" (ID: " + algorithmRecord.getId() + ") " + ((wasStored == 1) ? "succeeded." : "failed."));

        // Return the algorithm ID when successful
        return wasStored == 1 ? true : false;
    }
}
