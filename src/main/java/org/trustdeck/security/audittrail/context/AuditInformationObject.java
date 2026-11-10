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

package org.trustdeck.security.audittrail.context;

import org.trustdeck.security.audittrail.usertype.AuditUserType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Information object that contains the necessary 
 * information to create an Audit Trail entry.
 *
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditInformationObject {

    /** Source system that triggered the auditing. */
    private AuditSourceSystem auditSourceSystem;

    /** Username or technical actor/process. */
    private String username;

    /** The audit user type (e.g., human or technical). */
    private AuditUserType userType;

    /** The requester's IP, if available. */
    private String requesterIp;

    /** Request target, i.e. the HTTP method and URL. */
    private String target;

    /** Request body. */
    private String payload;
}