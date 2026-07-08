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

package org.trustdeck.service;

import lombok.extern.slf4j.Slf4j;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.Field;
import org.jooq.Insert;
import org.jooq.Row2;
import org.jooq.UpdateConditionStep;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.MappingException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.dto.PseudonymUpdateDTO;
import org.trustdeck.exception.UnexpectedResultSizeException;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.jooq.generated.tables.pojos.Pseudonym;
import org.trustdeck.jooq.generated.tables.records.PseudonymRecord;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.utils.Assertion;

import static org.trustdeck.jooq.generated.Tables.PSEUDONYM;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is used to encapsulate all methods needed to access the database for handling pseudonym-records.
 *
 * @author Armin Müller and Eric Wündisch
 */
@Service
@Slf4j
public class PseudonymDBAccessService {

    /** Enables the access to the domain specific database access methods. */
    @Autowired
    private DomainDBAccessService domainDBAccessService;

    /** References a jOOQ configuration object that configures jOOQ's behavior when executing queries. */
    @Autowired
    private DSLContext dsl;

    /** Represents the duplication status of a requested insertion of an identifier and idType combination into the database. */
    public static final String INSERTION_DUPLICATE_IDENTIFIER = "duplicate identifier";
    
    /** Represents the duplication status of a requested insertion of a pseudonym into the database. */
    public static final String INSERTION_DUPLICATE_PSEUDONYM = "duplicate pseudonym";

    /** Represents an erroneous insertion of a record into the database. */
    public static final String INSERTION_ERROR = "error";

    /** Represents a successful insertion of a record into the database. */
    public static final String INSERTION_SUCCESS = "success";

