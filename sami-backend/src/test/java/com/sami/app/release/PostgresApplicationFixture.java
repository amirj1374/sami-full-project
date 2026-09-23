package com.sami.app.release;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.junit.jupiter.api.AfterAll;

/** Shared real PostgreSQL/Spring fixture for persisted acceptance tests. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class PostgresApplicationFixture {
    private static final String EXTERNAL_URL = System.getProperty("sami.acceptance.jdbc-url",
            System.getenv("SAMI_ACCEPTANCE_JDBC_URL"));
    private static final String EXTERNAL_USER = System.getProperty("sami.acceptance.jdbc-user",
            System.getenv("SAMI_ACCEPTANCE_JDBC_USER"));
    private static final String EXTERNAL_PASSWORD = System.getProperty("sami.acceptance.jdbc-password",
            System.getenv("SAMI_ACCEPTANCE_JDBC_PASSWORD"));
    protected static final PostgreSQLContainer<?> POSTGRES = EXTERNAL_URL == null ?
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sami_test")
                    .withUsername("sami_test")
                    .withPassword("sami_test") : null;

    static {
        if (POSTGRES != null) {
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EXTERNAL_URL != null ? EXTERNAL_URL : POSTGRES.getJdbcUrl());
        registry.add("spring.datasource.username", () -> EXTERNAL_URL != null ? EXTERNAL_USER : POSTGRES.getUsername());
        registry.add("spring.datasource.password", () -> EXTERNAL_URL != null ? EXTERNAL_PASSWORD : POSTGRES.getPassword());
        registry.add("spring.flyway.url", () -> EXTERNAL_URL != null ? EXTERNAL_URL : POSTGRES.getJdbcUrl());
        registry.add("spring.flyway.user", () -> EXTERNAL_URL != null ? EXTERNAL_USER : POSTGRES.getUsername());
        registry.add("spring.flyway.password", () -> EXTERNAL_URL != null ? EXTERNAL_PASSWORD : POSTGRES.getPassword());
        registry.add("app.demo.enabled", () -> false);
    }

    @AfterAll
    static void stopAcceptanceDatabase() {
        if (POSTGRES != null) {
            POSTGRES.stop();
        }
    }
}
