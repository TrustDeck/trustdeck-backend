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

package org.trustdeck.security.audittrail.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.trustdeck.jooq.generated.tables.pojos.AuditEvent;
import org.trustdeck.security.audittrail.annotation.Audit;
import org.trustdeck.security.audittrail.context.AuditInformationObject;
import org.trustdeck.security.audittrail.context.AuditSourceSystem;
import org.trustdeck.security.audittrail.usertype.AuditUserType;

/**
 * Tests that the audit event carries the stated on-behalf-of and purpose information.
 */
class AuditEventBuilderTest {

	private final AuditEventBuilder builder = new AuditEventBuilder();

	/** The on-behalf-of and purpose values are stored next to the requester. */
	@Test
	void storesOnBehalfOfAndPurpose() {
		AuditInformationObject aio = AuditInformationObject.builder()
				.auditSourceSystem(AuditSourceSystem.REST)
				.username("service-account-fhir-dashboard")
				.userType(AuditUserType.TECHNICAL)
				.target("[POST] /domains/fhir/pseudonyms/lookup")
				.onBehalfOf("jdoe")
				.purpose("re-identification")
				.build();

		AuditEvent event = builder.build(annotation(), aio);
		assertEquals("service-account-fhir-dashboard", event.getUserName());
		assertEquals("jdoe", event.getOnBehalfOf());
		assertEquals("re-identification", event.getPurpose());
	}

	/** Without the headers, both values stay empty. */
	@Test
	void leavesValuesEmptyWithoutHeaders() {
		AuditInformationObject aio = AuditInformationObject.builder().username("jdoe").userType(AuditUserType.HUMAN).build();

		AuditEvent event = builder.build(annotation(), aio);
		assertNull(event.getOnBehalfOf());
		assertNull(event.getPurpose());
	}

	private static Audit annotation() {
		Audit audit = mock(Audit.class);
		when(audit.auditFor()).thenReturn(AuditUserType.ALL);
		return audit;
	}
}
