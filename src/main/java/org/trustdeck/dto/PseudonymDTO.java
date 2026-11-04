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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.context.annotation.Scope;
import org.trustdeck.jooq.generated.tables.interfaces.IPseudonym;
import org.trustdeck.model.IdentifierItem;
import org.trustdeck.service.DomainDBAccessService;
import org.trustdeck.utils.Assertion;
import org.trustdeck.utils.SpringBeanLocator;

import java.time.LocalDateTime;

/**
 * This class offers an Data Transfer Object (DTO) for a pseudonym.
 *
 * @author Armin Müller and Eric Wündisch
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Scope("prototype")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class PseudonymDTO implements IObjectDTO<IPseudonym, PseudonymDTO> {

    /** Enables the access to the domain specific database access methods. */
	@Getter(value=AccessLevel.NONE)
    @Setter(value=AccessLevel.NONE)
    @JsonIgnore
    private final DomainDBAccessService domainDBAccessService = SpringBeanLocator.getBean(DomainDBAccessService.class);

    /** Stores the identifier for that entry. */
    private IdentifierItem identifierItem;

    /** The pseudonym belonging to the identifier. */
    private String psn;

    /** The date (and time) when the validity period of the entry starts. */
    private LocalDateTime validFrom;

    /** Determines if the validFrom value was inherited from the super domain. */
    private Boolean validFromInherited;

    /** The date (and time) when the validity period of the entry ends. */
    private LocalDateTime validTo;

    /** Determines if the validFrom value was inherited from the super domain. */
    private Boolean validToInherited;
    
    /** An amount of time a record should be valid for. (Only needed for the creation.) */
    private String validityTime;

    /** The name of the domain this record belongs to. */
    private String domainName;

    /** The domain object this entry belongs to. */
    @JsonIgnore
    private DomainDTO domain;

    /**
     * Maps all values from jOOQ's Pseudonym object to a PseudonymDTO object.
     */
    @JsonIgnore
    @Override
    public PseudonymDTO assignPojoValues(IPseudonym pojo) {
        String identifier = pojo.getIdentifier() != null ? pojo.getIdentifier() : "";
        String idType = pojo.getIdtype() != null ? pojo.getIdtype() : "";
        this.setIdentifierItem(IdentifierItem.builder().identifier(identifier).idType(idType).build());
        this.setPsn(pojo.getPseudonym() != null ? pojo.getPseudonym() : "");
        this.setValidFrom(pojo.getValidfrom() != null ? pojo.getValidfrom() : null);
        this.setValidFromInherited(pojo.getValidfrominherited());
        this.setValidTo(pojo.getValidto() != null ? pojo.getValidto() : null);
        this.setValidToInherited(pojo.getValidtoinherited());
        DomainDTO d = pojo.getDomainid() != null ? new DomainDTO().assignPojoValues(domainDBAccessService.getDomainByID(pojo.getDomainid())) : null;
        this.setDomainName(d != null ? d.getName() : null);
        this.setDomain(d != null ? d : null);

        return this;
    }

    @Override
    @JsonIgnore
    public Boolean isValidStandardView() {
        return Assertion.assertNullAll(
        		this.getValidFromInherited(),
                this.getValidToInherited(),
                this.getDomain());
    }

    /**
     * This method creates a reduced standard view by setting all
     * attributes that shouldn't be displayed by default to {@code null},
     * so that they are excluded in the JSON representation.
     *
     * @return the altered record DTO containing only the standard
     * information (identifier, idType, psn, vFrom, vTo, domainName)
     */
    @Override
    @JsonIgnore
    public PseudonymDTO toReducedStandardView() {
        this.setValidFromInherited(null);
        this.setValidToInherited(null);
        this.setValidityTime(null);
        this.setDomain(null);
        
        return this;
    }

    /**
     * Creates a readable string representation.
     */
    @JsonIgnore
    @Override
    public String toRepresentationString() {
        String out = "";
        out += (this.getIdentifierItem() != null) ? "identifierItem: {" + this.getIdentifierItem().toRepresentationString() + "}, " : "";
        out += (this.getPsn() != null) ? "pseudonym: " + this.getPsn() + ", " : "";
        out += (this.getValidFrom() != null) ? "validFrom: " + this.getValidFrom().toString() + ", " : "";
        out += (this.getValidFromInherited() != null) ? "validFromInherited: " + this.getValidFromInherited() + ", " : "";
        out += (this.getValidTo() != null) ? "validTo: " + this.getValidTo().toString() + ", " : "";
        out += (this.getValidToInherited() != null) ? "validToInherited: " + this.getValidToInherited() + ", " : "";
        out += (this.getDomainName() != null) ? "domainName: " + this.getDomainName() + ", " : "";
        out += (this.getDomain() != null) ? "domain: {" + this.getDomain().toRepresentationString() + "}" : "";

        return (out.endsWith(", ") ? out.substring(0, out.length() - 2) : out);
    }

    @Override
    @JsonIgnore
    public Boolean validate() {
        // A request body without an identifier item is invalid, not a server error
        return this.getIdentifierItem() != null && this.getIdentifierItem().isNotNullNorEmpty();
    }
}
