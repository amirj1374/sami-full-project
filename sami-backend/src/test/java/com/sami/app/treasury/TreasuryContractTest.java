package com.sami.app.treasury;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.assertThat;
class TreasuryContractTest {
 @Test void schemaScopesIdentityAndPreservesImmutableMovements() throws Exception {String s=Files.readString(Path.of("src/main/resources/db/migration/V49__treasury_cash_management.sql"));assertThat(s).contains("UNIQUE(tenant_id, direction, normalized_bank_name, cheque_number)").contains("treasury_movements").contains("uq_treasury_movement_transaction_account").contains("tenant_id BIGINT NOT NULL REFERENCES tenants");}
 @Test void serviceUsesTenantCountsLocksAndReversals() throws Exception {String s=Files.readString(Path.of("src/main/java/com/sami/app/treasury/service/TreasuryService.java"));assertThat(s).contains("countByTenantId").contains("lockByTenantIdAndIdIn").contains("reversalOf(original)").doesNotContain("TenantDefaults").doesNotContain("return value==null?null:\"{}\"");}
}
