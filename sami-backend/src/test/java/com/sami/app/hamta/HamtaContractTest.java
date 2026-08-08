package com.sami.app.hamta;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HamtaContractTest {
    private static String read(String path) throws Exception { return Files.readString(Path.of(path)); }

    @Test void migrationOwnsOneCodePerCanonicalSerialAndConsistentDeliveryEvidence() throws Exception {
        String sql = read("src/main/resources/db/migration/V41__hamta_activation_codes.sql");
        assertThat(sql).contains("REFERENCES inventory_serial_units(id)")
                .contains("CONSTRAINT uq_hamta_serial_unit UNIQUE (serial_unit_id)")
                .contains("ck_hamta_delivery_state")
                .contains("delivery_datetime IS NOT NULL")
                .contains("delivered_sale_id IS NOT NULL");
    }

    @Test void serviceScopesEveryOperationalFlowAndNeverAuditsRawCode() throws Exception {
        String source = read("src/main/java/com/sami/app/hamta/HamtaService.java");
        assertThat(source).contains("tenantContext.requireTenantId()")
                .contains("A delivered HAMTA code is immutable")
                .contains("has already been delivered")
                .doesNotContain("detail,activation_code")
                .doesNotContain("activation_code,detail");
    }

    @Test void purchaseGateTargetsOnlyEligibleUsedPhonesAndRequiresImei() throws Exception {
        String receiving = read("src/main/java/com/sami/app/purchasing/service/PurchaseReceivingService.java");
        String service = read("src/main/java/com/sami/app/hamta/HamtaService.java");
        assertThat(service).contains("condition != PurchaseItemCondition.USED")
                .contains("p.hamta_eligible")
                .contains("s.enforcement_enabled");
        assertThat(receiving).contains("hamtaRequired")
                .contains("HAMTA-eligible used phones must require an IMEI")
                .contains("requires a HAMTA activation code");
    }
}
