package org.trustdeck.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.trustdeck.configuration.DefaultProperties;
import org.trustdeck.jooq.generated.tables.pojos.Algorithm;

import static org.trustdeck.jooq.generated.Tables.ALGORITHM;

class AlgorithmDBServiceTest {

    @Test
    void createsNewAlgorithm() {
        ScriptedProvider provider = new ScriptedProvider(Collections.singletonList(null), List.of(101));

        assertEquals(101, service(provider).createOrGetAlgorithm(algorithm("salt-one")));
        assertEquals(1, provider.insertCount());
    }

    @Test
    void reusesIdenticalAlgorithm() {
        ScriptedProvider provider = new ScriptedProvider(List.of(42), List.of());

        assertEquals(42, service(provider).createOrGetAlgorithm(algorithm("salt-one")));
        assertEquals(0, provider.insertCount());
    }

    @Test
    void createsSeparateAlgorithmWhenAConstraintFieldDiffers() {
        ScriptedProvider provider = new ScriptedProvider(Arrays.asList(null, null), List.of(11, 12));
        AlgorithmDBService service = service(provider);

        assertEquals(11, service.createOrGetAlgorithm(algorithm("salt-one")));
        assertEquals(12, service.createOrGetAlgorithm(algorithm("salt-two")));
        assertEquals(2, provider.insertCount());
        assertTrue(provider.selectSql().stream().allMatch(sql -> sql.contains("name")
                && sql.contains("alphabet")
                && sql.contains("random_algorithm_desired_size")
                && sql.contains("random_algorithm_desired_success_probability")
                && sql.contains("pseudonym_length")
                && sql.contains("padding_character")
                && sql.contains("add_check_digit")
                && sql.contains("length_includes_check_digit")
                && sql.contains("salt")
                && sql.contains("salt_length")));
        assertTrue(provider.selectSql().stream().allMatch(sql -> !sql.contains("consecutive_value_counter")));
    }

    @Test
    void retrievesAlgorithmAfterConcurrentConflict() {
        ScriptedProvider provider = new ScriptedProvider(Arrays.asList(null, 77), Collections.singletonList(null));

        assertEquals(77, service(provider).createOrGetAlgorithm(algorithm("salt-one")));
        assertEquals(1, provider.insertCount());
    }

    @Test
    void propagatesUnrelatedDatabaseErrors() {
        ScriptedProvider provider = new ScriptedProvider(List.of(), List.of());
        provider.failWith(new SQLException("database unavailable", "08006"));

        assertThrows(DataAccessException.class, () -> service(provider).createOrGetAlgorithm(algorithm("salt-one")));
    }

    private AlgorithmDBService service(MockDataProvider provider) {
        DSLContext dsl = DSL.using(new MockConnection(provider), SQLDialect.POSTGRES);
        AlgorithmDBService service = new AlgorithmDBService();
        ReflectionTestUtils.setField(service, "dsl", dsl);
        ReflectionTestUtils.setField(service, "defaults", new DefaultProperties());
        return service;
    }

    private Algorithm algorithm(String salt) {
        return new Algorithm(null, "SHA2", "ABCDEF", 100L, 0.9d, 1L, 8, "0", true, true, salt, salt.length());
    }

    private static final class ScriptedProvider implements MockDataProvider {
        private final Deque<Integer> selectResults;
        private final Deque<Integer> insertResults;
        private final List<String> selectSql = new ArrayList<>();
        private SQLException failure;
        private int insertCount;

        private ScriptedProvider(List<Integer> selectResults, List<Integer> insertResults) {
            this.selectResults = new LinkedList<>(selectResults);
            this.insertResults = new LinkedList<>(insertResults);
        }

        @Override
        public MockResult[] execute(MockExecuteContext context) throws SQLException {
            if (failure != null) {
                throw failure;
            }

            if (context.sql().startsWith("select")) {
                selectSql.add(context.sql());
                return results(selectResults.removeFirst());
            }

            insertCount++;
            return results(insertResults.removeFirst());
        }

        private MockResult[] results(Integer id) {
            DSLContext dsl = DSL.using(SQLDialect.POSTGRES);
            Result<Record1<Integer>> result = dsl.newResult(ALGORITHM.ID);
            if (id != null) {
                result.add(dsl.newRecord(ALGORITHM.ID).value1(id));
            }
            return new MockResult[] { new MockResult(id == null ? 0 : 1, result) };
        }

        private void failWith(SQLException failure) {
            this.failure = failure;
        }

        private int insertCount() {
            return insertCount;
        }

        private List<String> selectSql() {
            return selectSql;
        }
    }
}
