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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.trustdeck.algorithms.XxHashPseudonymizer;
import org.trustdeck.dto.DomainDTO;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.jooq.generated.tables.pojos.Pseudonym;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.service.AssertWebRequestService;

/**
 * This class offers tests to test only the record endpoints.
 *
 * @author Armin Müller and Eric Wündisch
 */
public class TestsRecordServiceIT extends AssertWebRequestService {
	
	/** Enables access to the permission grants database methods. */
//    @Autowired
//    private PermissionDBService permissionDBService;
	
    /**
     * Update record by identifier.
     *
     * @throws Exception the exception
     */
    @Test
    @DisplayName("updateRecordByIdentifier")
    public void updateRecordByIdentifier() throws Exception {

        String domainName = "TestStudie";
        String assertId = "10000008912";
        String assertIdType = "ANY-ID";
        String assertPseudonym = "TS-9EEEE39F0D5C03507CB9388609E925F9";

        // Update a record
        Map<String, String> updateParameter = new HashMap<>() {
            private static final long serialVersionUID = -8328746806921168250L;

            {
                put("id", assertId);
                put("idType", assertIdType);
            }
        };

        LocalDateTime d = LocalDateTime.now();

        PseudonymDTO updateRecordDto = new PseudonymDTO();
        updateRecordDto.setValidFrom(d);
        MockHttpServletResponse response = this.assertOkRequest("updateRecordByIdentifier", put("/api/domains/" + domainName + "/pseudonyms"), updateParameter, updateRecordDto, this.getAccessToken());
        String content = response.getContentAsString();
        PseudonymDTO r = this.applySingleJsonContentToClass(content, PseudonymDTO.class);
        assertEquals(assertPseudonym, r.getPsn());
        assertEquals(assertId, r.getIdentifierItem().getIdentifier());
        assertEquals(assertIdType, r.getIdentifierItem().getIdType());

        assertEquals(d.getMonthValue(), r.getValidFrom().getMonthValue());
        assertEquals(d.getYear(), r.getValidFrom().getYear());
        assertEquals(d.getDayOfMonth(), r.getValidFrom().getDayOfMonth());
        assertEquals(d.getHour(), r.getValidFrom().getHour());
        assertEquals(d.getMinute(), r.getValidFrom().getMinute());
        assertEquals(d.getSecond(), r.getValidFrom().getSecond());

    }

    /**
     * Update record by pseudonym.
     *
     * @throws Exception the exception
     */
    @Test
    @DisplayName("updateRecordByPseudonym")
    public void updateRecordByPseudonym() throws Exception {

        String domainName = "TestStudie";
        String assertId = "10000008912";
        String assertIdType = "ANY-ID";
        String assertPseudonym = "TS-9EEEE39F0D5C03507CB9388609E925F9";

        // Update a record
        Map<String, String> updateParameter = new HashMap<>() {
            private static final long serialVersionUID = -8328746806921168250L;

            {
                put("psn", assertPseudonym);
            }
        };

        LocalDateTime d = LocalDateTime.now();

        PseudonymDTO updateRecordDto = new PseudonymDTO();
        updateRecordDto.setValidFrom(d);
        MockHttpServletResponse response = this.assertOkRequest("updateRecordByPseudonym", put("/api/domains/" + domainName + "/pseudonyms"), updateParameter, updateRecordDto, this.getAccessToken());
        String content = response.getContentAsString();
        PseudonymDTO r = this.applySingleJsonContentToClass(content, PseudonymDTO.class);
        assertEquals(assertPseudonym, r.getPsn());
        assertEquals(assertId, r.getIdentifierItem().getIdentifier());
        assertEquals(assertIdType, r.getIdentifierItem().getIdType());

        assertEquals(d.getMonthValue(), r.getValidFrom().getMonthValue());
        assertEquals(d.getYear(), r.getValidFrom().getYear());
        assertEquals(d.getDayOfMonth(), r.getValidFrom().getDayOfMonth());
        assertEquals(d.getHour(), r.getValidFrom().getHour());
        assertEquals(d.getMinute(), r.getValidFrom().getMinute());
        assertEquals(d.getSecond(), r.getValidFrom().getSecond());

    }


