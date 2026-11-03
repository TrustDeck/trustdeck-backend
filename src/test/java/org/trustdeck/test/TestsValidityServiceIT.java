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

package org.trustdeck.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.trustdeck.dto.DomainDTO;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.service.AssertWebRequestService;

/**
 * This class offers tests for the validity periods of domains and pseudonyms,
 * including their inheritance in domain hierarchies.
 *
 * @author Armin Müller
 */
public class TestsValidityServiceIT extends AssertWebRequestService {

    /** The domain created for every test. */
    private static final String DOMAIN = "TestStudie";

    /** The abbreviation of the project that is created for every test. */
    private static final String PROJECT = "TEST";

    /** The start of the test domain's validity period. */
    private static final LocalDateTime DOMAIN_VALID_FROM = LocalDateTime.of(2022, 2, 26, 19, 15, 20, 885853000);

    /** The end of the test domain's validity period. */
    private static final LocalDateTime DOMAIN_VALID_TO = LocalDateTime.of(2052, 2, 19, 19, 15, 20, 885853000);

    /**
     * Tests that child domains inherit the validity period and its enforcement from their parent if not given.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("domainValidityInheritanceTest")
    public void domainValidityInheritanceTest() throws Exception {
        this.createDomain("TestStudie-Child", "TS-C", DOMAIN, null, null);
        this.createDomain("TestStudie-Grandchild", "TS-G", "TestStudie-Child", null, null);
        LocalDateTime explicitValidTo = LocalDateTime.of(2030, 1, 1, 0, 0);
        this.createDomain("TestStudie-Explicit", "TS-E", DOMAIN, null, explicitValidTo);

        // Validity period and enforcement are inherited through the hierarchy
        for (String name : new String[] {"TestStudie-Child", "TestStudie-Grandchild"}) {
            DomainDTO domain = this.getDomain(name);
            assertEquals(DOMAIN_VALID_FROM, domain.getValidFrom());
            assertEquals(DOMAIN_VALID_TO, domain.getValidTo());
            assertTrue(domain.getValidFromInherited());
            assertTrue(domain.getValidToInherited());
            assertTrue(domain.getEnforceStartDateValidity());
            assertTrue(domain.getEnforceStartDateValidityInherited());
            assertTrue(domain.getEnforceEndDateValidity());
            assertTrue(domain.getEnforceEndDateValidityInherited());
        }

        // Explicitly given values are not inherited
        DomainDTO explicit = this.getDomain("TestStudie-Explicit");
        assertEquals(DOMAIN_VALID_FROM, explicit.getValidFrom());
        assertTrue(explicit.getValidFromInherited());
        assertEquals(explicitValidTo, explicit.getValidTo());
        assertFalse(explicit.getValidToInherited());

        // A validity time is added to the inherited start
        DomainDTO withValidityTime = new DomainDTO();
        withValidityTime.setName("TestStudie-ValidityTime");
        withValidityTime.setPrefix("TS-V");
        withValidityTime.setSuperDomainName(DOMAIN);
        withValidityTime.setProjectAbbreviation(PROJECT);
        withValidityTime.setValidityTime("2 years");
        this.assertCreatedRequest("createDomainWithValidityTime", post("/api/domains"), null, withValidityTime, this.getAccessToken());
        DomainDTO validityTimeDomain = this.getDomain("TestStudie-ValidityTime");
        assertEquals(DOMAIN_VALID_FROM.plusSeconds(2 * 365 * 86400L), validityTimeDomain.getValidTo());
        assertFalse(validityTimeDomain.getValidToInherited());
    }

    /**
     * Tests that changes of a domain's validity period are passed on to the inheriting
     * child domains and pseudonyms when requested.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("recursiveValidityUpdateTest")
    public void recursiveValidityUpdateTest() throws Exception {
        this.createDomain("TestStudie-Child", "TS-C", DOMAIN, null, null);
        this.createDomain("TestStudie-Grandchild", "TS-G", "TestStudie-Child", null, null);
        LocalDateTime explicitValidTo = LocalDateTime.of(2030, 1, 1, 0, 0);
        this.createDomain("TestStudie-Explicit", "TS-E", DOMAIN, null, explicitValidTo);

        // Recursively update the end of the validity period
        LocalDateTime newValidTo = LocalDateTime.of(2045, 6, 30, 12, 0);
        DomainDTO update = this.getDomain(DOMAIN);
        update.setValidTo(newValidTo);
        this.assertOkRequest("updateDomainValidityRecursively", put("/api/domains/complete"), Map.of("name", DOMAIN, "recursive", "true"), update, this.getAccessToken());

        assertEquals(newValidTo, this.getDomain(DOMAIN).getValidTo());
        assertEquals(newValidTo, this.getDomain("TestStudie-Child").getValidTo());
        assertEquals(newValidTo, this.getDomain("TestStudie-Grandchild").getValidTo());
        assertEquals(explicitValidTo, this.getDomain("TestStudie-Explicit").getValidTo());

        // The test record inherited its validity period from the domain
        assertEquals(newValidTo, this.getTestRecord().getValidTo());

        // Non-recursive updates only affect the domain itself
        LocalDateTime otherValidTo = LocalDateTime.of(2040, 1, 1, 0, 0);
        update.setValidTo(otherValidTo);
        this.assertOkRequest("updateDomainValidityNonRecursively", put("/api/domains/complete"), Map.of("name", DOMAIN, "recursive", "false"), update, this.getAccessToken());

        assertEquals(otherValidTo, this.getDomain(DOMAIN).getValidTo());
        assertEquals(newValidTo, this.getDomain("TestStudie-Child").getValidTo());
        assertEquals(newValidTo, this.getDomain("TestStudie-Grandchild").getValidTo());
    }

    /**
     * Tests that the validity period of pseudonyms is limited to the domain's
     * validity period when the domain enforces it.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("pseudonymValidityEnforcementTest")
    public void pseudonymValidityEnforcementTest() throws Exception {
        // A period exceeding the enforced domain period is limited to it
        PseudonymDTO exceeding = this.createPseudonym(DOMAIN, "exceeding", LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.of(2100, 1, 1, 0, 0));
        assertEquals(DOMAIN_VALID_FROM, exceeding.getValidFrom());
        assertTrue(exceeding.getValidFromInherited());
        assertEquals(DOMAIN_VALID_TO, exceeding.getValidTo());
        assertTrue(exceeding.getValidToInherited());

        // A period within the domain period is kept
        LocalDateTime from = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2035, 1, 1, 0, 0);
        PseudonymDTO within = this.createPseudonym(DOMAIN, "within", from, to);
        assertEquals(from, within.getValidFrom());
        assertFalse(within.getValidFromInherited());
        assertEquals(to, within.getValidTo());
        assertFalse(within.getValidToInherited());

        // Without a given period, the domain's period is used
        PseudonymDTO inherited = this.createPseudonym(DOMAIN, "inherited", null, null);
        assertEquals(DOMAIN_VALID_FROM, inherited.getValidFrom());
        assertEquals(DOMAIN_VALID_TO, inherited.getValidTo());

        // Domains that don't enforce their period keep the given period (enforcement can only be set via the complete endpoint)
        DomainDTO notEnforcing = new DomainDTO();
        notEnforcing.setName("TestStudie-NotEnforcing");
        notEnforcing.setPrefix("TS-N");
        notEnforcing.setSuperDomainName(DOMAIN);
        notEnforcing.setProjectAbbreviation(PROJECT);
        notEnforcing.setEnforceStartDateValidity(false);
        notEnforcing.setEnforceEndDateValidity(false);
        MockHttpServletResponse response = this.assertCreatedRequest("createNotEnforcingDomain", post("/api/domains/complete"), null, notEnforcing, this.getAccessToken());
        assertEquals("/api/domains/TestStudie-NotEnforcing", response.getHeader("Location"));
        PseudonymDTO notEnforced = this.createPseudonym("TestStudie-NotEnforcing", "not-enforced", LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.of(2100, 1, 1, 0, 0));
        assertEquals(LocalDateTime.of(2000, 1, 1, 0, 0), notEnforced.getValidFrom());
        assertEquals(LocalDateTime.of(2100, 1, 1, 0, 0), notEnforced.getValidTo());
    }

    /**
     * Tests that no pseudonyms can be created in domains whose validity period has already ended.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("expiredDomainTest")
    public void expiredDomainTest() throws Exception {
        this.createDomain("TestStudie-Expired", "TS-X", DOMAIN, LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2024, 1, 1, 0, 0));

        PseudonymDTO pseudonym = new PseudonymDTO();
        pseudonym.setIdentifierItem(IdentifierItem.builder().identifier("expired").idType("ANY-ID").build());
        this.assertUnprocessableEntity("createPseudonymInExpiredDomain", post("/api/domains/TestStudie-Expired/pseudonyms"), null, pseudonym, this.getAccessToken());
    }

    /**
     * Helper to create a child domain in the test project.
     *
     * @param name the name of the domain
     * @param prefix the prefix of the domain
     * @param superDomainName the name of the parent domain
     * @param validFrom the start of the validity period (or {@code null} to inherit it)
     * @param validTo the end of the validity period (or {@code null} to inherit it)
     * @throws Exception forwards any internally thrown exceptions
     */
    private void createDomain(String name, String prefix, String superDomainName, LocalDateTime validFrom, LocalDateTime validTo) throws Exception {
        DomainDTO domain = new DomainDTO();
        domain.setName(name);
        domain.setPrefix(prefix);
        domain.setSuperDomainName(superDomainName);
        domain.setProjectAbbreviation(PROJECT);
        domain.setValidFrom(validFrom);
        domain.setValidTo(validTo);

        this.assertCreatedRequest("createDomainForValidity", post("/api/domains"), null, domain, this.getAccessToken());
    }

