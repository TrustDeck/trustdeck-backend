/*
 * Trust Deck Services
 * Copyright 2025-2026 Armin Müller and Eric Wündisch
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

package org.trustdeck.controller;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.trustdeck.dto.ProjectDTO;
import org.trustdeck.dto.ProjectImageDTO;
import org.trustdeck.exception.UnexpectedResultSizeException;
import org.trustdeck.security.audittrail.annotation.Audit;
import org.trustdeck.service.ProjectDBService;
import org.trustdeck.service.ProjectImageDBService;
import org.trustdeck.service.ResponseService;
import org.trustdeck.utils.Assertion;

import lombok.extern.slf4j.Slf4j;

/**
 * This class offers a REST API for interacting with project images.
 *
 * @author Armin Müller
 */
@RestController
@EnableMethodSecurity
@Slf4j
@RequestMapping(value = "/api")
public class ProjectImageController {

	/** Enables access to the project image database functionalities. */
	@Autowired
	private ProjectImageDBService imageService;

	/** Enables access to the project database functionalities. */
	@Autowired
	private ProjectDBService projectService;

	/** Enables service for working with predefined responses. */
    @Autowired
    private ResponseService responseService;

	/** The maximum image size in bytes. Must match the DB check (5MiB) */
	private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

	/** The set of media types that a given image needs to be in. */
	private static final Set<String> ALLOWED_IMAGE_MEDIA_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/svg+xml");
	
	/**
	 * Endpoint to store a new image for a project.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the image belongs to
	 * @param image the image that should be stored
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>201-CREATED</b> status when the project image was successfully stored</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when no image was provided, the abbreviation 
     *         is missing, or the image is otherwise invalid</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project does not exist</li>
     *         <li>a <b>410-GONE</b> status when the project has already ended</li>
     *         <li>a <b>413-PAYLOAD_TOO_LARGE</b> status when the image exceeds the maximum 
     *         allowed size</li>
     *         <li>a <b>415-UNSUPPORTED_MEDIA_TYPE</b> status when the file is not of a 
     *         supported image type</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the image could not be 
     *         processed or creation failed</li>
	 */
	@PostMapping(path = "/projects/{projectAbbreviation}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'image:create')")
    @Audit
    public ResponseEntity<?> createProjectImage(@PathVariable(name = "projectAbbreviation", required = true) String projectAbbreviation,
												@RequestPart("image") MultipartFile image,
												@RequestHeader(name = "accept", required = false) String responseContentType) {
		// Check that the given file is not null
		if (image == null || image.isEmpty()) {
			log.debug("No image provided.");
			return responseService.badRequest(responseContentType);
		}
		
		// Ensure that the image is not too big
		if (image.getSize() > MAX_IMAGE_SIZE) {
			log.debug("The given file was too big.");
			return responseService.payloadTooLarge(responseContentType);
		}
		
		// Check that the given file is an image
		if (!ALLOWED_IMAGE_MEDIA_TYPES.contains(image.getContentType())) {
			log.debug("The given file is not of a supported image type (was: \"" + image.getContentType() + "\").");
			return responseService.unsupportedMediaType(responseContentType);

		}
		
		// Retrieve the owning project
		if (Assertion.isNullOrEmpty(projectAbbreviation)) {
			log.debug("No project abbreviation was given.");
			return responseService.badRequest(responseContentType);
		}
		
		ProjectDTO project = projectService.getProjectByAbbreviation(projectAbbreviation);
		
		if (project == null) {
			log.debug("Could not find the project.");
			return responseService.notFound(responseContentType);
		} else if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			log.debug("Project has already ended. No changes allowed.");
			return responseService.gone(responseContentType);
		}
		
		// Transform image to bytes
		byte[] imageBytes;
		try {
			imageBytes = image.getBytes();
		} catch (IOException e) {
			log.debug("Representing the image as a byte array failed.");
			return responseService.unprocessableEntity(responseContentType);
		}
		
