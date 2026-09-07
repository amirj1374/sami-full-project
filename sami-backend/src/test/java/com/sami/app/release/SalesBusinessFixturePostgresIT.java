package com.sami.app.release;

import com.sami.app.licensing.domain.Tenant;
import com.sami.app.licensing.service.TenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** First real lifecycle proof: tenant creation and activation through application services. */
class SalesBusinessFixturePostgresIT extends PostgresApplicationFixture {
    @Autowired TenantService tenants;

    @AfterEach void clearSecurity() { SalesSecurityTestSupport.clear(); }

    @Test void shouldCreateAndActivateTenant() {
        SalesSecurityTestSupport.authenticatePlatformTenantUser(9001L, 1L, "platform@sami.test");
        String code = "it-" + UUID.randomUUID().toString().replace("-", "");
        Tenant created = tenants.create(code, "SAMI Integration Tenant", "", "platform@sami.test", Map.of());
        assertNotNull(created.getId());
        Tenant activated = tenants.activate(created.getId());
        assertEquals("active", activated.getStatus().getCode());
    }
}
