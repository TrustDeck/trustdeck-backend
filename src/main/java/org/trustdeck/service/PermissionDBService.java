/*
 * Trust Deck Services
 * Copyright 2026 Armin Müller
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

import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.Insert;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.MappingException;
import org.jooq.impl.DSL;
import org.jooq.Row4;
import org.jooq.UpdateConditionStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.trustdeck.configuration.RoleConfig;
import org.trustdeck.dto.EffectivePermissionDTO;
import org.trustdeck.dto.PermissionDTO;
import org.trustdeck.dto.PermissionUpdateDTO;
import org.trustdeck.dto.ProjectDTO;
import org.trustdeck.exception.PermissionManagementException;
import org.trustdeck.exception.UnexpectedResultSizeException;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.jooq.generated.tables.pojos.PermissionGrant;
import org.trustdeck.jooq.generated.tables.records.PermissionGrantRecord;
import org.trustdeck.security.audittrail.context.AuditInformationObject;
import org.trustdeck.security.audittrail.context.AuditSourceSystem;
import org.trustdeck.security.audittrail.context.AuditTriggerContext;
import org.trustdeck.utils.Assertion;
import org.trustdeck.utils.Utility;
import org.trustdeck.utils.Utility.Pair;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.trustdeck.jooq.generated.Tables.PERMISSION_GRANT;

/**
 * This class encapsulates the database access for permissions.
 * 
 * @author Armin Müller
 */
@Slf4j
@Service
public class PermissionDBService {
    
	/** References a jOOQ configuration object that configures jOOQ's behavior when executing queries. */
    @Autowired
	private DSLContext dsl;
    
    /** Enables the access to the domain specific database access methods. */
    @Autowired
    private DomainDBAccessService ddba;
    
    /** Enables the access to the project specific database access methods. */
    @Autowired
    private ProjectDBService pdba;

	/** Configuration for roles and operations. This is used to validate the operations and permissions. */
	@Autowired
	private RoleConfig roleConfig;
	
	/** Service that provides the methods for the interaction with Keycloak. */
	@Autowired
	private KeycloakService keycloakService;
	
	/**  */
	@Autowired
	private CachingService cachingService;
	
	/** The default duration for which newly created permissions are valid. */
	public static final Duration DEFAULT_VALIDITY_DURATION = Duration.ofDays(10 * 365);
    
    /** Represents the duplication status of a requested insertion of a permission into the database. */
    public static final String INSERTION_DUPLICATE_PERMISSION = "duplicate permission";

    /** Represents an erroneous insertion of a permission into the database. */
    public static final String INSERTION_ERROR = "error";

    /** Represents a successful insertion of a permission into the database. */
    public static final String INSERTION_SUCCESS = "success";

    /**
     * Method to insert multiple permissions at once in a batch. Duplicates will be ignored.
     * 
     * @param permissions a list of permissions to insert into the database
     * @return a list of result pairs in the same order as the input list.
	 *         For each entry, {@code first()} contains the created or duplicated permission
	 *         (or {@code null} on error), and {@code second()} contains one of: {@link #INSERTION_SUCCESS}, 
	 *         {@link #INSERTION_DUPLICATE_PERMISSION}, or {@link #INSERTION_ERROR}. Returns an empty list 
	 *         if no permissions were given, and {@code null} if the batch operation failed completely.
	 */
    @Transactional
    public List<Pair<PermissionDTO, String>> createPermissions(List<PermissionDTO> permissions) {
    	// Check if there is something to do
    	if (permissions == null || permissions.isEmpty()) {
    	    return Collections.emptyList();
    	}
    	
    	int n = permissions.size();
    	List<Pair<PermissionDTO, String>> results = new ArrayList<>(n);
    	
    	// Prefill the result list with nulls
    	for (int i = 0; i < n; i++) {
    		results.add(null);
    	}

    	// Prefill duplicate-check list
    	List<Boolean> existsFlags = new ArrayList<>(Collections.nCopies(n, Boolean.FALSE));

    	// Reused variables
    	String requester = subjectIdFromRequest();
    	OffsetDateTime now = OffsetDateTime.now();
    	
		// Check for duplicates in DB (skip duplicates before inserting)
    	// Create a list of permissions that we can use to query the database
		List<Row4<String, String, Integer, String>> idRows = new ArrayList<>(n);
		for (PermissionDTO dto : permissions) {
			// Skip nulls
			if (dto == null) {
				continue;
			}
			
			idRows.add(DSL.row(dto.getSubjectId(), dto.getResourceType(), dto.getResourceId(), dto.getAction()));
		}
		// Query the database and see if we find any of the user-provided permissions already in there
        Map<Row4<String, String, Integer, String>, PermissionDTO> existingMap = 
        		dsl.selectFrom(PERMISSION_GRANT)
				.where(DSL.row(PERMISSION_GRANT.SUBJECT_ID, PERMISSION_GRANT.RESOURCE_TYPE, PERMISSION_GRANT.RESOURCE_ID, PERMISSION_GRANT.ACTION).in(idRows))
				.fetchMap(r -> DSL.row(r.get(PERMISSION_GRANT.SUBJECT_ID), r.get(PERMISSION_GRANT.RESOURCE_TYPE), r.get(PERMISSION_GRANT.RESOURCE_ID), r.get(PERMISSION_GRANT.ACTION)),
						r -> {
							PermissionDTO p = new PermissionDTO().assignPojoValues(r.into(PermissionGrant.class));
							
							if ("DOMAIN".equalsIgnoreCase(r.getResourceType())) {
								Domain d = ddba.getDomainByID(r.getResourceId());
								p.setDomainName(d == null ? null : d.getName());
							} else if ("PROJECT".equalsIgnoreCase(r.getResourceType())) {
								ProjectDTO proj = pdba.getProjectByID(r.getResourceId());
								p.setProjectAbbreviation(proj == null ? null : proj.getAbbreviation());
							}

							return p;
						});
        
        // Mark duplicates in original order; marking null-permissions as "exists" will lead to them getting skipped later
        int rowIndex = 0;
        for (int i = 0; i < n; i++) {
        	PermissionDTO dto = permissions.get(i);
        	
        	if (dto == null) {
				existsFlags.set(i, Boolean.TRUE);
				continue;
			}

			PermissionDTO duplicate = existingMap.get(idRows.get(rowIndex));
			boolean isDuplicate = duplicate != null;
			existsFlags.set(i, isDuplicate);
			
			if (isDuplicate) {
				results.set(i, new Pair<PermissionDTO, String>(duplicate, INSERTION_DUPLICATE_PERMISSION));
			}
			
			// This index will only increased when the checked DTO was not null, so we can always assume that there is an accompanying idRow available
			rowIndex++;
		}

		// Prepare batch inserts for non-duplicates
		List<Insert<PermissionGrantRecord>> inserts = new ArrayList<>(n);
		List<Integer> insertIndices = new ArrayList<>(n); // For mapping batch result index -> permission index
		int skippedDuplicates = 0;

		for (int i = 0; i < n; i++) {
			PermissionDTO dto = permissions.get(i);
			
			if (dto == null || existsFlags.get(i)) {
				skippedDuplicates++;
				continue;
			}
			
			String decision = Assertion.isNotNullOrEmpty(dto.getDecision()) ? dto.getDecision() : "ALLOW";
			OffsetDateTime validFrom = dto.getValidFrom() != null ? dto.getValidFrom() : now;
			OffsetDateTime validTo = dto.getValidTo() != null ? dto.getValidTo() : validFrom.plus(DEFAULT_VALIDITY_DURATION);
			OffsetDateTime createdAt = dto.getCreatedAt() != null ? dto.getCreatedAt() : now;
			String createdBy = Assertion.isNotNullOrEmpty(dto.getCreatedBy()) ? dto.getCreatedBy() : requester;
			String updatedBy = Assertion.isNotNullOrEmpty(dto.getUpdatedBy()) ? dto.getUpdatedBy() : createdBy;

			inserts.add(dsl.insertInto(PERMISSION_GRANT)
					.set(PERMISSION_GRANT.SUBJECT_ID, dto.getSubjectId())
					.set(PERMISSION_GRANT.RESOURCE_TYPE, dto.getResourceType())
					.set(PERMISSION_GRANT.RESOURCE_ID, dto.getResourceId())
					.set(PERMISSION_GRANT.ACTION, dto.getAction())
					.set(PERMISSION_GRANT.DECISION, decision)
					.set(PERMISSION_GRANT.VALID_FROM, validFrom)
					.set(PERMISSION_GRANT.VALID_TO, validTo)
					.set(PERMISSION_GRANT.CREATED_AT, createdAt)
					.set(PERMISSION_GRANT.CREATED_BY, createdBy)
					.set(PERMISSION_GRANT.UPDATED_AT, now)
					.set(PERMISSION_GRANT.UPDATED_BY, updatedBy));

			// Track prepared indices
			insertIndices.add(i);
		}

		// If there is nothing to insert, we're done
		if (inserts.isEmpty()) {
			log.trace("No permissions to insert (" + skippedDuplicates + " duplicates skipped).");
			return results;
		}

		// Execute the batch
		int[] batchResult = dsl.batch(inserts).execute();

		// Evaluate results and fill output list (inserted DTOs; skipped stay null)
		int inserted = 0;
		int ignored = skippedDuplicates;

		for (int j = 0; j < batchResult.length; j++) {
			int individualResult = batchResult[j];
			int originalIndex = insertIndices.get(j);

			if (individualResult == 1) {
				inserted++;
				
				// Add created permission to the result list
				PermissionDTO p = permissions.get(originalIndex);
				PermissionDTO created = getPermission(p.getSubjectId(), p.getResourceType(), p.getResourceId(), p.getAction());
				results.set(originalIndex, new Pair<PermissionDTO, String>(created, INSERTION_SUCCESS));
			} else if (individualResult == 0) {
				ignored++;
				
				// Only mark error if it wasn't already marked duplicate
				if (results.get(originalIndex) == null) {
					results.set(originalIndex, new Pair<PermissionDTO, String>(null, INSERTION_ERROR));
				}
			} else {
                // Unexpected result size: abort
				throw new UnexpectedResultSizeException(1, individualResult);
			}
		}
		
		// Collect affected caches
		Set<String> affectedSubjects = new HashSet<>();
		Set<String> affectedContexts = new HashSet<>();

		for (int i = 0; i < results.size(); i++) {
		    PermissionDTO created = results.get(i).first();
		    if (created == null) {
		    	continue;
		    }

		    affectedSubjects.add(created.getSubjectId());
		    affectedContexts.add(created.getSubjectId() + "|" + created.getResourceType() + "|" + created.getResourceId());
		}

		// Invalidate the cache
		affectedSubjects.forEach(s -> cachingService.invalidateSubject(s));
		for (String ctx : affectedContexts) {
		    String[] parts = ctx.split("\\|", 3);
		    cachingService.invalidateContext(parts[0], parts[1], Integer.valueOf(parts[2]));
		}

		log.trace("Inserted " + inserted + " permission(s).");
		log.trace("Ignored " + ignored + " permission(s) (including " + skippedDuplicates + " duplicates).");
		log.debug("Successfully inserted " + inserted + " out of " + n + " permission(s)" + " into the database.");

		return results;

	}
	
