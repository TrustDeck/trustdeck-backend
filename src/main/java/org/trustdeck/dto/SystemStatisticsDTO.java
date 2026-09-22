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
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Statistics DTO describing the TrustDeck application, database, connection pool, and JVM.
 *
 * @author Armin Müller
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemStatisticsDTO {

	/** Information about the running application build. */
    private ApplicationInfo application;

    /** Information about the database and its tables. */
    private DatabaseInfo database;

    /** Information about the PostgreSQL connection-pool usage. */
    private ConnectionPoolInfo connectionPool;

    /** Information about the Java Virtual Machine's memory usage. */
    private JvmMemoryInfo jvmMemory;

    /** The application build's metadata. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationInfo {
        
    	/** The application version included in the build information. */
        private String version;

        /** The complete Git commit identifier from which the application was built. */
        private String buildCommit;
    }

    /** Database-wide and per-table statistics. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseInfo {

        /** The current time reported by the database in UTC. */
        private OffsetDateTime databaseTime;

        /** The total size of the database in bytes. */
        private long sizeBytes;

        /** Exact record-count and storage statistics for the logical database tables. */
        private List<TableInfo> tables;
    }

    /** Exact count and storage information for one logical table. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableInfo {

        /** The name of the database schema containing the table. */
        private String schemaName;

        /** The name of the logical database table. */
        private String tableName;

        /** The exact number of records stored in the table. */
        private long recordCount;

        /** Indicates whether the reported record count is exact rather than estimated. */
        private boolean recordCountExact;

        /** The table storage size in bytes, excluding indexes and separately reported TOAST storage. */
        private long tableSizeBytes;

        /** The total size of the indexes belonging to the table in bytes. */
        private long indexSizeBytes;

        /** The total size of the table's TOAST relation, including its indexes, in bytes. */
        private long toastSizeBytes;

        /** The complete size of the table relation, including indexes and TOAST storage, in bytes. */
        private long totalRelationSizeBytes;
    }

    /** TrustDeck Hikari connection-pool usage. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionPoolInfo {

        /** The number of connections currently in use. */
        private int activeConnections;

        /** The number of currently idle connections. */
        private int idleConnections;

        /** The total number of connections currently managed by the pool. */
        private int totalConnections;

        /** The number of threads currently waiting to obtain a database connection. */
        private int threadsAwaitingConnection;

        /** The configured maximum number of connections in the pool. */
        private int maximumPoolSize;

        /** The configured minimum number of idle connections maintained by the pool. */
        private int minimumIdleConnections;
    }

    /** JVM memory values, not host or container memory values. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JvmMemoryInfo {

        /** The amount of JVM heap memory currently in use, in bytes. */
        private long heapUsedBytes;

        /** The amount of JVM heap memory currently committed for use, in bytes. */
        private long heapCommittedBytes;

        /** The maximum amount of JVM heap memory available for use, in bytes. */
        private long heapMaximumBytes;

        /** The amount of JVM non-heap memory currently in use, in bytes. */
        private long nonHeapUsedBytes;

        /** The amount of JVM non-heap memory currently committed for use, in bytes. */
        private long nonHeapCommittedBytes;
    }
}
