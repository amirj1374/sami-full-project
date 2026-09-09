package com.sami.app.release;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** PostgreSQL acceptance entry point; the reusable fixture is intentionally the single bootstrap owner. */
class SalesReceiptPostgresAcceptanceIT extends SalesBusinessTestFixturePostgresIT {
    @Autowired JdbcTemplate jdbc;

    @Test
    void reusableSalesFixturePersistsReceiptPrerequisites() {
        assertEquals("64", jdbc.queryForObject("select version from flyway_schema_history where success=true order by installed_rank desc limit 1", String.class));
    }
}