    /**
     * Method to insert multiple pseudonyms at once in a batch. Duplicates will be ignored.
     *
     * @param pseudonyms a list of pseudonyms to insert into the database
     * @param domainId the ID of the domain in which the pseudonyms should be created
     * @param multiplePsnAllowed whether or not multiple pseudonyms per id &amp; idType combination are allowed
     * @return {@code INSERTION_SUCCESS} when the batch insertion was successful, {@code INSERTION_ERROR} otherwise
     */
    @Transactional
    public List<String> createPseudonyms(List<PseudonymDTO> pseudonyms, int domainId, boolean multiplePsnAllowed) {
    	// Check if there is something to do
    	if (pseudonyms == null || pseudonyms.isEmpty()) {
            return List.of();
        }
    	
    	int n = pseudonyms.size();
    	List<String> results = new ArrayList<>(n);
    	
    	// Prefill the result list with nulls
    	for (int i = 0; i < n; i++) {
    		results.add(null);
    	}
    	
    	// Prefill the identifier-idType-duplicate-check list (only used if !multiplePsnAllowed)
        List<Boolean> idExistsFlags = new ArrayList<>(Collections.nCopies(n, Boolean.FALSE));
        
        // Prefill the psn-duplicate-check list
        List<Boolean> psnExistsFlags = new ArrayList<>(Collections.nCopies(n, Boolean.FALSE));
    	
    	try {
    		// If multiple pseudonyms per id&idType-combination are not allowed, then check 
        	// if the id&idType-combination is already in the database
            if (!multiplePsnAllowed) {
            	// Create a list of identifier-idType-pairs that we can use to query the database
            	List<Row2<String, String>> idRows = new ArrayList<>(n);
	            for (PseudonymDTO dto : pseudonyms) {
	                idRows.add(DSL.row(dto.getIdentifierItem().getIdentifier(), dto.getIdentifierItem().getIdType()));
	            }
	            
	            // Query the database and see if we find any of the user-provided identifier-idType-pairs already in there
	            Map<Row2<String, String>, Boolean> existingIdMap = dsl.select(PSEUDONYM.IDENTIFIER, PSEUDONYM.IDTYPE)
	            	       .from(PSEUDONYM)
	            	       .where(DSL.row(PSEUDONYM.IDENTIFIER, PSEUDONYM.IDTYPE).in(idRows))
	            	       .and(PSEUDONYM.DOMAINID.eq(domainId))
	            	       .fetchMap(r -> DSL.row(r.get(PSEUDONYM.IDENTIFIER), r.get(PSEUDONYM.IDTYPE)), r -> Boolean.TRUE);

            	// Build List<Boolean> in the original order and mark duplicates
	            for (int i = 0; i < n; i++) {
	                boolean idIsDuplicate = existingIdMap.containsKey(idRows.get(i));
	                idExistsFlags.set(i, idIsDuplicate);

            		if (idIsDuplicate) {
            	        results.set(i, INSERTION_DUPLICATE_IDENTIFIER);
            	    }
            	}
            }
            
            // Check for duplicated pseudonyms (e.g. when using randomness-based algorithms and only a few unused pseudonyms are left)
            List<String> psns = new ArrayList<>(n);
            for (PseudonymDTO dto : pseudonyms) {
                psns.add(dto.getPsn());
            }

            // Fetch existing pseudonyms in the domain of interest
            Set<String> existingPsns = new HashSet<>(dsl.select(PSEUDONYM.PSEUDONYM_)
                      .from(PSEUDONYM)
                      .where(PSEUDONYM.DOMAINID.eq(domainId))
                      .and(PSEUDONYM.PSEUDONYM_.in(psns))
                      .fetch(PSEUDONYM.PSEUDONYM_));

            // Fill psnExistsFlags and mark results
            for (int i = 0; i < n; i++) {
                boolean psnIsDuplicate = existingPsns.contains(psns.get(i));
                psnExistsFlags.set(i, psnIsDuplicate);

                if (psnIsDuplicate && results.get(i) == null) {
                    results.set(i, INSERTION_DUPLICATE_PSEUDONYM);
                }
            }
            
            // Try inserting all (non-duplicate) pseudonym records
            // Prepare the batch insert
            List<Insert<PseudonymRecord>> inserts = new ArrayList<>(n);
            List<Integer> insertIndices = new ArrayList<>(n); // For mapping batch result index -> pseudonym index
            int skippedDuplicates = 0;

            for (int i = 0; i < n; i++) {
                // Skip if duplicate and duplicates are not allowed
                if ((!multiplePsnAllowed && idExistsFlags.get(i)) || psnExistsFlags.get(i)) {
                    skippedDuplicates++;
                    continue;
                }

                // Prepare and add the insert query
                PseudonymDTO dto = pseudonyms.get(i);
                inserts.add(dsl.insertInto(PSEUDONYM)
                    .set(PSEUDONYM.IDENTIFIER, dto.getIdentifierItem().getIdentifier())
                    .set(PSEUDONYM.IDTYPE, dto.getIdentifierItem().getIdType())
                    .set(PSEUDONYM.PSEUDONYM_, dto.getPsn())
                    .set(PSEUDONYM.VALIDFROM, dto.getValidFrom())
                    .set(PSEUDONYM.VALIDFROMINHERITED, dto.getValidFromInherited())
                    .set(PSEUDONYM.VALIDTO, dto.getValidTo())
                    .set(PSEUDONYM.VALIDTOINHERITED, dto.getValidToInherited())
                    .set(PSEUDONYM.DOMAINID, domainId));
                
                // Track prepared indices
                insertIndices.add(i);
            }

            // If there is nothing to insert, we’re done
            if (inserts.isEmpty()) {
                log.trace("No pseudonyms to insert (" + skippedDuplicates + " duplicates skipped).");
                return results;
            }
            
            // Execute the batch 
            int[] batchResult = dsl.batch(inserts).execute();
            
            // Evaluate the results
            int inserted = 0;
            int ignored = skippedDuplicates;

            for (int j = 0; j < batchResult.length; j++) {
                int individualResult = batchResult[j];
                int originalIndex = insertIndices.get(j);

                if (individualResult == 1) {
                    inserted++;
                    results.set(originalIndex, INSERTION_SUCCESS);
                } else if (individualResult == 0) {
                    ignored++;
                    if (results.get(originalIndex) == null) {
                        results.set(originalIndex, INSERTION_ERROR);
                    }
                } else {
                    // Unexpected result size: abort
                    throw new UnexpectedResultSizeException(1, individualResult);
                }
            }

            log.trace("Inserted " + inserted + " pseudonym(s).");
            log.trace("Ignored " + ignored + " pseudonym(s) (including " + skippedDuplicates + " duplicates).");
            log.debug("Successfully inserted " + inserted + " out of " + n + " pseudonym" + (n == 1 ? "" : "s") + " into the database.");

            return results;
        } catch (Exception e) {
        	// Force the outcome of this method to be a roll-back instead of committing the transaction
        	TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        	
            log.error("Couldn't insert the batch of " + n + " pseudonyms into the database: " + e.getMessage() + "\n");
            return null;
        }
    }
    
