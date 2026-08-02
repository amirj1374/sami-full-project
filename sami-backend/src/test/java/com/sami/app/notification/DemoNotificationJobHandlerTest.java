package com.sami.app.notification;

import com.sami.app.common.scheduler.spi.JobContext;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.notification.provider.DemoNotificationJobHandler;
import com.sami.app.notification.service.StaffNotificationService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DemoNotificationJobHandlerTest {

    private final StaffNotificationService service = mock(StaffNotificationService.class);
    private final TenantContext tenants = mock(TenantContext.class);
    private final DemoNotificationJobHandler handler = new DemoNotificationJobHandler(service, tenants);

    @Test
    void failsClosedWithoutTenant() {
        assertFalse(handler.execute(context(null)).success());
        verifyNoInteractions(service);
    }

    @Test
    @SuppressWarnings("unchecked")
    void executesInsidePersistedTenantScope() {
        when(tenants.callAsTenant(eq(9L), any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<Integer> operation = invocation.getArgument(1);
            return operation.get();
        });
        when(service.generateHourlyDemo(eq(9L), any())).thenReturn(2);
        assertTrue(handler.execute(context(9L)).success());
    }

    private JobContext context(Long tenantId) {
        return new JobContext("demo-hourly-notification", Map.of(), tenantId,
                Instant.parse("2026-08-02T10:00:00Z"), false, null);
    }
}
