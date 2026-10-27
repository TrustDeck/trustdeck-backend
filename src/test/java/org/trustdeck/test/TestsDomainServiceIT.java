/*
 * Trust Deck Services
 * Copyright 2021-2025 Armin Müller and Eric Wündisch
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.mock.web.MockHttpServletResponse;
import org.trustdeck.dto.DomainDTO;
import org.trustdeck.dto.ProjectDTO;
import org.trustdeck.service.AssertWebRequestService;

/**
 * This class offers tests to test only the domain endpoints.
 *
 * @author Armin Müller and Eric Wündisch
 */
@Slf4j
public class TestsDomainServiceIT extends AssertWebRequestService {
    
    /**
     * Test that tries to create a new domain with different given inputs
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("commonCreateDomainTest")
    public void commonCreateDomainTest() throws Exception {
        // A create request without the necessary domainName permission should work, the rest shouldn't
        String domainName = "WeitereStudie";

        // Check if common request works
        DomainDTO reducedDomainDto = new DomainDTO();
        reducedDomainDto.setName(domainName);
        reducedDomainDto.setPrefix("WS-");
        reducedDomainDto.setProjectAbbreviation("TEST");

        // Unauthorized tests for creating a domain
        this.assertUnauthorizedRequest("createDomainNoToken", post("/api/domains"), null, reducedDomainDto, "");
        this.assertUnauthorizedRequest("createDomainUnauth", post("/api/domains"), null, reducedDomainDto, "SomeToken");

        MockHttpServletResponse response = this.assertCreatedRequest("createDomain", post("/api/domains"), null, reducedDomainDto, this.getAccessToken());
        String content = response.getContentAsString();
        assertNotNull(this.applySingleJsonContentToClass(content, DomainDTO.class));

        String domainNameComplete = "WeitereStudieComplete";
        DomainDTO completeDomainDto = new DomainDTO();
        completeDomainDto.setName(domainNameComplete);
        completeDomainDto.setPrefix("WS-");
        completeDomainDto.setProjectAbbreviation("TEST");

        // Unauthorized tests for creating a domain
        this.assertUnauthorizedRequest("createDomainCompleteNoToken", post("/api/domains/complete"), null, completeDomainDto, "");
        this.assertUnauthorizedRequest("createDomainCompleteUnauth", post("/api/domains/complete"), null, completeDomainDto, "SomeToken");

        response = this.assertCreatedRequest("createDomainComplete", post("/api/domains/complete"), null, completeDomainDto, this.getAccessToken());
        content = response.getContentAsString();
        assertNotNull(this.applySingleJsonContentToClass(content, DomainDTO.class));

    }

    /**
     * Tests that every domain belongs to a project and domain trees cannot span projects.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("domainProjectAssociationTest")
    public void domainProjectAssociationTest() throws Exception {
        DomainDTO missingProject = new DomainDTO();
        missingProject.setName("Missing-Project");
        missingProject.setPrefix("MP-");
        this.assertUnprocessableEntity("createDomainWithoutProject", post("/api/domains"), null, missingProject, this.getAccessToken());

        DomainDTO associatedDomain = new DomainDTO();
        associatedDomain.setName("Associated-Domain");
        associatedDomain.setPrefix("AD-");
        associatedDomain.setProjectAbbreviation("TEST");
        MockHttpServletResponse response = this.assertCreatedRequest("createProjectAssociatedDomain", post("/api/domains"), null,
                associatedDomain, this.getAccessToken());
        DomainDTO storedDomain = this.applySingleJsonContentToClass(response.getContentAsString(), DomainDTO.class);
        assertEquals("TEST", storedDomain.getProjectAbbreviation());

        ProjectDTO otherProject = new ProjectDTO();
        otherProject.setName("Other Test Study");
        otherProject.setAbbreviation("OTHER");
        this.assertCreatedRequest("createOtherProject", post("/api/projects"), null, otherProject, this.getAccessToken());

        DomainDTO crossProjectChild = new DomainDTO();
        crossProjectChild.setName("Cross-Project-Child");
        crossProjectChild.setPrefix("CP-");
        crossProjectChild.setSuperDomainName("Associated-Domain");
        crossProjectChild.setProjectAbbreviation("OTHER");
        this.assertUnprocessableEntity("createCrossProjectDomainChild", post("/api/domains"), null, crossProjectChild,
                this.getAccessToken());
    }

    /**
     * Test that tries to trigger errors on domain endpoints for different status codes.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("triggerFailuresOnDomainsTest")
    public void triggerFailuresOnDomainsTest() throws Exception {
        String domainName = "TestStudie-Labor";

        // Test: get domain without creating it first
        // Requests on non-existing domains are forbidden, so that their existence is not revealed
        this.assertForbiddenRequest("getDomainNotFoundDomainName", get("/api/domains/" + domainName), null, null, this.getAccessToken());

        // Update domain while domain is not yet created
        Map<String, String> updateParameter = new HashMap<>() {
        	private static final long serialVersionUID = 1994787103897759608L;
		{
            put("name", domainName);
            put("recursive", "false");
        }};
        // Needs an non empty object to trigger not found
        DomainDTO domainDTO = new DomainDTO();
        domainDTO.setDescription("Just Something");
        this.assertForbiddenRequest("updateDomainNotFoundDomainName", put("/api/domains/complete"), updateParameter, domainDTO, this.getAccessToken());

        // Create domain with unknown parentName
        Map<String, String> createParameter = new HashMap<>() {
        	private static final long serialVersionUID = 7067526794461193299L;
		{
            put("parentName", "Unknown-ParentName");
            put("prefix", "TS-L");
            put("name", domainName);
        }};
        this.assertBadRequestRequest("createDomainNotFoundParentName", post("/api/domains"), createParameter, null, this.getAccessToken());

        // Delete domain with unknown parentName
        Map<String, String> deleteParameter = new HashMap<>() {
        	private static final long serialVersionUID = -6870663419604238488L;
		{
            put("name", domainName);
        }};
        this.assertForbiddenRequest("deleteDomainNotFoundParentName", delete("/api/domains"), deleteParameter, null, this.getAccessToken());

    }

    /**
     * Test that simulates common change-actions by a user on domains
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("commonChangesOnDomainTest")
    public void commonChangesOnDomainTest() throws Exception {
        String domainName = "TestStudie";

        Map<String, String> getParameter = new HashMap<>() {private static final long serialVersionUID = 4462487992226896288L;
		{
            put("name", domainName);
        }};

        // Unauthorized tests for getting a domain
        this.assertUnauthorizedRequest("getDomainNoToken", get("/api/domains"), getParameter, null, "");
        this.assertUnauthorizedRequest("getDomainUnauth", get("/api/domains"), getParameter, null, "SomeToken");

        MockHttpServletResponse response = this.assertOkRequest("getDomain", get("/api/domains/" + domainName), null, null, this.getAccessToken());
        String content = response.getContentAsString();
        DomainDTO d = this.applySingleJsonContentToClass(content, DomainDTO.class);

        // Content must be a valid JSON and mappable
        assertNotNull(d);
        
        assertNull(d.getDescription());

        String newDescription = "das ist ein test2";
        d.setDescription(newDescription);
        this.domainUpdateHelperReduced(d, domainName, d);

        // All fields must be filled with something (at least an empty string) and available
        assertEquals("TestStudie", d.getName());
        assertEquals("TS-", d.getPrefix());
        assertEquals("2022-02-26T19:15:20.885853", d.getValidFrom().toString());
        assertEquals("2052-02-19T19:15:20.885853", d.getValidTo().toString());
        assertTrue(d.getEnforceStartDateValidity());
        assertTrue(d.getEnforceEndDateValidity());
        assertEquals("MD5", d.getAlgorithm().getName());
        assertEquals(1, d.getAlgorithm().getConsecutiveValueCounter());
        assertFalse(d.getMultiplePsnAllowed());
        assertEquals(32, d.getAlgorithm().getPseudonymLength());
        assertEquals("0", d.getAlgorithm().getPaddingCharacter());
        assertTrue(d.getAlgorithm().getAddCheckDigit());
        assertFalse(d.getAlgorithm().getLengthIncludesCheckDigit());
        
        assertEquals("azMPTIQXJsept_4nDj5B1BXN83Bj_8VJ", d.getAlgorithm().getSalt());
        assertEquals(32, d.getAlgorithm().getSaltLength());

        // Change some of the domain's attributes

        //salt must be at least 8 chars long
        String newSalt = "foobar78";
        d.getAlgorithm().setSalt(newSalt);
        this.domainUpdateHelperComplete(d, domainName, d);

        newDescription = "das ist ein test";
        d.setDescription(newDescription);
        this.domainUpdateHelperComplete(d, domainName, d);

        String newPaddingChar = "1";
        d.getAlgorithm().setPaddingCharacter(newPaddingChar);
        this.domainUpdateHelperComplete(d, domainName, d);

        Integer newPsnLength = 16;
        d.getAlgorithm().setPseudonymLength(newPsnLength);
        this.domainUpdateHelperComplete(d, domainName, d);

        d.setValidFrom(d.getValidFrom().withNano(0).plusDays(10));
        this.domainUpdateHelperComplete(d, domainName, d);

        d.setValidTo(d.getValidTo().withNano(0).plusDays(10));
        this.domainUpdateHelperComplete(d, domainName, d);

        d.setEnforceStartDateValidity(false);
        this.domainUpdateHelperComplete(d, domainName, d);

        d.setEnforceStartDateValidity(true);
        this.domainUpdateHelperComplete(d, domainName, d);

        d.setEnforceEndDateValidity(false);
        this.domainUpdateHelperComplete(d, domainName, d);

        d.setEnforceEndDateValidity(true);
        this.domainUpdateHelperComplete(d, domainName, d);

        String newAlgo = "BLAKE3";
        d.getAlgorithm().setName(newAlgo);
        this.domainUpdateHelperComplete(d, domainName, d);

        // The consecutive value counter is the algorithm's runtime state and must not be changed by a domain update
        Long oldConsecVal = d.getAlgorithm().getConsecutiveValueCounter();
        d.getAlgorithm().setConsecutiveValueCounter(10L);
        Map<String, String> updateParameter = new HashMap<>() {
            private static final long serialVersionUID = -2708461912069760668L;
            {
                put("name", domainName);
                put("recursive", "false");
            }
        };
        this.assertOkRequest("commonUpdateDomainComplete", put("/api/domains/complete"), updateParameter, d, this.getAccessToken());

        d.getAlgorithm().setConsecutiveValueCounter(oldConsecVal);
        this.getAndCheckDomain(d, domainName);

        this.assertIsDeletedDomain(domainName);
    }

    /**
     * Test the endpoint for listing all domains.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("listDomainHierarchyTest")
    public void listDomainHierarchyTest() throws Exception {
        this.assertEqualsListDomainHierarchyLength(1);

        String parentDomainName = "TestStudie";

        // Add domains as children
        DomainDTO firstDomainDto = new DomainDTO();
        firstDomainDto.setName("TestStudie-Labor-Analyse");
        firstDomainDto.setPrefix("TS-L");
        firstDomainDto.setSuperDomainName(parentDomainName);
        firstDomainDto.setProjectAbbreviation("TEST");

        // Should have the permission on the domain
        this.assertCreatedRequest("addFirstDomainForListHierarchy", post("/api/domains"), null, firstDomainDto, this.getAccessToken());

        // Check the length again
        this.assertEqualsListDomainHierarchyLength(2);

        DomainDTO secondDomainDto = new DomainDTO();
        secondDomainDto.setName("TestStudie-Paper");
        secondDomainDto.setPrefix("TS-P");
        secondDomainDto.setSuperDomainName(parentDomainName);
        secondDomainDto.setProjectAbbreviation("TEST");

        // Should have the permission on the domain
        this.assertCreatedRequest("addSecondDomainForListHierarchy", post("/api/domains"), null, secondDomainDto, this.getAccessToken());

        // Check the length again
        this.assertEqualsListDomainHierarchyLength(3);

        // Should NOT have the permission on the domain
        DomainDTO thirdDomainDto = new DomainDTO();
        thirdDomainDto.setName("No-Permission-Domain");
        thirdDomainDto.setPrefix("NoPe");
        thirdDomainDto.setSuperDomainName("TestStudie-Labor-Analyse");
        thirdDomainDto.setProjectAbbreviation("TEST");

        // Should NOT have the permission on the domain
        this.assertCreatedRequest("addThirdDomainForListHierarchy", post("/api/domains"), null, thirdDomainDto, this.getAccessToken());

        // Check the length again
        List<DomainDTO> domains = this.assertEqualsListDomainHierarchyLength(4);

        // Check if the minimal domain-net makes sense here
        for (DomainDTO domain : domains) {

            switch (domain.getName()) {
                case "TestStudie":
                    assertNull(domain.getSuperDomainName());
                    break;
                case "TestStudie-Labor-Analyse":
                    assertEquals("TestStudie", domain.getSuperDomainName());
                    assertEquals(1, domain.getSuperDomainID());
                    break;
                case "TestStudie-Paper":
                    assertEquals("TestStudie", domain.getSuperDomainName());
                    assertEquals(1, domain.getSuperDomainID());
                    break;
                case "No-Permission-Domain":
                    assertEquals("TestStudie-Labor-Analyse", domain.getSuperDomainName());
                    assertEquals(2, domain.getSuperDomainID());
                    break;
                default:
                    throw new AssertionFailedError("Domain " + domain.getName() + " not expected");
            }
        }
    }
    
    /**
     * Test that updates the salt of a domain, including an explicitly allowed empty salt.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("updateDomainSaltTest")
    public void updateDomainSaltTest() throws Exception {
        String domainName = "TestStudie";
        String newSalt = "NewSaltValue_0123456789abcdefghi";

        // An empty salt must be rejected when it is not explicitly allowed
        Map<String, String> emptySaltParameter = new HashMap<>() {
            private static final long serialVersionUID = 5163487263450851837L;
            {
                put("salt", "");
            }
        };
        this.assertBadRequestRequest("updateDomainSaltEmptyNotAllowed", put("/api/domains/" + domainName + "/salt"), emptySaltParameter, null, this.getAccessToken());

        // Update to a new non-empty salt
        Map<String, String> saltParameter = new HashMap<>() {
            private static final long serialVersionUID = -4380158238715462307L;
            {
                put("salt", newSalt);
            }
        };
        this.assertOkRequest("updateDomainSalt", put("/api/domains/" + domainName + "/salt"), saltParameter, null, this.getAccessToken());

        MockHttpServletResponse response = this.assertOkRequest("getDomainAfterSaltUpdate", get("/api/domains/" + domainName), null, null, this.getAccessToken());
        DomainDTO domain = this.applySingleJsonContentToClass(response.getContentAsString(), DomainDTO.class);
        assertEquals(newSalt, domain.getAlgorithm().getSalt());
        assertEquals(newSalt.length(), domain.getAlgorithm().getSaltLength());

        // Update to an explicitly allowed empty salt
        emptySaltParameter.put("allowEmpty", "true");
        this.assertOkRequest("updateDomainSaltEmptyAllowed", put("/api/domains/" + domainName + "/salt"), emptySaltParameter, null, this.getAccessToken());

        response = this.assertOkRequest("getDomainAfterEmptySaltUpdate", get("/api/domains/" + domainName), null, null, this.getAccessToken());
        domain = this.applySingleJsonContentToClass(response.getContentAsString(), DomainDTO.class);
        assertEquals("", domain.getAlgorithm().getSalt());
        assertEquals(0, domain.getAlgorithm().getSaltLength());
    }

    /**
     * Test that retrieves a domain with its nested algorithm.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("getDomainWithAlgorithmTest")
    public void getDomainAttributesTest() throws Exception {
        String domainName = "TestStudie";

        MockHttpServletResponse response = this.assertOkRequest("getDomain", get("/api/domains/" + domainName), null, null, this.getAccessToken());
        DomainDTO domain = this.applySingleJsonContentToClass(response.getContentAsString(), DomainDTO.class);
        assertEquals(domainName, domain.getName());
        assertEquals("MD5", domain.getAlgorithm().getName());
    }
}
