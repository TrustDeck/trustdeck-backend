/*
 * Trust Deck Services
 * Copyright 2026 Armin Müller
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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Statistics DTO for one project.
 *
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectStatisticsDTO {

    /** The UTC time at which the statistics were generated. */
    private Instant generatedAt;

    /** The project identity and validity information. */
    private ProjectDetails project;

    /** The project's aggregate counts. */
    private Counts counts;

    /** Entity counts grouped by entity type. */
    private List<NamedCount> entitiesByType;

    /** Pseudonym counts grouped by project-owned domain. */
    private List<NamedCount> pseudonymsByDomain;

    /** Project identity and validity information. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectDetails {
    	
    	/** The unique name of the project. */
        private String name;

        /** The unique abbreviation of the project. */
        private String abbreviation;

        /** The start date and time of the project's validity period. */
        private OffsetDateTime startDate;

        /** The end date and time of the project's validity period. */
        private OffsetDateTime endDate;

        /**
         * The remaining project validity in seconds, or {@code null} when the
         * project has no end date. An expired project has zero remaining seconds.
         */
        private Long remainingValiditySeconds;
    }

    /** Aggregate project counts. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Counts {

        /** The exact number of domains owned by the project. */
        private long domains;

        /** The exact number of entity types belonging to the project. */
        private long entityTypes;

        /** The exact number of entities stored for the project. */
        private long entities;

        /** The exact number of pseudonyms contained in project-owned domains. */
        private long pseudonyms;
    }

    /** A stable resource identity and its count. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NamedCount {

        /** The name of the counted resource. */
        private String name;

        /** The exact number of records associated with the resource. */
        private long count;
    }
}
