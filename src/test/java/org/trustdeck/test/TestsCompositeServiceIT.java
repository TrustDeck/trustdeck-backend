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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.mock.web.MockHttpServletResponse;
import org.trustdeck.dto.DomainDTO;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.jooq.generated.tables.pojos.Pseudonym;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.service.AssertWebRequestService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;

/**
 * This class offers tests that only work in a combination of updating or deleting different objects first.
 *
 * @author Armin Müller and Eric Wündisch
 */
public class TestsCompositeServiceIT extends AssertWebRequestService {

    /**
     * This method is a helper and creates a simple domain network.
     *
     * @param parentDomainName the parent's domain name as string for its child
     * @param childName        the name of the new child as string
     * @param childNameOfChild the name of the child for the first created child
     * @throws Exception forwards any internally thrown exceptions
     */
    public void createNet(String parentDomainName, String childName, String childNameOfChild) throws Exception {
        this.assertEqualsListDomainHierarchyLength(1);

        // Should have the permission on domain
        this.createDomainHelper(childName, "TS-L", parentDomainName);
        
        // Check length
        this.assertEqualsListDomainHierarchyLength(2);

        // Should have the permission on domain
        this.createDomainHelper(childNameOfChild, "TS-P", childName);
        
        // Check length again
        this.assertEqualsListDomainHierarchyLength(3);
    }

    /**
     * Small helper to quickly create a domain
     *
     * @param name       the name of the domain
     * @param prefix     the prefix of the domain
     * @param parentName the parent domain's name if given, otherwise set it to null
     * @throws Exception
     */
    public void createDomainHelper(String name, String prefix, String parentName) throws Exception {
        DomainDTO d = new DomainDTO();
        d.setName(name);
        d.setPrefix(prefix);
        d.setSuperDomainName(parentName);
        this.assertCreatedRequest("addDomain", post("/api/domains"), null, d, this.getAccessToken());
    }

    /**
     * Helper method to create a record.
     *
     * @param id the identifier of the record
     * @param idType the identifier's type of the record
     * @param domainName the name of the domain where the record should be in
     * @throws Exception forwards all thrown exceptions
     */
    public PseudonymDTO createRecordHelper(String id, String idType, String domainName) throws Exception {
        PseudonymDTO createRecord = new PseudonymDTO();
        createRecord.setIdentifierItem(IdentifierItem.builder().identifier(id).idType(idType).build());

        MockHttpServletResponse response = this.assertCreatedRequest("createRecord", post("/api/domains/" + domainName + "/pseudonyms"), null, createRecord, this.getAccessToken());
        String content = response.getContentAsString();

        List<PseudonymDTO> r1l = this.mapJsonObjectsInStringToList(content, PseudonymDTO.class);
        PseudonymDTO r = r1l.get(0);

        PseudonymDTO nextRecord = new PseudonymDTO();
        nextRecord.setIdentifierItem(IdentifierItem.builder().identifier(r.getPsn()).idType(r.getDomainName()).build());

        return nextRecord;
    }

