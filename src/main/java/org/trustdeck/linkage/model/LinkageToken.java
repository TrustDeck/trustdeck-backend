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
 * This class represents a derived token for the record linkage index.
 * A token is generated from a entity's field value according 
 * to a linkage field rule and can be used for candidate generation or 
 * candidate scoring.
 *
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkageToken {

    /** The path of the field from which this token was generated. */
    private String fieldPath;

    /** The semantic tag of the field from which this token was generated. */
    private String tag;

    /** The type of token that was generated (e.g. norm, phonetic, block). */
    private LinkageTokenType tokenType;

    /** The actual token's value used for record linkage. */
    private String tokenValue;

    /** The weight contributed by this token during record linkage scoring. */
    private double weight;
}
