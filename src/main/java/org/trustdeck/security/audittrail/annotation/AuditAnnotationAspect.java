/*
 * Trust Deck Services
 * Copyright 2024-2026 Armin Müller and contributors
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

package org.trustdeck.security.audittrail.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.trustdeck.exception.AuditTrailException;
import org.trustdeck.jooq.generated.tables.daos.AuditEventDao;
import org.trustdeck.jooq.generated.tables.pojos.AuditEvent;
import org.trustdeck.security.audittrail.context.AuditInformationObject;
import org.trustdeck.security.audittrail.context.AuditInformationResolver;
import org.trustdeck.security.audittrail.context.AuditTriggerContext;
import org.trustdeck.security.audittrail.event.AuditEventBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Class that automatically intercepts @Audit annotations and stores 
 * the collected information using the same transaction as the database method.
 * Works for REST endpoints; invocations outside a REST request are not audited.
 * 
 * @author Armin Müller
 */
@Aspect
@Component
@Slf4j
public class AuditAnnotationAspect {

	/** The builder that creates a coherent audit object out of the collected information. */
    @Autowired
    private AuditEventBuilder auditEventBuilder;
    
    /** Resolver for current audit information. */
    @Autowired
    private AuditInformationResolver auditInformationResolver;

	/** The database context. */
    @Autowired
    private DSLContext dsl;
    
    /**
     * Method to retrieve the audit event data access object.
     *
     * @return the audit event DAO
     */
    private AuditEventDao getAuditeventDao() {
    	// Attaches DAO to the current transactional context
    	return new AuditEventDao(this.dsl.configuration());
    }

    /**
     * The method that intercepts the audit annotation, collects all necessary information
     * and writes the audit trail to the database.
     * 
     * @param joinPoint the point where the process execution should proceed from
     * @param auditAnnotation the audit annotation object that contains some audit information
     * @return the result of proceeding
     * @throws AuditTrailException if anything goes wrong while the audit trail is written, this exception
     * 			is thrown in order to abort the current transaction and roll back to a consistent database state.
     */
    @Around("@annotation(auditAnnotation)")
    @Transactional(propagation = Propagation.REQUIRED)
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audit auditAnnotation) throws AuditTrailException {
        // Gather information
        AuditInformationObject aio = auditInformationResolver.resolve();

        try {
            // Check if we have any information, or if we should skip auditing
            if (aio != null) {
            	// Add audit information to the current thread local context
            	AuditTriggerContext.set(aio);
            	
            	 // Create audit event object
	            AuditEvent auditEvent = auditEventBuilder.build(auditAnnotation, aio);
	            if (auditEvent != null) {
	            	// Write audit information into database
	            	this.getAuditeventDao().insert(auditEvent);
	            }
            } else {
                log.trace("No REST request information available. Skip auditing.");
            }
            
            // Proceed with the original method
            return joinPoint.proceed();
        } catch (Throwable e) {
        	log.debug("Audit trail information could not be stored: ", e);
            
            // Throw an exception to terminate the transaction that surrounds the audit annotation processing
            throw new AuditTrailException();
        } finally {
        	// Remove audit information from thread
        	AuditTriggerContext.clear();
        }
    }
}
