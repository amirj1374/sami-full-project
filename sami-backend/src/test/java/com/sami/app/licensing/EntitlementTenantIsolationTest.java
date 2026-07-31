package com.sami.app.licensing;

import com.sami.app.licensing.LicensingProperties;
import com.sami.app.licensing.repository.FeatureRepository;
import com.sami.app.licensing.repository.LicenseRepository;
import com.sami.app.licensing.repository.SubscriptionPlanRepository;
import com.sami.app.licensing.repository.TenantRepository;
import com.sami.app.licensing.service.Entitlement;
import com.sami.app.licensing.service.EntitlementService;
import com.sami.app.licensing.spi.ExpiryBehaviorRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EntitlementTenantIsolationTest {

    private final LicenseRepository licenses = mock(LicenseRepository.class);
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final EntitlementService service = new EntitlementService(
            licenses, tenants, mock(FeatureRepository.class), mock(SubscriptionPlanRepository.class),
            mock(ExpiryBehaviorRegistry.class), new LicensingProperties(false, 30));

    @Test
    void missingTenantDoesNotFallBackToTheOnlyTenant() {
        Entitlement entitlement = service.resolve(null);

        assertThat(entitlement.licensed()).isFalse();
        assertThat(entitlement.tenantId()).isNull();
        verifyNoInteractions(licenses, tenants);
    }

    @Test
    void entitlementQueriesOnlyTheExplicitTrustedTenant() {
        when(licenses.findForTenant(42L)).thenReturn(List.of());

        Entitlement entitlement = service.resolve(42L);

        assertThat(entitlement.tenantId()).isEqualTo(42L);
        assertThat(entitlement.licensed()).isFalse();
        verify(licenses).findForTenant(42L);
    }
}