	/**
	 * Creates permissions after enforcing bounded delegation for the requesting user.
	 * 
	 * @param permissions a list of the permissions to create
	 * @return the creation results in the same order as the given list, or {@code null} on failure
	 */
	@Transactional
	public List<Pair<PermissionDTO, String>> createPermissionsSecured(List<PermissionDTO> permissions) {
		if (permissions == null || permissions.isEmpty()) {
			return Collections.emptyList();
		}
		
		// Get the requesting subject from the current request object
		String requester = subjectIdFromRequest();
		
		// Create a list of validated permissions
		List<PermissionDTO> validated = new ArrayList<>();
		for (PermissionDTO p : permissions) {
			// Check that we have all the necessary information
			if (p == null) {
				continue;
			}
			
			if (Assertion.isNullOrEmpty(p.getSubjectId(), p.getResourceType(), p.getAction()) || p.getResourceId() == null) {
				log.trace("Skipping permission creation because mandatory data was missing.");
				continue;
			}
			
			// Check if the requesting subject/user is allowed to grant the permission
			if (!isBoundedDelegationAllowed(requester, p.getResourceType(), p.getResourceId(), p.getAction())) {
				log.debug("Permission creation denied because requester \"" + requester + "\" is not allowed to "
						+ "grant action \"" + p.getAction() + "\" for resource (type = " + p.getResourceType() 
						+ ", id = " + p.getResourceId() + ").");
				continue;
			}
			
			validated.add(p);
		}
		
		if (validated.isEmpty()) {
			log.trace("No permissions remained after bounded-delegation checks.");
			return Collections.emptyList();
		}
		
		return createPermissions(validated);
	}
    
    /**
     * Method to retrieve all permissions a given subject has.
     * 
     * @param subjectId the (Keycloak) ID of the subject
     * @return a list of all the permissions found
     */
    @Transactional
	public List<PermissionDTO> getAllPermissionsForSubject(String subjectId) {
		// Build and execute the query
		List<PermissionGrant> permissions = null;
		try {
			// Execute the query
			permissions = dsl.selectFrom(PERMISSION_GRANT)
					.where(PERMISSION_GRANT.SUBJECT_ID.equalIgnoreCase(subjectId))
					.fetchInto(PermissionGrant.class);
		} catch (MappingException e) {
			log.debug("Could not map the permission search result into the PermissionGrant-POJO.");
		} catch (DataAccessException f) {
			log.debug("Searching for the permission in the database failed: " + f.getMessage());
		}

		// Check if the search was successful
		if (permissions == null || permissions.size() == 0) {
			log.debug("No permission for subject with ID \"" + subjectId + "\" found.");
			return Collections.emptyList();
		}

		// Create a list of PermissionDTOs
		return permissions.stream().map(p -> new PermissionDTO().assignPojoValues(p)).toList();
	}
    
