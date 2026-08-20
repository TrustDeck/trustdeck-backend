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

import java.util.List;

import org.trustdeck.linkage.model.CandidateStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class represents a record linkage candidate together with its score.
 * It contains an entity that was identified as a potential match
 * as well as additional information about the corresponding linkage result.
 *
 * @author Armin Müller
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecordLinkageCandidateDTO {

    /** The entity that was identified as a potential record linkage candidate. */
    private EntityDTO entity;

    /** The score that indicates how well the candidate matches the input record. */
    private double score;
    
    /** The normalized score between 0.0 and 1.0 indicating how much of the possible query score matched. */
    private double normalizedScore;

    /** The list of fields or tags on which the candidate matched. */
    private List<String> matchedOn;

    /** The candidate status, e.g. ACTIVE or DELETED. */
    private CandidateStatus candidateStatus;
}