    /**
     * Helper to retrieve a domain.
     *
     * @param name the name of the domain
     * @return the domain
     * @throws Exception forwards any internally thrown exceptions
     */
    private DomainDTO getDomain(String name) throws Exception {
        MockHttpServletResponse response = this.assertOkRequest("getDomainForValidity", get("/api/domains/" + name), null, null, this.getAccessToken());

        return this.applySingleJsonContentToClass(response.getContentAsString(), DomainDTO.class);
    }

    /**
     * Helper to create a pseudonym.
     *
     * @param domainName the name of the domain
     * @param identifier the identifier
     * @param validFrom the requested start of the validity period
     * @param validTo the requested end of the validity period
     * @return the created pseudonym
     * @throws Exception forwards any internally thrown exceptions
     */
    private PseudonymDTO createPseudonym(String domainName, String identifier, LocalDateTime validFrom, LocalDateTime validTo) throws Exception {
        PseudonymDTO pseudonym = new PseudonymDTO();
        pseudonym.setIdentifierItem(IdentifierItem.builder().identifier(identifier).idType("ANY-ID").build());
        pseudonym.setValidFrom(validFrom);
        pseudonym.setValidTo(validTo);
        MockHttpServletResponse response = this.assertCreatedRequest("createPseudonymForValidity", post("/api/domains/" + domainName + "/pseudonyms"), null, pseudonym, this.getAccessToken());

        return this.applySingleJsonContentToClass(response.getContentAsString(), PseudonymDTO.class);
    }

    /**
     * Helper to retrieve the record that is created for every test.
     *
     * @return the test record
     * @throws Exception forwards any internally thrown exceptions
     */
    private PseudonymDTO getTestRecord() throws Exception {
        MockHttpServletResponse response = this.assertOkRequest("getTestRecordForValidity", get("/api/domains/" + DOMAIN + "/pseudonyms"),
                Map.of("id", "10000008912", "idType", "ANY-ID"), null, this.getAccessToken());

        return this.applySingleJsonContentToClass(response.getContentAsString(), PseudonymDTO.class);
    }
}