    /**
     * Method to retrieve a permission represented by the unique combination of subjectId, resourceType, resourceId,
     * and action.
     * 
     * @param subjectId the (Keycloak) ID of the subject of the permission
     * @param resourceType the type of the resource that this permission is about, e.g., 'Domain' or 'Project'
     * @param resourceId the (internal) database ID of the resource
     * @param action the action that is permitted, e.g., project:read or domain:create
     * @return the single permission identified by the given attributes
     */
    @Transactional
	public PermissionDTO getPermission(String subjectId, String resourceType, Integer resourceId, String action) {
		// Build and execute the query
		PermissionGrant permission = null;
		try {
			// Execute the query
			permission = dsl.selectFrom(PERMISSION_GRANT)
					.where(PERMISSION_GRANT.SUBJECT_ID.equalIgnoreCase(subjectId))
					.and(PERMISSION_GRANT.RESOURCE_TYPE.equalIgnoreCase(resourceType))
					.and(PERMISSION_GRANT.RESOURCE_ID.equal(resourceId))
					.and(PERMISSION_GRANT.ACTION.equalIgnoreCase(action))
					.fetchOneInto(PermissionGrant.class);
		} catch (MappingException e) {
			log.debug("Could not map the permission search result into the PermissionGrant-POJO.");
		} catch (DataAccessException f) {
			log.debug("Searching for the permission in the database failed: " + f.getMessage());
		}

		// Check if the search was successful
		if (permission == null) {
			log.trace("No permission for (subjectId, resourceType, resourceId, action) = (" + subjectId + ", " + resourceType + ", " + resourceId + ", " + action + ") found.");
			return null;
		}

		// Transform the found permission and add the resource name
		PermissionDTO p = new PermissionDTO().assignPojoValues(permission);
		
		if (resourceType.equalsIgnoreCase("Domain")) {
			Domain d = ddba.getDomainByID(p.getResourceId());
			p.setDomainName(d == null ? null : d.getName());
		} else if (resourceType.equalsIgnoreCase("Project")) {
			ProjectDTO proj = pdba.getProjectByID(p.getResourceId());
			p.setProjectAbbreviation(proj == null ? null : proj.getAbbreviation());
		}
		
		return p;
	}
    
    /**
     * Method to retrieve all permissions of a given user for a given resource.
     * 
     * @param resourceType the type of the resource, e.g. DOMAIN or PROJECT
     * @param resourceId the resource's internal database ID
     * @param subjectId the subject's/user's (Keycloak) ID
     * @return a list of permissions the user has for the given resource
     */
    @Transactional
    public List<PermissionDTO> getPermissionsForResource(String resourceType, Integer resourceId, String subjectId) {
    	// Build and execute the query
		List<PermissionGrant> permissions = null;
		try {
			// Execute the query
			permissions = dsl.selectFrom(PERMISSION_GRANT)
					.where(PERMISSION_GRANT.SUBJECT_ID.equalIgnoreCase(subjectId))
					.and(PERMISSION_GRANT.RESOURCE_TYPE.equalIgnoreCase(resourceType))
					.and(PERMISSION_GRANT.RESOURCE_ID.equal(resourceId))
					.fetchInto(PermissionGrant.class);
		} catch (MappingException e) {
			log.debug("Could not map the permission search result into the PermissionGrant-POJO.");
		} catch (DataAccessException f) {
			log.debug("Searching for the permission in the database failed: " + f.getMessage());
		}

		// Check if the search was successful
		if (permissions == null || permissions.size() == 0) {
			log.debug("No permissions for subject with ID \"" + subjectId + "\" for resource with id \"" + resourceId + "\" found.");
			return Collections.emptyList();
		}

		// Create a list of PermissionDTOs
		return permissions.stream().map(p -> new PermissionDTO().assignPojoValues(p)).toList();
    }
    
    /**
     * Method to retrieve all permissions of a given user for a given domain.
     * 
     * @param domainId the domain's internal database ID
     * @param subjectId the subject's/user's (Keycloak) ID
     * @return a list of permissions the user has for the given domain
     */
    @Transactional
    public List<PermissionDTO> getPermissionsForDomain(Integer domainId, String subjectId) {
    	return getPermissionsForResource("DOMAIN", domainId, subjectId);
    }
    
    /**
     * Method to retrieve all permissions of a given user for a given project.
     * 
     * @param domainId the project's internal database ID
     * @param subjectId the subject's/user's (Keycloak) ID
     * @return a list of permissions the user has for the given project
     */
    @Transactional
    public List<PermissionDTO> getPermissionsForProject(Integer projectId, String subjectId) {
    	return getPermissionsForResource("PROJECT", projectId, subjectId);
    }
	
	/**
	 * Method to retrieve the currently active permissions for a given subject
	 * from the database.
	 * 
	 * @param subjectId the (Keycloak) ID of the subject/user
	 * @return a list of the currently allowed actions' names
	 */
	@Transactional
	public List<EffectivePermissionDTO> getCurrentlyAllowedActionsForSubject(String subjectId) {
        if (!Assertion.isNotNullOrEmpty(subjectId)) {
            return Collections.emptyList();
        }
        
        // Check if the data is in the cache
        return cachingService.getEffectivePermissionsForSubject(subjectId, () -> {
	        // Cache miss: get data from DB
        	// We only want to have non-expired permissions
	        OffsetDateTime now = OffsetDateTime.now();
	
	        // Retrieve the permissions from the database
	        try {
	        	return dsl.selectDistinct(PERMISSION_GRANT.RESOURCE_TYPE, PERMISSION_GRANT.RESOURCE_ID, PERMISSION_GRANT.ACTION)
	        		       .from(PERMISSION_GRANT)
	        		       .where(PERMISSION_GRANT.SUBJECT_ID.eq(subjectId))
	        		       .and(PERMISSION_GRANT.DECISION.eq("ALLOW"))
	        		       .and(PERMISSION_GRANT.VALID_FROM.isNull().or(PERMISSION_GRANT.VALID_FROM.le(now)))
	        		       .and(PERMISSION_GRANT.VALID_TO.isNull().or(PERMISSION_GRANT.VALID_TO.gt(now)))
	        		       .orderBy(PERMISSION_GRANT.RESOURCE_TYPE.asc(), PERMISSION_GRANT.RESOURCE_ID.asc(), PERMISSION_GRANT.ACTION.asc())
	        		       .fetch(r -> EffectivePermissionDTO.builder()
	        		            .resourceType(r.get(PERMISSION_GRANT.RESOURCE_TYPE))
	        		            .resourceName(getResourceNameOrAbbreviationForID(r.get(PERMISSION_GRANT.RESOURCE_TYPE), r.get(PERMISSION_GRANT.RESOURCE_ID)))
	        		            .action(r.get(PERMISSION_GRANT.ACTION))
	        		            .build()
	        		       );
			} catch (DataAccessException e) {
				log.debug("Retrieving the permissions from the database lead to an exception: ", e.getMessage(), e);
	            return null;
			}
        });
    }
    
    /**
     * Checks if a certain action is allowed for the given set of attributes.
     * 
     * @param subjectId the (Keycloak) ID of the subject of the permission
     * @param resourceType the type of the resource that this permission is about, e.g., 'Domain' or 'Project'
     * @param resourceId the (internal) database ID of the resource
     * @param action the action that should be checked, e.g., project:read or domain:create
     * @return {@code true} when the action is allowed, {@code false} otherwise
     */
    @Transactional
	public boolean isActionAllowed(String subjectId, String resourceType, Integer resourceId, String action) {
    	if (!Assertion.isNotNullOrEmpty(subjectId, resourceType, action) || resourceId == null) {
    		return false;
        }

    	// Check if the data is in the cache
        Set<String> allowedActions = cachingService.getAllowedActionsForContext(subjectId, resourceType, resourceId, () -> {
        	// Cache miss: get data from DB
        	// We only want to have non-expired permissions
        	OffsetDateTime now = OffsetDateTime.now();
        	
        	// Retrieve the permissions from the database
            try {
                // Load ALL currently allowed actions for that context
                return new HashSet<>(dsl.selectDistinct(PERMISSION_GRANT.ACTION)
                        .from(PERMISSION_GRANT)
                        .where(PERMISSION_GRANT.SUBJECT_ID.equalIgnoreCase(subjectId))
                        .and(PERMISSION_GRANT.RESOURCE_TYPE.equalIgnoreCase(resourceType))
                        .and(PERMISSION_GRANT.RESOURCE_ID.eq(resourceId))
                        .and(PERMISSION_GRANT.DECISION.equalIgnoreCase("ALLOW"))
                        .and(PERMISSION_GRANT.VALID_FROM.isNull().or(PERMISSION_GRANT.VALID_FROM.le(now)))
                        .and(PERMISSION_GRANT.VALID_TO.isNull().or(PERMISSION_GRANT.VALID_TO.gt(now)))
                        .fetch(PERMISSION_GRANT.ACTION));
            } catch (DataAccessException e) {
            	log.debug("Searching for the permission in the database failed: ", e.getMessage(), e);
                return null;
            }
        });

        if (allowedActions == null) {
        	// No allowed actions were found
            return false;
        }

        // Is the desired action in the list of allowed ones?
        boolean actionIsAllowed = allowedActions.contains(action);
        if (!actionIsAllowed) {
            log.trace("No permission for (subjectId, resourceType, resourceId, action) = (" + subjectId + ", " 
            		+ resourceType + ", " + resourceId + ", " + action + ") found.");
        }
        
        return actionIsAllowed;
	}
    
