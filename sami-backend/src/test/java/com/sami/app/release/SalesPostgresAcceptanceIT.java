package com.sami.app.release;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Bootstrap contract proving the real application context and schema run on PostgreSQL. */
class SalesPostgresAcceptanceIT extends PostgresApplicationFixture {
    @Autowired JdbcTemplate jdbc;

    @Test void applicationStartsOnFreshMigratedPostgres() {
        assertEquals("63", jdbc.queryForObject("select version from flyway_schema_history where success=true order by installed_rank desc limit 1", String.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from sales_receipts", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from sales_receipt_allocations", Integer.class));
    }
}
