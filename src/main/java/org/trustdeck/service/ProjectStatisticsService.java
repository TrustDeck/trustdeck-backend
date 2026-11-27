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

package org.trustdeck.service;

import static org.trustdeck.jooq.generated.Tables.DOMAIN;
import static org.trustdeck.jooq.generated.Tables.DOMAIN_PSEUDONYM_COUNTS;
import static org.trustdeck.jooq.generated.Tables.ENTITY_TYPE;
import static org.trustdeck.jooq.generated.Tables.ENTITY_TYPE_ENTITY_COUNTS;
import static org.trustdeck.jooq.generated.Tables.PROJECT;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.trustdeck.dto.ProjectStatisticsDTO;
import org.trustdeck.dto.ProjectStatisticsDTO.Counts;
import org.trustdeck.dto.ProjectStatisticsDTO.NamedCount;
import org.trustdeck.dto.ProjectStatisticsDTO.ProjectDetails;
import org.trustdeck.jooq.generated.tables.pojos.Project;

/**
 * Provides project-isolated aggregate statistics.
 *
 * The numbers of pseudonyms and entities are read from counters that database triggers keep up
 * to date on every write (see the views {@code domain_pseudonym_counts} and
 * {@code entity_type_entity_counts}), so the statistics stay cheap for tables with hundreds of
 * millions of rows.
 *
 * @author Armin Müller
 */
@Service
@RequiredArgsConstructor
public class ProjectStatisticsService {

    /** References the TrustDeck database. */
    private final DSLContext dsl;

    /**
     * Gathers the statistics of a project.
     *
     * The entity counts cover the entity types owned by the project that are not deleted; soft-deleted
     * entities and entities of types not owned by the project (e.g. of a global base type) are not
     * counted. The pseudonym counts cover the domains of the project.
     *
     * @param abbreviation the project abbreviation
     * @return the project statistics, or {@code null} when the project does not exist
     */
    @Transactional(readOnly = true)
    public ProjectStatisticsDTO getStatistics(String abbreviation) {
        
    	// Get project from database
    	Project project = dsl.selectFrom(PROJECT)
                .where(PROJECT.ABBREVIATION.equalIgnoreCase(abbreviation))
                .fetchOneInto(Project.class);
    	
        if (project == null) {
            return null;
        }

        int pid = project.getId();
        Instant generatedAt = Instant.now();

        // Count the entities per entity type of the project, also of types without entities
        List<NamedCount> entitiesByType = dsl.select(ENTITY_TYPE.NAME, ENTITY_TYPE_ENTITY_COUNTS.RECORD_COUNT)
                .from(ENTITY_TYPE)
                .join(ENTITY_TYPE_ENTITY_COUNTS).on(ENTITY_TYPE_ENTITY_COUNTS.ENTITY_TYPE_ID.eq(ENTITY_TYPE.ID))
                .where(ENTITY_TYPE.PROJECT_ID.eq(pid))
                .and(ENTITY_TYPE.IS_DEPRECATED.isFalse())
                .orderBy(ENTITY_TYPE.ID.asc())
                .fetch(record -> new NamedCount(record.get(ENTITY_TYPE.NAME), record.get(ENTITY_TYPE_ENTITY_COUNTS.RECORD_COUNT)));

        // Count the pseudonyms per domain of the project, also of domains without pseudonyms
        List<NamedCount> pseudonymsByDomain = dsl.select(DOMAIN.NAME, DOMAIN_PSEUDONYM_COUNTS.RECORD_COUNT)
                .from(DOMAIN)
                .join(DOMAIN_PSEUDONYM_COUNTS).on(DOMAIN_PSEUDONYM_COUNTS.DOMAIN_ID.eq(DOMAIN.ID))
                .where(DOMAIN.PROJECT_ID.eq(pid))
                .orderBy(DOMAIN.ID.asc())
                .fetch(record -> new NamedCount(record.get(DOMAIN.NAME), record.get(DOMAIN_PSEUDONYM_COUNTS.RECORD_COUNT)));

        long entityTypes = entitiesByType.size();
        long entities = entitiesByType.stream().mapToLong(NamedCount::getCount).sum();
        long pseudonyms = pseudonymsByDomain.stream().mapToLong(NamedCount::getCount).sum();

        // Calculate how long the project is still valid for
        Long remainingValiditySeconds = null;
        if (project.getEndDate() != null) {
            remainingValiditySeconds = Math.max(0L, Duration.between(generatedAt, project.getEndDate().toInstant()).getSeconds());
        }

        return new ProjectStatisticsDTO(generatedAt,
                new ProjectDetails(project.getName(), project.getAbbreviation(), project.getStartDate(),
                        project.getEndDate(), remainingValiditySeconds),
                new Counts(pseudonymsByDomain.size(), entityTypes, entities, pseudonyms), entitiesByType, pseudonymsByDomain);
    }
}