    @SuppressWarnings("unused")
	@Test
    @DisplayName("linkPathTest")
    public void linkPathTest() throws Exception {

        // Create the domain network
        String rootDomainName = "TestStudie";
        String rootDomainPrefix = "TS-";

        String childOneName = "TestStudie-MRT";
        String childOnePrefix = "TS-MRT-";

        String childTwoName = "TestStudie-Paper";
        String childTwoPrefix = "TS-P-";

        String childThreeName = "TestStudie-Labor-Analyse";
        String childThreePrefix = "TS-L-";

        //this.createDomainHelper(rootDomainName, rootDomainPrefix, null);
        this.createDomainHelper(childOneName, childOnePrefix, rootDomainName);
        this.createDomainHelper(childTwoName, childTwoPrefix, rootDomainName);
        this.createDomainHelper(childThreeName, childThreePrefix, childOneName);

        // Create pseudonyms inside the network

        // Regular creation without issues
        PseudonymDTO a1 = this.createRecordHelper("1234356", "MP-ID", rootDomainName);
        PseudonymDTO a2 = this.createRecordHelper(a1.getIdentifierItem().getIdentifier(), a1.getIdentifierItem().getIdType(), childOneName);
        PseudonymDTO a3 = this.createRecordHelper(a1.getIdentifierItem().getIdentifier(), a1.getIdentifierItem().getIdType(), childTwoName);
        PseudonymDTO a4 = this.createRecordHelper(a2.getIdentifierItem().getIdentifier(), a2.getIdentifierItem().getIdType(), childThreeName);

        // Regular creation without issues
        PseudonymDTO b1 = this.createRecordHelper("1234357", "MP-ID", rootDomainName);
        PseudonymDTO b2 = this.createRecordHelper(b1.getIdentifierItem().getIdentifier(), b1.getIdentifierItem().getIdType(), childOneName);
        PseudonymDTO b3 = this.createRecordHelper(b1.getIdentifierItem().getIdentifier(), b1.getIdentifierItem().getIdType(), childTwoName);
        PseudonymDTO b4 = this.createRecordHelper(b2.getIdentifierItem().getIdentifier(), b2.getIdentifierItem().getIdType(), childThreeName);

        // Regular creation without issues
        PseudonymDTO c1 = this.createRecordHelper("1234357", "ANY-ID", rootDomainName);
        PseudonymDTO c2 = this.createRecordHelper(c1.getIdentifierItem().getIdentifier(), c1.getIdentifierItem().getIdType(), childOneName);
        PseudonymDTO c3 = this.createRecordHelper(c1.getIdentifierItem().getIdentifier(), c1.getIdentifierItem().getIdType(), childTwoName);
        PseudonymDTO c4 = this.createRecordHelper(c2.getIdentifierItem().getIdentifier(), c2.getIdentifierItem().getIdType(), childThreeName);

        //Regular creation but without childThreeName
        PseudonymDTO d1 = this.createRecordHelper("1234358", "MP-ID", rootDomainName);
        PseudonymDTO d2 = this.createRecordHelper(d1.getIdentifierItem().getIdentifier(), d1.getIdentifierItem().getIdType(), childOneName);
        PseudonymDTO d3 = this.createRecordHelper(d1.getIdentifierItem().getIdentifier(), d1.getIdentifierItem().getIdType(), childTwoName);

        // Set query parameter
        Map<String, String> getParameter = new HashMap<>() {
            private static final long serialVersionUID = 3844434370077016386L;
            {
                put("sourceDomain", childTwoName);
                put("targetDomain", childThreeName);
                //id is the next psn from helper method...
                put("sourcePsn", b3.getIdentifierItem().getIdentifier());
            }
        };

        this.assertOkRequest("getSomething", get("/api/domains/hierarchy"), getParameter, null, this.getAccessToken());
    }

