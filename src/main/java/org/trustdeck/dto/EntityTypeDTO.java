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

import org.jooq.JSONB;
import org.springframework.context.annotation.Scope;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.jooq.generated.tables.pojos.EntityType;
import org.trustdeck.service.DomainDBAccessService;
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
 * This class represents a Data Transfer Object (DTO) for an entity type.
 *
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@Scope("prototype")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"name", "version", "isDeprecated", "isBaseType", "baseTypeName", "projectName", "associatedDomainName", "typeDefinition"})
@Slf4j
public class EntityTypeDTO implements IObjectDTO<EntityType, EntityTypeDTO> {
	
	/** The (internal) ID of this entity type. Do not expose it to users. */
	@JsonIgnore
	private Integer id;
	
	/** The object type's name. */
	private String name;
	
	/** The version number of this entity type. */
	private String version;
	
	/** Flag that determines if this type is "deleted"/marked as deprecated. */
	private Boolean isDeprecated;
	
	/** Flag that determines if this type is a base type that is defined by TrustDeck admins. Other types can extend from this type. */
	private Boolean isBaseType;
	
	/** The type definition of this entity. */
	private JsonNode typeDefinition;
	
	/** The name of the base type if this type is not a base type itself. */
	private String baseTypeName;
	
	/** The ID of the base type if this type is not a base type itself. */
	@JsonIgnore
	private Integer baseTypeId;
	
	/* The name of the domain that is used to generate pseudonyms in. */
	private String associatedDomainName;
	
	/** The name of the project where this type is defined in. */
	private String projectName;
	
	/** The ID of the project where this type is defined in. */
	@JsonIgnore
	private Integer projectId;
	
	/** Enables access to domain database functions. */
	@Getter(value=AccessLevel.NONE)
    @Setter(value=AccessLevel.NONE)
	@JsonIgnore
	private DomainDBAccessService ddba = SpringBeanLocator.getBean(DomainDBAccessService.class);
	
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
	public EntityTypeDTO assignPojoValues(EntityType pojo) {
		if (pojo == null) {
	        return null;
	    }
		
		EntityTypeDTO baseType = pojo.getBaseTypeId() == null ? null : etdbs.getEntityTypeByID(pojo.getBaseTypeId());
		Domain domain = pojo.getAssociatedDomainId() == null ? null : ddba.getDomainByID(pojo.getAssociatedDomainId());
		ProjectDTO project = pojo.getProjectId() == null ? null : pdbs.getProjectByID(pojo.getProjectId());
		
	    this.setId(pojo.getId());
	    this.setName(pojo.getName());
	    this.setVersion(pojo.getVersion());
	    this.setIsDeprecated(pojo.getIsDeprecated());
	    this.setIsBaseType(pojo.getIsBaseType());
	    this.setTypeDefinition(toJsonNode(pojo.getTypeDefinition()));
	    this.setBaseTypeName(baseType == null ? null : baseType.getName());
	    this.setBaseTypeId(pojo.getBaseTypeId());
	    this.setAssociatedDomainName(domain == null ? null : domain.getName());
	    this.setProjectName(project == null ? null : project.getName());
	    this.setProjectId(pojo.getProjectId());

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
	public EntityTypeDTO toReducedStandardView() {
		// Currently not needed
		return null;
	}

	@JsonIgnore
	@Override
	public String toRepresentationString() {
		String out = "";

	    out += (this.getId() != null) ? "id: " + this.getId().toString() + ", " : "";
	    out += (this.getName() != null) ? "name: " + this.getName() + ", " : "";
	    out += (this.getVersion() != null) ? "version: " + this.getVersion() + ", " : "";
	    out += (this.getIsDeprecated() != null) ? "isDeprecated: " + this.getIsDeprecated() + ", " : "";
	    out += (this.getIsBaseType() != null) ? "isBaseType: " + this.getIsBaseType() + ", " : "";
	    out += (this.getTypeDefinition() != null) ? "typeDefinition: " + this.getTypeDefinition().toString() + ", " : "";
	    out += (this.getBaseTypeName() != null) ? "baseTypeName: " + this.getBaseTypeName() + ", " : "";
	    out += (this.getBaseTypeId() != null) ? "baseTypeId: " + this.getBaseTypeId() + ", " : "";
	    out += (this.getAssociatedDomainName() != null) ? "associatedDomainName: " + this.getAssociatedDomainName() + ", " : "";
	    out += (this.getProjectName() != null) ? "projectName: " + this.getProjectName() + ", " : "";
	    out += (this.getProjectId() != null) ? "projectID: " + this.getProjectId() + ", " : "";
	    
	    return (out.endsWith(", ") ? out.substring(0, out.length() - 2) : out);
	}

	@JsonIgnore
	@Override
	public Boolean validate() {
		return Assertion.assertNotNullAll(this.getName(), this.getVersion()) && !this.getIsDeprecated();
	}
}
