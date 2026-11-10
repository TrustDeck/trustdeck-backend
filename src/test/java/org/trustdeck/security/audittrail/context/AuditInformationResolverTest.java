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

package org.trustdeck.security.audittrail.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.trustdeck.configuration.AuditUserTypeConfiguration;
import org.trustdeck.security.audittrail.usertype.AuditUserType;

/**
 * Tests that audit information is resolved from REST requests only.
 */
class AuditInformationResolverTest {

	private final AuditInformationResolver resolver = new AuditInformationResolver();

	@BeforeEach
	void setUp() {
		AuditUserTypeConfiguration configuration = new AuditUserTypeConfiguration();
		configuration.setTokenClaimName("trustdeck_groups");
		ReflectionTestUtils.setField(resolver, "auditUserTypeConfig", configuration);
	}

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
		SecurityContextHolder.clearContext();
	}

	/** Outside a REST request there is nothing to audit. */
	@Test
	void returnsNullWithoutRequest() {
		assertNull(resolver.resolve());
	}

	/** A REST request is resolved with its method, URL, body, and requester IP. */
	@Test
	void resolvesRestRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/domains/test-domain/pseudonyms");
		request.setQueryString("omitPrefix=false");
		request.addParameter("omitPrefix", "false");
		request.setRemoteAddr("192.0.2.10");
		request.setCharacterEncoding("UTF-8");
		request.setContent("{\"psn\": \"TST-1\"}".getBytes(StandardCharsets.UTF_8));
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		AuditInformationObject aio = resolver.resolve();

		assertEquals(AuditSourceSystem.REST, aio.getAuditSourceSystem());
		assertEquals("[POST] /domains/test-domain/pseudonyms?omitPrefix=false", aio.getTarget());
		assertEquals("{\"psn\":\"TST-1\"}", aio.getPayload());
		assertEquals("192.0.2.10", aio.getRequesterIp());
		assertEquals("UNKNOWN", aio.getUsername());
		assertEquals(AuditUserType.UNKNOWN, aio.getUserType());
	}
}