    /**
     * Tests that building and recursive deleting on a domain-net works.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("domainNetRecursionTest")
    public void domainNetRecursionTest() throws Exception {
        String parentDomainName = "TestStudie";
        String child = "TestStudie-Labor-Analyse";
        String childOfChild = "TestStudie-Paper";

        this.assertEqualsListDomainHierarchyLength(1);

        // Create domain network for "TestStudie", "TestStudie-Labor-Analyse" and "TestStudie-Paper"
        this.createNet(parentDomainName, child, childOfChild);

        // Recursive deletion of "child" must also lead to the deletion of "childOfChild"
        Map<String, String> deleteParameter = new HashMap<>() {
            private static final long serialVersionUID = -8572862716962724488L;

            {
                put("name", child);
                put("recursive", "true");
            }
        };

        this.obtainNewAccessToken("test", "test");

        // Note: Recursive deletion of "child" must also lead to the deletion of the respective domain groups in the OIDC service
        this.assertNoContent("deleteDomain", delete("/api/domains"), deleteParameter, null, this.getAccessToken());

        // Check that recursive deleting worked. Length must be 1.
        this.assertEqualsListDomainHierarchyLength(1);

        // Creating the net again must lead to a different domainID.
        this.createNet(parentDomainName, child, childOfChild);

        MockHttpServletResponse response = this.assertOkRequest("listDomainHierarchy", get("/api/domains/hierarchy"), null, null, this.getAccessToken());
        String content = response.getContentAsString();
        List<DomainDTO> domains = this.mapJsonObjectsInStringToList(content, DomainDTO.class);

        // Then check net: i.e. the ID must be increased
        for (DomainDTO domain : domains) {
            switch (domain.getId()) {
                case 1:
                    assertEquals("TestStudie", domain.getName());
                    assertNull(domain.getSuperDomainID());
                    break;
                case 4:
                    assertEquals("TestStudie-Labor-Analyse", domain.getName());
                    assertEquals(1, domain.getSuperDomainID());
                    break;
                case 5:
                    assertEquals("TestStudie-Paper", domain.getName());
                    assertEquals(4, domain.getSuperDomainID());
                    break;
                default:
                    throw new AssertionFailedError("Domain ID " + domain.getId() + " not found");
            }
        }

        String assertIdType = "EINE-ID";
        String firstId = "1234356";
        String secondId = "1234357";

        // Create a record
        PseudonymDTO recordCreate = new PseudonymDTO();
        recordCreate.setIdentifierItem(IdentifierItem.builder().identifier(firstId).idType(assertIdType).build());

        // Inserting the same record in two different domains (that have a relation between each other) must work.
        this.assertCreatedRequest("createNewRecord", post("/api/domains/" + child + "/pseudonyms"), null, recordCreate, this.getAccessToken());
        this.assertCreatedRequest("createNewRecord", post("/api/domains/" + childOfChild + "/pseudonyms"), null, recordCreate, this.getAccessToken());

        PseudonymDTO anotherCreateRecord = new PseudonymDTO();
        anotherCreateRecord.setIdentifierItem(IdentifierItem.builder().identifier(secondId).idType(assertIdType).build());

        this.assertCreatedRequest("createNewRecord", post("/api/domains/" + childOfChild + "/pseudonyms"), null, anotherCreateRecord, this.getAccessToken());

        // Check if the records are available
        Map<String, String> getParameter = new HashMap<>() {
            private static final long serialVersionUID = -6352351748269439647L;

            {
                put("id", firstId);
                put("idType", assertIdType);
            }
        };

        response = this.assertOkRequest("readRecord", get("/api/domains/" + child + "/pseudonyms"), getParameter, null, this.getAccessToken());
        content = response.getContentAsString();

        List<PseudonymDTO> r1l = this.mapJsonObjectsInStringToList(content, PseudonymDTO.class);
        PseudonymDTO r1 = r1l.get(0);

        response = this.assertOkRequest("readRecord", get("/api/domains/" + childOfChild + "/pseudonyms"), getParameter, null, this.getAccessToken());
        content = response.getContentAsString();
        List<PseudonymDTO> r2l = this.mapJsonObjectsInStringToList(content, PseudonymDTO.class);
        PseudonymDTO r2 = r2l.get(0);

        assertNotNull(r1.getPsn());
        assertNotNull(r2.getPsn());

        assertEquals(firstId, r1.getIdentifierItem().getIdentifier());
        assertEquals(firstId, r2.getIdentifierItem().getIdentifier());

        assertEquals(assertIdType, r1.getIdentifierItem().getIdType());
        assertEquals(assertIdType, r2.getIdentifierItem().getIdType());

        // Because of a different salt this musn't be equal.
        assertNotEquals(r1.getPsn(), r2.getPsn());

        Map<String, String> anotherGetParameter = new HashMap<>() {
            private static final long serialVersionUID = 2912185610891383926L;

            {
                put("id", secondId);
                put("idType", assertIdType);
            }
        };

        response = this.assertOkRequest("readRecord", get("/api/domains/" + childOfChild + "/pseudonyms"), anotherGetParameter, null, this.getAccessToken());
        content = response.getContentAsString();

        List<PseudonymDTO> r3l = this.mapJsonObjectsInStringToList(content, PseudonymDTO.class);
        PseudonymDTO r3 = r3l.get(0);

        assertNotNull(r3.getPsn());
        assertEquals(secondId, r3.getIdentifierItem().getIdentifier());
        assertEquals(assertIdType, r3.getIdentifierItem().getIdType());
        // At this point all pseudonyms are saved correctly

        // Deleting the child recursively again must lead to the deletion of the records.
        this.assertNoContent("deleteDomain", delete("/api/domains"), deleteParameter, null, this.getAccessToken());

        // Check if recursive deleting worked. Length must be 1.
        this.assertEqualsListDomainHierarchyLength(1);
        this.assertForbiddenRequest("readRecord", get("/api/domains/" + child + "/pseudonyms"), getParameter, null, this.getAccessToken());
        this.assertForbiddenRequest("readRecord", get("/api/domains/" + childOfChild + "/pseudonyms"), getParameter, null, this.getAccessToken());
        this.assertForbiddenRequest("readRecord", get("/api/domains/" + childOfChild + "/pseudonyms"), anotherGetParameter, null, this.getAccessToken());

        // Create the domain-net again
        this.createNet(parentDomainName, child, childOfChild);

        // Check that re-creating the domain doesn't restore the records formerly stored in it. Must lead to a not found.
        this.assertNotFoundRequest("readRecord", get("/api/domains/" + child + "/pseudonyms"), getParameter, null, this.getAccessToken());
        this.assertNotFoundRequest("readRecord", get("/api/domains/" + childOfChild + "/pseudonyms"), getParameter, null, this.getAccessToken());
        this.assertNotFoundRequest("readRecord", get("/api/domains/" + childOfChild + "/pseudonyms"), anotherGetParameter, null, this.getAccessToken());
    }

    /**
     * Tests if a domain properly handles records in regard to its valid to/from dates.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("dateValidationOnDomainsAndRecordsTest")
    public void dateValidationOnDomainsAndRecordsTest() throws Exception {
        String domainName = "TestStudie";
        String assertId = "1234356";
        String assertIdType = "EINE-ID";

        MockHttpServletResponse response = this.assertOkRequest("getDomain", get("/api/domains/" + domainName), null, null, this.getAccessToken());
        String content = response.getContentAsString();
        DomainDTO d = this.applySingleJsonContentToClass(content, DomainDTO.class);

        // Change some dates on the current domain which makes absolutely no sense but is needed for the test
        d.setValidFrom(LocalDateTime.now().withNano(0).minusDays(20));
        this.domainUpdateHelperComplete(d, domainName, d);

        d.setValidTo(LocalDateTime.now().withNano(0).minusDays(10));
        this.domainUpdateHelperComplete(d, domainName, d);

        // Create a record
        PseudonymDTO recordCreate = new PseudonymDTO();
        recordCreate.setIdentifierItem(IdentifierItem.builder().identifier(assertId).idType(assertIdType).build());

        this.assertUnprocessableEntity("createNewRecord", post("/api/domains/" + domainName + "/pseudonyms"), null, recordCreate, this.getAccessToken());

        Pseudonym pseudonym1 = new Pseudonym();
        pseudonym1.setIdentifier("10000008913");
        pseudonym1.setIdtype("ANY-ID");
        pseudonym1.setValidfrominherited(true);
        pseudonym1.setValidtoinherited(true);

        Pseudonym pseudonym2 = new Pseudonym();
        pseudonym2.setIdentifier("10000008914");
        pseudonym2.setIdtype("ANY-ID");
        pseudonym2.setValidfrominherited(true);
        pseudonym2.setValidtoinherited(true);

        List<PseudonymDTO> recordDtoList = new ArrayList<>();
        recordDtoList.add(new PseudonymDTO().assignPojoValues(pseudonym1));
        recordDtoList.add(new PseudonymDTO().assignPojoValues(pseudonym2));
        this.assertUnprocessableEntity("createNewRecordBatch", post("/api/domains/" + domainName + "/pseudonyms"), null, recordDtoList, this.getAccessToken());
    }

    /**
     * Tests the user-friendly validity time feature.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("userFriendlyValidityTimeTest")
    public void userFriendlyValidityTimeTest() throws Exception {
        // Prepare
        String parentDomainName = "TestStudie";
        String child = "TestStudie-Labor-Analyse";
        String childOfChild = "TestStudie-Paper";

        // Create domain network for "TestStudie", "TestStudie-Labor" and "TestStudie-Paper"
        this.createNet(parentDomainName, child, childOfChild);

        // Tests
        // Create a domain with a user-friendly validity time string
        DomainDTO domainDTO = new DomainDTO();
        domainDTO.setName("valTimeTest");
        domainDTO.setPrefix("VTT-");
        domainDTO.setValidFrom(LocalDateTime.parse("2023-01-01T00:00:00"));
        domainDTO.setValidityTime("1 year");

        this.assertCreatedRequest("createDomain", post("/api/domains"), null, domainDTO, this.getAccessToken());
        /*Map<String, String> getP = new HashMap<>() {
        	private static final long serialVersionUID = -2537459720418280492L;
		{
            put("name", "valTimeTest");
        }};
        MockHttpServletResponse response = this.assertOkRequest("getDomain", get("/api/domains/"), getP, null, this.getAccessToken());
        DomainDTO d = this.applySingleJsonContentToClass(response.getContentAsString(), DomainDTO.class);
        assertEquals(domainDto.getValidFrom().plusSeconds(365*86400L), d.getValidTo());*/