    /**
     * Update record complete by identifier.
     *
     * @throws Exception the exception
     */
    @Test
    @DisplayName("updateRecordCompleteByIdentifier")
    public void updateRecordCompleteByIdentifier() throws Exception {

        String domainName = "TestStudie";
        String assertId = "10000008912";
        String assertIdType = "ANY-ID";
        String assertNewIdType = "OTHER-ID";
        
        // Update a record
        Map<String, String> updateParameter = new HashMap<>() {
            private static final long serialVersionUID = -8328746806921168250L;

            {
                put("id", assertId);
                put("idType", assertIdType);
            }
        };

        PseudonymDTO updateRecordDto = new PseudonymDTO();
        updateRecordDto.setIdentifierItem(IdentifierItem.builder().idType(assertNewIdType).build());

        MockHttpServletResponse response = this.assertOkRequest("updateRecordCompleteByIdentifier", put("/api/domains/" + domainName + "/pseudonyms/complete"), updateParameter, updateRecordDto, this.getAccessToken());
        String content = response.getContentAsString();
        PseudonymDTO r = this.applySingleJsonContentToClass(content, PseudonymDTO.class);
        assertEquals(assertId, r.getIdentifierItem().getIdentifier());
        assertEquals(assertNewIdType, r.getIdentifierItem().getIdType());
    }


    /**
     * Update record complete by pseudonym.
     *
     * @throws Exception the exception
     */
    @Test
    @DisplayName("updateRecordCompleteByPseudonym")
    public void updateRecordCompleteByPseudonym() throws Exception {

        String domainName = "TestStudie";
        String assertPseudonym = "TS-9EEEE39F0D5C03507CB9388609E925F9";
        String assertNewIdType = "OTHER-ID";
        // Update a record
        Map<String, String> updateParameter = new HashMap<>() {
            private static final long serialVersionUID = -8328746806921168250L;

            {
                put("psn", assertPseudonym);
            }
        };

        PseudonymDTO updateRecordDto = new PseudonymDTO();
        updateRecordDto.setIdentifierItem(IdentifierItem.builder().idType(assertNewIdType).build());

        MockHttpServletResponse response = this.assertOkRequest("updateRecordCompleteByPseudonym", put("/api/domains/" + domainName + "/pseudonyms/complete"), updateParameter, updateRecordDto, this.getAccessToken());
        String content = response.getContentAsString();
        PseudonymDTO r = this.applySingleJsonContentToClass(content, PseudonymDTO.class);
        assertEquals(assertPseudonym, r.getPsn());
        assertEquals(assertNewIdType, r.getIdentifierItem().getIdType());
    }


    /**
     * Tests the CRUD-Interface on records.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("recordTest")
    public void recordTest() throws Exception {

        String domainName = "TestStudie";
        String assertId = "1234356";
        String assertIdType = "EINE-ID";
        String assertNewIdType = "ANY-ID";
        String assertPseudonym = "TS-DEBB85F4AD634BB9413517C0DA5342260";

        // Create a record
        /*Map<String, String> createParameter = new HashMap<>() {{
            put("id", assertId);
            put("idType", assertIdType);
        }};*/

        PseudonymDTO createRecordDto = new PseudonymDTO();
        createRecordDto.setIdentifierItem(IdentifierItem.builder().identifier(assertId).idType(assertIdType).build());

        // Unauthorized tests for creating
        this.assertBadRequestRequest("createNewRecordBadRequest", post("/api/domains/" + domainName + "/pseudonyms"), null, createRecordDto, "");
        this.assertUnauthorizedRequest("createNewRecordUnauth", post("/api/domains/" + domainName + "/pseudonyms"), null, createRecordDto, "SomeToken");

