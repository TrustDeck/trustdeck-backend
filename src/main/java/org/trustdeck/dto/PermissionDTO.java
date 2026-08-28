/*
 * Trust Deck Services
 * Copyright 2024-2026 Armin Müller and Eric Wündisch
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

import org.springframework.context.annotation.Scope;
import org.trustdeck.jooq.generated.tables.interfaces.IPermissionGrant;
import org.trustdeck.utils.Assertion;

/**
 * Data Transfer Object (DTO) for permissions. This class represents the
 * permissions of a user for a specific domain/project and role.
 *
 * @author Eric Wündisch, Armin Müller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Scope("prototype")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PermissionDTO implements IObjectDTO<IPermissionGrant, PermissionDTO> {

	/** The (internal) ID of this permission. Do not expose it to users. */
	@JsonIgnore
	private Integer id;

	/** The subject ID (from Keycloak) associated with the permission. */
	private String subjectId;
	
	/** The type of the resource, e.g. Domain, Project */
	private String resourceType;
	
	/** The internal database ID of the resource. */
	@JsonIgnore
	private Integer resourceId;
	
	/** The domain this permission applies to. Either this or the projectAbbreviation should be used. */
	private String domainName;
	
	/** The project this permission applies to. Either this or the domainName should be used. */
	private String projectAbbreviation;

	/** The entity type this permission applies to. */
	private String entityTypeName;
	
	/** The action that this permission represents on the resource, e.g. domain:read, pseudonym:create. */
	private String action;
	
	/** The effective decision for the permission, e.g. ALLOW, DENY. */
	private String decision;
	
	/** The start date from which this permission is valid. */
	private OffsetDateTime validFrom;
	
	/** The end date up until this permission is valid. */
	private OffsetDateTime validTo;
	
	/** The date when this permission was first created. */
	private OffsetDateTime createdAt;
	
	/** The (Keycloak) ID of the user/account that initially granted this permission. */
	private String createdBy;
	
	/** The date when this permission was last updated. */
	private OffsetDateTime updatedAt;
	
	/** The (Keycloak) ID of the user/account that last updated this permission. */
	private String updatedBy;
	
	@Override
	@JsonIgnore
	public PermissionDTO assignPojoValues(IPermissionGrant pojo) {
		if (pojo == null) {
	        return this;
	    }

	    this.setId(pojo.getId());

	    this.setSubjectId(pojo.getSubjectId() != null ? pojo.getSubjectId() : "");
	    this.setResourceType(pojo.getResourceType() != null ? pojo.getResourceType() : "");
	    this.setResourceId(pojo.getResourceId());
	    this.setAction(pojo.getAction() != null ? pojo.getAction() : "");
	    this.setDecision(pojo.getDecision() != null ? pojo.getDecision() : "");
	    this.setValidFrom(pojo.getValidFrom());
	    this.setValidTo(pojo.getValidTo());
	    this.setCreatedAt(pojo.getCreatedAt());
	    this.setCreatedBy(pojo.getCreatedBy() != null ? pojo.getCreatedBy() : "");
	    this.setUpdatedAt(pojo.getUpdatedAt());
	    this.setUpdatedBy(pojo.getUpdatedBy() != null ? pojo.getUpdatedBy() : "");

	    return this;
	}

	/**
	 * Checks whether the default view of the object is valid.
	 *
	 * @return {@code true} if the default view is valid, otherwise {@code false}
	 */
	@Override
	@JsonIgnore
	public Boolean isValidStandardView() {
		return null;
	}

	/**
	 * Reduces the object to its standard view.
	 *
	 * @return the reduced PermissionDTO object
	 */
	@Override
	@JsonIgnore
	public PermissionDTO toReducedStandardView() {
		return this;
	}

	/**
	 * Returns a string representation of the object. The representation includes
	 * the values for role, domain, and user ID.
	 *
	 * @return a string representation of the object
	 */
	@Override
	@JsonIgnore
	public String toRepresentationString() {
		String out = "";
		
		out += (this.getId() != null) ? "id: " + this.getId().toString() + ", " : "";
	    out += (this.getSubjectId() != null) ? "subjectId: " + this.getSubjectId() + ", " : "";
	    out += (this.getResourceType() != null) ? "resourceType: " + this.getResourceType() + ", " : "";
	    out += (this.getResourceId() != null) ? "resourceId: " + this.getResourceId() + ", " : "";
	    out += (this.getDomainName() != null) ? "domainName: " + this.getDomainName() + ", " : "";
	    out += (this.getProjectAbbreviation() != null) ? "projectAbbreviation: " + this.getProjectAbbreviation() + ", " : "";
	    out += (this.getEntityTypeName() != null) ? "entityTypeName: " + this.getEntityTypeName() + ", " : "";
	    out += (this.getAction() != null) ? "action: " + this.getAction() + ", " : "";
	    out += (this.getDecision() != null) ? "decision: " + this.getDecision() + ", " : "";
	    out += (this.getValidFrom() != null) ? "validFrom: " + this.getValidFrom().toString() + ", " : "";
	    out += (this.getValidTo() != null) ? "validTo: " + this.getValidTo().toString() + ", " : "";
	    out += (this.getCreatedAt() != null) ? "createdAt: " + this.getCreatedAt().toString() + ", " : "";
	    out += (this.getCreatedBy() != null) ? "createdBy: " + this.getCreatedBy() + ", " : "";
	    out += (this.getUpdatedAt() != null) ? "updatedAt: " + this.getUpdatedAt().toString() + ", " : "";
	    out += (this.getUpdatedBy() != null) ? "updatedBy: " + this.getUpdatedBy() + ", " : "";


		return (out.endsWith(", ") ? out.substring(0, out.length() - 2) : out);
	}

	@Override
	@JsonIgnore
	public Boolean validate() {
		// Should either be a domain-specific permission and needs a domain name 
		// or (XOR) is a project-specific one with a project name
		// or (XOR) is a global permission with neither domain name nor project abbreviation
		return (this.getResourceType().equalsIgnoreCase("Domain") && Assertion.isNotNullOrEmpty(this.getDomainName()))
			 ^ (this.getResourceType().equalsIgnoreCase("Project") && Assertion.isNotNullOrEmpty(this.getProjectAbbreviation()))
			 ^ (this.getResourceType().equalsIgnoreCase("Entity_Type") && Assertion.isNotNullOrEmpty(this.getEntityTypeName()))
			 ^ (this.getResourceType().equalsIgnoreCase("Global") && Assertion.isNullOrEmpty(this.getDomainName(), this.getProjectAbbreviation()));
	}
}
