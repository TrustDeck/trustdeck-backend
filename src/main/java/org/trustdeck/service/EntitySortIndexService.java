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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Statement;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.trustdeck.model.SortableAttribute;
import org.trustdeck.service.EntityTypeDBService.EntityTypeDefinitionChangedEvent;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.trustdeck.jooq.generated.Tables.ENTITY_TYPE;

/**
 * Builds the expressions by which entity searches sort by an attribute, and keeps the database
 * indexes for these expressions in line with the type definitions.
 *
 * A scalar attribute of a type definition may set {@code "sortIndex": true}. For every such
 * attribute, the partition of the entity type ({@code entity_t<id>}) gets two indexes on the
 * attribute's sort expression, one per sort direction, so that a page of a large search sorted by
 * the attribute can be read from the index. The indexes are created and dropped with
 * {@code CONCURRENTLY}, which cannot run inside a transaction, so that the synchronization runs
 * after the transaction that changed the type definition was committed, in the background.
 *
 * @author Armin Müller
 */
@Slf4j
@Service
public class EntitySortIndexService {

	/** The part of an index name between the partition name and the hash of the indexed expression. */
	private static final String INDEX_NAME_INFIX = "_sort_";

	/** The suffix of the name of the index for sorting in descending order. */
	private static final String DESCENDING_SUFFIX = "_desc";

	/** The characters allowed in a part of the path to an attribute value (the meta schema's attribute names and array indexes). */
	private static final Pattern ALLOWED_PATH_CHARS = Pattern.compile("^[a-zA-Z0-9_-]+$");

	/** The column containing the entity data. Unqualified, so that the expression is the same in queries and index definitions. */
	private static final Field<JSONB> DATA = DSL.field(DSL.name("data"), SQLDataType.JSONB);

	/** References a jOOQ configuration object that configures jOOQ's behavior when executing queries. */
	private final DSLContext dsl;

	/** Resolves the sortable attributes from the type definitions. */
	private final JsonSchemaService jsonSchemaService;

	/** Enables access to the mapper to transform JSONB into JsonNode. */
	private final ObjectMapper objectMapper;

