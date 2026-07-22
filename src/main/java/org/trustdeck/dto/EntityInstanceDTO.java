/*
 * Trust Deck Services
 * Copyright 2025 Armin Müller
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

package org.trustdeck.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.jooq.JSONB;
import org.springframework.context.annotation.Scope;
import org.trustdeck.jooq.generated.tables.pojos.EntityInstance;
import org.trustdeck.service.EntityTypeDBService;
import org.trustdeck.service.ProjectDBService;
import org.trustdeck.utils.Assertion;
import org.trustdeck.utils.SpringBeanLocator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * This class represents a Data Transfer Object (DTO) for an entity instance.
 *
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@Scope("prototype")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"trustdeckID", "projectName", "entityTypeName", "isDeleted", "createdAt", "updatedAt", "data"})
@Slf4j
public class EntityInstanceDTO implements IObjectDTO<EntityInstance, EntityInstanceDTO> {
	
	/** The (internal) ID of this entity instance. Do not expose it to users. */
	@JsonIgnore
	private Long id;
	
	/** The unique UUID for this entity instance, used for publicly accessing this instance. */
	private UUID trustdeckID;
	
	/** The ID of the project where this entity instance is scoped in. */
	@JsonIgnore
	private Integer projectID;
	
	/** The name of the project where this entity instance is scoped in. */
	private String projectName;
	
	/** The ID of the type of this entity instance. */
	@JsonIgnore
	private Integer entityTypeID;
	
	/** The name of the type of this entity instance. */
	private String entityTypeName;
	
	/** This entity instance's data (i.e. the attributes and values). */
	private JsonNode data;
	
	// TODO: introduce different post & update dtos where the below attributes are properly placed
	/** Flag that determines if this type is marked as deleted. */
	private Boolean isDeleted;
	
	/** The date and time when this entity instance was created. */
	private OffsetDateTime createdAt;
	
	/** The date and time when this entity instance was last updated. */
	private OffsetDateTime updatedAt;
	
	/** Enables access to the project specific database functions. */
	@Getter(value=AccessLevel.NONE)
    @Setter(value=AccessLevel.NONE)
	@JsonIgnore
	private ProjectDBService pdbs = SpringBeanLocator.getBean(ProjectDBService.class);
	
	/** Enables access to the entity type database functions. */
	@Getter(value=AccessLevel.NONE)
    @Setter(value=AccessLevel.NONE)
	@JsonIgnore
	private EntityTypeDBService etdbs = SpringBeanLocator.getBean(EntityTypeDBService.class);

	@JsonIgnore
	@Override
	public EntityInstanceDTO assignPojoValues(EntityInstance pojo) {
		if (pojo == null) {
	        return null;
	    }
		
		ProjectDTO project = pojo.getProjectId() == null ? null : pdbs.getProjectByID(pojo.getProjectId());
	    EntityTypeDTO type = pojo.getEntityTypeId() == null ? null : etdbs.getEntityTypeById(pojo.getEntityTypeId(), pojo.getProjectId());
		
	    this.setId(pojo.getId());
	    this.setTrustdeckID(pojo.getTrustdeckId());
	    this.setProjectID(pojo.getProjectId());
	    this.setProjectName(project == null ? null : project.getName());
	    this.setEntityTypeID(pojo.getEntityTypeId());
	    this.setEntityTypeName(type == null ? null : type.getName());
	    this.setData(toJsonNode(pojo.getData()));
	    this.setIsDeleted(pojo.getIsDeleted());
	    this.setCreatedAt(pojo.getCreatedAt());
	    this.setUpdatedAt(pojo.getUpdatedAt());

	    return this;
	}
	
	/**
	 * Helper method to parse a JSONB into a JsonNode.
	 * 
	 * @param jsonb the JSONB data
	 * @return a JsonNode representation of the given data, or {@code null} if parsing failed
	 */
	@JsonIgnore
	private JsonNode toJsonNode(JSONB jsonb) {
		JsonNode node = null;
		
		if (jsonb != null) {
			ObjectMapper objectMapper = SpringBeanLocator.getBean(ObjectMapper.class);
			try {
				node = objectMapper.readTree(jsonb.toString());
			} catch (JsonProcessingException e) {
				log.debug("Could not parse JSONB into JsonNode.");
			}
		}
		
		return node;
	}

	@JsonIgnore
	@Override
	public Boolean isValidStandardView() {
		// Currently not needed
		return null;
	}

	@JsonIgnore
	@Override
	public EntityInstanceDTO toReducedStandardView() {
		// Currently not needed
		return null;
	}

	@JsonIgnore
	@Override
	public String toRepresentationString() {
		String out = "";

	    out += (this.getId() != null) ? "id: " + this.getId().toString() + ", " : "";
	    out += (this.getTrustdeckID() != null) ? "trustDeckID: " + this.getTrustdeckID().toString() + ", " : "";
	    out += (this.getProjectID() != null) ? "projectID: " + this.getProjectID().toString() + ", " : "";
	    out += (this.getProjectName() != null) ? "projectName: " + this.getProjectName() + ", " : "";
	    out += (this.getEntityTypeID() != null) ? "entityTypeID: " + this.getEntityTypeID().toString() + ", " : "";
	    out += (this.getEntityTypeName() != null) ? "entityTypeName: " + this.getEntityTypeName() + ", " : "";
	    out += (this.getData() != null) ? "data: " + this.getData().toString() + ", " : "";
	    out += (this.getIsDeleted() != null) ? "isDeleted: " + this.getIsDeleted().toString() + ", " : "";
	    out += (this.getCreatedAt() != null) ? "createdAt: " + this.getCreatedAt().toString() + ", " : "";
	    out += (this.getUpdatedAt() != null) ? "updatedAt: " + this.getUpdatedAt().toString() + ", " : "";

	    return (out.endsWith(", ") ? out.substring(0, out.length() - 2) : out);
	}

	@JsonIgnore
	@Override
	public Boolean validate() {
		return this.getEntityTypeID() != null && this.getData() != null && !this.getIsDeleted() 
				&& Assertion.isNotNullOrEmpty(this.getTrustdeckID().toString(), this.getProjectID().toString());
	}
}
