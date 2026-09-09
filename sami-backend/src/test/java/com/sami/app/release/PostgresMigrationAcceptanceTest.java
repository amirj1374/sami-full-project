package com.sami.app.release;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL migration harness. Enable with SAMI_PG_TEST_URL/USER/PASSWORD. */
@TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
class PostgresMigrationAcceptanceTest {
    private static String url, user, password;

    @BeforeAll static void configured() {
        url = System.getenv("SAMI_PG_TEST_URL");
        user = System.getenv().getOrDefault("SAMI_PG_TEST_USER", "postgres");
        password = System.getenv().getOrDefault("SAMI_PG_TEST_PASSWORD", "test");
        Assumptions.assumeTrue(url != null && !url.isBlank(), "SAMI_PG_TEST_URL is not configured");
    }

    @Test @Order(1) void freshV1ToLatestMigration() throws Exception {
        migrateClean();
        assertLatestVersion();
    }

    @Test @Order(2) void upgradePreservesRepresentativeLegacyData() throws Exception {
        Flyway.configure().dataSource(url, user, password).cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(url, user, password).locations("classpath:db/migration").target("50").load().migrate();
        try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement()) {
            s.execute("create table if not exists migration_harness_legacy_marker(id bigint primary key, value varchar(80) not null)");
            s.execute("insert into migration_harness_legacy_marker values (1,'preserved')");
        }
        migrateLatest();
        try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select value from migration_harness_legacy_marker where id=1")) {
            assertTrue(rs.next());
            assertEquals("preserved", rs.getString(1));
        }
        assertLatestVersion();
    }

    private static void migrateClean() {
        Flyway.configure().dataSource(url, user, password).cleanDisabled(false).load().clean();
        migrateLatest();
    }

    private static void migrateLatest() {
        Flyway.configure().dataSource(url, user, password).locations("classpath:db/migration").load().migrate();
    }

    private static void assertLatestVersion() throws Exception {
        try (Connection c = DriverManager.getConnection(url, user, password); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select version from flyway_schema_history where success=true order by installed_rank desc limit 1")) {
            assertTrue(rs.next());
            assertEquals("63", rs.getString(1));
        }
    }
}
