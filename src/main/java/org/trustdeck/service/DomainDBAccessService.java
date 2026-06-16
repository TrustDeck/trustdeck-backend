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

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.impl.DSL;
import org.jooq.JoinType;
import org.jooq.Record;
import org.jooq.SelectQuery;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.MappingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.trustdeck.algorithms.PathFinder;
import org.trustdeck.dto.DomainDTO;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.dto.PseudonymUpdateDTO;
import org.trustdeck.exception.DomainNotFoundException;
import org.trustdeck.exception.DuplicateDomainException;
import org.trustdeck.exception.FailedChildDomainDeletionException;
import org.trustdeck.exception.FailedChildDomainUpdateException;
import org.trustdeck.exception.FailedPseudonymDeletionException;
import org.trustdeck.exception.FailedPseudonymUpdateException;
import org.trustdeck.exception.PermissionManagementException;
import org.trustdeck.exception.UnexpectedResultSizeException;
import org.trustdeck.jooq.generated.tables.daos.DomainDao;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.jooq.generated.tables.pojos.Pseudonym;
import org.trustdeck.jooq.generated.tables.records.PseudonymRecord;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.utils.Assertion;
import org.trustdeck.utils.Utility.Pair;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.trustdeck.jooq.generated.Tables.DOMAIN;
import static org.trustdeck.jooq.generated.Tables.PSEUDONYM;

/**
 * This class is used to encapsulate all methods needed to access the database for handling domains.
 *
 * @author Armin Müller and Eric Wündisch
 */
@Service
@Slf4j
public class DomainDBAccessService {

	/** Representation for a domain that is not found during counting the records in it. */
	public static final Integer DOMAIN_NOT_FOUND = -1;

    /** References a jOOQ configuration object that configures jOOQ's behavior when executing queries. */
    @Autowired
    private DSLContext dsl;

    /** Enables the access to the domain specific database access methods. */
    @Autowired
    private PseudonymDBAccessService pseudonymDBAccessService;
    
    /** Enables access to the permission grants database methods. */
    @Autowired
    private PermissionDBService permissionDBService;

    /** Represents the duplication status of a requested insertion of a domain into the database. */
    public static final String INSERTION_DUPLICATE = "duplicate";

    /** Represents an erroneous insertion of a domain into the database. */
    public static final String INSERTION_ERROR = "error";

    /** Represents a successful insertion of a domain into the database. */
    public static final String INSERTION_SUCCESS = "success";

    /**
     * Method to retrieve the domain data access object.
     *
     * @return the domain DAO
     */
    private DomainDao getDomainDao() {
    	// Attaches DAO to the current transactional context
    	return new DomainDao(dsl.configuration());
    }

    /**
     * Method to retrieve a domain from the database given its name.
     *
     * @param domainName the name of the domain you want to get
     * @return the retrieved domain as a jOOQ Domain object, or {@code null} if nothing was found
     */
    @Transactional
    public Domain getDomainByName(String domainName) {
    	// Check if all the necessary arguments are available
    	if (Assertion.isNullOrEmpty(domainName.trim())) {
    		log.trace("The given domain name is null or empty.");
    		return null;
    	}
    	
    	// Build and execute the query
    	List<Domain> domains = null;
    	try {
			domains = dsl.selectFrom(DOMAIN)
					.where(DOMAIN.NAME.equalIgnoreCase(domainName.trim()))
					.fetchInto(Domain.class);
        } catch (MappingException e) {
        	log.debug("Could not map the domain search result into the Domain-POJO.", e);
        	return null;
        } catch (DataAccessException f) {
        	log.debug("Searching for the domain in the database failed.", f);
        	return null;
        }
    	
    	// Check if the search was successful
    	if (domains == null || domains.size() == 0) {
    		log.debug("No domain for name \"" + domainName + "\" found.");
            return null;
    	} else if (domains.size() > 1) {
        	log.debug("Too many domains with name \"" + domainName + "\" were found.");
        	return null;
        }

    	log.trace("Successfully queried the database for the domain using the name (" + domainName + ").");
        return domains.getFirst();
    }