        // Create a record with a user-friendly validity time string
        PseudonymDTO record = new PseudonymDTO();
        record.setIdentifierItem(IdentifierItem.builder().identifier("testIdentifier").idType("testType").build());
        record.setValidFrom(LocalDateTime.parse("2023-01-01T12:00:00"));
        record.setValidityTime("1d");

        MockHttpServletResponse response = this.assertCreatedRequest("createRecord", post("/api/domains/" + parentDomainName + "/pseudonyms"), null, record, this.getAccessToken());

        List<PseudonymDTO> r1l = this.mapJsonObjectsInStringToList(response.getContentAsString(), PseudonymDTO.class);
        PseudonymDTO r = r1l.get(0);

        assertEquals(record.getValidFrom().plusSeconds(86400L), r.getValidTo());

        // Update a record with a user-friendly validity time string via identifier
        Map<String, String> updateViaIdentifierParams = new HashMap<>() {
            private static final long serialVersionUID = -2537459720418280492L;

            {
                put("id", "testIdentifier");
                put("idType", "testType");
            }
        };
        PseudonymDTO updateViaIdentifier = new PseudonymDTO();
        updateViaIdentifier.setValidityTime("8 w");

        response = this.assertOkRequest("updateRecordViaIdentifier", put("/api/domains/" + parentDomainName + "/pseudonyms"), updateViaIdentifierParams, updateViaIdentifier, this.getAccessToken());
        PseudonymDTO updatedViaIdentifier = this.applySingleJsonContentToClass(response.getContentAsString(), PseudonymDTO.class);
        assertEquals(record.getValidFrom().plusSeconds(8 * 7 * 86400L), updatedViaIdentifier.getValidTo());