    /**
     * Method to delete multiple permissions at once in a batch. Not found permissions will be ignored.
     * Can also update a single permission (given as a list).
     * 
     * @param permissions a list of permissions to delete from the database
     * @return a list of the success results of each individual permission-delete-request
     */
	@Transactional
	public List<Boolean> deletePermissions(List<PermissionDTO> permissions) {
		if (permissions == null || permissions.isEmpty()) {
			return Collections.emptyList();
		}
		
		int n = permissions.size();
		List<Boolean> deleteSuccess = new ArrayList<>(n);
		
		// Create a list of delete statements
		List<DeleteConditionStep<PermissionGrantRecord>> deletions = new ArrayList<>(n);
		List<PermissionDTO> deletionKeysForCache = new ArrayList<>(n);

		for (PermissionDTO p : permissions) {
			// If input contains null, add a no-op (keeps result alignment)
			if (p == null) {
				deletions.add(dsl.delete(PERMISSION_GRANT).where(PERMISSION_GRANT.ID.eq(-1)));
				deletionKeysForCache.add(null);
				continue;
			}
			
			// Assert that we have the information we need
			if (Assertion.isNullOrEmpty(p.getSubjectId(), p.getResourceType(), p.getAction()) || p.getResourceId() == null) {
				// We are lacking information, see if an ID is given, so we can get the information first
				if (p.getId() != null) {
					// ID is given: overwrite the DTO with the data from the database 
					p = new PermissionDTO().assignPojoValues(dsl.selectFrom(PERMISSION_GRANT)
					        .where(PERMISSION_GRANT.ID.eq(p.getId()))
					        .fetchOne());
				} else {
					log.debug("There is not enough information to delete the record.");
					continue;
				}
			}

			// Create and add deletion statement
			deletions.add(dsl.delete(PERMISSION_GRANT)
					.where(PERMISSION_GRANT.SUBJECT_ID.equalIgnoreCase(p.getSubjectId()))
					.and(PERMISSION_GRANT.RESOURCE_TYPE.equalIgnoreCase(p.getResourceType()))
					.and(PERMISSION_GRANT.RESOURCE_ID.eq(p.getResourceId()))
					.and(PERMISSION_GRANT.ACTION.equalIgnoreCase(p.getAction())));
			
			// Keep track of which cached objects need to be removed
			deletionKeysForCache.add(p);
		}

		// Batch the delete statements and execute the batch
		int[] result = dsl.batch(deletions).execute();

		// Process the result
		int deleted = 0;
		int ignored = 0;

		for (int i = 0; i < result.length; i++) {
			if (result[i] == 1) {
				deleted++;
				deleteSuccess.add(true);
				
				// Invalidate cache-entries that are of deleted records
				PermissionDTO p = deletionKeysForCache.get(i);
				if (p != null) {
					cachingService.invalidateSubject(p.getSubjectId());
					cachingService.invalidateContext(p.getSubjectId(), p.getResourceType(), p.getResourceId());
				}
			} else if (result[i] == 0) {
				ignored++;
				deleteSuccess.add(false);
			} else {
				// Unexpected result, abort the complete transaction by throwing an exception
				log.error("The deletion would have affected an unexpected number of records (" + result[i] + ") "
						+ "when it should have only affected one record. The deletion process "
						+ "was therefore rolled back.");
				throw new UnexpectedResultSizeException(1, result[i]);
			}
		}

		log.trace("Deleted " + deleted + " permission(s).");
		log.trace("Ignored " + ignored + " permission(s).");
		log.debug("Successfully deleted " + deleted + " out of " + deletions.size() + " permission(s) in the database.");

		return deleteSuccess;
	}
	
	/**
	 * Deletes permissions after enforcing bounded delegation for the requesting user.
	 * 
	 * @param permissions a list of the permissions to delete
	 * @return the deletion results in the same order as the given list, or {@code null} on failure
	 */
	@Transactional
	public List<Boolean> deletePermissionsSecured(List<PermissionDTO> permissions) {
		if (permissions == null || permissions.isEmpty()) {
			return Collections.emptyList();
		}
		
		// Get the requesting subject from the current request object
		String requester = subjectIdFromRequest();
		
		// Create a list of validated permissions
		List<PermissionDTO> validated = new ArrayList<>();
		for (PermissionDTO p : permissions) {
			// Check that we have all the necessary information
			if (p == null) {
				continue;
			}
			
			if (Assertion.isNullOrEmpty(p.getSubjectId(), p.getResourceType(), p.getAction()) || p.getResourceId() == null) {
				log.trace("Skipping permission deletion because mandatory data was missing.");
				continue;
			}

			// Check if the requesting subject/user is allowed to revoke the permission
			if (!isBoundedDelegationAllowed(requester, p.getResourceType(), p.getResourceId(), p.getAction())) {
				log.debug("Permission deletion denied because requester \"" + requester + "\" is not allowed to "
						+ "manage action \"" + p.getAction() + "\" for resource (type = " + p.getResourceType() 
						+ ", id = " + p.getResourceId() + ").");
				continue;
			}
			
			validated.add(p);
		}
		
		if (validated.isEmpty()) {
			log.debug("No permissions remained to delete after bounded-delegation checks.");
			return Collections.emptyList();
		}
		
		return deletePermissions(validated);
	}
		
