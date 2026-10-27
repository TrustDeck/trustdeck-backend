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

import java.time.Duration;
import java.util.UUID;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Provides the PostgreSQL and Keycloak containers for the integration tests.
 * The containers are started once per test run and are initialized with the
 * example databases from {@code src/main/resources/db}.
 *
 * Set the environment variable {@code TRUSTDECK_TESTS_USE_LOCAL_CONTAINERS=true}
 * to run the tests against the locally running development containers instead
 * (configured through the environment variables from {@code trustdeck.env}).
 *
 * @author Armin Müller
 */
public final class TrustDeckTestContainers {

    /** The name of the environment variable that switches to the locally running containers. */
    private static final String USE_LOCAL_CONTAINERS = "TRUSTDECK_TESTS_USE_LOCAL_CONTAINERS";

    /** The PostgreSQL image; matches the version the example databases were dumped from. */
    private static final String POSTGRES_IMAGE = "postgres:17.4";

    /** The Keycloak image; matches the version of the example Keycloak database. */
    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.4.7";

    /** The database user; the example databases are owned by this user. */
    private static final String DATABASE_USER = "trustdeck-manager";

    /** The database password used inside the test containers. */
    private static final String DATABASE_PASSWORD = "trustdeck-test";

    /** The Keycloak realm from the example Keycloak database. */
    private static final String REALM = "development";

    /** The Keycloak client from the example Keycloak database. */
    private static final String CLIENT_ID = "trustdeck";

    /** 
     * The secret of the Keycloak client. It is generated for every test run and replaces the
     * public secret of the example Keycloak database, just like during a regular installation.
     */
    private static final String CLIENT_SECRET = UUID.randomUUID().toString();

    /** The network alias under which Keycloak reaches the database. */
    private static final String DATABASE_ALIAS = "trustdeck-postgresql";

    /** The network shared by the containers. */
    private static final Network NETWORK = Network.newNetwork();

    /** The database container containing the TrustDeck and Keycloak databases. */
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE)
            .withNetwork(NETWORK)
            .withNetworkAliases(DATABASE_ALIAS)
            .withDatabaseName("trustdeck")
            .withUsername(DATABASE_USER)
            .withPassword(DATABASE_PASSWORD)
            .withCopyFileToContainer(MountableFile.forClasspathResource("db/trustdeck-example.sql"), "/docker-entrypoint-initdb.d/1-trustdeck-example.sql")
            .withCopyFileToContainer(MountableFile.forClasspathResource("db/keycloak-example.sql"), "/docker-entrypoint-initdb.d/2-keycloak-example.sql")
            .withCopyFileToContainer(MountableFile.forClasspathResource("db/keycloak-rotate-client-secret.sh", 0755), "/docker-entrypoint-initdb.d/3-keycloak-rotate-client-secret.sh")
            .withEnv("KEYCLOAK_CLIENT_ID", CLIENT_ID)
            .withEnv("KEYCLOAK_CLIENT_SECRET", CLIENT_SECRET)
            .withEnv("KEYCLOAK_REALM_NAME", REALM);

    /** The Keycloak container using the Keycloak database from the database container. */
    private static final GenericContainer<?> KEYCLOAK = new GenericContainer<>(KEYCLOAK_IMAGE)
            .withNetwork(NETWORK)
            .dependsOn(POSTGRES)
            .withEnv("KC_DB", "postgres")
            .withEnv("KC_DB_URL", "jdbc:postgresql://" + DATABASE_ALIAS + ":5432/keycloak")
            .withEnv("KC_DB_USERNAME", DATABASE_USER)
            .withEnv("KC_DB_PASSWORD", DATABASE_PASSWORD)
            .withEnv("KC_DB_SCHEMA", "public")
            .withEnv("KC_HOSTNAME_STRICT", "false")
            .withCommand("start-dev")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/realms/" + REALM).forPort(8080).withStartupTimeout(Duration.ofMinutes(3)));

    /**
     * Utility class; not meant to be instantiated.
     */
    private TrustDeckTestContainers() {
    }

    /**
     * Checks whether the tests should use the locally running development containers.
     *
     * @return {@code true} if the locally running containers should be used, {@code false} otherwise
     */
    public static boolean useLocalContainers() {
        return Boolean.parseBoolean(System.getenv(USE_LOCAL_CONTAINERS));
    }

    /**
     * Starts the containers (if not already running) and registers their connection
     * details as the properties that are otherwise read from the environment.
     *
     * @param registry the registry for the dynamic properties of the test context
     */
    public static void registerProperties(DynamicPropertyRegistry registry) {
        if (useLocalContainers()) {
            return;
        }

        // Start both containers once; already running containers are not started again
        Startables.deepStart(POSTGRES, KEYCLOAK).join();

        String keycloakUri = "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);
        registry.add("DATABASE_TRUSTDECK_HOST", POSTGRES::getHost);
        registry.add("DATABASE_TRUSTDECK_PORT", () -> POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
        registry.add("DATABASE_TRUSTDECK_USER", () -> DATABASE_USER);
        registry.add("DATABASE_TRUSTDECK_PASSWORD", () -> DATABASE_PASSWORD);
        registry.add("KEYCLOAK_SERVER_URI", () -> keycloakUri);
        registry.add("KEYCLOAK_HOSTNAME", () -> keycloakUri);
        registry.add("KEYCLOAK_REALM_NAME", () -> REALM);
        registry.add("KEYCLOAK_CLIENT_ID", () -> CLIENT_ID);
        registry.add("KEYCLOAK_CLIENT_SECRET", () -> CLIENT_SECRET);
        registry.add("TRUSTDECK_PPRL_SECRET", () -> "trustdeck-test-pprl-secret");
    }
}