    /**
     * Method to retrieve pseudonym-records. This method actually only 
     * evaluates the given information and delegates the getting-process.
     * 
     * @param domainName the name of the domain to search in
     * @param identifierItem the identifierItem to search for
     * @param psn the pseudonym to search for
     * @return the pseudonymization data element referring to the given information, or
     * {@code null} when nothing is found or an error occurs
     */
    @Transactional
    public List<PseudonymDTO> getPseudonym(String domainName, IdentifierItem identifierItem, String psn) {
    	// Decide on which get-method to use depending on the given information
    	if (identifierItem != null && identifierItem.isNotNullNorEmpty() && psn == null) {
    		// Retrieve pseudonym through the identifier
    		return getPseudonymFromIdentifier(domainName, identifierItem);
    	} else if ((identifierItem == null || identifierItem.isNullOrEmpty()) && psn != null) {
    		// Retrieve pseudonym through the pseudonym
    		PseudonymDTO p = getPseudonymFromPsn(domainName, psn);
    		return p == null ? null : List.of(p);
    	} else if (identifierItem != null && Assertion.isNotNullOrEmpty(identifierItem.getIdentifier(), identifierItem.getIdType(), psn)) {
    		// The identifier, idType, and pseudonym were given, check that all attributes are correct
    		List<PseudonymDTO> pseudonyms = getPseudonymFromIdentifier(domainName, identifierItem);
    		
    		if (pseudonyms == null) {
    			log.debug("Nothing was found for the given parameter configuration.");
        		return null;
    		}
    		
    		for (PseudonymDTO p : pseudonyms) {
    			if (p.getIdentifierItem().equals(identifierItem) && p.getPsn().equals(psn)) {
        			return List.of(p);
        		}
        	}
    		
    		log.debug("None of the records that were found matched the given pseudonym.");
    		return null;
    	} else {
    		log.debug("Invalid configuration of parameters. Either id and idType, the psn, or all three are needed.");
    		return null;
    	}
    }