        MockHttpServletResponse response = this.assertCreatedRequest("createNewRecord", post("/api/domains/" + domainName + "/pseudonyms"), null, createRecordDto, this.getAccessToken());

        String content = response.getContentAsString();

        List<PseudonymDTO> r1l = this.mapJsonObjectsInStringToList(content, PseudonymDTO.class);
        PseudonymDTO r = r1l.get(0);

        assertEquals(assertPseudonym, r.getPsn());
        assertNotNull(r.getDomain());
        assertEquals(domainName, r.getDomain().getName());

        // Try duplicate
        this.assertOkRequest("createNewRecordAgainDuplicate", post("/api/domains/" + domainName + "/pseudonyms"), null, createRecordDto, this.getAccessToken());

        // Read a record
        Map<String, String> getParameter = new HashMap<>() {
            private static final long serialVersionUID = 6408752344331371975L;

            {
                put("id", assertId);
                put("idType", assertIdType);
            }
        };

        // Unauthorized test for reading
        this.assertBadRequestRequest("readRecordBadRequest", get("/api/domains/" + domainName + "/pseudonyms"), getParameter, null, "");
        this.assertUnauthorizedRequest("readRecordUnauth", get("/api/domains/" + domainName + "/pseudonyms"), getParameter, null, "SomeToken");

        response = this.assertOkRequest("readRecordByIdentifier", get("/api/domains/" + domainName + "/pseudonyms"), getParameter, null, this.getAccessToken());
        content = response.getContentAsString();

        List<PseudonymDTO> r2l = this.mapJsonObjectsInStringToList(content, PseudonymDTO.class);
        r = r2l.get(0);

        String psn = r.getPsn();
        assertEquals(assertPseudonym, psn);

        Map<String, String> getParameter2 = new HashMap<>() {
            private static final long serialVersionUID = 6408752344331371975L;

            {
                put("psn", psn);
            }
        };

        response = this.assertOkRequest("readRecordByPseudonym", get("/api/domains/" + domainName + "/pseudonyms"), getParameter2, null, this.getAccessToken());
        content = response.getContentAsString();
        List<PseudonymDTO> r3l = this.mapJsonObjectsInStringToList(content, PseudonymDTO.class);
        r = r3l.get(0);

        assertEquals(assertId, r.getIdentifierItem().getIdentifier());
        assertEquals(assertIdType, r.getIdentifierItem().getIdType());

        // Update a record
        Map<String, String> updateParameter = new HashMap<>() {
            private static final long serialVersionUID = -8328746806921168250L;
            {
                put("id", assertId);
                put("idType", assertIdType);
            }
        };

        PseudonymDTO updateRecordDto = new PseudonymDTO();
        updateRecordDto.setIdentifierItem(IdentifierItem.builder().idType(assertNewIdType).build());

        // Unauthorized test for updating
        this.assertBadRequestRequest("updateRecordBadRequest", put("/api/domains/" + domainName + "/pseudonyms/complete"), updateParameter, updateRecordDto, "");
        this.assertUnauthorizedRequest("updateRecordUnauth", put("/api/domains/" + domainName + "/pseudonyms/complete"), updateParameter, updateRecordDto, "SomeToken");

        // Delete a record
        Map<String, String> deleteParameter = new HashMap<>() {
            private static final long serialVersionUID = -6287903986808748519L;

            {
                put("id", "10000008912");
                put("idType", "ANY-ID");
            }
        };

        // Unauthorized test for deleting
        this.assertBadRequestRequest("deleteRecordBadRequest", delete("/api/domains/" + domainName + "/pseudonyms"), deleteParameter, null, "");
        this.assertUnauthorizedRequest("deleteRecordUnauth", delete("/api/domains/" + domainName + "/pseudonyms"), deleteParameter, null, "SomeToken");

        this.assertNoContent("deleteRecord", delete("/api/domains/" + domainName + "/pseudonyms"), deleteParameter, null, this.getAccessToken());