        // Update a record with a user-friendly validity time string via psn
        Map<String, String> updateViaPsnParams = new HashMap<>() {
            private static final long serialVersionUID = 3409656673301886926L;

            {
                put("psn", r.getPsn());
            }
        };
        PseudonymDTO updateViaPsn = new PseudonymDTO();
        updateViaPsn.setValidityTime("10hours");

        response = this.assertOkRequest("updateRecordViaPsnHours", put("/api/domains/" + parentDomainName + "/pseudonyms"), updateViaPsnParams, updateViaPsn, this.getAccessToken());
        PseudonymDTO updatedViaPsn = this.applySingleJsonContentToClass(response.getContentAsString(), PseudonymDTO.class);
        assertEquals(record.getValidFrom().plusSeconds(10 * 3600L), updatedViaPsn.getValidTo());

        // Update using minutes
        PseudonymDTO updateMinutes = new PseudonymDTO();
        updateMinutes.setValidityTime("12 minutes");

        response = this.assertOkRequest("updateRecordViaPsnMinutes", put("/api/domains/" + parentDomainName + "/pseudonyms"), updateViaPsnParams, updateMinutes, this.getAccessToken());
        PseudonymDTO updatedMinutes = this.applySingleJsonContentToClass(response.getContentAsString(), PseudonymDTO.class);
        assertEquals(record.getValidFrom().plusSeconds(12 * 60L), updatedMinutes.getValidTo());

        // Update using seconds
        PseudonymDTO updateSeconds = new PseudonymDTO();
        updateSeconds.setValidityTime("53secs");

        response = this.assertOkRequest("updateRecordViaPsnSeconds", put("/api/domains/" + parentDomainName + "/pseudonyms"), updateViaPsnParams, updateSeconds, this.getAccessToken());
        PseudonymDTO updatedSeconds = this.applySingleJsonContentToClass(response.getContentAsString(), PseudonymDTO.class);
        assertEquals(record.getValidFrom().plusSeconds(53L), updatedSeconds.getValidTo());
    }
}