    /**
     * This method retrieves a domain for a given ID
     *
     * @param domainID the id of the domain you want to get
     * @return the retrieved domain as a jOOQ Domain object, or {@code null} if nothing was found
     */
    public Domain getDomainByID(int domainID) {
    	try {
            Domain domain = this.dsl.transactionResult(configuration -> {
                // Get domain
                return this.getDomainDao().fetchOneById(domainID);

                // Implicit transaction commit here
            });

            // Check result
            if (domain == null) {
            	return null;
            } else {
            	return domain;
            }
        } catch (Exception e) {
            log.error("Couldn't retrieve the domain with ID " + domainID + " from the database: ", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Helper method that creates the subtree of a domain starting from the given domain.
     *
     * @param domainName a domain name from which the search should start
     * @return a list of domain DTOs which represent all domains of the subtree starting with the given domain
     */
    @Transactional
    public List<DomainDTO> getSubtreeFromDomainName(String domainName) {
    	if (domainName == null) {
    		log.debug("Could not search for a subtree since the domain ID was null.");
    		return List.of();
    	}
    	
    	/*
	        WITH RECURSIVE tree_structure AS (
	    	    SELECT * FROM DOMAIN WHERE name = :start_name
	    	    UNION ALL
	    	    SELECT domain.* FROM DOMAIN
	    	    INNER JOIN tree_structure ON domain.superdomainid = tree_structure.id
	    	)
	    	SELECT * FROM tree_structure;
        */
    	
    	// Build the query
        CommonTableExpression<Record> treeStructure = name("tree_structure").as(
        		select(DOMAIN.asterisk())
                .from(DOMAIN)
                .where(DOMAIN.NAME.equalIgnoreCase(domainName))
                .unionAll(
                		select(DOMAIN.asterisk())
                        .from(DOMAIN)
                        .innerJoin(name("tree_structure"))
                        .on(DOMAIN.SUPERDOMAINID.eq(field(name("tree_structure", "id"), DOMAIN.ID.getDataType())))
                )
        );
        
        // Execute the query
        try {
        	return dsl.withRecursive(treeStructure)
        			.selectFrom(treeStructure)
        			.fetchInto(Domain.class)
        			.stream().map(d -> new DomainDTO().assignPojoValues(d)).toList();
		} catch (Exception e) {
			log.error("Couldn't generate the subtree from the given domain: " + e.getClass() + ": " + e.getMessage());
            return List.of();
		}
    }

    /**
     * Helper method that creates the complete tree of domains for any given domain inside this tree.
     *
     * @param domain any domain object from the tree
     * @return a list of domain objects which represent all domains of the tree surrounding the given domain
     */
    public List<Domain> getDomainTreeStructure(Domain domain) {

        if (domain == null || domain.getId() == null) {
            return null;
        }

        /**

         WITH RECURSIVE find_root AS (
         	SELECT * FROM DOMAIN WHERE id = 1
         	UNION ALL
         	SELECT domain.* FROM DOMAIN
         	INNER JOIN find_root ON domain.id = find_root.superdomainid
         ), tree_structure AS (
			SELECT * FROM DOMAIN WHERE id = (
				SELECT id FROM find_root WHERE superdomainid IS NULL
			)
         	UNION ALL
         	SELECT domain.* FROM DOMAIN
         	INNER JOIN tree_structure ON domain.superdomainid = tree_structure.id
         )
         SELECT * FROM tree_structure;
         
         */
        
        // Build the queries
        CommonTableExpression<Record> findRoot = name("find_root").as(
                select(DOMAIN.asterisk()).from(DOMAIN)
                        .where(DOMAIN.ID.eq(domain.getId()))
                        .unionAll(
                                select(DOMAIN.asterisk())
                                        .from(DOMAIN)
                                        .innerJoin(name("find_root"))
                                        .on(DOMAIN.ID.eq(field(name("find_root", "superdomainid"), Integer.class)))
                        )
        );

        CommonTableExpression<Record> treeStructure = name("tree_structure").as(
                select(DOMAIN.asterisk()).from(DOMAIN)
                        .where(DOMAIN.ID.eq(
                                select(field("id"))
                                        .from(name("find_root"))
                                        .where(field("superdomainid").isNull()).asField()
                        ))
                        .unionAll(
                                select(DOMAIN.asterisk())
                                        .from(DOMAIN)
                                        .innerJoin(name("tree_structure"))
                                        .on(DOMAIN.SUPERDOMAINID.eq(field(name("tree_structure", "id"), Integer.class)))
                        )
        );

        // Execute the query
        List<Domain> result = null;
        try {
			result = this.dsl.transactionResult(configuration -> {
			    List<Domain> r = DSL.using(configuration).withRecursive(findRoot, treeStructure)
			            .select()
			            .from(name("tree_structure")).fetchInto(Domain.class);
			    return r;
			    
			    // Implicit transaction commit here
			});
		} catch (Exception e) {
			log.error("Couldn't generate the tree structure: " + e.getMessage() + "\n");
            return null;
		}
        
        return result;
    }
    
    /**
	* Retrieves a flat list of domain names for a given domain that represents the domain-
	* tree that the given domain is in.
	*
	* @param domainName the name of the domain to retrieve the tree structure for
	* @return a list of domain names in a flat structure
	* @throws IllegalArgumentException when the domain or it's tree could not be retrieved
	*/
	public List<String> getFlatDomainTree(String domainName) throws IllegalArgumentException {
		Domain domain = getDomainByName(domainName);
		List<Domain> domainTree = domain == null ? null : getDomainTreeStructure(domain);

		// Check if the tree has at least one domain in it
		if (domainTree == null || domainTree.size() <= 0) {
			throw new IllegalArgumentException("No domains found for the specified domain name.");
		}

		// Transform the flat tree of domain objects into one of domain names only
		List<String> domainNames = new ArrayList<>();
		for (Domain thisDomain : domainTree) {
			domainNames.add(thisDomain.getName());
		}

		return domainNames;
	}

	/**
	 * Retrieves the root domain from a list of domain nodes.
	 * The list of nodes need to be in a tree and connected via the super-domain 
	 * properties, otherwise a random parent-less domain is returned.
	 *
	 * @param nodes a list of domain objects representing the nodes in the domain tree
	 * @return the root domain object if found, or {@code null} if anything went wrong
	 */
	public Domain getRootDomainFromTree(List<Domain> nodes) {
		if (nodes == null || nodes.size() == 0) {
			return null;
		}
		
		for (Domain domain : nodes) {
			if (domain.getSuperdomainid() == null) {
				return domain;
			}
		}
		
		// If we reach this point, no root domain was found
		log.trace("Could not find root of the given tree.");
		return null;
	}

    /**
     * Method to find pseudonym(s) linked to the given one in the provided domain.
     * For that, either the identifier and idType or the psn of the starting record is needed.
     * 
     * @param sourceDomainName the name of the domain to start from
     * @param sourceIdentifier the identifier of the record to start the search from
     * @param sourceIdType the idType of the starting identifier
     * @param sourcePsn the pseudonym of the record to start the search from
     * @param targetDomainName the name of the domain where to find the matching pseudonym records in
     * @return a list of pairs <b>(A, B)</b> of linked pseudonym records, with <b>A</b> being the source 
     * 			record, and <b>B</b> being the linked record, i.e., [(A, B), (A, C), (A, D), ..., (A, N)]
     * 			or {@code null}.
     */
    public List<Pair<PseudonymDTO, PseudonymDTO>> getLinkedPseudonyms(String sourceDomainName, String sourceIdentifier, String sourceIdType, String sourcePsn, String targetDomainName) {
    	// Retrieve the source domain object (will not be audited)
		Domain sourceDomain = getDomainByName(sourceDomainName);
		Domain targetDomain = getDomainByName(targetDomainName);
		IdentifierItem sourceII = IdentifierItem.builder().identifier(sourceIdentifier).idType(sourceIdType).build();
		List<PseudonymDTO> sourcePseudonymList = pseudonymDBAccessService.getPseudonym(sourceDomainName, sourceII, sourcePsn);
		
		// Check if the given information is sufficient to start the linking-process
        if (sourceDomain == null) {
        	log.debug("The domain to start the search from (\"" + sourceDomainName + "\") was not found.");
            return null;
        } else if (targetDomain == null) {
        	log.debug("The domain to find the target in (\"" + targetDomainName + "\") was not found.");
            return null;
        } else if (sourcePseudonymList == null || sourcePseudonymList.isEmpty()) {
        	log.debug("The record to start the search from was not found.");
            return null;
        }
        
        PseudonymDTO sourcePseudonym = sourcePseudonymList.getFirst();
        
        // Generate the tree starting from the given source-domain and generate the path to the destination domain
        PathFinder pathFinder = new PathFinder(getDomainTreeStructure(sourceDomain));
        List<Domain> path = pathFinder.getPath(sourceDomainName, targetDomainName);

        if (Assertion.assertNullAll(path)) {
        	log.debug("No path between the domains (" + sourceDomainName + ", " + targetDomainName + ") was found.");
        	return null;
        }

        // Only use the path if a minimum of two different domains are in there
        // If the user wants to retrieve linked pseudonyms in the same domain, they need to do this manually
        int pathSize = path.size();
        if (pathSize < 2) {
        	log.debug("Too few domains for linking provided.");
            return null;
        }

        try {
            List<Pair<PseudonymDTO, PseudonymDTO>> result = dsl.transactionResult(configuration -> {
                // Start building the query
            	SelectQuery<Record> query = dsl.select(PSEUDONYM.as("p" + pathSize).asterisk()).getQuery();
                query.addFrom(PSEUDONYM.as("p1"));

                // Iterate over the path to find all the pseudonyms along the path
                // Builds all pseudonym record-chains of length pathSize
                for (int i = 2; i <= pathSize; i++) {
                	// Include forwards and backwards chains
                    query.addJoin(PSEUDONYM.as("p" + i), JoinType.RIGHT_OUTER_JOIN,
                    		// Forwards/down the tree: psn_i-1 = identifier_i
                    		PSEUDONYM.as("p" + (i - 1)).PSEUDONYM_.equal(PSEUDONYM.as("p" + i).IDENTIFIER)
                    		// Backwards/up the tree: identifier_i-1 = psn_i
                    		.or(PSEUDONYM.as("p" + (i - 1)).IDENTIFIER.equal(PSEUDONYM.as("p" + i).PSEUDONYM_)));
                }

                // Filter by removing every record-chain that doesn't start with the user-given record
                if (sourceIdentifier != null && sourceIdType != null) {
                	// Using identifier & idType
                	query.addConditions(PSEUDONYM.as("p1").IDENTIFIER.equal(sourceIdentifier),
                						PSEUDONYM.as("p1").IDTYPE.equal(sourceIdType));
                } else if (sourcePsn != null) {
                	// Using psn
                	query.addConditions(PSEUDONYM.as("p1").PSEUDONYM_.equal(sourcePsn));
                } else {
                	// If nothing (or only the identifier or only the idType) was given, match everything from the sourceDomain
                	query.addConditions(PSEUDONYM.as("p1").DOMAINID.equal(sourceDomain.getId()));
                }
                
                // We only want those records that are in the target domain
                query.addConditions(PSEUDONYM.as("p" + pathSize).DOMAINID.equal(targetDomain.getId()));

                // Execute query and fetch the result
                List<Pseudonym> resultList = query.fetchInto(Pseudonym.class);
                
                // Transform the query result into a list of pairs
                List<Pair<PseudonymDTO, PseudonymDTO>> pairList = new ArrayList<>();
                for (Pseudonym r : resultList) {
                	if (r != null) {
                		pairList.add(new Pair<PseudonymDTO, PseudonymDTO>(sourcePseudonym, new PseudonymDTO().assignPojoValues(r)));
                	}
                }
                
                return pairList;
                
                // Implicit transaction commit here
            });

            return result;
        } catch (Exception e) {
            log.error("Couldn't successfully query the database: " + e.getMessage() + "\n");
            return null;
        }
    }
    
    /**
     * Method to retrieve all pseudonym-values that exist in a domain from the database.
     * 
     * @param domainName the name of the domain where the values should be retrieved from
     * @return a list of pseudonyms existing in the given domain when successful, {@code null} otherwise
     */
	public List<String> getAllPseudonymValuesInDomain(String domainName) {
		try {
			List<String> pseudonyms = this.dsl.transactionResult(configuration -> {
				// Retrieve all pseudonyms from the domain
				return DSL.using(configuration)
						.select(PSEUDONYM.PSEUDONYM_)
						.from(PSEUDONYM)
						.where(PSEUDONYM.DOMAINID.equal(this.getDomainByName(domainName).getId()))
						.fetchInto(String.class);
				
			    // Implicit transaction commit here
			});
			
			// At this point the retrieval was successful
            log.debug("Successfully retrieved all pseudonym-values from the domain.");
            return pseudonyms;
        } catch (Exception e) {
            log.error("Couldn't retrieve the pseudonym-values of domain " + domainName + " from the database: " + e.getMessage() + "\n");
            return null;
        }
	}

    /**
     * Method to retrieve all records stored in a domain.
     *
     * @param domainName the name of the domain where the records should be retrieved from
     * @return a list of records stored in the given domain when successful, {@code null} otherwise
     */
	@Transactional
    public List<PseudonymDTO> getAllPseudonymsInDomain(String domainName) {
        try {
            // Get domain
            Domain d = this.getDomainByName(domainName);

            // Retrieve all records from a domain
            List<PseudonymDTO> ps = dsl.selectFrom(PSEUDONYM)
                    .where(PSEUDONYM.DOMAINID.eq(d.getId()))
                    .fetchInto(Pseudonym.class)
                    .stream()
                    .map(p -> new PseudonymDTO().assignPojoValues(p))
                    .toList();

            // At this point the retrieval was successful
            log.debug("Successfully retrieved all records from the domain.");
            return ps;
        } catch (Exception e) {
            log.error("Couldn't retrieve the records of domain " + domainName + " from the database: " + e.getMessage() + "\n");
            return null;
        }
    }

    /**
     * Returns the number of records in a domain
     *
     * @param domainName the name of the domain where the number of records should be retrieved from
     * @return the counted number
     */
    public Integer getAmountOfPseudonymsInDomain(String domainName) {
        try {
            Integer count = this.dsl.transactionResult(configuration -> {
                // Get domain
                Domain d = this.getDomainByName(domainName);
                
                if (d == null) {
                	// Domain wasn't found
                	throw new DomainNotFoundException(domainName);
                }

                // Retrieve all records from a domain
                return DSL.using(configuration)
                        .selectCount()
                        .from(PSEUDONYM)
                        .where(PSEUDONYM.DOMAINID.equal(d.getId()))
                        .fetchOne(0, Integer.class);
                
                // Implicit transaction commit here
            });

            // At this point the retrieval was successful
            log.debug("Successfully retrieved the number of records in the domain.");
            return count;
        } catch (DomainNotFoundException e) {
        	log.error("Domain \"" + domainName + "\" wasn't found in the database.");
        	return DOMAIN_NOT_FOUND;
    	} catch (Exception f) {
            log.error("Couldn't retrieve the number of records in the domain " + domainName + " from the database: " + f.getMessage() + "\n");
            return null;
        }
    }

    /**
     * Deletes a record from the domain table.
     *
     * @param domainName the name of the domain that should be deleted
     * @param recursiveDeletion determines if possible child domains should also be deleted
     * @return {@code true} when the deletion was successful or the record
     * was not in the database, {@code false} otherwise
     */
    public boolean deleteDomain(String domainName, boolean recursiveDeletion) {
        try {
            this.dsl.transaction(configuration -> {
                // Retrieve the domain object
                Domain domain = getDomainByName(domainName);

                // Check if the domain is in the database
                if (domain == null) {
                    // The domain is not in the database. Nothing to delete. Use exception to break the transaction.
                    throw new DomainNotFoundException(domainName);
                }

                // Check if sub-domains should also be deleted -> if so, perform DFS and delete bottom-up
                if (recursiveDeletion) {
                    // Retrieve a list of sub-domains. Bind variable and execute query.
                    List<Integer> ids = DSL.using(configuration).select(DOMAIN.ID)
                            .from(DOMAIN)
                            .where(DOMAIN.SUPERDOMAINID.equal(domain.getId()))
                            .fetchInto(Integer.class);

                    // Iterate over the sub-domain IDs
                    for (int id : ids) {
                        String childName = getDomainByID(id).getName();

                        // Recursive call of the delete method -> DFS
                        if (deleteDomain(childName, recursiveDeletion)) {
                            /* (deletions on the connections opened in this DFS are committed only if
                             * the deletion is successful from bottom to top, otherwise the deletion fails
                             * and the error is transferred bottom-up.)
                             */
                            // Success; continue with deletion process
                            log.debug("Successfully deleted the child-domain \"" + childName + "\".");
                        } else {
                            // Failed deletion. Use exception to break the transaction.
                            throw new FailedChildDomainDeletionException(childName);
                        }
                    }
                }

                /* NOTE: This point is reached the first time, when the DFS found the last sub-domain in
                 * the "domain-tree". From here the bottom-up deletion starts. First delete the records in
                 * the domain, then delete the domain itself and return one recursion level upward.
                 */
                // Delete by creating multiple delete statements and executing them as a batch.
                DSLContext ctx = DSL.using(configuration);
                
                // Retrieve a list of all pseudonym-records in the domain
                List<Pseudonym> pseudonyms = ctx
                        .selectFrom(PSEUDONYM)
                        .where(PSEUDONYM.DOMAINID.equal(domain.getId()))
                        .fetchInto(Pseudonym.class);

                // Create a list of delete statements
                List<DeleteConditionStep<PseudonymRecord>> deletions = new ArrayList<>();
                for (Pseudonym p : pseudonyms) {
                    deletions.add(ctx.delete(PSEUDONYM)
                            .where(PSEUDONYM.IDENTIFIER.equal(p.getIdentifier()))
                            .and(PSEUDONYM.IDTYPE.equal(p.getIdtype()))
                            .and(PSEUDONYM.PSEUDONYM_.equal(p.getPseudonym()))
                            .and(PSEUDONYM.DOMAINID.equal(p.getDomainid())));
                }

                // Batch the delete statements and execute the batch
                int[] result = ctx.batch(deletions).execute();

                // Process the result
                for (int i = 0; i < result.length; i++) {
                    if (result[i] != 1) {
                        // Unexpected result, abort the complete transaction by throwing an exception
                        throw new UnexpectedResultSizeException(1, result[i]);
                    }
                }

                // Success; continue with the deletion process
                log.debug("Successfully deleted all records in the domain \"" + domain.getName() + "\".");

                // Now delete the domain itself
                int deletedDomains = DSL.using(configuration).deleteFrom(DOMAIN)
                        .where(DOMAIN.ID.equal(domain.getId()))
                        .execute();

                // Determine deletion success
                if (deletedDomains != 1) {
                    // An unexpected number of records was affected. Log it and abort by throwing
                    // an exception (which will rollback everything from the transaction).
                    log.error("Couldn't delete the domain \"" + domain.getName() + "\" from the database.");
                    throw new UnexpectedResultSizeException(1, deletedDomains);
                }
                
                // Handle permissions
                if (!permissionDBService.removeDomainPermissionsForSubject(domain.getName())) {
                	log.debug("Failed to remove the domain's permissions. Aborting.");
                	throw new PermissionManagementException(domain.getName());
                }

                // Implicit transaction commit here
            });

            // At this point the deletion was successful
            log.debug("Successfully deleted the domain \"" + domainName + "\" from the database.");
            return true;
        } catch (DomainNotFoundException e) {
            log.info("The domain \"" + e.getDomainName() + "\" is not in the database. Nothing to delete.");
            return true;
        } catch (FailedChildDomainDeletionException f) {
            log.error("Deleting the child-domain \"" + f.getChildName() + "\" was unsuccessful. The deletion was therefore rolled back.");
            return false;
        } catch (FailedPseudonymDeletionException g) {
            log.error("The deletion of a pseudonym-record in the domain \"" + g.getDomainName() + "\" was unsuccessful and therefore rolled back.");
            return false;
        } catch (UnexpectedResultSizeException h) {
            log.error("The deletion would have affected an unexpected number of database entries. It should only affect 1, "
                    + "but affected " + h.getActual() + " database entries.");
            return false;
        } catch (PermissionManagementException i) {
            log.error("Couldn't delete permissions of domain \"" + i.getCausingResource() + "\".");
            return false;
        } catch (Exception j) {
            log.error("Couldn't delete the record from the database: ", j.getMessage(), j);
            return false;
        }
    }

    /**
     * Method to insert information of a domain into the according table.
     *
     * @param domain the domain object to insert
     * @return {@code INSERTION_SUCCESS} when the insertion was successful,
     * {@code INSERTION_DUPLICATE} when the domain was already in
     * the database, {@code INSERTION_ERROR} otherwise.
     */
    public String insertDomain(Domain domain) {
        try {
            this.dsl.transaction(configuration -> {
                // Check if the domain is already in the database
                if (getDomainByName(domain.getName()) != null) {
                    // Record is already in the DB. Use exception to break the transaction.
                    throw new DuplicateDomainException(domain.getName());
                }

                // Insert the domain object into database
                Integer domainId = DSL.using(configuration)
                        .insertInto(DOMAIN, DOMAIN.NAME, DOMAIN.PREFIX, DOMAIN.VALIDFROM, DOMAIN.VALIDFROMINHERITED,
                                DOMAIN.VALIDTO, DOMAIN.VALIDTOINHERITED, DOMAIN.ENFORCESTARTDATEVALIDITY,
                                DOMAIN.ENFORCESTARTDATEVALIDITYINHERITED, DOMAIN.ENFORCEENDDATEVALIDITY,
                                DOMAIN.ENFORCEENDDATEVALIDITYINHERITED, DOMAIN.ALGORITHM, DOMAIN.ALGORITHMINHERITED,
                                DOMAIN.ALPHABET, DOMAIN.ALPHABETINHERITED, DOMAIN.RANDOMALGORITHMDESIREDSIZE,
                                DOMAIN.RANDOMALGORITHMDESIREDSIZEINHERITED, DOMAIN.RANDOMALGORITHMDESIREDSUCCESSPROBABILITY,
                                DOMAIN.RANDOMALGORITHMDESIREDSUCCESSPROBABILITYINHERITED, DOMAIN.MULTIPLEPSNALLOWED, 
                                DOMAIN.MULTIPLEPSNALLOWEDINHERITED, DOMAIN.CONSECUTIVEVALUECOUNTER, DOMAIN.PSEUDONYMLENGTH, 
                                DOMAIN.PSEUDONYMLENGTHINHERITED, DOMAIN.PADDINGCHARACTER, DOMAIN.PADDINGCHARACTERINHERITED, 
                                DOMAIN.ADDCHECKDIGIT, DOMAIN.ADDCHECKDIGITINHERITED, DOMAIN.LENGTHINCLUDESCHECKDIGIT,
                                DOMAIN.LENGTHINCLUDESCHECKDIGITINHERITED, DOMAIN.DESCRIPTION, DOMAIN.SALT, 
                                DOMAIN.SALTLENGTH, DOMAIN.SUPERDOMAINID)
                        .values(domain.getName(), domain.getPrefix(), domain.getValidfrom(), domain.getValidfrominherited(),
                                domain.getValidto(), domain.getValidtoinherited(), domain.getEnforcestartdatevalidity(),
                                domain.getEnforcestartdatevalidityinherited(), domain.getEnforceenddatevalidity(),
                                domain.getEnforceenddatevalidityinherited(), domain.getAlgorithm(), domain.getAlgorithminherited(),
                                domain.getAlphabet(), domain.getAlphabetinherited(), domain.getRandomalgorithmdesiredsize(),
                                domain.getRandomalgorithmdesiredsizeinherited(), domain.getRandomalgorithmdesiredsuccessprobability(),
                                domain.getRandomalgorithmdesiredsuccessprobabilityinherited(), domain.getMultiplepsnallowed(),
                                domain.getMultiplepsnallowedinherited(), domain.getConsecutivevaluecounter(), domain.getPseudonymlength(), 
                                domain.getPseudonymlengthinherited(), domain.getPaddingcharacter(), domain.getPaddingcharacterinherited(), 
                                domain.getAddcheckdigit(), domain.getAddcheckdigitinherited(), domain.getLengthincludescheckdigit(), 
                                domain.getLengthincludescheckdigitinherited(), domain.getDescription(), domain.getSalt(), 
                                domain.getSaltlength(), 
                                (domain.getSuperdomainid() == null || domain.getSuperdomainid() == 0) ? null : domain.getSuperdomainid())
                        .returning(DOMAIN.ID)
                        .fetchOne(DOMAIN.ID);

                // Determine insertion success
                if (domainId == null) {
                    // An unexpected number of records was affected. Log it and abort by throwing
                    // an exception (which will rollback everything from the transaction).
                    log.error("Couldn't insert the domain into the database.");
                    throw new UnexpectedResultSizeException(1, 0);
                }

                // Handle permissions
                if (!permissionDBService.addDomainPermissionsForSubject(domainId)) {
                	log.debug("Failed to add the domain's permissions. Aborting.");
                	throw new PermissionManagementException(domain.getName());
                }

                // Implicit transaction commit here
            });

            // At this point the insertion was successful
            log.debug("Successfully added the new domain into the database.");
            return INSERTION_SUCCESS;
        } catch (DuplicateDomainException e) {
            log.debug("The domain that should be inserted (\"" + e.getDomainName() + "\") was already in the database. No insertion performed.");
            return INSERTION_DUPLICATE;
        } catch (DuplicateKeyException f) {
            log.debug("The domain that should be inserted was already in the database. No insertion performed.");
            return INSERTION_DUPLICATE;
        } catch (UnexpectedResultSizeException g) {
            log.error("The insertion would have affected an unexpected number of domains. It should only affect 1 domain, "
                    + "but affected " + g.getActual() + " domains.");
            return INSERTION_ERROR;
        } catch (PermissionManagementException h) {
            log.error("Creating permissions for the domain \"" + h.getCausingResource() + "\" failed. No insertion performed.");
            return INSERTION_ERROR;
        } catch (Exception i) {
            log.error("Couldn't insert the domain into the database: " + i.getMessage(), i);
            return INSERTION_ERROR;
        }
    }

    /**
     * This method lists all domains from the database.
     *
     * @return a list of domains, if nothing was found an empty list is returned.
     */
    @Transactional
    public List<DomainDTO> listDomains() {
        try {
			return dsl.selectFrom(DOMAIN)
					.fetchInto(Domain.class)
                    .stream().map(d -> new DomainDTO().assignPojoValues(d)).toList();
        } catch (Exception e) {
            log.error("Couldn't query the database: " + e.getClass() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Method to update only the counter value of a domain.
     * 
     * @param counter the new counter value
     * @param domainName the name of the domain where the counter should be updated
     * @return {@code true}, when the update was successful, {@code false} otherwise.
     */
    public boolean updateCounter(Long counter, String domainName) {
    	try {
            this.dsl.transaction(configuration -> {
                // Create and execute the update query
                int updatedDomain = DSL.using(configuration).update(DOMAIN)
                        .set(DOMAIN.CONSECUTIVEVALUECOUNTER, counter)
                        .where(DOMAIN.NAME.equal(domainName))
                        .execute();
                
                // Determine success
                if (updatedDomain != 1) {
                    // An unexpected number of domains was affected. Log it and abort by throwing
                    // an exception (which will rollback everything from the transaction).
                    throw new UnexpectedResultSizeException(1, updatedDomain);
                }

                // Implicit transaction commit here
            });

            // Successful update
            log.debug("Successfully updated the counter of domain \"" + domainName + "\".");
            return true;
    	} catch (UnexpectedResultSizeException f) {
            log.error("The update would have affected an unexpected number of records. It should only affect 1 record, "
                    + "but affected " + f.getActual() + " records. It was therefore rolled back.");
            return false;
        } catch (Exception i) {
            log.error("Couldn't update the domain: " + i.getMessage() + "\n");
            return false;
        }
    }

    /**
     * Method to update a domain in the database.
     * Updates only those parameters that aren't {@code null}.
     * Recursive updating of affected child-domains is possible.
     *
     * @param oldDomain the domain object of the domain that should be updated
     * @param newDomain the domain object of the new domain; variables that aren't needed can be {@code null}
     * @param recursiveChanges determines if possible child domains should also be updated
     * @return {@code true} if the update was successful, {@code false} otherwise
     */
    public Domain updateDomain(Domain oldDomain, Domain newDomain, boolean recursiveChanges) {
        try {
            this.dsl.transaction(configuration -> {
                // Check if the domain to be updated exists
                if (!getDomainDao().exists(oldDomain)) {
                    // Domain is not in the DB. Use exception to break the transaction.
                    throw new DomainNotFoundException(oldDomain.getName());
                }

                // Create and execute the update query
                int updatedDomain = DSL.using(configuration).update(DOMAIN)
                        .set(DOMAIN.NAME, (newDomain.getName() != null && !newDomain.getName().trim().equals("")) ? newDomain.getName() : oldDomain.getName())
                        .set(DOMAIN.PREFIX, (newDomain.getPrefix() != null && !newDomain.getPrefix().trim().equals("")) ? newDomain.getPrefix() : oldDomain.getPrefix())
                        .set(DOMAIN.VALIDFROM, (newDomain.getValidfrom() != null) ? newDomain.getValidfrom() : oldDomain.getValidfrom())
                        .set(DOMAIN.VALIDFROMINHERITED, (newDomain.getValidfrominherited() != null) ? newDomain.getValidfrominherited() : oldDomain.getValidfrominherited())
                        .set(DOMAIN.VALIDTO, (newDomain.getValidto() != null) ? newDomain.getValidto() : oldDomain.getValidto())
                        .set(DOMAIN.VALIDTOINHERITED, (newDomain.getValidtoinherited() != null) ? newDomain.getValidtoinherited() : oldDomain.getValidtoinherited())
                        .set(DOMAIN.ENFORCESTARTDATEVALIDITY, (newDomain.getEnforcestartdatevalidity() != null) ? newDomain.getEnforcestartdatevalidity() : oldDomain.getEnforcestartdatevalidity())
                        .set(DOMAIN.ENFORCESTARTDATEVALIDITYINHERITED, (newDomain.getEnforcestartdatevalidityinherited() != null) ? newDomain.getEnforcestartdatevalidityinherited() : oldDomain.getEnforcestartdatevalidityinherited())
                        .set(DOMAIN.ENFORCEENDDATEVALIDITY, (newDomain.getEnforceenddatevalidity() != null) ? newDomain.getEnforceenddatevalidity() : oldDomain.getEnforceenddatevalidity())
                        .set(DOMAIN.ENFORCEENDDATEVALIDITYINHERITED, (newDomain.getEnforceenddatevalidityinherited() != null) ? newDomain.getEnforceenddatevalidityinherited() : oldDomain.getEnforceenddatevalidityinherited())
                        .set(DOMAIN.ALGORITHM, (newDomain.getAlgorithm() != null && !newDomain.getAlgorithm().trim().equals("")) ? newDomain.getAlgorithm() : oldDomain.getAlgorithm())
                        .set(DOMAIN.ALGORITHMINHERITED, (newDomain.getAlgorithminherited() != null) ? newDomain.getAlgorithminherited() : oldDomain.getAlgorithminherited())
                        .set(DOMAIN.ALPHABET, (newDomain.getAlphabet() != null && !newDomain.getAlphabet().trim().equals("")) ? newDomain.getAlphabet() : oldDomain.getAlphabet())
                        .set(DOMAIN.ALPHABETINHERITED, (newDomain.getAlphabetinherited() != null) ? newDomain.getAlphabetinherited() : oldDomain.getAlphabetinherited())
                        .set(DOMAIN.RANDOMALGORITHMDESIREDSIZE, (newDomain.getRandomalgorithmdesiredsize() != null) ? newDomain.getRandomalgorithmdesiredsize() : oldDomain.getRandomalgorithmdesiredsize())
                        .set(DOMAIN.RANDOMALGORITHMDESIREDSIZEINHERITED, (newDomain.getRandomalgorithmdesiredsizeinherited() != null) ? newDomain.getRandomalgorithmdesiredsizeinherited() : oldDomain.getRandomalgorithmdesiredsizeinherited())
                        .set(DOMAIN.RANDOMALGORITHMDESIREDSUCCESSPROBABILITY, (newDomain.getRandomalgorithmdesiredsuccessprobability() != null) ? newDomain.getRandomalgorithmdesiredsuccessprobability() : oldDomain.getRandomalgorithmdesiredsuccessprobability())
                        .set(DOMAIN.RANDOMALGORITHMDESIREDSUCCESSPROBABILITYINHERITED, (newDomain.getRandomalgorithmdesiredsuccessprobabilityinherited() != null) ? newDomain.getRandomalgorithmdesiredsuccessprobabilityinherited() : oldDomain.getRandomalgorithmdesiredsuccessprobabilityinherited())
                        .set(DOMAIN.MULTIPLEPSNALLOWED, (newDomain.getMultiplepsnallowed() != null) ? newDomain.getMultiplepsnallowed() : oldDomain.getMultiplepsnallowed())
                        .set(DOMAIN.MULTIPLEPSNALLOWEDINHERITED, (newDomain.getMultiplepsnallowedinherited() != null) ? newDomain.getMultiplepsnallowedinherited() : oldDomain.getMultiplepsnallowedinherited())
                        .set(DOMAIN.CONSECUTIVEVALUECOUNTER, (newDomain.getConsecutivevaluecounter() != null) ? newDomain.getConsecutivevaluecounter() : oldDomain.getConsecutivevaluecounter())
                        .set(DOMAIN.PSEUDONYMLENGTH, (newDomain.getPseudonymlength() != null) ? newDomain.getPseudonymlength() : oldDomain.getPseudonymlength())
                        .set(DOMAIN.PSEUDONYMLENGTHINHERITED, (newDomain.getPseudonymlengthinherited() != null) ? newDomain.getPseudonymlengthinherited() : oldDomain.getPseudonymlengthinherited())
                        .set(DOMAIN.PADDINGCHARACTER, (newDomain.getPaddingcharacter() != null && !newDomain.getPaddingcharacter().trim().equals("")) ? newDomain.getPaddingcharacter() : oldDomain.getPaddingcharacter())
                        .set(DOMAIN.PADDINGCHARACTERINHERITED, (newDomain.getPaddingcharacterinherited() != null) ? newDomain.getPaddingcharacterinherited() : oldDomain.getPaddingcharacterinherited())
                        .set(DOMAIN.ADDCHECKDIGIT, (newDomain.getAddcheckdigit() != null) ? newDomain.getAddcheckdigit() : oldDomain.getAddcheckdigit())
                        .set(DOMAIN.ADDCHECKDIGITINHERITED, (newDomain.getAddcheckdigitinherited() != null) ? newDomain.getAddcheckdigitinherited() : oldDomain.getAddcheckdigitinherited())
                        .set(DOMAIN.LENGTHINCLUDESCHECKDIGIT, (newDomain.getLengthincludescheckdigit() != null) ? newDomain.getLengthincludescheckdigit() : oldDomain.getLengthincludescheckdigit())
                        .set(DOMAIN.LENGTHINCLUDESCHECKDIGITINHERITED, (newDomain.getLengthincludescheckdigitinherited() != null) ? newDomain.getLengthincludescheckdigitinherited() : oldDomain.getLengthincludescheckdigitinherited())
                        .set(DOMAIN.DESCRIPTION, (newDomain.getDescription() != null) ? newDomain.getDescription() : oldDomain.getDescription())
                        .set(DOMAIN.SALT, (newDomain.getSalt() != null) ? newDomain.getSalt() : oldDomain.getSalt())
                        .set(DOMAIN.SALTLENGTH, (newDomain.getSaltlength() != null) ? newDomain.getSaltlength() : oldDomain.getSaltlength())
                        .where(DOMAIN.ID.equal(oldDomain.getId()))
                        .execute();

                // Determine success
                if (updatedDomain != 1) {
                    // An unexpected number of domains was affected. Log it and abort by throwing
                    // an exception (which will rollback everything from the transaction).
                    throw new UnexpectedResultSizeException(1, updatedDomain);
                }

                // Retrieve a list of all pseudonym-records in the updated domain
                List<Pseudonym> records = DSL.using(configuration).selectFrom(PSEUDONYM)
                        .where(PSEUDONYM.DOMAINID.equal(oldDomain.getId()))
                        .fetchInto(Pseudonym.class);

                // Iterate over the pseudonym-records, if there are any, and create updatable DTOs
                List<PseudonymUpdateDTO> updates = new ArrayList<>();
                for (Pseudonym old : records) {
                    // Create a new record with the necessary changes
                	IdentifierItem oldIdItem = IdentifierItem.builder().identifier(old.getIdentifier()).idType(old.getIdtype()).build();
                	
                    PseudonymUpdateDTO update = new PseudonymUpdateDTO();
                    update.setOldIdentifierItem(oldIdItem);
                    update.setOldPsn(old.getPseudonym());
                    update.setOldDomain(oldDomain);
                    update.setNewIdentifierItem(oldIdItem);
                    update.setNewPsn(old.getPseudonym());
                    update.setNewDomain(newDomain);
                    update.setNewDomainName(newDomain.getName());
                    update.setValidFrom(old.getValidfrominherited() ? newDomain.getValidfrom() : oldDomain.getValidfrom());
                    update.setValidFromInherited(old.getValidfrominherited());
                    update.setValidTo(old.getValidtoinherited() ? newDomain.getValidto() : oldDomain.getValidto());
                    update.setValidToInherited(old.getValidtoinherited());

                    updates.add(update);
                }
                
                // Update the inheritable variables
                List<Boolean> results = pseudonymDBAccessService.updatePseudonyms(updates);
                if (!updates.isEmpty() && results.contains(false)) {
                    // Failed update of a pseudonym-record: use exception to break the transaction
                	IdentifierItem ii = updates.get(results.indexOf(false)).getNewIdentifierItem();
                    throw new FailedPseudonymUpdateException((newDomain.getName() != null) ? newDomain.getName() : oldDomain.getName(), ii.getIdentifier(), ii.getIdType());
                }

                // Check if child domains should also be updated
                if (recursiveChanges) {
                    // Find child domains
                    List<Domain> oldChilds = DSL.using(configuration).select(DOMAIN.ID)
                            .from(DOMAIN)
                            .where(DOMAIN.SUPERDOMAINID.equal(oldDomain.getId()))
                            .fetchInto(Domain.class);

                    // Iterate over child domains
                    for (Domain oldChild : oldChilds) {
                        // Gather all variables for the new child domain. Update inherited values, keep non-inherited ones.
                        Domain updateDomain = new Domain();
                        updateDomain.setValidfrom(oldChild.getValidfrominherited() ? newDomain.getValidfrom() : oldChild.getValidfrom());
                        updateDomain.setValidfrominherited(oldChild.getValidfrominherited());
                        updateDomain.setValidto(oldChild.getValidtoinherited() ? newDomain.getValidto() : oldChild.getValidto());
                        updateDomain.setValidtoinherited(oldChild.getValidtoinherited());
                        updateDomain.setEnforcestartdatevalidity(oldChild.getEnforcestartdatevalidityinherited() ? newDomain.getEnforcestartdatevalidity() : oldChild.getEnforcestartdatevalidity());
                        updateDomain.setEnforcestartdatevalidityinherited(oldChild.getEnforcestartdatevalidityinherited());
                        updateDomain.setEnforceenddatevalidity(oldChild.getEnforceenddatevalidityinherited() ? newDomain.getEnforceenddatevalidity() : oldChild.getEnforceenddatevalidity());
                        updateDomain.setEnforceenddatevalidityinherited(oldChild.getEnforceenddatevalidityinherited());
                        updateDomain.setAlgorithm(oldChild.getAlgorithminherited() ? newDomain.getAlgorithm() : oldChild.getAlgorithm());
                        updateDomain.setAlgorithminherited(oldChild.getAlgorithminherited());
                        updateDomain.setAlphabet(oldChild.getAlphabetinherited() ? newDomain.getAlphabet() : oldDomain.getAlphabet());
                        updateDomain.setAlphabetinherited(oldChild.getAlphabetinherited());
                        updateDomain.setRandomalgorithmdesiredsize(oldChild.getRandomalgorithmdesiredsizeinherited() ? newDomain.getRandomalgorithmdesiredsize() : oldDomain.getRandomalgorithmdesiredsize());
                        updateDomain.setRandomalgorithmdesiredsizeinherited(oldChild.getRandomalgorithmdesiredsizeinherited());
                        updateDomain.setRandomalgorithmdesiredsuccessprobability(oldChild.getRandomalgorithmdesiredsuccessprobabilityinherited() ? newDomain.getRandomalgorithmdesiredsuccessprobability() : oldDomain.getRandomalgorithmdesiredsuccessprobability());
                        updateDomain.setRandomalgorithmdesiredsuccessprobabilityinherited(oldChild.getRandomalgorithmdesiredsuccessprobabilityinherited());
                        updateDomain.setPseudonymlength(oldChild.getPseudonymlengthinherited() ? newDomain.getPseudonymlength() : oldChild.getPseudonymlength());
                        updateDomain.setPseudonymlengthinherited(oldChild.getPseudonymlengthinherited());
                        updateDomain.setPaddingcharacter(oldChild.getPaddingcharacterinherited() ? newDomain.getPaddingcharacter() : oldChild.getPaddingcharacter());
                        updateDomain.setPaddingcharacterinherited(oldChild.getPaddingcharacterinherited());
                        updateDomain.setAddcheckdigit(oldChild.getAddcheckdigitinherited() ? newDomain.getAddcheckdigit() : oldDomain.getAddcheckdigit());
                        updateDomain.setAddcheckdigitinherited(oldChild.getAddcheckdigitinherited());
                        updateDomain.setLengthincludescheckdigit(oldChild.getLengthincludescheckdigitinherited() ? newDomain.getLengthincludescheckdigit() : oldDomain.getLengthincludescheckdigit());
                        updateDomain.setLengthincludescheckdigitinherited(oldChild.getLengthincludescheckdigitinherited());

                        // Recursive call of the update method
                        if (updateDomain(oldChild, updateDomain, recursiveChanges) != null) {
                            log.debug("Successfully updated the child-domain \"" + oldChild.getName() + "\".");
                        } else {
                            // Failed update of child-domain. Use exception to break the transaction.
                            throw new FailedChildDomainUpdateException(oldChild.getName());
                        }
                    }
                } // End of recursive child-handling
                
                // Check if the permissions need to be adapted
                if (newDomain.getName() != null && !oldDomain.getName().equals(newDomain.getName())) {
                	// The domain name has changed, so we need to update the permissions
                    if (!permissionDBService.removeDomainPermissionsForSubject(newDomain.getName())) {
                    	log.debug("Failed to remove the domain's permissions. Aborting.");
                    	throw new PermissionManagementException(newDomain.getName());
                    }
                    
                    if (!permissionDBService.addDomainPermissionsForSubject(oldDomain.getId())) {
                    	log.debug("Failed to add the domain's permissions. Aborting.");
                    	throw new PermissionManagementException(newDomain.getName());
                    }
                }

                // Implicit transaction commit here
            });

            // Successful update
            String name = (newDomain.getName() != null) ? newDomain.getName() : oldDomain.getName();
            log.debug("Successfully updated the domain \"" + name + "\".");
            return getDomainByName(name);
        } catch (DomainNotFoundException e) {
            log.info("The domain (\"" + e.getDomainName() + "\") is not in the database. Nothing to update.");
            return null;
        } catch (UnexpectedResultSizeException f) {
            log.error("The update would have affected an unexpected number of records. It should only affect 1 record, "
                    + "but affected " + f.getActual() + " records.");
            return null;
        } catch (FailedPseudonymUpdateException g) {
            log.error("While updating the records in the domain \"" + g.getDomainName() + "\" an error occurred. The update was therefore rolled back.");
            return null;
        } catch (FailedChildDomainUpdateException h) {
            log.error("Updating the child-domain \"" + h.getChildName() + "\" was unsuccessful and was therefore rolled back.");
            return null;
        } catch (PermissionManagementException i) {
            log.error("Updating the permissions failed for the domain (" + i.getCausingResource() + "). The update was therefore rolled back.");
            return null;
        } catch (Exception j) {
            log.error("Couldn't update the domain: " + j.getClass() + ": " + j.getMessage());
            log.trace("", j);
            return null;
        }
    }
}