    /**
     * Retrieves a pseudonym-record from a specified identifier.
     *
     * @param domainName the name of the domain to search in
     * @param identifierItem the identifierItem to search for
     * @return the pseudonymization data element referring to the given identifier, or
     * {@code null} when nothing is found or an error occurs
     */
    @Transactional
    public List<PseudonymDTO> getPseudonymFromIdentifier(String domainName, IdentifierItem identifierItem) {
        try {
            // Check that the domain name is valid
        	Domain d = domainDBAccessService.getDomainByName(domainName);
            if (d == null) {
                log.debug("The domain to search the pseudonym in, wasn't found.");
                return null;
            }
        	
        	// Build and execute the query
            List<Pseudonym> pseudonyms = dsl.selectFrom(PSEUDONYM)
                    .where(PSEUDONYM.IDENTIFIER.equal(identifierItem.getIdentifier()))
                    .and(PSEUDONYM.IDTYPE.equal(identifierItem.getIdType()))
                    .and(PSEUDONYM.DOMAINID.eq(d.getId()))
                    .fetchInto(Pseudonym.class);

            // Check if a pseudonym-record was found or not
            if (pseudonyms == null || pseudonyms.size() == 0) {
                log.debug("There is no element in the database matching the given identifier and type.");
                return null;
            } else {
            	List<PseudonymDTO> dtos = pseudonyms.stream()
                        .map(p -> new PseudonymDTO().assignPojoValues(p))
                        .collect(Collectors.toList());

                log.trace("Successfully retrieved " + dtos.size() + " pseudonym" + (dtos.size() == 1 ? "." : "s."));
                return dtos;
            }
        } catch (Exception e) {
        	// Force the outcome of this method to be a roll-back instead of committing the transaction
        	TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        	
            log.error("Couldn't query the database: " + e.getClass() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves a pseudonym-record from a specified pseudonym.
     *
     * @param domainName the name of the domain to search in
     * @param psn the pseudonym to search for
     * @return the pseudonymization data element referring to the given pseudonym, or
     * {@code null} when nothing is found or an error occurs
     */
    @Transactional
    public PseudonymDTO getPseudonymFromPsn(String domainName, String psn) {
        try {
        	// Check that the domain name is valid
            Domain d = domainDBAccessService.getDomainByName(domainName);
            if (d == null) {
                log.debug("The domain to search the pseudonym in, wasn't found.");
                return null;
            }
        	
        	// Build and execute the query
            Pseudonym pseudonym = dsl.selectFrom(PSEUDONYM)
                    .where(PSEUDONYM.PSEUDONYM_.equal(psn))
                    .and(PSEUDONYM.DOMAINID.eq(d.getId()))
                    .fetchOneInto(Pseudonym.class);
            
            // Check if a pseudonym-record was found or not
            if (pseudonym == null) {
                log.debug("There is no element in the database matching the given pseudonym-value.");
                return null;
            } else {
            	log.debug("Successfully retrieved a pseudonym.");
                return new PseudonymDTO().assignPojoValues(pseudonym);
            }
        } catch (Exception e) {
        	// Force the outcome of this method to be a roll-back instead of committing the transaction
        	TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        	
            log.error("Couldn't query the database: " + e.getClass() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Method to update multiple pseudonyms at once in a single batch. Not found pseudonyms will be ignored.
     * Can also update a single pseudonym (given as a list).
     *
     * @param pseudonymUpdates a list of pseudonyms that are to be updated in the database
     * @return List of {@code true} when the individual update was successful, {@code false} otherwise
     */
    @Transactional
    public List<Boolean> updatePseudonyms(List<PseudonymUpdateDTO> pseudonymUpdates) {
        // Check if there is something to do
    	if (pseudonymUpdates == null || pseudonymUpdates.isEmpty()) {
            return List.of();
        }
    	
    	int n = pseudonymUpdates.size();
    	List<Boolean> updateSuccess = new ArrayList<>(n);
    	
    	// Prefill the result list with nulls
    	for (int i = 0; i < n; i++) {
    		updateSuccess.add(null);
    	}
    	
    	try {
            List<UpdateConditionStep<PseudonymRecord>> updates = new ArrayList<>();
            List<Integer> originalIndex = new ArrayList<>();
            int updated = 0;
            int ignored = 0;
        	
        	// Create a list of update statements
            for (int j = 0; j < n; j++) {
            	PseudonymUpdateDTO p = pseudonymUpdates.get(j);
            	
            	// Check if the pseudonym record to be updated exists
            	List<PseudonymDTO> found = getPseudonym(p.getOldDomain().getName(), p.getOldIdentifierItem(), p.getOldPsn());
            	if (found == null || found.size() == 0 || found.getFirst() == null) {
            		log.debug("The pseudonym record was not found in the database, so this update is ignored.");
            		ignored++;
                    updateSuccess.set(j, false);
                    continue;
                } else if (found.size() != 1) {
                	log.debug("Found too many matching pseudonym records, so this update is ignored.");
            		ignored++;
                    updateSuccess.set(j, false);
                    continue;
                }
            	
            	PseudonymDTO old = found.getFirst();
                
                // Sanitize update values
                String identifier = (p.getNewIdentifierItem() != null && Assertion.isNotNullOrEmpty(p.getNewIdentifierItem().getIdentifier())) ? p.getNewIdentifierItem().getIdentifier() : old.getIdentifierItem().getIdentifier();
                String idType = (p.getNewIdentifierItem() != null && Assertion.isNotNullOrEmpty(p.getNewIdentifierItem().getIdType())) ? p.getNewIdentifierItem().getIdType() : old.getIdentifierItem().getIdType();
                String psn = Assertion.isNotNullOrEmpty(p.getNewPsn()) ? p.getNewPsn() : old.getPsn();
                LocalDateTime validFrom = (p.getValidFrom() != null) ? p.getValidFrom() : old.getValidFrom();
                Boolean validFromInh = (p.getValidFromInherited() != null) ? p.getValidFromInherited() : old.getValidFromInherited();
                LocalDateTime validTo = (p.getValidTo() != null) ? p.getValidTo() : old.getValidTo();
                Boolean validToInh = (p.getValidToInherited() != null) ? p.getValidToInherited() : old.getValidToInherited();
                int domainId = (p.getNewDomain() != null) ? p.getNewDomain().getId() : p.getOldDomain().getId();
            	
            	// Add to batch update
            	updates.add(dsl.update(PSEUDONYM)
                        .set(PSEUDONYM.IDENTIFIER, identifier)
                        .set(PSEUDONYM.IDTYPE, idType)
                        .set(PSEUDONYM.PSEUDONYM_, psn)
                        .set(PSEUDONYM.VALIDFROM, validFrom)
                        .set(PSEUDONYM.VALIDFROMINHERITED, validFromInh)
                        .set(PSEUDONYM.VALIDTO, validTo)
                        .set(PSEUDONYM.VALIDTOINHERITED, validToInh)
                        .set(PSEUDONYM.DOMAINID, domainId)
                        .where(PSEUDONYM.IDENTIFIER.eq(old.getIdentifierItem().getIdentifier()))
            	        .and(PSEUDONYM.IDTYPE.eq(old.getIdentifierItem().getIdType()))
            	        .and(PSEUDONYM.PSEUDONYM_.eq(old.getPsn()))
            	        .and(PSEUDONYM.DOMAINID.eq(p.getOldDomain().getId())));
            	
            	// Store the index of the original list
            	originalIndex.add(j);
			}
            
        	// Batch the update statements and execute the batch
            int[] result = dsl.batch(updates).execute();

            // Process the result
            for (int i = 0; i < result.length; i++) {
                if (result[i] == 1) {
                    // Successful update of exactly one record
                    updated++;
                    updateSuccess.set(originalIndex.get(i), true);
                } else if (result[i] == 0) {
                    // Update didn't affect any record (e.g. because the record to be updated wasn't found)
                    ignored++;
                    updateSuccess.set(originalIndex.get(i), false);
                } else {
                    // Unexpected result, abort the complete transaction by throwing an exception
                    throw new UnexpectedResultSizeException(1, result[i]);
                }
            }

            // Log information about the batch processing
            log.trace("Updated " + updated + " pseudonym(s).");
            log.trace("Ignored " + ignored + " pseudonym(s).");

            // Return the list of success or failure
            log.debug("Successfully updated " + updated + " out of " + pseudonymUpdates.size() + " pseudonym" + (updates.size() == 1 ? "" : "s") + " in the database.");
            return updateSuccess;
        } catch (UnexpectedResultSizeException e) {
        	// Force the outcome of this method to be a roll-back instead of committing the transaction
        	TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        	
            log.error("The update would have affected an unexpected number of records (" + e.getActual() + ") "
                    + "when it should have only affected " + e.getExpected() + " record(s). The batch update "
                    + "was therefore rolled back.");
            return null;
        } catch (Exception f) {
        	// Force the outcome of this method to be a roll-back instead of committing the transaction
        	TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        	
        	log.error("Couldn't update the batch of pseudonyms in the database: " + f.getClass() + ": " + f.getMessage());
            return null;
        }
    }

    /**
     * Method to delete multiple pseudonyms at once in a batch. Not found pseudonyms will be ignored.
     *
     * @param pseudonyms a list of pseudonyms that are to be deleted from the database
     * @param domainId the ID of the domain in which the pseudonym should be updated
     * @return {@code true} when the batch deletion was successful, {@code false} otherwise
     */
    @Transactional
    public List<Boolean> deletePseudonyms(List<PseudonymDTO> pseudonyms, int domainId) {
    	List<Boolean> deleteSuccess = new ArrayList<>();
    	
    	if (pseudonyms == null || pseudonyms.isEmpty()) {
            return List.of();
        }
    	
    	try {
        	// Create a list of delete statements
            List<DeleteConditionStep<PseudonymRecord>> deletions = new ArrayList<>(pseudonyms.size());
            for (PseudonymDTO p : pseudonyms) {
                deletions.add(dsl.delete(PSEUDONYM)
                        .where(PSEUDONYM.IDENTIFIER.equal(p.getIdentifierItem().getIdentifier()))
                        .and(PSEUDONYM.IDTYPE.equal(p.getIdentifierItem().getIdType()))
                        .and(PSEUDONYM.PSEUDONYM_.equal(p.getPsn()))
                        .and(PSEUDONYM.DOMAINID.equal(domainId)));
            }

            // Batch the delete statements and execute the batch
            int[] result = dsl.batch(deletions).execute();

            // Process the result
            int deleted = 0;
            int ignored = 0;

            for (int i = 0; i < result.length; i++) {
                if (result[i] == 1) {
                    // Successful deletion of exactly one record
                    deleted++;
                    deleteSuccess.add(true);
                } else if (result[i] == 0) {
                    // Deletion didn't affect any record (e.g. because the record to be deleted wasn't found)
                    ignored++;
                    deleteSuccess.add(false);
                } else {
                    // Unexpected result, abort the complete transaction by throwing an exception
                    throw new UnexpectedResultSizeException(1, result[i]);
                }
            }

            // Log information about the batch processing
            log.trace("Deleted " + deleted + " pseudonym(s).");
            log.trace("Ignored " + ignored + " pseudonym(s).");

            // Return the list of deletion success or failure
            log.debug("Successfully deleted " + deleted + " out of " + deletions.size() + " pseudonyms in the database.");
            return deleteSuccess;
        } catch (UnexpectedResultSizeException e) {
        	// Force the outcome of this method to be a roll-back instead of committing the transaction
        	TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        	
            log.error("The deletion would have affected an unexpected number of records (" + e.getActual() + ") "
                    + "when it should have only affected " + e.getExpected() + " record(s). The deletion process "
                    + "was therefore rolled back.");
            return null;
        } catch (Exception f) {
        	// Force the outcome of this method to be a roll-back instead of committing the transaction
        	TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        	
            log.error("Couldn't delete a batch of pseudonyms from the database: " + f.getClass() + ": " + f.getMessage());
            return null;
        }
    }
    
    /**
     * Method to search for pseudonyms.
     * This search supports searching in the identifier, idType, and psn-value.
     * 
     * @param query the search query
     * @param domainId the id of the domain in which to search for
     * @return a list of pseudonyms that match the search query
     */
    @Transactional
    public List<PseudonymDTO> searchPseudonyms(String query, int domainId) {
    	if (Assertion.isNullOrEmpty(query)) {
            log.debug("Search query is empty.");
            return null;
        }
    	
    	// Allow wildcard-search with '*'
        if (query.trim().equals("*")) {
            List<Pseudonym> results;
            try {
                // Return all currently valid pseudonyms in the given domain scope
                Field<LocalDateTime> now = DSL.currentLocalDateTime();

                results = dsl.selectFrom(PSEUDONYM)
                        .where(PSEUDONYM.DOMAINID.eq(domainId))
                        .and(PSEUDONYM.VALIDFROM.le(now))
                        .and(PSEUDONYM.VALIDTO.gt(now))
                        .orderBy(PSEUDONYM.IDENTIFIER.asc(), PSEUDONYM.IDTYPE.asc())
                        .limit(500)
                        .fetchInto(Pseudonym.class);
            } catch (MappingException e) {
                log.debug("Could not map pseudonym search result.", e);
                return null;
            } catch (DataAccessException e) {
                log.debug("Searching pseudonyms failed.", e);
                return null;
            }

            // Return all currently valid pseudonyms
            return results.stream().map(p -> new PseudonymDTO().assignPojoValues(p)).toList();
        }

        // Split query-parts on whitespaces; every part should match at least one column
        String[] parts = query.trim().split("\\s+");
        
        // Built the search conditions statement
        Condition condition = DSL.trueCondition();
        for (String part : parts) {
            String pattern = "%" + part + "%";

            // Search across identifier, idType, and psn
            Condition partCond = PSEUDONYM.IDENTIFIER.likeIgnoreCase(pattern)
                .or(PSEUDONYM.IDTYPE.likeIgnoreCase(pattern))
                .or(PSEUDONYM.PSEUDONYM_.likeIgnoreCase(pattern));

            // AND-connect all search parts
            condition = condition.and(partCond);
        }

        // Exclude pseudonyms which are not valid anymore
        Field<LocalDateTime> now = DSL.currentLocalDateTime();
        condition = condition.and(PSEUDONYM.VALIDFROM.le(now)).and(PSEUDONYM.VALIDTO.gt(now));
        
        // Only search in a domain scope
        condition = condition.and(PSEUDONYM.DOMAINID.eq(domainId));

        // Execute the search
        List<Pseudonym> results;
        try {
            results = dsl.selectFrom(PSEUDONYM)
                      .where(condition)
                      .orderBy(PSEUDONYM.IDENTIFIER.asc(), PSEUDONYM.IDTYPE.asc())
                      .fetchInto(Pseudonym.class);
        } catch (MappingException e) {
            log.debug("Could not map pseudonym search result.", e);
            return null;
        } catch (DataAccessException e) {
            log.debug("Searching pseudonyms failed.", e);
            return null;
        }

        // Evaluate the search results
        if (results == null || results.isEmpty()) {
            log.debug("No pseudonym matched the query \"" + query + "\".");
            return null;
        }

        // Return the found types
        return results.stream().map(row -> new PseudonymDTO().assignPojoValues(row)).toList();
    }
}
