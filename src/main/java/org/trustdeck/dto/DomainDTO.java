/*
 * Trust Deck Services
 * Copyright 2022-2024 Armin Müller and Eric Wündisch
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

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.context.annotation.Scope;
import org.trustdeck.jooq.generated.tables.interfaces.IDomain;
import org.trustdeck.jooq.generated.tables.pojos.Domain;
import org.trustdeck.service.DomainDBAccessService;
import org.trustdeck.service.AlgorithmDBService;
import org.trustdeck.utils.Assertion;
import org.trustdeck.utils.SpringBeanLocator;

import java.time.LocalDateTime;

/**
 * Data transfer object for the exchange of domain data.
 *
 * @author Armin Müller and Eric Wündisch
 */
@Data
@NoArgsConstructor
@Scope("prototype")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainDTO implements IObjectDTO<IDomain, DomainDTO> {
	
	/** Enables the access to the domain specific database access methods. */
	@Getter(value=AccessLevel.NONE)
    @Setter(value=AccessLevel.NONE)
    @JsonIgnore
    private DomainDBAccessService domainDBAccessService = SpringBeanLocator.getBean(DomainDBAccessService.class);

    @Getter(value=AccessLevel.NONE)
    @Setter(value=AccessLevel.NONE)
    @JsonIgnore
    private AlgorithmDBService algorithmDBService = SpringBeanLocator.getBean(AlgorithmDBService.class);
	
    /** The id of this domain. */
    private Integer id;

    /** The name of the domain. */
    private String name;

    /** The prefix for the domain. Used in the pseudonyms. */
    private String prefix;

    /** The date (and time) when the validity period of the entry starts. */
    private LocalDateTime validFrom;

    /** Determines if the validFrom value was inherited from the super domain. */
    private Boolean validFromInherited;

    /** The date (and time) when the validity period of the entry ends. */
    private LocalDateTime validTo;
    
    /** An amount of time a domain should be valid for. (Only needed for the creation.) */
    private String validityTime;

    /** Determines if the validTo value was inherited from the super domain. */
    private Boolean validToInherited;

    /** An option to ensure that the valid-from date of entries is always after or equal to the domain one. */
    private Boolean enforceStartDateValidity;

    /** Indicates whether or not the enforceStartDateValidity option was inherited. */
    private Boolean enforceStartDateValidityInherited;

    /** An option to ensure that the valid-to date of entries is always before or equal to the domain one. */
    private Boolean enforceEndDateValidity;

    /** Indicates whether or not the enforceEndDateValidity option was inherited. */
    private Boolean enforceEndDateValidityInherited;

    /** The algorithm used for pseudonymization. */
    private AlgorithmDTO algorithm;

    /** Determines if the algorithm value was inherited from the super domain. */
    private Boolean algorithmInherited;
    
    /** Determines if the domain can store multiple pseudonyms per given identifier or only one (1:n vs. 1:1). */
    private Boolean multiplePsnAllowed;
    
    /** Determines whether or not the multiple allowed pseudonyms option was inherited from the super domain. */
    private Boolean multiplePsnAllowedInherited;
	
    /** A description of the domain. */
    private String description;

    /** The super-domain ID of this domain. */
    private Integer superDomainID;

    /** The super-domain name of this domain. */
    private String superDomainName;

    /**
     * Maps all values from jOOQ's Domain object to a DomainDTO object.
     */
    @JsonIgnore
    @Override
    public DomainDTO assignPojoValues(IDomain pojo) {
    	this.setId(null);
        this.setName(pojo.getName());
        this.setPrefix(pojo.getPrefix());
        this.setValidFrom(pojo.getValidfrom());
        this.setValidFromInherited(pojo.getValidfrominherited());
        this.setValidTo(pojo.getValidto());
        this.setValidToInherited(pojo.getValidtoinherited());
        this.setEnforceStartDateValidity(pojo.getEnforcestartdatevalidity());
        this.setEnforceStartDateValidityInherited(pojo.getEnforcestartdatevalidityinherited());
        this.setEnforceEndDateValidity(pojo.getEnforceenddatevalidity());
        this.setEnforceEndDateValidityInherited(pojo.getEnforceenddatevalidityinherited());
        this.setAlgorithm(pojo.getAlgorithmId() == null ? null : new AlgorithmDTO().assignPojoValues(algorithmDBService.getAlgorithmByID(pojo.getAlgorithmId())));
        this.setAlgorithmInherited(pojo.getAlgorithmInherited());
        this.setMultiplePsnAllowed(pojo.getMultiplepsnallowed());
        this.setMultiplePsnAllowedInherited(pojo.getMultiplepsnallowedinherited());
        this.setDescription(Assertion.isNotNullOrEmpty(pojo.getDescription()) ? pojo.getDescription() : null);
        Domain d = (pojo.getSuperdomainid() != null && pojo.getSuperdomainid() > 0) ? domainDBAccessService.getDomainByID(pojo.getSuperdomainid()) : null;
        this.setSuperDomainName(d != null ? d.getName() : null);
        this.setSuperDomainID(d != null ? d.getId() : null);
        
        return this;
    }

    @Override
    @JsonIgnore
    public Boolean isValidStandardView() {
        return Assertion.assertNullAll(this.getId(),
        		this.getValidFromInherited(),
                this.getValidToInherited(),
                this.getEnforceStartDateValidity(),
                this.getEnforceStartDateValidityInherited(),
                this.getEnforceEndDateValidity(),
                this.getEnforceEndDateValidityInherited(),
                this.getAlgorithm(),
                this.getAlgorithmInherited(),
                this.getMultiplePsnAllowed(),
                this.getMultiplePsnAllowedInherited(),
                this.getSuperDomainID());
    }
    
    /**
     * This method creates a reduced standard view by setting all 
     * attributes that shouldn't be displayed by default to {@code null},
     * so that they are excluded in the JSON representation.
     * 
     * @return the altered domain DTO containing only the standard 
     * information (name, prefix, validFrom, validTo, description, 
     * superDomainName)
     */
    @Override
    @JsonIgnore
    public DomainDTO toReducedStandardView() {
    	this.setId(null);
        this.setValidFromInherited(null);
        this.setValidToInherited(null);
        this.setValidityTime(null);
        this.setEnforceStartDateValidity(null);
        this.setEnforceStartDateValidityInherited(null);
        this.setEnforceEndDateValidity(null);
        this.setEnforceEndDateValidityInherited(null);
        if (this.getAlgorithm() != null) {
            this.setAlgorithm(this.getAlgorithm().toReducedStandardView());
        }
        this.setAlgorithmInherited(null);
        this.setMultiplePsnAllowed(null);
        this.setMultiplePsnAllowedInherited(null);
        this.setSuperDomainID(null);
    	
    	return this;
    }

    /**
     * Creates a readable string representation.
     */
    @JsonIgnore
    @Override
    public String toRepresentationString() {
        String out = "";
        out += (this.getId() != null) ? "id: " + this.getId().toString() + ", " : "";
        out += (this.getName() != null) ? "name: " + this.getName() + ", " : "";
        out += (this.getPrefix() != null) ? "prefix: " + this.getPrefix() + ", " : "";
        out += (this.getValidFrom() != null) ? "validFrom: " + this.getValidFrom().toString() + ", " : "";
        out += (this.getValidFromInherited() != null) ? "validFromInherited: " + this.getValidFromInherited() + ", " : "";
        out += (this.getValidTo() != null) ? "validTo: " + this.getValidTo().toString() + ", " : "";
        out += (this.getValidToInherited() != null) ? "validToInherited: " + this.getValidToInherited() + ", " : "";
        out += (this.getEnforceStartDateValidity() != null) ? "enforceStartDateValidity: " + this.getEnforceStartDateValidity() + ", " : "";
        out += (this.getEnforceStartDateValidityInherited() != null) ? "enforceStartDateValidityInherited: " + this.getEnforceStartDateValidityInherited() + ", " : "";
        out += (this.getEnforceEndDateValidity() != null) ? "enforceEndDateValidity: " + this.getEnforceEndDateValidity() + ", " : "";
        out += (this.getEnforceEndDateValidityInherited() != null) ? "enforceEndDateValidityInherited: " + this.getEnforceEndDateValidityInherited() + ", " : "";
        out += (this.getAlgorithm() != null) ? "algorithm: " + this.getAlgorithm() + ", " : "";
        out += (this.getAlgorithmInherited() != null) ? "algorithmInherited: " + this.getAlgorithmInherited() + ", " : "";
        out += (this.getMultiplePsnAllowed() != null) ? "multiplePsnAllowed: " + this.getMultiplePsnAllowed() + ", " : "";
        out += (this.getMultiplePsnAllowedInherited() != null) ? "multiplePsnAllowedInherited: " + this.getMultiplePsnAllowedInherited() + ", " : "";
        out += (this.getDescription() != null) ? "description: " + this.getDescription() + ", " : "";
        out += (this.getSuperDomainID() != null) ? "superDomainID: " + this.getSuperDomainID() + ", " : "";
        out += (this.getSuperDomainName() != null) ? "superDomainName: " + this.getSuperDomainName() + ", " : "";

        return (out.endsWith(", ") ? out.substring(0, out.length() - 2) : out);
    }

    @Override
    @JsonIgnore
    public Boolean validate() {
        if (this.getName() == null || this.getName().trim().equals("") || this.getPrefix() == null) {
            return false;
        }

        return true;
    }
}
