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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.trustdeck.dto.ProjectDTO;
import org.trustdeck.service.AssertWebRequestService;

/**
 * This class offers tests for the project and project image endpoints.
 *
 * @author Armin Müller
 */
public class TestsProjectServiceIT extends AssertWebRequestService {

    /** The project created for every test. */
    private static final String PROJECT = "TEST";

    /** A minimal PNG image (1x1 pixel). */
    private static final byte[] PNG = new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    /**
     * Tests creating, listing, retrieving, updating, and deleting projects.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("projectLifecycleTest")
    public void projectLifecycleTest() throws Exception {
        ProjectDTO project = new ProjectDTO();
        project.setName("Second Study");
        project.setAbbreviation("SECOND");
        project.setDescription("A second study");
        project.setStartDate(OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        project.setEndDate(OffsetDateTime.of(2034, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));

        // Unauthorized tests for creating a project
        this.assertUnauthorizedRequest("createProjectNoToken", post("/api/projects"), null, project, "");
        this.assertUnauthorizedRequest("createProjectUnauth", post("/api/projects"), null, project, "SomeToken");

        // Create the project
        MockHttpServletResponse response = this.assertCreatedRequest("createProject", post("/api/projects"), null, project, this.getAccessToken());
        assertEquals("/api/projects/SECOND", response.getHeader("Location"));
        ProjectDTO created = this.applySingleJsonContentToClass(response.getContentAsString(), ProjectDTO.class);
        assertEquals("Second Study", created.getName());
        assertEquals("SECOND", created.getAbbreviation());
        assertEquals(project.getStartDate().toInstant(), created.getStartDate().toInstant());
        assertEquals(project.getEndDate().toInstant(), created.getEndDate().toInstant());
        assertNotNull(created.getStoreEntities());
        assertNotNull(created.getStorePseudonyms());

        // Creating it again returns the existing project
        this.assertOkRequest("createProjectAgain", post("/api/projects"), null, project, this.getAccessToken());

        // Retrieve the project
        response = this.assertOkRequest("getProject", get("/api/projects/SECOND"), null, null, this.getAccessToken());
        assertEquals("A second study", this.applySingleJsonContentToClass(response.getContentAsString(), ProjectDTO.class).getDescription());

        // The creator can see the project in the list of projects
        response = this.assertOkRequest("listProjects", get("/api/projects"), null, null, this.getAccessToken());
        List<String> abbreviations = this.mapJsonObjectsInStringToList(response.getContentAsString(), ProjectDTO.class).stream()
                .map(ProjectDTO::getAbbreviation).toList();
        assertTrue(abbreviations.containsAll(List.of(PROJECT, "SECOND")));

        // Update the project
        ProjectDTO update = new ProjectDTO();
        update.setName("Second Study (updated)");
        update.setDescription("An updated description");
        response = this.assertOkRequest("updateProject", put("/api/projects/SECOND"), null, update, this.getAccessToken());
        ProjectDTO updated = this.applySingleJsonContentToClass(response.getContentAsString(), ProjectDTO.class);
        assertEquals("Second Study (updated)", updated.getName());
        assertEquals("SECOND", updated.getAbbreviation());
        assertEquals("An updated description", updated.getDescription());

        // Delete (tombstone) the project, after which it can't be used anymore
        this.assertNoContent("deleteProject", delete("/api/projects/SECOND"), null, null, this.getAccessToken());
        this.assertRequestFromRequestBuilder("getDeletedProject", get("/api/projects/SECOND"), null, null, this.getAccessToken(), status().isGone());
        this.assertRequestFromRequestBuilder("updateDeletedProject", put("/api/projects/SECOND"), null, update, this.getAccessToken(), status().isGone());
        this.assertRequestFromRequestBuilder("deleteDeletedProject", delete("/api/projects/SECOND"), null, null, this.getAccessToken(), status().isGone());
    }

    /**
     * Tests that projects that already ended can't be used anymore.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("endedProjectTest")
    public void endedProjectTest() throws Exception {
        ProjectDTO project = new ProjectDTO();
        project.setName("Ended Study");
        project.setAbbreviation("ENDED");
        project.setStartDate(OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        project.setEndDate(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        this.assertCreatedRequest("createEndedProject", post("/api/projects"), null, project, this.getAccessToken());

        ProjectDTO update = new ProjectDTO();
        update.setDescription("Too late");
        this.assertRequestFromRequestBuilder("getEndedProject", get("/api/projects/ENDED"), null, null, this.getAccessToken(), status().isGone());
        this.assertRequestFromRequestBuilder("updateEndedProject", put("/api/projects/ENDED"), null, update, this.getAccessToken(), status().isGone());
        this.assertRequestFromRequestBuilder("deleteEndedProject", delete("/api/projects/ENDED"), null, null, this.getAccessToken(), status().isGone());
        this.assertRequestFromRequestBuilder("createImageForEndedProject",
                this.authorized(multipart("/api/projects/ENDED/image").file(this.image(PNG, "image/png"))), status().isGone());
    }

    /**
     * Tests that invalid projects are rejected.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("invalidProjectTest")
    public void invalidProjectTest() throws Exception {
        ProjectDTO withoutName = new ProjectDTO();
        withoutName.setAbbreviation("NONAME");
        this.assertBadRequestRequest("createProjectWithoutName", post("/api/projects"), null, withoutName, this.getAccessToken());

        ProjectDTO withoutAbbreviation = new ProjectDTO();
        withoutAbbreviation.setName("Without Abbreviation");
        this.assertBadRequestRequest("createProjectWithoutAbbreviation", post("/api/projects"), null, withoutAbbreviation, this.getAccessToken());

        ProjectDTO invalidAbbreviation = new ProjectDTO();
        invalidAbbreviation.setName("Invalid Abbreviation");
        invalidAbbreviation.setAbbreviation("IN VALID/");
        this.assertBadRequestRequest("createProjectInvalidAbbreviation", post("/api/projects"), null, invalidAbbreviation, this.getAccessToken());

        ProjectDTO endBeforeStart = new ProjectDTO();
        endBeforeStart.setName("End Before Start");
        endBeforeStart.setAbbreviation("EBS");
        endBeforeStart.setStartDate(OffsetDateTime.of(2030, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        endBeforeStart.setEndDate(OffsetDateTime.of(2029, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        this.assertBadRequestRequest("createProjectEndBeforeStart", post("/api/projects"), null, endBeforeStart, this.getAccessToken());
    }

    /**
     * Tests retrieving the domains of a project.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("projectDomainsTest")
    public void projectDomainsTest() throws Exception {
        MockHttpServletResponse response = this.assertOkRequest("getProjectDomains", get("/api/projects/" + PROJECT + "/domains"), null, null, this.getAccessToken());
        assertTrue(response.getContentAsString().contains("\"TestStudie\""));
    }

    /**
     * Tests creating, retrieving, updating, and deleting a project's image.
     *
     * @throws Exception forwards any internally thrown exceptions
     */
    @Test
    @DisplayName("projectImageTest")
    public void projectImageTest() throws Exception {
        String imagePath = "/api/projects/" + PROJECT + "/image";

        // No image yet
        this.assertNotFoundRequest("getProjectImageNotFound", this.authorized(get(imagePath)));

        // Unauthorized test
        this.assertUnauthorizedRequest("createProjectImageUnauth", multipart(imagePath).file(this.image(PNG, "image/png")).header("Authorization", "Bearer SomeToken"));

        // Upload the image
        this.assertCreatedRequest("createProjectImage", this.authorized(multipart(imagePath).file(this.image(PNG, "image/png"))));
        MockHttpServletResponse response = this.assertOkRequest("getProjectImage", this.authorized(get(imagePath)));
        assertEquals("image/png", response.getContentType());
        assertArrayEquals(PNG, response.getContentAsByteArray());

        // Update the image
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes();
        this.assertOkRequest("updateProjectImage", this.authorized(this.asPut(multipart(imagePath).file(this.image(svg, "image/svg+xml")))));
        response = this.assertOkRequest("getUpdatedProjectImage", this.authorized(get(imagePath)));
        assertEquals("image/svg+xml", response.getContentType());
        assertArrayEquals(svg, response.getContentAsByteArray());

        // Invalid images are rejected
        this.assertRequestFromRequestBuilder("createProjectImageWrongType", this.authorized(multipart(imagePath).file(this.image("text".getBytes(), "text/plain"))),
                status().isUnsupportedMediaType());
        this.assertBadRequestRequest("createProjectImageEmpty", this.authorized(multipart(imagePath).file(this.image(new byte[0], "image/png"))));
        this.assertRequestFromRequestBuilder("createProjectImageTooLarge", this.authorized(multipart(imagePath).file(this.image(new byte[5 * 1024 * 1024 + 1], "image/png"))),
                status().isPayloadTooLarge());

        // Delete the image
        this.assertNoContent("deleteProjectImage", this.authorized(delete(imagePath)));
        this.assertNotFoundRequest("getDeletedProjectImage", this.authorized(get(imagePath)));
    }

    /**
     * Helper to create a multipart image file.
     *
     * @param content the image's content
     * @param contentType the image's media type
     * @return the multipart file
     */
    private MockMultipartFile image(byte[] content, String contentType) {
        return new MockMultipartFile("image", "image", contentType, content);
    }

    /**
     * Helper to change a multipart request into a PUT request.
     *
     * @param builder the multipart request builder
     * @return the request builder using the PUT method
     */
    private MockMultipartHttpServletRequestBuilder asPut(MockMultipartHttpServletRequestBuilder builder) {
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        return builder;
    }

    /**
     * Helper to add the access token of the user "test" to a request.
     *
     * @param <T> the type of the request builder
     * @param builder the request builder
     * @return the request builder with the authorization header
     */
    private <T extends AbstractMockHttpServletRequestBuilder<T>> T authorized(T builder) {
        builder.header("Authorization", "Bearer " + this.getAccessToken());

        return builder;
    }
}
