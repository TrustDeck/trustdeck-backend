/*
 * Trust Deck Services
 * Copyright 2026 Armin Müller and Eric Wündisch
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This DTO represents the *effective* permissions a user has at a certain point in time.
 *
 * This is intentionally smaller than {@link PermissionDTO} which represents the full 
 * grant row / admin view of a permission as it is in the database.
 *
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EffectivePermissionDTO {

    /** The type of the resource, e.g. "PROJECT", "DOMAIN". */
    private String resourceType;

    /** The name of the resource. */
    private String resourceName;

    /** The owning project for project-scoped entity type resources. */
    private String projectAbbreviation;

    /** The action that is effectively allowed on the resource, e.g. "domain:read". */
    private String action;

    /** Optional: start date from which this permission is valid. */
    private OffsetDateTime validFrom;

    /** Optional: end date up until which this permission is valid. */
    private OffsetDateTime validTo;

    /**
     * Helper method to check whether or not the permission is valid at a given time.
     * When passing {@code null} as a parameter, the method checks if the permission
     * is valid at OffsetDateTime.now().
     * 
     * @param time the time at which to check if the permission is valid
     * @return {@code true} if the permission is valid at the given time, {@code false} otherwise
     * 
     */
    @JsonIgnore
    public boolean isValidAt(OffsetDateTime time) {
        if (time == null) {
            time = OffsetDateTime.now();
        }
        
        boolean afterStart = (validFrom == null) || !validFrom.isAfter(time);
        boolean beforeEnd = (validTo == null) || validTo.isAfter(time);
        
        return afterStart && beforeEnd;
    }
    
    /**
	 * Converts the DTO into a string representation.
	 *
	 * @return the string representation of this DTO
	 */
	@JsonIgnore
	public String toRepresentationString() {
		String out = "";

		out += (this.getResourceType() != null) ? "resourceType: " + this.getResourceType() + ", " : "";
		out += (this.getResourceName() != null) ? "resourceName: " + this.getResourceName() + ", " : "";
		out += (this.getProjectAbbreviation() != null) ? "projectAbbreviation: " + this.getProjectAbbreviation() + ", " : "";
		out += (this.getAction() != null) ? "action: " + this.getAction() + ", " : "";
		out += (this.getValidFrom() != null) ? "validFrom: " + this.getValidFrom().toString() + ", " : "";
		out += (this.getValidTo() != null) ? "validTo: " + this.getValidTo().toString() + ", " : "";
		
		return (out.endsWith(", ") ? out.substring(0, out.length() - 2) : out);
	}
}
