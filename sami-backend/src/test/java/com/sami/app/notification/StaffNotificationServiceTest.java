package com.sami.app.notification;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.notification.repository.StaffNotificationRepository;
import com.sami.app.notification.service.StaffNotificationService;
import com.sami.app.user.domain.User;
import com.sami.app.user.repository.UserExperiencePreferenceRepository;
import com.sami.app.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StaffNotificationServiceTest {

    private final StaffNotificationRepository notifications = mock(StaffNotificationRepository.class);
    private final UserExperiencePreferenceRepository preferences = mock(UserExperiencePreferenceRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final TenantContext tenantContext = mock(TenantContext.class);

    @BeforeEach
    void trustedTenant() {
        when(tenantContext.requireTenantId()).thenReturn(7L);
    }

    @Test
    void disabledDeploymentCreatesNothing() {
        StaffNotificationService service = service(false);
        assertEquals(0, service.generateHourlyDemo(7L, Instant.parse("2026-08-02T10:15:00Z")));
        verify(users, never()).findActiveStaffByTenantId(7L);
    }

    @Test
    void optedOutUsersAreSkipped() {
        User user = User.builder().tenantId(7L).build();
        setId(user, 12L);
        when(users.findActiveStaffByTenantId(7L)).thenReturn(List.of(user));
        when(preferences.existsByTenantIdAndUserIdAndDemoNotificationsEnabledTrue(7L, 12L))
                .thenReturn(false);

        assertEquals(0, service(true).generateHourlyDemo(7L, Instant.parse("2026-08-02T10:15:00Z")));
        verify(notifications, never()).insertDemo(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(), anyString(), anyString());
    }

    @Test
    void hourlyWindowUsesDatabaseIdempotency() {
        User user = User.builder().tenantId(7L).build();
        setId(user, 12L);
        when(users.findActiveStaffByTenantId(7L)).thenReturn(List.of(user));
        when(preferences.existsByTenantIdAndUserIdAndDemoNotificationsEnabledTrue(7L, 12L))
                .thenReturn(true);
        when(notifications.insertDemo(7L, 12L, "notifications.demo.messages.2",
                "demo-hourly:2026-08-02T10:00Z")).thenReturn(1, 0);
        StaffNotificationService service = service(true);
        Instant sameWindow = Instant.parse("2026-08-02T10:15:00Z");

        assertEquals(1, service.generateHourlyDemo(7L, sameWindow));
        assertEquals(0, service.generateHourlyDemo(7L, sameWindow.plusSeconds(1200)));
        verify(tenantContext, org.mockito.Mockito.times(2)).requireAccessTo(7L);
    }

    private StaffNotificationService service(boolean enabled) {
        return new StaffNotificationService(notifications, preferences, users, tenantContext,
                new DemoNotificationProperties(enabled));
    }

    private static void setId(User user, Long id) {
        try {
            var field = user.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
