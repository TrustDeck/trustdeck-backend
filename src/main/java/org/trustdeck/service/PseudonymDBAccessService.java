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
import org.jooq.Row2;
import org.jooq.Row4;
import org.jooq.Row8;
import org.jooq.Result;
import org.jooq.UpdateConditionStep;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.MappingException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.dto.PseudonymUpdateDTO;
import org.trustdeck.exception.UnexpectedResultSizeException;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.jooq.generated.tables.pojos.Pseudonym;
import org.trustdeck.jooq.generated.tables.records.PseudonymRecord;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.utils.Assertion;

import static org.trustdeck.jooq.generated.Keys.PSEUDONYM_IDENTIFIER_IDTYPE_DOMAINID_PSEUDONYM_KEY;
import static org.trustdeck.jooq.generated.Tables.PSEUDONYM;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    /** Maximum rows per insert statement to stay below PostgreSQL's 65,535 bind-parameter limit. */
    private static final int INSERT_BATCH_SIZE = 5_000;

    /**
     * Inserts multiple pseudonyms in one conflict-safe PostgreSQL statement.
     *
     * The pre-insert lookup preserves the established identifier and pseudonym duplicate
     * precedence. Candidates are deduplicated only for persistence; the returned status list
     * still has one entry for every input position. The insert targets only the generated
     * four-column uniqueness constraint, so an unrelated database failure is not silently
     * treated as an expected duplicate. Rows skipped by a concurrent insert are resolved by
     * one set-based reload after {@code RETURNING}.
     *
     * @param pseudonyms the normalized pseudonyms to insert, in request order
     * @param domainId the ID of the domain in which the pseudonyms should be created
     * @param multiplePsnAllowed whether multiple pseudonyms per identifier and type are allowed
     * @return one explicit insertion status for each input pseudonym; actual database failures
     *         return {@link #INSERTION_ERROR} for every input position after rollback
     */
    @Transactional
    public List<String> createPseudonyms(List<PseudonymDTO> pseudonyms, int domainId, boolean multiplePsnAllowed) {
        if (pseudonyms == null || pseudonyms.isEmpty()) {
            return List.of();
        }

        int n = pseudonyms.size();
        // Every input position gets a deliberate value, this prevents a failed persistence path
        // from being mistaken for a successful duplicate and keeps callers null-safe
        List<String> results = new ArrayList<>(Collections.nCopies(n, INSERTION_ERROR));

        try {
        	// Build order-matched identifier and pseudonym lists
            List<Row2<String, String>> identifierRows = new ArrayList<>(n);
            List<String> pseudonymValues = new ArrayList<>(n);
            for (PseudonymDTO dto : pseudonyms) {
                identifierRows.add(DSL.row(dto.getIdentifierItem().getIdentifier(), dto.getIdentifierItem().getIdType()));
                pseudonymValues.add(dto.getPsn());
            }

            // Set-based lookups retain the old duplicate precedence without introducing one
            // query per request element. Chunks keep the identifier and pseudonym predicates
            // below PostgreSQL's bind-parameter limit for the endpoint's largest batch.
            List<PseudonymRecord> existingRecords = new ArrayList<>();
            for (int queryStart = 0; queryStart < n; queryStart += INSERT_BATCH_SIZE) {
                int queryEnd = Math.min(queryStart + INSERT_BATCH_SIZE, n);
                Condition existingCondition = PSEUDONYM.PSEUDONYM_.in(pseudonymValues.subList(queryStart, queryEnd));
                
                if (!multiplePsnAllowed) {
                    existingCondition = existingCondition.or(
                            DSL.row(PSEUDONYM.IDENTIFIER, PSEUDONYM.IDTYPE).in(identifierRows.subList(queryStart, queryEnd)));
                }
                
                existingRecords.addAll(dsl.selectFrom(PSEUDONYM)
                        .where(PSEUDONYM.DOMAINID.eq(domainId))
                        .and(existingCondition)
                        .fetch());
            }

            Set<Row2<String, String>> existingIdentifiers = new HashSet<>();
            Set<String> existingPsns = new HashSet<>();
            for (PseudonymRecord record : existingRecords) {
                existingIdentifiers.add(DSL.row(record.getIdentifier(), record.getIdtype()));
                existingPsns.add(record.getPseudonym());
            }

            List<PseudonymDTO> candidates = new ArrayList<>(n);
            List<Integer> candidateOriginalIndices = new ArrayList<>(n);
            Map<PseudonymPersistenceKey, Integer> candidatePositions = new HashMap<>();

            // Deduplicate only the persistence candidates. candidateOriginalIndices and the
            // explicit result list retain a mapping for every original request position.
            for (int i = 0; i < n; i++) {
                PseudonymDTO dto = pseudonyms.get(i);
                Row2<String, String> identifier = DSL.row(dto.getIdentifierItem().getIdentifier(), dto.getIdentifierItem().getIdType());
                PseudonymPersistenceKey key = new PseudonymPersistenceKey(
                        dto.getIdentifierItem().getIdentifier(), dto.getIdentifierItem().getIdType(), domainId, dto.getPsn());

                if (!multiplePsnAllowed && existingIdentifiers.contains(identifier)) {
                    results.set(i, INSERTION_DUPLICATE_IDENTIFIER);
                } else if (existingPsns.contains(dto.getPsn())) {
                    results.set(i, INSERTION_DUPLICATE_PSEUDONYM);
                } else if (candidatePositions.containsKey(key)) {
                    // A repeated key in one request is handled without sending duplicate rows
                    // to PostgreSQL. Keep the established identifier-first classification.
                    results.set(i, multiplePsnAllowed ? INSERTION_DUPLICATE_PSEUDONYM : INSERTION_DUPLICATE_IDENTIFIER);
                } else {
                    candidatePositions.put(key, candidates.size());
                    candidates.add(dto);
                    candidateOriginalIndices.add(i);
                }
            }

            if (candidates.isEmpty()) {
                log.trace("No pseudonyms to insert (all " + n + " request positions were duplicates).");
                return results;
            }

            // Build one multi-row INSERT. The targeted ON CONFLICT clause closes the concurrent
            // check-then-insert race for this exact constraint without suppressing other errors.
            Set<PseudonymPersistenceKey> insertedKeys = new HashSet<>();
            for (int chunkStart = 0; chunkStart < candidates.size(); chunkStart += INSERT_BATCH_SIZE) {
                int chunkEnd = Math.min(chunkStart + INSERT_BATCH_SIZE, candidates.size());
                List<Row8<String, String, String, LocalDateTime, Boolean, LocalDateTime, Boolean, Integer>> valueRows = new ArrayList<>(chunkEnd - chunkStart);
                
                for (int i = chunkStart; i < chunkEnd; i++) {
                    PseudonymDTO dto = candidates.get(i);
                    valueRows.add(DSL.row(dto.getIdentifierItem().getIdentifier(), dto.getIdentifierItem().getIdType(),
                            dto.getPsn(), dto.getValidFrom(), dto.getValidFromInherited(), dto.getValidTo(),
                            dto.getValidToInherited(), domainId));
                }

                // Eight bound values per row require chunking for the endpoint's larger
                // supported batches; each chunk remains one true multi-row statement.
                Result<PseudonymRecord> returnedRecords = dsl.insertInto(PSEUDONYM, PSEUDONYM.IDENTIFIER, 
                			PSEUDONYM.IDTYPE, PSEUDONYM.PSEUDONYM_, PSEUDONYM.VALIDFROM, PSEUDONYM.VALIDFROMINHERITED, 
                			PSEUDONYM.VALIDTO, PSEUDONYM.VALIDTOINHERITED, PSEUDONYM.DOMAINID)
                        .valuesOfRows(valueRows)
                        .onConflictOnConstraint(PSEUDONYM_IDENTIFIER_IDTYPE_DOMAINID_PSEUDONYM_KEY)
                        .doNothing()
                        .returning(PSEUDONYM.IDENTIFIER, PSEUDONYM.IDTYPE, PSEUDONYM.PSEUDONYM_, PSEUDONYM.DOMAINID)
                        .fetch();

                for (PseudonymRecord record : returnedRecords) {
                    PseudonymPersistenceKey key = new PseudonymPersistenceKey(
                            record.getIdentifier(), record.getIdtype(), record.getDomainid(), record.getPseudonym());
                    
                    Integer candidatePosition = candidatePositions.get(key);
                    if (candidatePosition != null) {
                        insertedKeys.add(key);
                        results.set(candidateOriginalIndices.get(candidatePosition), INSERTION_SUCCESS);
                    }
                }
            }

            List<PseudonymPersistenceKey> unresolvedKeys = new ArrayList<>();
            for (PseudonymPersistenceKey key : candidatePositions.keySet()) {
                if (!insertedKeys.contains(key)) {
                    unresolvedKeys.add(key);
                }
            }

            if (!unresolvedKeys.isEmpty()) {
                // RETURNING omits conflict-skipped rows. Reload all of them in one query so a
                // concurrent winner is classified as a duplicate rather than an insertion error.
                List<PseudonymRecord> resolvedRecords = new ArrayList<>();
                for (int queryStart = 0; queryStart < unresolvedKeys.size(); queryStart += INSERT_BATCH_SIZE) {
                    int queryEnd = Math.min(queryStart + INSERT_BATCH_SIZE, unresolvedKeys.size());
                    
                    List<Row4<String, String, Integer, String>> unresolvedRows = unresolvedKeys.subList(queryStart, queryEnd).stream()
                            .map(key -> DSL.row(key.identifier(), key.idType(), key.domainId(), key.pseudonym())).toList();
                    
                    resolvedRecords.addAll(dsl.selectFrom(PSEUDONYM)
                            .where(DSL.row(PSEUDONYM.IDENTIFIER, PSEUDONYM.IDTYPE, PSEUDONYM.DOMAINID, PSEUDONYM.PSEUDONYM_).in(unresolvedRows))
                            .fetch());
                }
                
                Set<PseudonymPersistenceKey> resolvedKeys = new HashSet<>();
                for (PseudonymRecord record : resolvedRecords) {
                    resolvedKeys.add(new PseudonymPersistenceKey(
                            record.getIdentifier(), record.getIdtype(), record.getDomainid(), record.getPseudonym()));
                }

                // Reconstruct skipped results through the original-index mapping. No result
                // cardinality or request ordering is changed by persistence deduplication.
                for (PseudonymPersistenceKey key : unresolvedKeys) {
                    Integer candidatePosition = candidatePositions.get(key);
                    if (candidatePosition != null && resolvedKeys.contains(key)) {
                        results.set(candidateOriginalIndices.get(candidatePosition), multiplePsnAllowed
                                ? INSERTION_DUPLICATE_PSEUDONYM
                                : INSERTION_DUPLICATE_IDENTIFIER);
                    }
                }
            }

            log.debug("Successfully inserted " + results.stream().filter(INSERTION_SUCCESS::equals).count()
                    + " out of " + n + " pseudonym" + (n == 1 ? "" : "s") + " into the database.");
            return results;
        } catch (Exception e) {
            // Any genuine database failure rolls back the transaction and remains an explicit error
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            
            log.error("Couldn't insert the batch of " + n + " pseudonyms into the database: " + e.getClass() + ": " + e.getMessage());
            return Collections.nCopies(n, INSERTION_ERROR);
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

    /**
     * Identifies one persisted pseudonym row using every column of the targeted unique key.
     *
     * @param identifier the identifier value
     * @param idType the identifier type
     * @param domainId the domain identifier
     * @param pseudonym the pseudonym value
     */
    private record PseudonymPersistenceKey(String identifier, String idType, int domainId, String pseudonym) {
        
    	/**
         * Creates a key for one row in the pseudonym uniqueness constraint.
         */
        private PseudonymPersistenceKey {}
    }
}
