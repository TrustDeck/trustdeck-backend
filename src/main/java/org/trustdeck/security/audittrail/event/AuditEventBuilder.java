/*
 * Trust Deck Services
 * Copyright 2023-2026 Armin Müller and Eric Wündisch
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

package org.trustdeck.security.audittrail.event;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.trustdeck.jooq.generated.tables.pojos.AuditEvent;
import org.trustdeck.security.audittrail.annotation.Audit;
import org.trustdeck.security.audittrail.context.AuditInformationObject;
import org.trustdeck.security.audittrail.usertype.AuditUserType;

import java.time.LocalDateTime;

/**
 * A builder class for creating jOOQ AuditEvent POJOs.
 *
 * @author Armin Müller
 */
@Service
@NoArgsConstructor
@Slf4j
public class AuditEventBuilder {

    /**
	 * Builds an Auditevent object which can then be inserted into the database.
	 *
	 * @param auditAnnotation the audit annotation object, containing information on when to audit
	 * @param aio the resolved audit information object containing information on what to audit
	 * @return the audit event or {@code null}
	 */
	public AuditEvent build(Audit auditAnnotation, AuditInformationObject aio) {
		// Ensure that we have the information needed
		if (auditAnnotation == null || aio == null) {
			// No information available
			return null;
		}

        // Decide if auditing is even required
        if (!isAuditingRequired(auditAnnotation, aio.getUserType())) {
            return null;
        }
        
        // Build storable object
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setRequestTime(LocalDateTime.now());
        auditEvent.setUserName(aio.getUsername() != null ? aio.getUsername() : "UNKNOWN");
        auditEvent.setRequesterIp(aio.getRequesterIp() != null ? aio.getRequesterIp() : "UNKNOWN");
        auditEvent.setRequestUrl(aio.getTarget() != null ? aio.getTarget() : "UNKNOWN");
        auditEvent.setRequestBody(aio.getPayload());
        auditEvent.setSourceSystem(aio.getAuditSourceSystem() != null ? aio.getAuditSourceSystem().name() : "UNKNOWN");

        return auditEvent;
    }

    /**
     * Helper method to decide if auditing should be performed for the current user.
     * (Depends on the group the user is in.)
     *
     * @param auditAnnotation the audit annotation containing the information for which user type the requests should be audited
     * @param userType the requester's user type (e.g., human, technical, ... )
     * @return {@code true} when the request for the current user and endpoint should be audited, {@code false} otherwise.
     * @apiNote User-assigned groups (no-auditing, audit-everything) have priority over method-annotated auditing targets
     * (e.g., requests from a user with an assigned no-auditing group will not be audited even when the method's
     * AuditAnnotation targets all requests).
     */
    private boolean isAuditingRequired(Audit auditAnnotation, AuditUserType userType) {
        // Get the type of the user that is triggering the auditing
    	if (auditAnnotation == null || userType == null) {
    		return false;
    	}

        // Check if the user is assigned to the no-auditing or audit-everything group
        if (userType == AuditUserType.NONE) {
            return false;
        } else if (userType == AuditUserType.ALL) {
            return true;
        }

        // Check if the auditing group assigned to the user and the target group in the Audit annotation match
        switch (auditAnnotation.auditFor()) {
            case HUMAN:
            	return userType == AuditUserType.HUMAN;
            case TECHNICAL:
            	return userType == AuditUserType.TECHNICAL;
            case NONE:
                return false;
            case ALL:
            case UNKNOWN:
            default:
                return true;
        }
    }
}
