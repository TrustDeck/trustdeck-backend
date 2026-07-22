/*
 * Trust Deck Services
 * Copyright 2024-2025 Armin Müller
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

import org.springframework.context.annotation.Scope;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Data transfer object for the exchange of algorithm data.
 *
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@Scope("prototype")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlgorithmDTO implements IObjectDTO<Algorithm, AlgorithmDTO> {
	
	/** The (internal) id of the algorithm object. */
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	private Integer id;
	
	/** The name given to this algorithm object. */
	private String name;
	
	/** The alphabet used in creating the pseudonyms. */
	private String alphabet;
	
	/** The desired number of possible pseudonyms when using a randomness-based algorithm. */
    private Long randomAlgorithmDesiredSize;
	
	/** The desired probability with which the pseudonymization should be successful when using a randomness-based algorithm. */
    private Double randomAlgorithmDesiredSuccessProbability;
	
	/** The current value of the counter when using a consecutive value-based algorithm or when multiple pseudonyms per identifier are allowed. */
    private Long consecutiveValueCounter;
	
	/** The length the pseudonym should have. */
    private Integer pseudonymLength;
	
	/** The character that should be used for padding if needed. */
    private String paddingCharacter;
	
	/** Whether or not to add a check digit to the pseudonym. */
    private Boolean addCheckDigit;
	
	/** Whether or not the desired pseudonym length should include the check digit (thus, the check digit replaces the last character of the generated pseudonym). */
    private Boolean lengthIncludesCheckDigit;
	
	/** The salt value. */
    private String salt;
	
	/** The desired length of the salt value. */
    private Integer saltLength;
	
	@JsonIgnore
	@Override
	public AlgorithmDTO assignPojoValues(Algorithm pojo) {
		if (pojo == null) {
	        return null;
	    }
	    
	    this.setId(pojo.getId());
	    this.setName(pojo.getName());
	    this.setAlphabet(pojo.getAlphabet());
	    this.setRandomAlgorithmDesiredSize(pojo.getRandomAlgorithmDesiredSize());
	    this.setRandomAlgorithmDesiredSuccessProbability(pojo.getRandomAlgorithmDesiredSuccessProbability());
	    this.setConsecutiveValueCounter(pojo.getConsecutiveValueCounter());
	    this.setPseudonymLength(pojo.getPseudonymLength());
	    this.setPaddingCharacter(pojo.getPaddingCharacter());
	    this.setAddCheckDigit(pojo.getAddCheckDigit());
	    this.setLengthIncludesCheckDigit(pojo.getLengthIncludesCheckDigit());
	    this.setSalt(pojo.getSalt());
	    this.setSaltLength(pojo.getSaltLength());
	    
	    return this;
	}
	
	/**
	 * Helper method that transforms an algorithm DTO object back into a POJO.
	 * 
	 * @return the POJO representation of this algorithm DTO
	 */
	@JsonIgnore
	public Algorithm convertToPOJO() {
		Algorithm algorithm = new Algorithm();
		algorithm.setId(this.getId());
		algorithm.setName(this.getName());
		algorithm.setAlphabet(this.getAlphabet());
		algorithm.setRandomAlgorithmDesiredSize(this.getRandomAlgorithmDesiredSize());
		algorithm.setRandomAlgorithmDesiredSuccessProbability(this.getRandomAlgorithmDesiredSuccessProbability());
		algorithm.setConsecutiveValueCounter(this.getConsecutiveValueCounter());
		algorithm.setPseudonymLength(this.getPseudonymLength());
		algorithm.setPaddingCharacter(this.getPaddingCharacter());
		algorithm.setAddCheckDigit(this.getAddCheckDigit());
		algorithm.setLengthIncludesCheckDigit(this.getLengthIncludesCheckDigit());
		algorithm.setSalt(this.getSalt());
		algorithm.setSaltLength(this.getSaltLength());
		
        return algorithm;
    }
	
	@JsonIgnore
	@Override
	public Boolean isValidStandardView() {
		return this.getId() == null;
	}

	@JsonIgnore
	@Override
	public AlgorithmDTO toReducedStandardView() {
		this.setId(null);
		this.setSalt(null);
		return this;
	}

	@JsonIgnore
	@Override
	public String toRepresentationString() {
		String out = "";
		out += this.getId() != null ? "id: " + this.getId() + ", " : "";
		out += this.getName() != null ? "name: " + this.getName() + ", " : "";
		out += this.getAlphabet() != null ? "alphabet: " + this.getAlphabet() + ", " : "";
		out += this.getRandomAlgorithmDesiredSize() != null ? "randomAlgorithmDesiredSize: " + this.getRandomAlgorithmDesiredSize() + ", " : "";
		out += this.getRandomAlgorithmDesiredSuccessProbability() != null ? "randomAlgorithmDesiredSuccessProbability: " + this.getRandomAlgorithmDesiredSuccessProbability() + ", " : "";
		out += this.getConsecutiveValueCounter() != null ? "consecutiveValueCounter: " + this.getConsecutiveValueCounter() + ", " : "";
		out += this.getPseudonymLength() != null ? "pseudonymLength: " + this.getPseudonymLength() + ", " : "";
		out += this.getPaddingCharacter() != null ? "paddingCharacter: " + this.getPaddingCharacter() + ", " : "";
		out += this.getAddCheckDigit() != null ? "addCheckDigit: " + this.getAddCheckDigit() + ", " : "";
		out += this.getLengthIncludesCheckDigit() != null ? "lengthIncludesCheckDigit: " + this.getLengthIncludesCheckDigit() + ", " : "";
		out += this.getSaltLength() != null ? "saltLength: " + this.getSaltLength() + ", " : "";
		return out.endsWith(", ") ? out.substring(0, out.length() - 2) : out;
	}

	@JsonIgnore
	@Override
	public Boolean validate() {
		return this.getName() != null && !this.getName().isBlank();
	}

}