	/**
	 * Method to update multiple permissions at once in a batch. Not found permissions will be ignored.
     * Can also update a single permission (given as a list).
	 * 
	 * @param permissionUpdates a list of permission updates to insert into the database
	 * @return a list of the success results of each individual permission-update-request
	 */
	@Transactional
	public List<Boolean> updatePermissions(List<PermissionUpdateDTO> permissionUpdates) {
		// Check if there is something to do
		if (permissionUpdates == null || permissionUpdates.isEmpty()) {
			return Collections.emptyList();
		}

		int n = permissionUpdates.size();
		List<Boolean> updateSuccess = new ArrayList<>(n);

		// Prefill the result list with nulls
		for (int i = 0; i < n; i++) {
			updateSuccess.add(null);
		}

		String requester = subjectIdFromRequest();
		OffsetDateTime now = OffsetDateTime.now();

		List<UpdateConditionStep<PermissionGrantRecord>> updates = new ArrayList<>();
		List<Integer> originalIndex = new ArrayList<>();
		List<Pair<PermissionDTO, PermissionDTO>> invalidationPairs = new ArrayList<>();
		int updated = 0;
		int ignored = 0;

		// Create a list of update statements
		for (int j = 0; j < n; j++) {
			PermissionUpdateDTO u = permissionUpdates.get(j);

			// Check if the permission record that should be updated exists
			if (u == null || !u.hasIdentifyingInformation() || !u.hasUpdateData()) {
				log.debug("Permission update ignored: missing identifying information or no update data.");
				ignored++;
				updateSuccess.set(j, false);
				continue;
			}

			// Check existence of the record to update
			// Unique key: oldSubjectId + oldResourceType + oldResourceId + oldAction
			PermissionGrantRecord old = dsl.selectFrom(PERMISSION_GRANT)
					.where(PERMISSION_GRANT.SUBJECT_ID.eq(u.getOldSubjectId()))
					.and(PERMISSION_GRANT.RESOURCE_TYPE.eq(u.getOldResourceType()))
					.and(PERMISSION_GRANT.RESOURCE_ID.eq(u.getOldResourceId()))
					.and(PERMISSION_GRANT.ACTION.eq(u.getOldAction()))
					.fetchOne();

			if (old == null) {
				ignored++;
				updateSuccess.set(j, false);
				continue;
			}

			// Sanitize update values
			String newSubjectId = (Assertion.isNotNullOrEmpty(u.getNewSubjectId())) ? u.getNewSubjectId() : old.getSubjectId();
			String newResourceType = (Assertion.isNotNullOrEmpty(u.getNewResourceType())) ? u.getNewResourceType() : old.getResourceType();
			Integer newResourceId = (u.getNewResourceId() != null) ? u.getNewResourceId() : old.getResourceId();
			String newAction = (Assertion.isNotNullOrEmpty(u.getNewAction())) ? u.getNewAction() : old.getAction();
			String newDecision = (Assertion.isNotNullOrEmpty(u.getDecision())) ? u.getDecision() : old.getDecision();
			OffsetDateTime newValidFrom = (u.getValidFrom() != null) ? u.getValidFrom() : old.getValidFrom();
			OffsetDateTime newValidTo = (u.getValidTo() != null) ? u.getValidTo() : old.getValidTo();
			OffsetDateTime updatedAt = (u.getUpdatedAt() != null) ? u.getUpdatedAt() : now;
			String updatedBy = Assertion.isNotNullOrEmpty(u.getUpdatedBy()) ? u.getUpdatedBy() : requester;

			// Add to batch update
			updates.add(dsl.update(PERMISSION_GRANT)
					.set(PERMISSION_GRANT.SUBJECT_ID, newSubjectId)
					.set(PERMISSION_GRANT.RESOURCE_TYPE, newResourceType)
					.set(PERMISSION_GRANT.RESOURCE_ID, newResourceId)
					.set(PERMISSION_GRANT.ACTION, newAction)
					.set(PERMISSION_GRANT.DECISION, newDecision)
					.set(PERMISSION_GRANT.VALID_FROM, newValidFrom)
					.set(PERMISSION_GRANT.VALID_TO, newValidTo)
					.set(PERMISSION_GRANT.UPDATED_AT, updatedAt)
					.set(PERMISSION_GRANT.UPDATED_BY, updatedBy)
					.where(PERMISSION_GRANT.SUBJECT_ID.equalIgnoreCase(u.getOldSubjectId()))
					.and(PERMISSION_GRANT.RESOURCE_TYPE.equalIgnoreCase(u.getOldResourceType()))
					.and(PERMISSION_GRANT.RESOURCE_ID.eq(u.getOldResourceId()))
					.and(PERMISSION_GRANT.ACTION.equalIgnoreCase(u.getOldAction())));

			// Store the index of the original list
			originalIndex.add(j);
			
			// Keep track of the pairs of permission-grants that should be invalidated in the cache
			PermissionDTO oldPerm = PermissionDTO.builder().subjectId(old.getSubjectId()).resourceType(old.getResourceType()).resourceId(old.getResourceId()).build();
			PermissionDTO newPerm = PermissionDTO.builder().subjectId(newSubjectId).resourceType(newResourceType).resourceId(newResourceId).build();
			invalidationPairs.add(new Pair<PermissionDTO, PermissionDTO>(oldPerm, newPerm));
		}

		// If there is nothing to update, we’re done
		if (updates.isEmpty()) {
			log.trace("No permissions to update.");
			return updateSuccess;
		}

		// Batch the update statements and execute the batch
		int[] result = dsl.batch(updates).execute();

		// Process the result
		for (int i = 0; i < result.length; i++) {
			if (result[i] == 1) {
				// Successful
				updated++;
				updateSuccess.set(originalIndex.get(i), true);
				
				// Invalidate cache-entries that are of deleted records
				Pair<PermissionDTO, PermissionDTO> p = invalidationPairs.get(i);
				if (p != null && p.first() != null && p.second() != null) {
					cachingService.invalidateSubject(p.first().getSubjectId());
					cachingService.invalidateSubject(p.second().getSubjectId());
					cachingService.invalidateContext(p.first().getSubjectId(), p.first().getResourceType(), p.first().getResourceId());
					cachingService.invalidateContext(p.second().getSubjectId(), p.second().getResourceType(), p.second().getResourceId());
				}
			} else if (result[i] == 0) {
				// Failed, due to for example not finding the record
				ignored++;
				updateSuccess.set(originalIndex.get(i), false);
			} else {
				// Affected an unexpected number of records, so abort the update
				log.error("The update would have affected an unexpected number of records (" + result[i] + ") when it should "
				+ "have only affected one record(s). The batch update was therefore rolled back.");
				throw new UnexpectedResultSizeException(1, result[i]);
			}
		}

		log.trace("Updated " + updated + " permission(s).");
		log.trace("Ignored " + ignored + " permission(s).");
		log.debug("Successfully updated " + updated + " out of " + permissionUpdates.size() + " permission(s) in the database.");

		return updateSuccess;
	}
	
	/**
     * Method to add all domain-specific permissions at once for a given domain.
     * The user is identified through the request.
     * 
     * @param domainId the (internal) ID of the domain for which these permissions should be created
     * @return {@code true} when the insertion was successful, {@code false} otherwise
     */
	@Transactional
	public boolean addDomainPermissionsForSubject(int domainId) {
		// Get list of domain-related rights
		List<String> domainRights = roleConfig.getACERoles();
		if (domainRights == null || domainRights.isEmpty()) {
			log.trace("No permissions to add --> done.");
			return true;
		}
		
		String subjectID = subjectIdFromRequest();
		String resourceType = "DOMAIN";
		
		// Prepare a list of all permissions
		List<PermissionDTO> permissions = new ArrayList<PermissionDTO>();
		for (String action : domainRights) {
			permissions.add(PermissionDTO.builder()
					.subjectId(subjectID)
					.resourceType(resourceType)
					.resourceId(domainId)
					.action(action)
					.build());
		}
		
		// Add the permissions
		log.trace("Adding " + permissions.size() + " permissions for the domain with id: " + domainId);
		List<Pair<PermissionDTO, String>> results = createPermissions(permissions);
		
		if (results == null || results.isEmpty()) {
			log.error("Could not create any permissions for domain with id \"" + domainId + "\", so the process was aborted.");
			return false;
		} else if (results.contains(new Pair<PermissionDTO, String>(null, INSERTION_ERROR))) {
			log.warn("Could not add all permissions for domain with id \"" + domainId + "\". The permissions might be incomplete.");
			return true;
		}
		
		log.debug("Successfully created the permissions for domain with id: " + domainId);
		return true;
	}
	
