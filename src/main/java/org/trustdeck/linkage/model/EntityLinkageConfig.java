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

package org.trustdeck.linkage.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity-level record-linkage configuration.
 *
 * Privacy mode, PPRL parameters, score thresholds, and automatic linkage
 * behavior belong to the complete entity type. Attribute-level rules only
 * decide which values participate and how those values are prepared and
 * weighted.
 *
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityLinkageConfig {
	
	/** Represents the plain-text record linkage mode. */
	public static final String PRIVACY_MODE_PLAIN = "plain";
	
	/** Represents the privacy-preserving record linkage mode. */
	public static final String PRIVACY_MODE_PPRL = "pprl";
	
	/** Represents the linkage decision of rejecting a linkage. */
	public static final String MATCH_ACTION_REJECT = "reject";
	
	/** Represents the linkage decision of returning an existing entity. */
	public static final String MATCH_ACTION_RETURN_EXISTING = "returnExisting";

	/** Whether record linkage is enabled for this entity type. */
	@Builder.Default
	private boolean enabled = true;

	/** Privacy mode that is applied to all linkage-enabled attributes. */
	@Builder.Default
	private String privacyMode = PRIVACY_MODE_PLAIN;

	/** PPRL settings applied to all linkage-enabled attributes in PPRL mode. */
	@Builder.Default
	private PPRLConfig pprlConfig = new PPRLConfig();

	/** Minimum raw entity score required for a candidate. */
	@Builder.Default
	private double minScore = 4.0;

	/** Minimum normalized entity score required for a candidate. */
	@Builder.Default
	private double minNormalizedScore = 0.50;

	/** Minimum Dice similarity at which a Bloom-filter field contributes. */
	@Builder.Default
	private double bloomMinSimilarity = 0.75;

	/** Maximum number of blocked candidates loaded before detailed scoring. */
	@Builder.Default
	private int candidateLimit = 250;

	/** Whether candidate search is automatically performed before creation. */
	@Builder.Default
	private boolean autoLinkOnCreate = false;

	/** Behavior when automatic linkage finds a candidate. */
	@Builder.Default
	private String onMatch = MATCH_ACTION_REJECT;

	/**
	 * Helper method that is used to check if the current config object uses PPRL.
	 * 
	 * @return {@code true}, when the config uses PPRL as the privacy mode, {@code false} otherwise
	 */
	public boolean usesPprl() {
		return PRIVACY_MODE_PPRL.equalsIgnoreCase(this.privacyMode);
	}

	/**
	 * Helper method that is used to check if the current config object 
	 * returns the existing object when a match was found.
	 * 
	 * @return {@code true}, when it is defined to return the matching object when linkage found a match, {@code false} otherwise
	 */
	public boolean returnsExistingOnMatch() {
		return MATCH_ACTION_RETURN_EXISTING.equalsIgnoreCase(this.onMatch);
	}
}