        // Check that the record is not found after its deletion
        this.assertNotFoundRequest("readRecordAfterDeletion", get("/api/domains/" + domainName + "/pseudonyms"), deleteParameter, null, this.getAccessToken());
    }

    /**
     * Test that tries to trigger different errors on endpoints.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("recordFailureTest")
    public void recordFailureTest() throws Exception {
        // Trigger forbidden e.g. no permission on all single object endpoints
        String failDomainName = "No-Permission";
        String goodDomain = "TestStudie";
        String goodDomainButNotFound = "TestStudie-Labor";
        String assertId = "1234356";
        String assertIdType = "EINE-ID";
        String assertNewIdType = "ANY-ID";

        // Create record forbidden
        PseudonymDTO createRecordDto = new PseudonymDTO();
        createRecordDto.setIdentifierItem(IdentifierItem.builder().identifier(assertId).idType(assertIdType).build());

        this.assertForbiddenRequest("createRecordForbidden", post("/api/domains/" + failDomainName + "/pseudonyms"), null, createRecordDto, this.getAccessToken());

        // Read record forbidden
        Map<String, String> getParameter = new HashMap<>() {
            private static final long serialVersionUID = 2157400189927185293L;

            {
                put("id", assertId);
                put("idType", assertIdType);
            }
        };
        this.assertForbiddenRequest("readRecordForbidden", get("/api/domains/" + failDomainName + "/pseudonyms"), getParameter, null, this.getAccessToken());

        // Update record forbidden
        Map<String, String> updateParameter = new HashMap<>() {
            private static final long serialVersionUID = -4330102644230952601L;

            {
                put("id", assertId);
                put("idType", assertIdType);
            }
        };
        PseudonymDTO updateRecordDto = new PseudonymDTO();
        updateRecordDto.setIdentifierItem(IdentifierItem.builder().idType(assertNewIdType).build());
        this.assertForbiddenRequest("updateRecordForbidden", put("/api/domains/" + failDomainName + "/pseudonyms/complete"), updateParameter, updateRecordDto, this.getAccessToken());

        // Delete record forbidden
        Map<String, String> deleteParameter = new HashMap<>() {
            private static final long serialVersionUID = -927325045565379459L;

            {
                put("id", assertId);
                put("idType", assertIdType);
            }
        };
        this.assertForbiddenRequest("deleteRecordForbidden", delete("/api/domains/" + failDomainName + "/pseudonyms"), deleteParameter, null, this.getAccessToken());

        // Trigger a "bad request" in a update-record without providing query parameters or a request-body
        this.assertBadRequestRequest("updateRecordBadRequest", put("/api/domains/" + goodDomain + "/pseudonyms"), null, null, this.getAccessToken());
        // Trigger a "not found" in a get-record without providing query parameters or a request-body
        this.assertNotFoundRequest("readRecordBadRequest", get("/api/domains/" + goodDomain + "/pseudonyms"), null, null, this.getAccessToken());
        // Trigger a "bad request" in a update-record-complete without providing query parameters or a request-body
        this.assertBadRequestRequest("updateCompleteRecordBadRequest", put("/api/domains/" + goodDomain + "/pseudonyms/complete"), updateParameter, null, this.getAccessToken());
        // Trigger a "bad request" in a delete-record without providing query parameters or a request-body
        this.assertBadRequestRequest("deleteRecordBadRequest", delete("/api/domains/" + goodDomain + "/pseudonyms"), null, null, this.getAccessToken());

        // Trigger a "not found" if permission for a domain is given but the domain is not yet created
        //domainOidcService.createDomainGroupsAndRolesAndJoin(goodDomainButNotFound, "3dfb6717-3def-493b-a237-b7345fc42718");
        this.assertNotFoundRequest("createRecordNotFound", post("/api/domains/" + goodDomainButNotFound + "/pseudonyms"), null, createRecordDto, this.getAccessToken());
        this.assertNotFoundRequest("readRecordNotFound", get("/api/domains/" + goodDomainButNotFound + "/pseudonyms"), getParameter, null, this.getAccessToken());
        this.assertNotFoundRequest("updateRecordNotFound", put("/api/domains/" + goodDomainButNotFound + "/pseudonyms/complete"), updateParameter, updateRecordDto, this.getAccessToken());

        Map<String, String> deleteParameterNotFound = new HashMap<>() {
            private static final long serialVersionUID = 7777773605144609107L;

            {
                put("id", "anything11");
                put("idType", "NotFound");
            }
        };
        this.assertNotFoundRequest("deleteRecordNotFound", delete("/api/domains/" + goodDomain + "/pseudonyms"), deleteParameterNotFound, null, this.getAccessToken());

        Map<String, String> deleteParameterInternalError = new HashMap<>() {
            private static final long serialVersionUID = 2685491390373289195L;

            {
                put("id", "anything11");
                put("idType", "NotFound");
                put("psn", "SomeValue");
            }
        };
        this.assertUnprocessableEntity("deleteRecordNotFound", delete("/api/domains/" + goodDomainButNotFound + "/pseudonyms"), deleteParameterInternalError, null, this.getAccessToken());

        Map<String, String> deleteParameterNotFound2 = new HashMap<>() {
            private static final long serialVersionUID = 3603630754239551087L;

            {
                put("psn", "SomeValue");
            }
        };
        this.assertNotFoundRequest("deleteRecordNotFound", delete("/api/domains/" + goodDomainButNotFound + "/pseudonyms"), deleteParameterNotFound2, null, this.getAccessToken());

    }

    /**
     * Tests the available batch endpoints
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("recordBatchTest")
    public void recordBatchTest() throws Exception {

        String goodDomain = "TestStudie";
        String goodDomainButNotFound = "TestStudie-Labor";

        // Create records in a batch
        // Make it look like a database object ...
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

        // Unauthorized test for batch create
        this.assertBadRequestRequest("createNewRecordBatchBadRequest", post("/api/domains/" + goodDomain + "/pseudonyms"), null, recordDtoList, "");
        this.assertUnauthorizedRequest("createNewRecordBatchUnauth", post("/api/domains/" + goodDomain + "/pseudonyms"), null, recordDtoList, "SomeToken");

        // No body
        this.assertBadRequestRequest("createNewRecordBatchBadRequest", post("/api/domains/" + goodDomain + "/pseudonyms"), null, null, this.getAccessToken());

        // Common batch request
        this.assertCreatedRequest("createNewRecordBatch", post("/api/domains/" + goodDomain + "/pseudonyms"), null, recordDtoList, this.getAccessToken());

        // Domain wrong
        //domainOidcService.createDomainGroupsAndRolesAndJoin(goodDomainButNotFound, "3dfb6717-3def-493b-a237-b7345fc42718");
        this.assertNotFoundRequest("createNewRecordBatchDomainNotFound", post("/api/domains/" + goodDomainButNotFound + "/pseudonyms"), null, recordDtoList, this.getAccessToken());

        // Trigger too many records
        List<PseudonymDTO> tooManyRecords = new ArrayList<>();
        for (int i = 1; i < 100002; i++) {
            Pseudonym p = new Pseudonym();
            p.setIdentifier(Integer.toString(i));
            p.setIdtype("ANY-ID");
            p.setValidfrominherited(true);
            p.setValidtoinherited(true);
            tooManyRecords.add(new PseudonymDTO().assignPojoValues(p));

        }

        this.assertUnprocessableEntity("createNewRecordBatchTooMany", post("/api/domains/" + goodDomain + "/pseudonyms"), null, tooManyRecords, this.getAccessToken());

        //tests for read records from batch -> also check length of output
        this.assertEqualsListRecordsLength(3, goodDomainButNotFound, goodDomain);

        this.assertOkRequest("readRecordBatch", get("/api/domains/" + goodDomain + "/pseudonyms"), null, null, this.getAccessToken());

        //must be a full object
        Pseudonym pseudonymUpdate1 = new Pseudonym();
        pseudonymUpdate1.setIdentifier("10000008913");
        pseudonymUpdate1.setIdtype("ANY-ID");
        pseudonymUpdate1.setValidfrom(LocalDateTime.now());
        pseudonymUpdate1.setValidfrominherited(false);
        pseudonymUpdate1.setValidto(LocalDateTime.now().plusMonths(30));
        pseudonymUpdate1.setValidtoinherited(false);
        pseudonymUpdate1.setPseudonym("TS-123456");

        Pseudonym pseudonymUpdate2 = new Pseudonym();
        pseudonymUpdate2.setIdentifier("10000008914");
        pseudonymUpdate2.setIdtype("ANY-ID");
        pseudonymUpdate2.setValidfrom(LocalDateTime.now());
        pseudonymUpdate2.setValidfrominherited(false);
        pseudonymUpdate2.setValidto(LocalDateTime.now().plusMonths(30));
        pseudonymUpdate2.setValidtoinherited(false);
        pseudonymUpdate2.setPseudonym("TS-123457");

        List<PseudonymDTO> recordDtoUpdateList = new ArrayList<>();
        recordDtoUpdateList.add(new PseudonymDTO().assignPojoValues(pseudonymUpdate1));
        recordDtoUpdateList.add(new PseudonymDTO().assignPojoValues(pseudonymUpdate2));

        this.assertNotFoundRequest("updateRecordBatchNotFound", put("/api/domains/" + goodDomainButNotFound + "/pseudonyms"), null, recordDtoUpdateList, this.getAccessToken());

        this.assertOkRequest("updateRecordBatch", put("/api/domains/" + goodDomain + "/pseudonyms"), null, recordDtoUpdateList, this.getAccessToken());


        //check if updated correctly
        Map<String, String> getParameter1 = new HashMap<>() {
            private static final long serialVersionUID = 27095263001488656L;

            {
                put("id", "10000008914");
                put("idType", "ANY-ID");
            }
        };
        MockHttpServletResponse response = this.assertOkRequest("readRecordCheckUpdate1", get("/api/domains/" + goodDomain + "/pseudonyms"), getParameter1, null, this.getAccessToken());
        String content = response.getContentAsString();

        List<PseudonymDTO> r1l = this.mapJsonObjectsInStringToList(content, PseudonymDTO.class);
        PseudonymDTO r = r1l.get(0);

        assertEquals("TS-123457", r.getPsn());

        Map<String, String> getParameter2 = new HashMap<>() {
            private static final long serialVersionUID = 7892918180869983266L;

            {
                put("id", "10000008913");
                put("idType", "ANY-ID");
            }
        };
        response = this.assertOkRequest("readRecordCheckUpdate2", get("/api/domains/" + goodDomain + "/pseudonyms"), getParameter2, null, this.getAccessToken());
        content = response.getContentAsString();
        List<PseudonymDTO> r2l = this.mapJsonObjectsInStringToList(content, PseudonymDTO.class);
        r = r2l.get(0);
        assertEquals("TS-123456", r.getPsn());

        //tests for delete records from batch
        this.assertNotFoundRequest("deleteRecordFromBatchNotFound", delete("/api/domains/" + goodDomainButNotFound + "/pseudonyms"), null, null, this.getAccessToken());
        this.assertNoContent("deleteRecordFromBatch", delete("/api/domains/" + goodDomain + "/pseudonyms"), null, null, this.getAccessToken());

        //really all deleted?
        this.assertEqualsListRecordsLength(0, goodDomainButNotFound, goodDomain);
    }

    /**
     * Tests some legacy support and also xxHash
     *
     * @throws Exception
     */
    @Test
    @DisplayName("legacyTest")
    public void legacyTest() throws Exception {

        String domainName = "TestStudie";

        Map<String, String> getParameter = new HashMap<>() {
            private static final long serialVersionUID = 4462487992226896288L;

            {
                put("name", domainName);
            }
        };


        MockHttpServletResponse response = this.assertOkRequest("getDomain", get("/api/domains/" + domainName), null, null, this.getAccessToken());
        String content = response.getContentAsString();
        DomainDTO d = this.applySingleJsonContentToClass(content, DomainDTO.class);
        d.setMultiplePsnAllowed(false);

        this.assertOkRequest("commonUpdateDomain", put("/api/domains"), getParameter, d, this.getAccessToken()); //works only if domain is empty

        Map<String, String> updateParameter = new HashMap<>() {
            private static final long serialVersionUID = 4462487992226896288L;
            {
                put("name", domainName);
                put("recursive", "false");
            }
        };

        this.assertOkRequest("commonUpdateDomainComplete", put("/api/domains/complete"), updateParameter, d, this.getAccessToken());

        XxHashPseudonymizer xx = new XxHashPseudonymizer(domainName);
        assertNotNull(xx);

        assertEquals("0ff9ec2d200c293e", xx.pseudonymize("fad0fcfe01f8e15592af59626aebadd17a0f3340ad1f9339fd586cde9ef43dbdea750336056381cbe68a21bedb9c312fc7a39e872beacde0cae8f7162a73d0dd", ""));
        assertEquals("0b242d361fda71bc", xx.pseudonymize("The quick brown fox jumps over the lazy dog", ""));
        assertEquals("44ad33705751ad73", xx.pseudonymize("The quick brown fox jumps over the lazy dog.", ""));
        assertEquals("b7b41276360564d4", xx.pseudonymize("1", ""));
        assertEquals("0ed74b125c8ae7f1", xx.pseudonymize("fewfwewwedwdwwwwwxsdafvdsssss", ""));
    }

    /**
     * Tests the available batch endpoints
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("recordLinkTest")
    public void recordLinkTest() throws Exception {

        String goodDomain = "TestStudie";

        // Create record forbidden
        PseudonymDTO createRecordDto = new PseudonymDTO();
        createRecordDto.setIdentifierItem(IdentifierItem.builder().identifier("123456").idType("MP-ID").build());

        MockHttpServletResponse response = this.assertCreatedRequest("createCommonRecord", post("/api/domains/" + goodDomain + "/pseudonyms"), null, createRecordDto, this.getAccessToken());

        String content = response.getContentAsString();

        List<PseudonymDTO> r1l = this.mapJsonObjectsInStringToList(content, PseudonymDTO.class);
        PseudonymDTO r = r1l.get(0);

        assertNotNull(r.getDomain());

        //like insert in a separate domain only for ids
        PseudonymDTO createLinkedRecordDto = new PseudonymDTO();
        createLinkedRecordDto.setIdentifierItem(IdentifierItem.builder().identifier(r.getPsn()).idType("TestStudie").build());
        createLinkedRecordDto.setPsn("1234356");

        Map<String, String> createLinkedPsnParams = new HashMap<>() {
            private static final long serialVersionUID = 4029631638184652287L;

            {
                put("omitPrefix", "true");
            }
        };

        MockHttpServletResponse linkedResponse = this.assertCreatedRequest("createCommonLinkedRecord", post("/api/domains/" + goodDomain + "/pseudonyms"), createLinkedPsnParams, createLinkedRecordDto, this.getAccessToken());
        List<PseudonymDTO> r2l = this.mapJsonObjectsInStringToList(linkedResponse.getContentAsString(), PseudonymDTO.class);
        PseudonymDTO linkedRecord = r2l.get(0);

        assertNotNull(r.getPsn());
        assertNotNull(linkedRecord.getPsn());
        assertNotNull(linkedRecord.getIdentifierItem().getIdentifier());
        assertNotEquals("", r.getPsn());
        assertNotEquals("", linkedRecord.getIdentifierItem().getIdentifier());
        assertEquals(r.getPsn(), linkedRecord.getIdentifierItem().getIdentifier());
    }
}