	/**
     * Method to add all project-specific permissions at once for a given project.
     * The user is identified through the request.
     * 
     * @param projectAbbreviation the abbreviation of the project for which these permissions should be created
     * @return {@code true} when the insertion was successful, {@code false} otherwise
     */
	@Transactional
	public boolean addProjectPermissionsForSubject(String projectAbbreviation) {
		// Get list of project-related rights
		List<String> projectRights = roleConfig.getKINGRoles();
		if (projectRights == null || projectRights.isEmpty()) {
			log.trace("No permissions to add --> done.");
			return true;
		}
		
		String subjectID = subjectIdFromRequest();
		String resourceType = "PROJECT";
		int resourceID = pdba.getProjectByAbbreviation(projectAbbreviation).getId();
		
		// Prepare a list of all permissions
		List<PermissionDTO> permissions = new ArrayList<PermissionDTO>();
		for (String action : projectRights) {
			permissions.add(PermissionDTO.builder()
					.subjectId(subjectID)
					.resourceType(resourceType)
					.resourceId(resourceID)
					.action(action)
					.build());
		}
		
		// Add the permissions
		log.trace("Adding " + permissions.size() + " permissions for the project \"" + projectAbbreviation + "\".");
		List<Pair<PermissionDTO, String>> results = createPermissions(permissions);
		
		if (results == null || results.isEmpty()) {
			log.error("Could not create any permissions for project \"" + projectAbbreviation + "\", so the process was aborted.");
			return false;
		} else if (results.contains(new Pair<PermissionDTO, String>(null, INSERTION_ERROR))) {
			log.warn("Could not add all permissions for project \"" + projectAbbreviation + "\". The permissions might be incomplete.");
			return true;
		}
		
		log.debug("Successfully created the permissions for project \"" + projectAbbreviation + "\".");
		return true;
	}
	
	/**
     * Method to remove all domain-specific permissions at once for a given domain.
     * The user is identified through the request.
     * 
     * @param domainName the name of the domain for which the permissions should be removed
     * @return {@code true} when the deletion was successful, {@code false} otherwise
     */
	@Transactional
	public boolean removeDomainPermissionsForSubject(String domainName) {
		String subjectID = subjectIdFromRequest();
		String type = "DOMAIN";
		int resourceID = ddba.getDomainByName(domainName).getId();
		
		// Get list of domain-related permissions from the database
		List<PermissionDTO> activePermissions = getAllPermissionsForSubject(subjectID);

		// Check if the list is empty
		if (activePermissions == null || activePermissions.isEmpty()) {
			log.trace("No permissions to remove --> done.");
			return true;
		}
		
		// Remove all permissions in this list that are not related to the given domain
		activePermissions = activePermissions.stream().filter(p -> p.getResourceType().equalsIgnoreCase(type) && p.getResourceId() == resourceID).toList();
		
		// Check again if the list is empty
		if (activePermissions == null || activePermissions.isEmpty()) {
			log.trace("No permissions to remove --> done.");
			return true;
		}
		
		// Remove the permissions
		log.trace("Removing " + activePermissions.size() + " permissions for the domain \"" + domainName + "\".");
		List<Boolean> results = deletePermissions(activePermissions);
		
		if (results == null || results.isEmpty()) {
			log.error("Could not remove any permissions for domain \"" + domainName + "\", so the process was aborted.");
			return false;
		} else if (results.contains(false)) {
			log.warn("Could not remove all permissions for domain \"" + domainName + "\". There might be orphaned permissions.");
			return true;
		}

		// Invalidate cache entries
		cachingService.invalidateSubject(subjectID);
		cachingService.invalidateContext(subjectID, type, resourceID);
		
		log.debug("Successfully removed all permissions for domain \"" + domainName + "\".");
		return true;
	}
	
	/**
     * Method to remove all project-specific permissions at once for a given domain.
     * The user is identified through the request.
     * 
     * @param projectAbbreviation the abbreviation of the project for which the permissions should be removed
     * @return {@code true} when the deletion was successful, {@code false} otherwise
     */
	@Transactional
	public boolean removeProjectPermissionsForSubject(String projectAbbreviation) {
		String subjectID = subjectIdFromRequest();
		String type = "PROJECT";
		int resourceID = pdba.getProjectByAbbreviation(projectAbbreviation).getId();
		
		// Get list of project-related permissions from the database
		List<PermissionDTO> activePermissions = getAllPermissionsForSubject(subjectID);

		// Check if the list is empty
		if (activePermissions == null || activePermissions.isEmpty()) {
			log.trace("No permissions to remove --> done.");
			return true;
		}
		
		// Remove all permissions in this list that are not related to the given domain
		activePermissions = activePermissions.stream().filter(p -> p.getResourceType().equalsIgnoreCase(type) && p.getResourceId() == resourceID).toList();
		
		// Check again if the list is empty
		if (activePermissions == null || activePermissions.isEmpty()) {
			log.trace("No permissions to remove --> done.");
			return true;
		}
		
		// Remove the permissions
		log.trace("Removing " + activePermissions.size() + " permissions for the project \"" + projectAbbreviation + "\".");
		List<Boolean> results = deletePermissions(activePermissions);
		
		if (results == null || results.isEmpty()) {
			log.error("Could not remove any permissions for project \"" + projectAbbreviation + "\", so the process was aborted.");
			return false;
		} else if (results.contains(false)) {
			log.warn("Could not remove all permissions for project \"" + projectAbbreviation + "\". There might be orphaned permissions.");
			return true;
		}
		
		// Invalidate cache entries
		cachingService.invalidateSubject(subjectID);
		cachingService.invalidateContext(subjectID, type, resourceID);
		
		log.debug("Successfully removed all permissions for project \"" + projectAbbreviation + "\".");
		return true;
	}
	
	/**
     * Method to remove all permissions from the database.
     * Also resets serial-counters.
     * 
     * @return {@code true} when the deletion was successful, {@code false} otherwise
     */
	@Transactional
	public boolean resetAllPermissions() {
		try {
			dsl.truncate(PERMISSION_GRANT).restartIdentity().execute();
		} catch (DataAccessException e) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			
			log.error("Couldn't remove all permissions from the database and aborted: " + e.getMessage(), e);
			return false;
		}
		
		// Invalidate cache
		cachingService.clearAllPermissionCaches();
		
