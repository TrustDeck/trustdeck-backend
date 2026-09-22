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

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.trustdeck.dto.SystemStatisticsDTO;
import org.trustdeck.dto.SystemStatisticsDTO.ApplicationInfo;
import org.trustdeck.dto.SystemStatisticsDTO.ConnectionPoolInfo;
import org.trustdeck.dto.SystemStatisticsDTO.DatabaseInfo;
import org.trustdeck.dto.SystemStatisticsDTO.JvmMemoryInfo;
import org.trustdeck.dto.SystemStatisticsDTO.TableInfo;

/**
 * Provides database, build, connection-pool, and JVM statistics.
 *
 * @author Armin Müller
 */
@Service
@RequiredArgsConstructor
public class SystemStatisticsService {

    /** References the TrustDeck database. */
    private final DSLContext dsl;

    /** The qualified TrustDeck Hikari data source. */
    @Qualifier("trustdeckDataSource")
    private final HikariDataSource dataSource;

    /** Optional build metadata, absent during non-package builds. */
    private final ObjectProvider<BuildProperties> buildProperties;

    /** Optional Git metadata, absent when built outside a Git checkout. */
    private final ObjectProvider<GitProperties> gitProperties;

    /** Query mapping every non-partitioned logical table to its physical leaf relations and retrieves their storage sizes. */
    private static final String TABLE_RELATIONS_SQL = """
            WITH RECURSIVE roots AS (
                SELECT c.oid AS root_oid, n.nspname AS schema_name, c.relname AS table_name
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p') AND NOT c.relispartition
            ), descendants(root_oid, relation_oid) AS (
                SELECT root_oid, root_oid FROM roots
                UNION ALL
                SELECT d.root_oid, child.inhrelid
                FROM descendants d
                JOIN pg_inherits child ON child.inhparent = d.relation_oid
            )
            SELECT r.root_oid::bigint, r.schema_name, r.table_name, d.relation_oid::bigint,
                   pg_class.reltoastrelid::bigint,
                   pg_table_size(d.relation_oid) AS table_size_with_toast,
                   pg_indexes_size(d.relation_oid) AS index_size,
                   pg_total_relation_size(d.relation_oid) AS total_size
            FROM roots r
            JOIN descendants d ON d.root_oid = r.root_oid
            JOIN pg_class ON pg_class.oid = d.relation_oid
            WHERE NOT EXISTS (
                SELECT 1 FROM pg_inherits leaf_check WHERE leaf_check.inhparent  = d.relation_oid
            )
            ORDER BY r.schema_name, r.table_name, d.relation_oid
            """;

    /**
     * Retrieves system statistics.
     *
     * @return the current system statistics
     */
    @Transactional(readOnly = true)
    public SystemStatisticsDTO getStatistics() {
    	// Get database time and size
        OffsetDateTime databaseTime = dsl.select(DSL.field("current_timestamp", OffsetDateTime.class)).fetchOne(0, OffsetDateTime.class);
        long databaseSize = dsl.select(DSL.field("pg_database_size(current_database())", Long.class)).fetchOne(0, Long.class);

        // Gather relation sizes
        Map<Long, TableSize> sizesByRoot = new LinkedHashMap<>();
        Map<Long, TableIdentity> identities = new LinkedHashMap<>();
        Result<Record> relationRows = dsl.fetch(TABLE_RELATIONS_SQL);
        for (Record row : relationRows) {
            long rootOid = row.get("root_oid", Long.class);
            identities.put(rootOid, new TableIdentity(row.get("schema_name", String.class), row.get("table_name", String.class)));
            
            // 
            long toastSize = relationSize(row.get("reltoastrelid", Long.class));
            TableSize physicalSize = new TableSize(row.get("table_size_with_toast", Long.class) - toastSize,
                    row.get("index_size", Long.class), toastSize, row.get("total_size", Long.class));
            
            sizesByRoot.merge(rootOid, physicalSize, TableSize::add);
        }

        // Gather table sizes and counts
        List<TableInfo> tables = new ArrayList<>();
        for (Map.Entry<Long, TableIdentity> entry : identities.entrySet()) {
            TableIdentity identity = entry.getValue();
            long count = dsl.fetchCount(DSL.table(DSL.name(identity.schemaName(), identity.tableName())));
            TableSize size = sizesByRoot.getOrDefault(entry.getKey(), new TableSize(0, 0, 0, 0));
            
            tables.add(new TableInfo(identity.schemaName(), identity.tableName(), count, true,
                    size.tableSizeBytes(), size.indexSizeBytes(), size.toastSizeBytes(), size.totalRelationSizeBytes()));
        }

        // Gather connection pool info
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        ConnectionPoolInfo connectionPool = new ConnectionPoolInfo(pool.getActiveConnections(), 
        		pool.getIdleConnections(), pool.getTotalConnections(), pool.getThreadsAwaitingConnection(), 
        		dataSource.getMaximumPoolSize(), dataSource.getMinimumIdle());

        // Gather JVM memory info
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();
        JvmMemoryInfo jvmMemory = new JvmMemoryInfo(heap.getUsed(), heap.getCommitted(), heap.getMax(),
                nonHeap.getUsed(), nonHeap.getCommitted());

        // Gather build, git, and version info
        BuildProperties build = buildProperties.getIfAvailable();
        GitProperties git = gitProperties.getIfAvailable();
        String version = build == null ? "unknown" : build.getVersion();
        String commit = "unknown";
        
        if (git != null) {
            String full = git.get("git.commit.id.full");
            commit = full == null ? git.getCommitId() : full;
        }
        
        return new SystemStatisticsDTO(new ApplicationInfo(version, commit),
                new DatabaseInfo(databaseTime, databaseSize, tables), connectionPool, jvmMemory);
    }

    /**
     * Retrieves the complete storage size of a PostgreSQL relation.
     *
     * @param relationOid the PostgreSQL object identifier of the relation
     * @return the relation size in bytes, or {@code 0} when the identifier is missing, zero, or no size was returned
     */
    private long relationSize(Long relationOid) {
        if (relationOid == null || relationOid == 0) {
            return 0;
        }
        
        Long size = dsl.select(DSL.field("pg_total_relation_size({0}::oid::regclass)", Long.class, DSL.val(relationOid)))
                .fetchOne(0, Long.class);
        
        return size == null ? 0 : size;
    }

    /**
     * Identifies a logical database table.
     *
     * @param schemaName the name of the schema containing the table
     * @param tableName the name of the table
     */
    private record TableIdentity(String schemaName, String tableName) {}

    /**
     * Contains the aggregated storage sizes of a logical database table.
     *
     * @param tableSizeBytes the table size excluding indexes and separately reported TOAST storage, in bytes
     * @param indexSizeBytes the size of the table's indexes, in bytes
     * @param toastSizeBytes the complete size of the table's TOAST relation, including its indexes, in bytes
     * @param totalRelationSizeBytes the complete relation size, including table, index, and TOAST storage, in bytes
     */
    private record TableSize(long tableSizeBytes, long indexSizeBytes, long toastSizeBytes, long totalRelationSizeBytes) {
        
    	/**
         * Adds another relation's storage values to these values.
         *
         * @param other the storage values to add
         * @return new storage values containing the sum of both instances
         */
    	private TableSize add(TableSize other) {
            return new TableSize(tableSizeBytes + other.tableSizeBytes, indexSizeBytes + other.indexSizeBytes,
                    toastSizeBytes + other.toastSizeBytes, totalRelationSizeBytes + other.totalRelationSizeBytes);
        }
    }
}
