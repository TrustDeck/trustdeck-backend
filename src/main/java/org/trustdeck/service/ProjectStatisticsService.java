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
import static org.trustdeck.jooq.generated.Tables.ENTITY;
import static org.trustdeck.jooq.generated.Tables.ENTITY_TYPE;
import static org.trustdeck.jooq.generated.Tables.PSEUDONYM;
import static org.trustdeck.jooq.generated.Tables.PROJECT;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
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
 * @author Armin Müller
 */
@Service
@RequiredArgsConstructor
public class ProjectStatisticsService {

    /** References the TrustDeck database. */
    private final DSLContext dsl;

    /**
     * Retrieves statistics for a project.
     *
     * @param abbreviation the project abbreviation
     * @return the statistics, or {@code null} when the project does not exist
     */
    @Transactional(readOnly = true)
    public ProjectStatisticsDTO getStatistics(String abbreviation) {
        return getStatistics(abbreviation, false);
    }

    /**
     * Gathers the statistics of a project.
     *
     * Domains and entity types are always counted exactly; these tables are small. Entities and
     * pseudonyms, in total and per entity type or domain, require scanning tables that can hold
     * hundreds of millions of rows. They are only counted with {@code exact}; otherwise the counts
     * are {@code null} and the lists contain the entity types and domains without counts. Use the
     * system statistics' live-row estimates and insert counters to follow progress cheaply.
     *
     * @param abbreviation the project abbreviation
     * @param exact whether to count entities and pseudonyms exactly
     * @return the project statistics, or {@code null} when the project does not exist
     */
    public ProjectStatisticsDTO getStatistics(String abbreviation, boolean exact) {
        
    	// Get project from database
    	Project project = dsl.selectFrom(PROJECT)
                .where(PROJECT.ABBREVIATION.equalIgnoreCase(abbreviation))
                .fetchOneInto(Project.class);
    	
        if (project == null) {
            return null;
        }

        // Gather statistics
        int pid = project.getId();
        Instant generatedAt = Instant.now();
        long domains = dsl.selectCount().from(DOMAIN).where(DOMAIN.PROJECT_ID.eq(pid)).fetchOne(0, Long.class);
        long entityTypes = dsl.selectCount().from(ENTITY_TYPE).where(ENTITY_TYPE.PROJECT_ID.eq(pid)).fetchOne(0, Long.class);
        Long entities = exact ? dsl.selectCount().from(ENTITY).where(ENTITY.PROJECT_ID.eq(pid)).fetchOne(0, Long.class) : null;
        Long pseudonyms = exact ? dsl.selectCount().from(PSEUDONYM).join(DOMAIN).on(PSEUDONYM.DOMAINID.eq(DOMAIN.ID))
                .where(DOMAIN.PROJECT_ID.eq(pid)).fetchOne(0, Long.class) : null;

        // Calculate how long the project is still valid for
        Long remainingValiditySeconds = null;
        if (project.getEndDate() != null) {
            remainingValiditySeconds = Math.max(0L, Duration.between(generatedAt, project.getEndDate().toInstant()).getSeconds());
        }

        List<NamedCount> entitiesByType;
        List<NamedCount> pseudonymsByDomain;

        if (exact) {
            // Count the entities for each entity type belonging to the project. The left join
            // also includes entity types without entities, for which COUNT(ENTITY.ID) returns zero.
            Field<Long> entityCount = DSL.count(ENTITY.ID).cast(Long.class).as("entity_count");
            entitiesByType = dsl.select(ENTITY_TYPE.ID, ENTITY_TYPE.NAME, entityCount)
                    .from(ENTITY_TYPE)
                    .leftJoin(ENTITY).on(ENTITY.ENTITY_TYPE_ID.eq(ENTITY_TYPE.ID))
                    .and(ENTITY.PROJECT_ID.eq(pid))
                    .where(ENTITY_TYPE.PROJECT_ID.eq(pid))
                    .groupBy(ENTITY_TYPE.ID, ENTITY_TYPE.NAME)
                    .orderBy(ENTITY_TYPE.ID.asc())
                    .fetch(record -> new NamedCount(record.get(ENTITY_TYPE.NAME), record.get(entityCount)));

            // Count the pseudonyms for each domain belonging to the project. The left join
            // also includes project-owned domains without pseudonyms, returning zero for them.
            Field<Long> pseudonymCount = DSL.count(PSEUDONYM.ID).cast(Long.class).as("pseudonym_count");
            pseudonymsByDomain = dsl.select(DOMAIN.ID, DOMAIN.NAME, pseudonymCount)
                    .from(DOMAIN)
                    .leftJoin(PSEUDONYM).on(PSEUDONYM.DOMAINID.eq(DOMAIN.ID))
                    .where(DOMAIN.PROJECT_ID.eq(pid))
                    .groupBy(DOMAIN.ID, DOMAIN.NAME)
                    .orderBy(DOMAIN.ID.asc())
                    .fetch(record -> new NamedCount(record.get(DOMAIN.NAME), record.get(pseudonymCount)));
        } else {
            // List the entity types and domains without touching the large entity and pseudonym tables
            entitiesByType = dsl.select(ENTITY_TYPE.NAME).from(ENTITY_TYPE)
                    .where(ENTITY_TYPE.PROJECT_ID.eq(pid))
                    .orderBy(ENTITY_TYPE.ID.asc())
                    .fetch(record -> new NamedCount(record.get(ENTITY_TYPE.NAME), null));
            pseudonymsByDomain = dsl.select(DOMAIN.NAME).from(DOMAIN)
                    .where(DOMAIN.PROJECT_ID.eq(pid))
                    .orderBy(DOMAIN.ID.asc())
                    .fetch(record -> new NamedCount(record.get(DOMAIN.NAME), null));
        }

        return new ProjectStatisticsDTO(generatedAt,
                new ProjectDetails(project.getName(), project.getAbbreviation(), project.getStartDate(),
                        project.getEndDate(), remainingValiditySeconds),
                new Counts(domains, entityTypes, entities, pseudonyms), entitiesByType, pseudonymsByDomain);
    }
}