		// Build DTO
		ProjectImageDTO imageDTO = new ProjectImageDTO();
		imageDTO.setProjectId(project.getId());
		imageDTO.setImageBytes(imageBytes);
		imageDTO.setMimeType(image.getContentType());
		
		// Create the image in the DB
		ProjectImageDTO dto = imageService.createProjectImage(imageDTO);
		
		// Check if the creation was successful
		if (dto == null) {
			log.debug("Creating the project image failed.");
			return responseService.unprocessableEntity(responseContentType);
		} else {
			log.info("Successfully saved a new image for the project \"" + project.getName() + "\".");
			return responseService.created(responseContentType);
		}
	}
	
	/**
	 * Endpoint to retrieve the image of a project.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the image belongs to
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status with the image bytes and corresponding MIME type 
	 * 		   on success</li>
     *         <li>a <b>204-NO_CONTENT</b> status when an image entry exists but contains no 
     *         data</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when the abbreviation is missing</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project or its image does not exist</li>
     *         <li>a <b>410-GONE</b> status when the project has already ended</li>
	 */
	@GetMapping(path = "/projects/{projectAbbreviation}/image")
	@PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'image:read')")
    @Audit
	public ResponseEntity<?> getProjectImage(@PathVariable(name = "projectAbbreviation", required = true) String projectAbbreviation,
											 @RequestHeader(name = "accept", required = false) String responseContentType) {
		// Retrieve the owning project
		if (Assertion.isNullOrEmpty(projectAbbreviation)) {
			log.debug("No project abbreviation was given.");
			return responseService.badRequest(responseContentType);
		}
		
		ProjectDTO project = projectService.getProjectByAbbreviation(projectAbbreviation);
		
		if (project == null) {
			log.debug("Could not find the project.");
			return responseService.notFound(responseContentType);
		} else if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			log.debug("Project has already ended. No changes allowed.");
			return responseService.gone(responseContentType);
		}
		
		// Retrieve the image from the database		
		ProjectImageDTO dto = imageService.getProjectImageByProjectId(project.getId());
		
		if (dto == null) {
			log.debug("Could not find the project's image.");
			return responseService.notFound(responseContentType);
		} else if (dto.getImageBytes() == null || dto.getMimeType() == null) {
			log.debug("Image was empty.");
			return responseService.noContent(responseContentType);
		}
		
		log.debug("Successfully retrieved the project's image.");
		return responseService.ok(dto.getMimeType(), dto.getImageBytes());
	}
	
	/**
	 * Endpoint to update a project image.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the updated image belongs to
	 * @param image the updated image that should be stored
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>200-OK</b> status when the project image was successfully updated</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when no image was provided or the 
     *         abbreviation is missing</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project does not exist</li>
     *         <li>a <b>410-GONE</b> status when the project has already ended</li>
     *         <li>a <b>413-PAYLOAD_TOO_LARGE</b> status when the image exceeds the maximum 
     *         allowed size</li>
     *         <li>a <b>415-UNSUPPORTED_MEDIA_TYPE</b> status when the file is not of a 
     *         supported image type</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the image could not be 
     *         processed or the update failed</li>
	 */
	@PutMapping(path = "/projects/{projectAbbreviation}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'image:update')")
    @Audit
	public ResponseEntity<?> updateProjectImage(@PathVariable(name = "projectAbbreviation", required = true) String projectAbbreviation,
												@RequestPart("image") MultipartFile image,
												@RequestHeader(name = "accept", required = false) String responseContentType) {
		// Retrieve the owning project
		if (Assertion.isNullOrEmpty(projectAbbreviation)) {
			log.debug("No project abbreviation was given.");
			return responseService.badRequest(responseContentType);
		}
		
		ProjectDTO project = projectService.getProjectByAbbreviation(projectAbbreviation);
		
		if (project == null) {
			log.debug("Could not find the project.");
			return responseService.notFound(responseContentType);
		} else if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			log.debug("Project has already ended. No changes allowed.");
			return responseService.gone(responseContentType);
		}
		
		// Check that the given file is not null
		if (image == null || image.isEmpty()) {
			log.debug("No image provided.");
			return responseService.badRequest(responseContentType);
		}
		
		// Ensure that the image is not too big
		if (image.getSize() > MAX_IMAGE_SIZE) {
			log.debug("The given file was too big.");
			return responseService.payloadTooLarge(responseContentType);
		}
		
		// Check that the given file is an image
		if (!ALLOWED_IMAGE_MEDIA_TYPES.contains(image.getContentType())) {
			log.debug("The given file is not of a supported image type (was: \"" + image.getContentType() + "\").");
			return responseService.unsupportedMediaType(responseContentType);

		}
		
		// Transform image to bytes
		byte[] imageBytes;
		try {
			imageBytes = image.getBytes();
		} catch (IOException e) {
			log.debug("Representing the image as a byte array failed.");
			return responseService.unprocessableEntity(responseContentType);
		}
		
		// Build DTO
		ProjectImageDTO imageDTO = new ProjectImageDTO();
		imageDTO.setProjectId(project.getId());
		imageDTO.setImageBytes(imageBytes);
		imageDTO.setMimeType(image.getContentType());
		
		// Update the image in the DB
		ProjectImageDTO dto = imageService.updateProjectImage(project.getId(), imageDTO);
		
		// Check if the creation was successful
		if (dto == null) {
			log.debug("Updating the project image failed.");
			return responseService.unprocessableEntity(responseContentType);
		} else {
			log.info("Successfully saved an updated image for the project \"" + project.getName() + "\".");
			return responseService.ok(responseContentType);
		}
	}
	
	/**
	 * Endpoint to delete an image from a project.
	 * 
	 * @param projectAbbreviation the abbreviation of the project to which the image belongs to
	 * @param responseContentType (optional) the response content type
	 * @return <li>a <b>204-NO_CONTENT</b> status when the project image was successfully 
	 * 		   deleted</li>
     *         <li>a <b>400-BAD_REQUEST</b> status when the abbreviation is missing</li>
     *         <li>a <b>404-NOT_FOUND</b> status when the project does not exist</li>
     *         <li>a <b>410-GONE</b> status when the project has already ended</li>
     *         <li>a <b>422-UNPROCESSABLE_ENTITY</b> status when the deletion failed or was 
     *         rolled back</li>
	 */
	@DeleteMapping(path = "/projects/{projectAbbreviation}/image")
	@PreAuthorize("isAuthenticated() and @auth.hasProjectPermission(#root, #projectAbbreviation, 'image:delete')")
    @Audit
	public ResponseEntity<?> deleteProjectImage(@PathVariable(name = "projectAbbreviation", required = true) String projectAbbreviation,
												@RequestHeader(name = "accept", required = false) String responseContentType) {
		// Retrieve the owning project
		if (Assertion.isNullOrEmpty(projectAbbreviation)) {
			log.debug("No project abbreviation was given.");
			return responseService.badRequest(responseContentType);
		}
		
		ProjectDTO project = projectService.getProjectByAbbreviation(projectAbbreviation);
		
		if (project == null) {
			log.debug("Could not find the project.");
			return responseService.notFound(responseContentType);
		} else if (project.getEndDate().isBefore(OffsetDateTime.now())) {
			log.debug("Project has already ended. No changes allowed.");
			return responseService.gone(responseContentType);
		}
		
		// Perform deletion
		boolean deleted;
		try {
			deleted = imageService.deleteProjectImage(project.getId());
		} catch (UnexpectedResultSizeException e) {
			log.warn("Project image deletion would affect an unexpected amount of records and was therefore rolled back.");
			return responseService.unprocessableEntity(responseContentType);
		}
		
		// Evaluate deletion result
		if (deleted) {
			log.info("Successfully deleted the project image.");
			return responseService.noContent(responseContentType);
		} else {
			log.debug("Project image deletion failed.");
			return responseService.unprocessableEntity(responseContentType);
		}
	}
}
