package com.sami.app.siminvestment;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SimInvestmentContractTest {
    @Test void migrationProtectsTenantIdentityIdempotencyAndMarketEvidence() throws Exception {
        String sql=Files.readString(Path.of("src/main/resources/db/migration/V47__sim_0912_investment.sql"));
        assertThat(sql).contains("uq_sim_investment_import_hash UNIQUE (tenant_id, sha256)")
                .contains("uq_sim_investment_listing UNIQUE (tenant_id, source_code, normalized_phone)")
                .contains("price_toman NUMERIC(18,2)")
                .contains("m.code||':'||a.action")
                .contains("('recalculate','Recalculate investment analysis')")
                .contains("is_production_ready");
    }

    @Test void investmentImportDoesNotWriteInventoryPurchasingOrSalesTables() throws Exception {
        String source=Files.readString(Path.of("src/main/java/com/sami/app/siminvestment/SimInvestmentImportService.java"));
        assertThat(source).doesNotContain("insert into inventory_")
                .doesNotContain("update inventory_")
                .doesNotContain("insert into purchases")
                .doesNotContain("insert into sales")
                .contains("sim_investment_listing_history")
                .contains("excluded.last_seen_on>=sim_investment_listings.last_seen_on")
                .contains("first_seen_on=least")
                .contains("sha256");
    }
}
