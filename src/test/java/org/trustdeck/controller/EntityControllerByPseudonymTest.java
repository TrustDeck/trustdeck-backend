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

package org.trustdeck.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.trustdeck.dto.EntityDTO;
import org.trustdeck.dto.EntityTypeDTO;
import org.trustdeck.dto.ProjectDTO;
import org.trustdeck.dto.PseudonymDTO;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.service.AuthorizationService;
import org.trustdeck.service.DomainDBAccessService;
import org.trustdeck.service.EntityDBService;
import org.trustdeck.service.EntityTypeDBService;
import org.trustdeck.service.ProjectDBService;
import org.trustdeck.service.PseudonymDBAccessService;
import org.trustdeck.service.ResponseService;
import org.trustdeck.utils.SpringBeanLocator;
import org.trustdeck.utils.Utility.Pair;

/**
 * Tests retrieving the entity of a pseudonym: directly in the associated domain, through linked
 * pseudonyms from another domain, the permission checks, and the routing next to GET /{trustDeckId}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EntityControllerByPseudonymTest {

	private static final String TRUSTDECK_ID = "3f1c3c7e-5f0a-4c1e-9f55-2a8f6d2b1a11";

	@Mock
	private ResponseService responseService;

	@Mock
	private ProjectDBService projectDBService;

	@Mock
	private EntityTypeDBService entityTypeDBService;

	@Mock
	private EntityDBService entityDBService;

	@Mock
	private DomainDBAccessService ddba;

	@Mock
	private PseudonymDBAccessService pdba;

	@Mock
	private AuthorizationService authorizationService;

	@InjectMocks
	private EntityController controller;

	private final EntityDTO entity = new EntityDTO();

	/** The DTOs resolve service beans on construction; a stub context is enough here. */
	@BeforeAll
	static void stubSpringContext() {
		new SpringBeanLocator().setApplicationContext(mock(ApplicationContext.class));
	}

	@BeforeEach
	void setUp() {
		ProjectDTO project = new ProjectDTO();
		project.setId(1);
		project.setAbbreviation("PRJ");
		project.setEndDate(OffsetDateTime.now().plusYears(1));
		when(projectDBService.getProjectByAbbreviation("PRJ")).thenReturn(project);

		EntityTypeDTO type = new EntityTypeDTO();
		type.setId(2);
		type.setName("patient");
		type.setIsDeprecated(false);
		type.setAssociatedDomainName("patient-root");
		when(entityTypeDBService.getEntityTypeByName("patient", 1)).thenReturn(type);

		entity.setIsDeleted(false);
		when(entityDBService.getEntity(TRUSTDECK_ID, 1, 2)).thenReturn(entity);

		when(authorizationService.hasDomainPermission(anyString(), anyString())).thenReturn(true);

		when(responseService.ok(any(), any(EntityDTO.class))).thenAnswer(invocation -> ResponseEntity.ok(invocation.getArgument(1)));
		when(responseService.badRequest(any())).thenReturn(ResponseEntity.badRequest().build());
		when(responseService.forbidden(any())).thenReturn(ResponseEntity.status(403).build());
		when(responseService.notFound(any())).thenReturn(ResponseEntity.notFound().build());
		when(responseService.gone(any())).thenReturn(ResponseEntity.status(410).build());
		when(responseService.unprocessableEntity(any())).thenReturn(ResponseEntity.unprocessableEntity().build());
	}

	/** A pseudonym of the TrustDeck ID in the associated domain leads directly to the entity. */
	@Test
	void resolvesPseudonymInAssociatedDomain() {
		PseudonymDTO p1 = pseudonym(TRUSTDECK_ID, "TrustDeckID", "ROOT-1");
		when(pdba.getPseudonymFromPsn("patient-root", "ROOT-1")).thenReturn(p1);

		ResponseEntity<?> response = controller.getEntityByPseudonym("PRJ", "patient", "patient-root", " ROOT-1 ", null);
		assertEquals(200, response.getStatusCode().value());
		assertSame(entity, response.getBody());
		verify(ddba, never()).getLinkedPseudonyms(any(), any(), any(), any(), any());
	}

	/** A pseudonym in another domain is followed through the linked pseudonyms to the associated domain. */
	@Test
	void resolvesPseudonymInLinkedDomain() {
		PseudonymDTO source = pseudonym("ROOT-1", "pseudonym", "FHIR-1");
		PseudonymDTO target = pseudonym(TRUSTDECK_ID, "TrustDeckID", "ROOT-1");
		when(ddba.getLinkedPseudonyms("fhir", null, null, "FHIR-1", "patient-root")).thenReturn(List.of(new Pair<>(source, target)));

		ResponseEntity<?> response = controller.getEntityByPseudonym("PRJ", "patient", "fhir", "FHIR-1", null);
		assertEquals(200, response.getStatusCode().value());
		assertSame(entity, response.getBody());
	}

	/** Without reading the pseudonym, or without linking when the domains differ, the request is forbidden. */
	@Test
	void requiresReadAndLinkPermissions() {
		when(authorizationService.hasDomainPermission("fhir", "pseudonym:read")).thenReturn(false);
		assertEquals(403, controller.getEntityByPseudonym("PRJ", "patient", "fhir", "FHIR-1", null).getStatusCode().value());

		when(authorizationService.hasDomainPermission("fhir", "pseudonym:read")).thenReturn(true);
		when(authorizationService.hasDomainPermission("patient-root", "pseudonym:link")).thenReturn(false);
		assertEquals(403, controller.getEntityByPseudonym("PRJ", "patient", "fhir", "FHIR-1", null).getStatusCode().value());
		verify(ddba, never()).getLinkedPseudonyms(any(), any(), any(), any(), any());

		// Linking is not needed within the associated domain
		PseudonymDTO p2 = pseudonym(TRUSTDECK_ID, "TrustDeckID", "ROOT-1");
		when(pdba.getPseudonymFromPsn("patient-root", "ROOT-1")).thenReturn(p2);
		assertEquals(200, controller.getEntityByPseudonym("PRJ", "patient", "patient-root", "ROOT-1", null).getStatusCode().value());
	}

	/** Unknown pseudonyms, pseudonyms of other identifiers, and deleted entities give 404. */
	@Test
	void reportsNotFound() {
		assertEquals(404, controller.getEntityByPseudonym("PRJ", "patient", "patient-root", "UNKNOWN", null).getStatusCode().value());
		assertEquals(404, controller.getEntityByPseudonym("PRJ", "patient", "fhir", "UNKNOWN", null).getStatusCode().value());

		PseudonymDTO p3 = pseudonym("12345", "patientId", "ROOT-2");
		when(pdba.getPseudonymFromPsn("patient-root", "ROOT-2")).thenReturn(p3);
		assertEquals(404, controller.getEntityByPseudonym("PRJ", "patient", "patient-root", "ROOT-2", null).getStatusCode().value());

		PseudonymDTO p4 = pseudonym(TRUSTDECK_ID, "TrustDeckID", "ROOT-1");
		when(pdba.getPseudonymFromPsn("patient-root", "ROOT-1")).thenReturn(p4);
		entity.setIsDeleted(true);
		assertEquals(404, controller.getEntityByPseudonym("PRJ", "patient", "patient-root", "ROOT-1", null).getStatusCode().value());

		assertEquals(404, controller.getEntityByPseudonym("UNKNOWN", "patient", "patient-root", "ROOT-1", null).getStatusCode().value());
		assertEquals(404, controller.getEntityByPseudonym("PRJ", "unknown", "patient-root", "ROOT-1", null).getStatusCode().value());
	}

	/** A pseudonym that leads to several entities cannot be resolved. */
	@Test
	void rejectsAmbiguousPseudonym() {
		PseudonymDTO source = pseudonym("ROOT-1", "pseudonym", "FHIR-1");
		PseudonymDTO first = pseudonym(TRUSTDECK_ID, "TrustDeckID", "ROOT-1");
		PseudonymDTO second = pseudonym("6b0b7a52-1f8e-4b8e-8f5e-3c9a2f1d0e22", "TrustDeckID", "ROOT-2");
		when(ddba.getLinkedPseudonyms("fhir", null, null, "FHIR-1", "patient-root")).thenReturn(List.of(
				new Pair<>(source, first), new Pair<>(source, second)));

		assertEquals(422, controller.getEntityByPseudonym("PRJ", "patient", "fhir", "FHIR-1", null).getStatusCode().value());
	}

	/** Entity types without an associated domain give 422. */
	@Test
	void requiresAssociatedDomain() {
		EntityTypeDTO type = new EntityTypeDTO();
		type.setId(3);
		type.setIsDeprecated(false);
		when(entityTypeDBService.getEntityTypeByName("other", 1)).thenReturn(type);

		assertEquals(422, controller.getEntityByPseudonym("PRJ", "other", "fhir", "FHIR-1", null).getStatusCode().value());
	}

	/** The endpoint needs the right to read entities of the type. */
	@Test
	void requiresEntityReadPermission() throws NoSuchMethodException {
		PreAuthorize rule = EntityController.class.getMethod("getEntityByPseudonym", String.class, String.class, String.class,
				String.class, String.class).getAnnotation(PreAuthorize.class);
		assertEquals("isAuthenticated() and @auth.hasEntityTypePermission(#root, #projectAbbreviation, #entityTypeName, 'entity:read')", rule.value());
	}

	/** The literal path segment "by-pseudonym" is routed to this endpoint, not to GET /{trustDeckId}. */
	@Test
	void routesByPseudonymBeforeTrustDeckId() throws Exception {
		when(responseService.ok(any(), any(EntityDTO.class))).thenReturn(ResponseEntity.ok().build());
		PseudonymDTO p5 = pseudonym(TRUSTDECK_ID, "TrustDeckID", "ROOT-1");
		when(pdba.getPseudonymFromPsn("patient-root", "ROOT-1")).thenReturn(p5);
		MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

		mvc.perform(get("/api/projects/PRJ/entities/patient/by-pseudonym").param("domain", "patient-root").param("psn", "ROOT-1"))
				.andExpect(status().isOk());
		verify(pdba).getPseudonymFromPsn("patient-root", "ROOT-1");
		verify(entityDBService, never()).getEntity(eq("by-pseudonym"), anyInt(), anyInt());
	}

	private static PseudonymDTO pseudonym(String identifier, String idType, String psn) {
		PseudonymDTO p = new PseudonymDTO();
		p.setIdentifierItem(IdentifierItem.builder().identifier(identifier).idType(idType).build());
		p.setPsn(psn);
		return p;
	}
}
