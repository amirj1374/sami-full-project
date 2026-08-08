package com.sami.app.legacyimport;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyImportContractTest {
    @Test void everyBatchAndEvidenceReadIsTenantScoped() throws Exception {
        String source=Files.readString(Path.of("src/main/java/com/sami/app/legacyimport/LegacyImportService.java"));
        assertThat(source).contains("WHERE tenant_id=? AND id=?")
                .contains("WHERE tenant_id=? AND import_batch_id=?")
                .contains("c.tenant_id=r.tenant_id")
                .doesNotContain("TenantDefaults");
    }

    @Test void importNeverWritesCanonicalBusinessTables() throws Exception {
        String source=Files.readString(Path.of("src/main/java/com/sami/app/legacyimport/LegacyImportService.java"));
        assertThat(source).doesNotContain("INSERT INTO customers")
                .doesNotContain("UPDATE customers")
                .doesNotContain("INSERT INTO products")
                .doesNotContain("INSERT INTO invoices")
                .contains("INSERT INTO legacy_records");
    }
}