	/** Runs the synchronizations one after another in the background. */
	private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "entity-sort-index-sync");
		thread.setDaemon(true);
		return thread;
	});

	/**
	 * Constructor.
	 *
	 * @param dsl the jOOQ context
	 * @param jsonSchemaService resolves the sortable attributes from the type definitions
	 * @param objectMapper transforms JSONB into JsonNode
	 */
	public EntitySortIndexService(DSLContext dsl, JsonSchemaService jsonSchemaService, ObjectMapper objectMapper) {
		this.dsl = dsl;
		this.jsonSchemaService = jsonSchemaService;
		this.objectMapper = objectMapper;
	}

	/**
	 * Builds the expression by which entities are sorted by the given attribute. Both the searches
	 * and the sort indexes use this method, since the database only uses an expression index when
	 * the query contains the same expression.
	 *
	 * Numbers are compared numerically (values that are not JSON numbers sort like missing values);
	 * all other values as text, which sorts the ISO formatted dates and date-times chronologically.
	 *
	 * The expression is PostgreSQL-specific: it uses the JSONB path operators {@code #>} and
	 * {@code #>>}, {@code jsonb_typeof}, and the {@code ::numeric} cast.
	 *
	 * @param attribute the attribute to sort by
	 * @return the sort expression
	 * @throws IllegalArgumentException when the attribute's path contains characters that the
	 *         meta schema does not allow in attribute names
	 */
	public static Field<?> sortExpression(SortableAttribute attribute) {
		// The path parts come from the validated type definition; check them anyway, as they are inlined
		for (String pathPart : attribute.jsonPath()) {
			if (!ALLOWED_PATH_CHARS.matcher(pathPart).matches()) {
				throw new IllegalArgumentException("The attribute path \"" + attribute.path() + "\" contains invalid characters.");
			}
		}

		// Write the path as a PostgreSQL text array literal, e.g. {"address","city"}, which the JSONB path operators expect
		Field<String> path = DSL.inline(attribute.jsonPath().stream().map(e -> "\"" + e + "\"").collect(Collectors.joining(",", "{", "}")));

		// Numeric attributes: the value as a number if it is a JSON number, otherwise NULL, so that it sorts like a missing value
		if (attribute.isNumeric()) {
			return DSL.field("(CASE WHEN jsonb_typeof({0} #> {1}) = {2} THEN ({0} #>> {1})::numeric END)", SQLDataType.NUMERIC,
					DATA, path, DSL.inline("number"));
		}

		// All other attributes: the value as text
		return DSL.field("({0} #>> {1})", SQLDataType.CLOB, DATA, path);
	}

	/**
	 * Resolves the sortable attributes of an entity type, including those of its base type.
	 *
	 * @param entityTypeId the ID of the entity type
	 * @return the sortable attributes keyed by their path, or {@code null} when the type could not be read
	 */
	public Map<String, SortableAttribute> getSortableAttributes(int entityTypeId) {
		Record2<JSONB, Integer> type;
		JSONB baseDefinition = null;
		try {
			type = dsl.select(ENTITY_TYPE.TYPE_DEFINITION, ENTITY_TYPE.BASE_TYPE_ID)
					.from(ENTITY_TYPE)
					.where(ENTITY_TYPE.ID.eq(entityTypeId))
					.fetchOne();
			if (type == null) {
				return null;
			}

			// A derived type inherits the attributes of its base type, so read the base type's definition as well
			if (type.value2() != null) {
				baseDefinition = dsl.select(ENTITY_TYPE.TYPE_DEFINITION)
						.from(ENTITY_TYPE)
						.where(ENTITY_TYPE.ID.eq(type.value2()))
						.fetchOne(ENTITY_TYPE.TYPE_DEFINITION);
			}
		} catch (DataAccessException e) {
			log.debug("Reading the type definition of the entity type " + entityTypeId + " failed.", e);
			return null;
		}

		// Collect the sortable attributes from both definitions
		try {
			return jsonSchemaService.resolveSortableAttributes(toJsonNode(type.value1()), toJsonNode(baseDefinition));
		} catch (JacksonException e) {
			log.debug("Could not parse the type definition of the entity type " + entityTypeId + ".", e);
			return null;
		}
	}

	/**
	 * Synchronizes the sort indexes in the background after an entity type was created or its
	 * definition was updated, once the change is committed.
	 *
	 * @param event the event naming the changed entity type
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onEntityTypeDefinitionChanged(EntityTypeDefinitionChangedEvent event) {
		executor.submit(() -> syncSortIndexesOfTypeAndDerivedTypes(event.entityTypeId()));
	}

	/**
	 * Waits until the synchronizations that were started in the background so far are finished.
	 *
	 * @param timeout the maximum time to wait
	 * @return {@code true} when they finished in time, {@code false} otherwise
	 */
	public boolean awaitBackgroundSyncs(Duration timeout) {
		try {
			// The executor runs the tasks one after another, so this one runs after the earlier ones
			executor.submit(() -> {}).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		} catch (ExecutionException | TimeoutException e) {
			return false;
		}
	}

	/**
	 * Synchronizes the sort indexes of an entity type and of all types derived from it (when it is a base type).
	 *
	 * @param entityTypeId the ID of the entity type
	 */
	public void syncSortIndexesOfTypeAndDerivedTypes(int entityTypeId) {
		try {
			syncSortIndexes(entityTypeId);

			List<Integer> derivedTypeIds = dsl.select(ENTITY_TYPE.ID)
					.from(ENTITY_TYPE)
					.where(ENTITY_TYPE.BASE_TYPE_ID.eq(entityTypeId))
					.fetch(ENTITY_TYPE.ID);
			for (int derivedTypeId : derivedTypeIds) {
				syncSortIndexes(derivedTypeId);
			}
		} catch (RuntimeException e) {
			log.error("Synchronizing the sort indexes of the entity type " + entityTypeId + " and its derived types failed.", e);
		}
	}

	/**
	 * Synchronizes the sort indexes on the partition of an entity type with its type definition:
	 * creates the missing indexes of the attributes that set {@code sortIndex}, rebuilds invalid
	 * ones (left over by a failed concurrent build), and drops the indexes of attributes that no
	 * longer set it. Base types and deprecated types do not store entities and get no indexes.
	 *
	 * Synchronizations run one at a time, so that two of them never build the same index.
	 *
	 * @param entityTypeId the ID of the entity type
	 * @return {@code true} when the indexes are in line with the type definition, {@code false}
	 *         when something failed (see the log)
	 */
	public synchronized boolean syncSortIndexes(int entityTypeId) {
		String partition = "entity_t" + entityTypeId;
		log.info("Synchronizing the sort indexes of the entity type " + entityTypeId + ".");

		// Determine the indexes the partition should have
		Map<String, String> wanted = new LinkedHashMap<>();
		Record type;
		try {
			type = dsl.select(ENTITY_TYPE.IS_BASE_TYPE, ENTITY_TYPE.IS_DEPRECATED)
					.from(ENTITY_TYPE)
					.where(ENTITY_TYPE.ID.eq(entityTypeId))
					.fetchOne();
		} catch (DataAccessException e) {
			log.error("Reading the entity type " + entityTypeId + " for synchronizing its sort indexes failed.", e);
			return false;
		}

		if (type != null && !type.get(ENTITY_TYPE.IS_BASE_TYPE) && !type.get(ENTITY_TYPE.IS_DEPRECATED)) {
			Map<String, SortableAttribute> attributes = getSortableAttributes(entityTypeId);
			if (attributes == null) {
				log.error("Resolving the sortable attributes of the entity type " + entityTypeId + " failed.");
				return false;
			}

			for (SortableAttribute attribute : attributes.values()) {
				if (attribute.sortIndex()) {
					String expression = dsl.renderInlined(sortExpression(attribute));
					String name = indexName(entityTypeId, expression);
					wanted.put(name, indexDefinition(name, partition, expression + ", " + quote("id")));
					wanted.put(name + DESCENDING_SUFFIX, indexDefinition(name + DESCENDING_SUFFIX, partition,
							expression + " DESC NULLS LAST, " + quote("id")));
				}
			}
		}

		// Read the sort indexes the partition has
		Map<String, Boolean> existing = readExistingSortIndexes(partition);
		if (existing == null) {
			return false;
		}

		boolean success = true;

		// Drop the indexes that are no longer wanted, and the invalid ones, which are rebuilt below
		for (Map.Entry<String, Boolean> index : existing.entrySet()) {
			if (!wanted.containsKey(index.getKey()) || !index.getValue()) {
				success &= execute(dropIndexStatement(index.getKey()), "Dropping the sort index " + index.getKey());
			}
		}

		// Create the missing indexes
		for (Map.Entry<String, String> index : wanted.entrySet()) {
			if (!Boolean.TRUE.equals(existing.get(index.getKey()))) {
				success &= execute(index.getValue(), "Creating the sort index " + index.getKey());
			}
		}

		log.info("Synchronizing the sort indexes of the entity type " + entityTypeId + (success ? " finished." : " failed."));
		return success;
	}

	/**
	 * Stops the background synchronizations when the application shuts down.
	 */
	@PreDestroy
	public void shutdown() {
		executor.shutdownNow();
	}

	/**
	 * Builds the name of a sort index from the partition and a hash of the indexed expression, so that
	 * a changed expression (e.g. a changed attribute type) gets a new index and the old one is dropped.
	 *
	 * @param entityTypeId the ID of the entity type
	 * @param expression the indexed expression
	 * @return the index name
	 */
	private static String indexName(int entityTypeId, String expression) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-1").digest(expression.getBytes(StandardCharsets.UTF_8));
			return "entity_t" + entityTypeId + INDEX_NAME_INFIX + HexFormat.of().formatHex(hash).substring(0, 12);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 is not available.", e);
		}
	}

	/**
	 * Reads the sort indexes a partition has, together with whether each one is valid.
	 *
	 * The query is PostgreSQL-specific: it reads the system catalogs {@code pg_index} and
	 * {@code pg_class} and uses {@code to_regclass} and {@code starts_with}.
	 *
	 * @param partition the name of the partition
	 * @return whether each sort index is valid, keyed by the index name, or {@code null} when reading failed
	 */
	private Map<String, Boolean> readExistingSortIndexes(String partition) {
		try {
			return dsl.select(DSL.field("c.relname", String.class), DSL.field("i.indisvalid", Boolean.class))
					.from(DSL.table("pg_index i"))
					.join(DSL.table("pg_class c")).on(DSL.condition("c.oid = i.indexrelid"))
					.where(DSL.condition("i.indrelid = to_regclass({0})", DSL.val("public." + partition)))
					.and(DSL.condition("starts_with(c.relname, {0})", DSL.val(partition + INDEX_NAME_INFIX)))
					.fetchMap(DSL.field("c.relname", String.class), DSL.field("i.indisvalid", Boolean.class));
		} catch (DataAccessException e) {
			log.error("Reading the sort indexes of the partition " + partition + " failed.", e);
			return null;
		}
	}

	/**
	 * Builds the statement that drops a sort index without blocking reads and writes on the partition.
	 *
	 * The statement is PostgreSQL-specific ({@code DROP INDEX CONCURRENTLY}).
	 *
	 * @param name the index name
	 * @return the statement
	 */
	private static String dropIndexStatement(String name) {
		return "DROP INDEX CONCURRENTLY IF EXISTS " + quote("public") + "." + quote(name);
	}

	/**
	 * Builds the statement that creates a sort index.
	 *
	 * The statement is PostgreSQL-specific: {@code CREATE INDEX CONCURRENTLY} builds the index without
	 * blocking writes, and the {@code WHERE} clause makes it a partial index of the entities that are not deleted.
	 *
	 * @param name the index name
	 * @param partition the name of the partition
	 * @param columns the indexed expressions
	 * @return the statement
	 */
	private static String indexDefinition(String name, String partition, String columns) {
		return "CREATE INDEX CONCURRENTLY IF NOT EXISTS " + quote(name) + " ON " + quote("public") + "." + quote(partition)
				+ " (" + columns + ") WHERE " + quote("is_deleted") + " = false";
	}

	/**
	 * Executes a statement outside of a transaction and logs its outcome.
	 * The statement is executed by JDBC directly, since jOOQ would interpret the braces in its literals.
	 *
	 * @param sql the statement
	 * @param description the description of the statement for the log
	 * @return {@code true} when the statement succeeded, {@code false} otherwise
	 */
	private boolean execute(String sql, String description) {
		long start = System.nanoTime();
		try {
			dsl.connection(connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.execute(sql);
				}
			});
			log.info(description + " took " + Duration.ofNanos(System.nanoTime() - start).toMillis() + " ms.");
			return true;
		} catch (DataAccessException e) {
			log.error(description + " failed: " + sql, e);
			return false;
		}
	}

	/**
	 * Quotes an identifier for PostgreSQL.
	 *
	 * @param identifier the identifier (only names generated by this class)
	 * @return the quoted identifier
	 */
	private static String quote(String identifier) {
		return "\"" + identifier.replace("\"", "\"\"") + "\"";
	}

	/**
	 * Transforms JSONB into a JsonNode.
	 *
	 * @param jsonb the JSONB value, or {@code null}
	 * @return the JsonNode, or {@code null} for {@code null}
	 */
	private JsonNode toJsonNode(JSONB jsonb) {
		return jsonb == null ? null : objectMapper.readTree(jsonb.data());
	}
}