		return true;
	}

	/**
     * Method to remove all domain-specific permissions from the database.
     * 
     * @return {@code true} when the deletion was successful, {@code false} otherwise
     */
	@Transactional
	public boolean removeDomainPermissions() {
		try {
			dsl.deleteFrom(PERMISSION_GRANT).where(PERMISSION_GRANT.RESOURCE_TYPE.eq("DOMAIN")).execute();
		} catch (DataAccessException e) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			
			log.error("Couldn't remove all domain-specific permissions from the database and aborted: " + e.getMessage(), e);
			return false;
		}
		
		// Invalidate cache
		cachingService.clearAllPermissionCaches();

		return true; 
	}

	/**
     * Method to remove all project-specific permissions from the database.
     * 
     * @return {@code true} when the deletion was successful, {@code false} otherwise
     */
	@Transactional
	public boolean removeProjectPermissions() {
		try {
			dsl.deleteFrom(PERMISSION_GRANT).where(PERMISSION_GRANT.RESOURCE_TYPE.eq("PROJECT")).execute();
		} catch (DataAccessException e) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			
			log.error("Couldn't remove all project-specific permissions from the database and aborted: " + e.getMessage(), e);
			return false;
		}
		
		// Invalidate cache
		cachingService.clearAllPermissionCaches();

		return true;
	}

	/**
     * Method to remove all global permissions from the database.
     * 
     * @return {@code true} when the deletion was successful, {@code false} otherwise
     */
	@Transactional
	public boolean removeGlobalPermissions() {
		try {
			dsl.deleteFrom(PERMISSION_GRANT).where(PERMISSION_GRANT.RESOURCE_TYPE.eq("GLOBAL")).execute();
		} catch (DataAccessException e) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			
			log.error("Couldn't remove all global permissions from the database and aborted: " + e.getMessage(), e);
			return false;
		}
		
		// Invalidate cache
		cachingService.clearAllPermissionCaches();

		return true;
	}

	/**
     * Method to remove all permissions for a given user.
     * 
     * @param subjectId the (Keycloak) ID of the subject/user
     * @return {@code true} when the deletion was successful, {@code false} otherwise
     */
	@Transactional
	public boolean removePermissionsForSubjectByID(String subjectId) {
		try {
			dsl.deleteFrom(PERMISSION_GRANT).where(PERMISSION_GRANT.SUBJECT_ID.eq(subjectId)).execute();
		} catch (DataAccessException e) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			
			log.error("Couldn't remove all permissions for the given user from the database and aborted: " + e.getMessage(), e);
			return false;
		}
		
		// Invalidate cache entries
		cachingService.invalidateSubject(subjectId);

		return true;
	}

	/**
     * Method to remove all permissions for a given user.
     * (Since we need the Keycloak ID of the subject, this method does a 
     * Keycloak lookup to map the given name to the proper ID.)
     * 
     * @param subjectName the subject's/user's name
     * @return {@code true} when the deletion was successful, {@code false} otherwise
     */
	@Transactional
	public boolean removePermissionsForSubjectByName(String subjectName) {
		// Query Keycloak for the ID from the name 
		String subjectId = keycloakService.subjectIdByUsername(subjectName);
		
		// Check if we found anything
		if (subjectId == null || subjectId.isBlank()) {
			log.debug("Cannot reset the subject's permissions because it's name was'nt found in Keycloak.");
			return false;
		}
		
		try {
			dsl.deleteFrom(PERMISSION_GRANT).where(PERMISSION_GRANT.SUBJECT_ID.eq(subjectId)).execute();
		} catch (DataAccessException e) {
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
			
			log.error("Couldn't remove all permissions for the given user from the database and aborted: " + e.getMessage(), e);
			return false;
		}
		
		// Invalidate cache entries
		cachingService.invalidateSubject(subjectId);

		return true;
	}
	
	/**
	 * Checks whether a subject is allowed to manage permissions for the given resource.
	 * 
	 * @param subjectId the subject's Keycloak ID
	 * @param resourceType the resource type
	 * @param resourceId the resource ID
	 * @return {@code true} if the subject may manage permissions for that resource, {@code false} otherwise
	 */
	private boolean isPermissionManagementAllowed(String subjectId, String resourceType, Integer resourceId) {
		if (Assertion.isNullOrEmpty(resourceType) || resourceId == null) {
			return false;
		}
		
		// Get the action for the resourceType that is affected
		String action = null;		
		if (resourceType.equalsIgnoreCase("DOMAIN")) {
			action = "domain:manage-permissions";
		} else if (resourceType.equalsIgnoreCase("PROJECT")) {
			action = "project:manage-permissions";
		} else if (resourceType.equalsIgnoreCase("GLOBAL")) {
			action = "global:manage-permissions";
		} else {
			log.debug("Unrecognized resourceType: " + resourceType);
			return false;
		}
		
		return Assertion.isNullOrEmpty(action) ? false : isActionAllowed(subjectId, resourceType, resourceId, action);
	}
	
	/**
	 * Checks whether a subject is allowed to grant a specific action for the given resource.
	 * This is the bounded delegation rule: 
	 * 1. the assigning subject must be allowed to manage permissions on the resource
	 * 2. the assigning subject must already have the exact action that should be granted
	 * 
	 * @param subjectId the assigning subject's Keycloak ID
	 * @param resourceType the resource type
	 * @param resourceId the resource ID
	 * @param action the action to grant
	 * @return {@code true} when granting is allowed, {@code false} otherwise
	 */
	private boolean isBoundedDelegationAllowed(String subjectId, String resourceType, Integer resourceId, String action) {
		if (Assertion.isNullOrEmpty(subjectId, resourceType, action) || resourceId == null) {
			return false;
		}
		
		// The action must match the given resource type
		if (resourceType.equalsIgnoreCase("DOMAIN") && !roleConfig.getACERoles().contains(action)) {
			log.trace("Bounded delegation not allowed, as the given action (" + action + ") is not domain-scoped, as the resourceType suggested.");
			return false;
		} else if (resourceType.equalsIgnoreCase("PROJECT") && !roleConfig.getKINGRoles().contains(action)) {
			log.trace("Bounded delegation not allowed, as the given action (" + action + ") is not project-scoped, as the resourceType suggested.");
			return false;
		} else if (resourceType.equalsIgnoreCase("GLOBAL") && !roleConfig.getGlobalRoles().contains(action)) {
			log.trace("Bounded delegation not allowed, as the given action (" + action + ") is not global-scoped, as the resourceType suggested.");
			return false;
		}
		
		// The assigning subject must be allowed to manage permissions on this resource
		if (!isPermissionManagementAllowed(subjectId, resourceType, resourceId)) {
			log.trace("The assigning subject is not allowed to grant actions for this resourceType (" + resourceType + 
					") and resource (ID = " + resourceId + ").");
			return false;
		}
		
		// The assigning subject must already have the exact action they want to delegate
		if (!isActionAllowed(subjectId, resourceType, resourceId, action)) {
			log.trace("The assigning subject itself does not have the permission that should be granted, "
					+ "so it can't be granted to someone else.");
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Replaces the ALLOWed-permissions for (subjectId, resourceType, resourceId) with exactly the provided actions.
     * After this method, the database will contain exactly one ALLOWed grant per action in {@code actions}.
     * Existing DENY grants are left untouched (if there are any).
	 * 
	 * @param subjectId the (Keycloak) ID of the subject for which the permissions should be changed
     * @param resourceType the type of the resource that these permissions are about, e.g., 'Domain' or 'Project'
     * @param resourceId the (internal) database ID of the resource
     * @param actions the desired list of actions that should be allowed
	 * @return {@code true} if the operation completed successfully, {@code false} if input invalid; {@code null} on failure.
	 */
	@Transactional
	public boolean replacePermissionsForResource(String subjectId, String resourceType, Integer resourceId, List<String> actions) {
		if (!Assertion.isNotNullOrEmpty(subjectId, resourceType) || resourceId == null || actions == null) {
            log.debug("Missing parameters.");
			return false;
        }

        // Normalize desired actions: drop nulls, trim, drop blanks, ensure uniqueness
        Set<String> desired = actions.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toCollection(HashSet::new));

        // Retrieve current ALLOWed actions for this subject and resource
        Set<String> current = new HashSet<>(dsl.select(PERMISSION_GRANT.ACTION)
                      .from(PERMISSION_GRANT)
                      .where(PERMISSION_GRANT.SUBJECT_ID.eq(subjectId))
                      .and(PERMISSION_GRANT.RESOURCE_TYPE.eq(resourceType))
                      .and(PERMISSION_GRANT.RESOURCE_ID.eq(resourceId))
                      .and(PERMISSION_GRANT.DECISION.eq("ALLOW"))
                      .fetch(PERMISSION_GRANT.ACTION)
        );

        // Compute differences between current and desired
        Set<String> actionsToInsert = new HashSet<>(desired);
        actionsToInsert.removeAll(current);

        Set<String> actionsToDelete = new HashSet<>(current);
        actionsToDelete.removeAll(desired);

        // Check if there is anything to do
        if (actionsToInsert.isEmpty() && actionsToDelete.isEmpty()) {
        	log.debug("There are no actions to add or delete. Done.");
            return true;
        }

        // Delete those actions that are not needed anymore
        if (!actionsToDelete.isEmpty()) {
        	List<PermissionDTO> deletes = new ArrayList<>(actionsToDelete.size());
        	
        	// Build a list of PermissionDTOs
        	for (String action : actionsToDelete) {
        		deletes.add(PermissionDTO.builder().subjectId(subjectId).resourceType(resourceType)
        				.resourceId(resourceId).action(action).build());
        	}
        	
        	// Call the delete method
        	List<Boolean> result = deletePermissions(deletes);
        	if (result == null || result.contains(false)) {
        		log.debug("Could not properly remove the actions that are not needed anymore. Aborting.");
        		throw new PermissionManagementException(resourceType + ":" + resourceId.toString());
        	}
        }
        
        // Insert missing actions
        if (!actionsToInsert.isEmpty()) {
        	List<PermissionDTO> inserts = new ArrayList<>(actionsToInsert.size());
        	
        	// Build a list of PermissionDTOs
        	for (String action : actionsToInsert) {
        		inserts.add(PermissionDTO.builder().subjectId(subjectId).resourceType(resourceType)
        				.resourceId(resourceId).decision("ALLOW").action(action).build());
        	}
        	
        	// Call the create method
        	List<Pair<PermissionDTO, String>> result = createPermissions(inserts);
        	if (result == null || result.contains(new Pair<PermissionDTO, String>(null, INSERTION_ERROR))) {
        		log.debug("Could not properly add the actions that were required. Aborting.");
        		throw new PermissionManagementException(resourceType + ":" + resourceId.toString());
        	}
        }
        
        // Invalidate cache for entries of the replaced permissions
        cachingService.invalidateContext(subjectId, resourceType, resourceId);
        cachingService.invalidateSubject(subjectId);

        log.debug("Successfully replaced the permissions as required.");
        return true;
	}
	
	/**
	 * Replaces all permissions for a resource after enforcing bounded delegation with the list of given permissions.
	 * The requester may only grant actions that they already hold on the same resource,
	 * and only if they also hold the corresponding *:manage-permissions action.
	 * 
	 * @param subjectId the target subject for whom permissions should be replaced
	 * @param resourceType the resource type
	 * @param resourceId the resource ID
	 * @param actions the desired target set of allowed actions
	 * @return {@code true} when the replacement was successful, {@code false} otherwise
	 */
	@Transactional
	public boolean replacePermissionsForResourceSecured(String subjectId, String resourceType, Integer resourceId, List<String> actions) {
		if (!Assertion.isNotNullOrEmpty(subjectId, resourceType) || resourceId == null || actions == null) {
			log.debug("Missing parameters.");
			return false;
		}
		
		// Get the requesting subject from the current request object
		String requester = subjectIdFromRequest();
		
		// Check that the requester is allowed to manage permissions on this resource
		if (!isPermissionManagementAllowed(requester, resourceType, resourceId)) {
			log.debug("Permission replacement denied because requester \"" + requester + "\" is not allowed "
					+ "to manage permissions for resource (type = " + resourceType + ", id = " + resourceId + ").");
			return false;
		}
		
		// Normalize the action strings and remove duplicates
		List<String> normalizedRequestedActions = actions.stream()
				.filter(Objects::nonNull)
				.map(String::trim)
				.filter(s -> !s.isBlank())
				.distinct()
				.toList();
		
		// Check if there are any permissions in the list that the requester is not allowed to grant to others
		boolean areAllActionsGrantable = normalizedRequestedActions.stream()
				.allMatch(a -> isBoundedDelegationAllowed(requester, resourceType, resourceId, a));
		
		if (!areAllActionsGrantable) {
			log.debug("Permission replacement denied because at least one requested action was not "
					+ "grantable by requester \"" + requester + "\".");
			return false;
		}
		
		return replacePermissionsForResource(subjectId, resourceType, resourceId, normalizedRequestedActions);
	}

	/**
	 * Method that extracts the subject ID from the request object.
	 * 
	 * @return the subject ID that is in the JWT token in the request
	 */
	private String subjectIdFromRequest() {
		// Try HTTP request principal first
		HttpServletRequest request = Utility.getCurrentRequest();
		if (request != null) {
			// Check if the information needed is in the token
			Principal principal = request.getUserPrincipal();
	        if (principal instanceof JwtAuthenticationToken token
	                && token.getToken() != null
	                && token.getToken().getSubject() != null
	                && !token.getToken().getSubject().isBlank()) {
	            return token.getToken().getSubject();
	        }
		}
		
		// Try security context next
	    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	    if (authentication instanceof JwtAuthenticationToken token
	            && token.getToken() != null
	            && token.getToken().getSubject() != null
	            && !token.getToken().getSubject().isBlank()) {
	        return token.getToken().getSubject();
	    }
	    
	    // Finally, try the thread-local context to see if there is any information in there
	    AuditInformationObject aio = AuditTriggerContext.get();
	    if (aio != null && Assertion.isNotNullOrEmpty(aio.getUsername())) {
	        return aio.getUsername();
	    } else if (aio != null && aio.getAuditSourceSystem() == AuditSourceSystem.KAFKA) {
	        return "KAFKA";
	    }
		
		// The subject ID could not be found
		return "UNKNOWN";
	}
	
	/**
	 * Helper method that returns the name of a domain or the abbreviation of a project
	 * given the resource's (internal) ID.
	 * 
	 * @param resourceType the resource's type, e.g. "DOMAIN" or "PROJECT"
	 * @param id the resource's (internal) ID
	 * @return the name (for domains) or the abbreviation (for projects) of the resource
	 */
	public String getResourceNameOrAbbreviationForID(String resourceType, int id) {
		if (resourceType == null || id < 0) {
			log.trace("Invalid parameters for ID to resource name/abbreviation mapping.");
			return null;
		}
		
		if (resourceType.equalsIgnoreCase("DOMAIN")) {
			Domain d = ddba.getDomainByID(id);
			return d == null ? null : d.getName();
		} else if (resourceType.equalsIgnoreCase("PROJECT")) {
			ProjectDTO p = pdba.getProjectByID(id);
			return p == null ? null : p.getAbbreviation();
		} else if (resourceType.equalsIgnoreCase("GLOBAL")) {
			return null;
		} else {
			return null;
		}
	}
	
	/**
	 * Helper method that returns the resource's (internal) ID given 
	 * the name of a domain or the abbreviation of a project.
	 * 
	 * @param resourceType the resource's type, e.g. "DOMAIN" or "PROJECT"
	 * @param nameOrAbbreviation the resource's name (for domains) or abbreviation (for projects)
	 * @return the (internal) ID of the resource, '0' for resourceType=GLOBAL, null on failure
	 */
	public Integer getResourceIDForNameOrAbbreviation(String resourceType, String nameOrAbbreviation) {
		if (Assertion.isNullOrEmpty(resourceType, nameOrAbbreviation)) {
			log.trace("Invalid parameters for resource name/abbreviation to ID mapping.");
			return null;
		}
		
		if (resourceType.equalsIgnoreCase("DOMAIN")) {
			Domain d = ddba.getDomainByName(nameOrAbbreviation);
			return d == null ? null : d.getId();
		} else if (resourceType.equalsIgnoreCase("PROJECT")) {
			ProjectDTO p = pdba.getProjectByAbbreviation(nameOrAbbreviation);
			return p == null ? null : p.getId();
		} else if (resourceType.equalsIgnoreCase("GLOBAL")) {
			return 0;
		} else {
			return null;
		}
	}
}
