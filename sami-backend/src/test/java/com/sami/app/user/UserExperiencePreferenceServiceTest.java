package com.sami.app.user;

import com.sami.app.authz.service.MenuService;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.user.domain.UserExperiencePreference;
import com.sami.app.user.dto.UserExperiencePreferenceRequest;
import com.sami.app.user.repository.UserExperiencePreferenceRepository;
import com.sami.app.user.service.UserExperiencePreferenceService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserExperiencePreferenceServiceTest {

    @Test
    void readsOnlyAuthenticatedTenantAndUserPreference() {
        UserExperiencePreferenceRepository repository = mock(UserExperiencePreferenceRepository.class);
        TenantContext tenants = mock(TenantContext.class);
        when(tenants.requireTenantId()).thenReturn(42L);
        UserExperiencePreferenceService service = new UserExperiencePreferenceService(
                repository, mock(MenuService.class), tenants);

        var result = service.get(17L);

        verify(repository).findByTenantIdAndUserId(42L, 17L);
        assertFalse(result.demoNotificationsEnabled());
        assertTrue(result.keyboardShortcutsEnabled());
        assertFalse(result.mobileNavigationConfigured());
    }

    @Test
    void preservesShortcutPreferenceForOlderReplacementPayloads() {
        UserExperiencePreferenceRepository repository = mock(UserExperiencePreferenceRepository.class);
        TenantContext tenants = mock(TenantContext.class);
        MenuService menus = mock(MenuService.class);
        UserExperiencePreference preference = UserExperiencePreference.builder()
                .tenantId(42L)
                .userId(17L)
                .keyboardShortcutsEnabled(true)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(preference, "version", 0L);
        when(tenants.requireTenantId()).thenReturn(42L);
        when(menus.menuForCurrentUser()).thenReturn(List.of());
        when(repository.findByTenantIdAndUserId(42L, 17L)).thenReturn(Optional.of(preference));
        when(repository.save(any(UserExperiencePreference.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserExperiencePreferenceService service = new UserExperiencePreferenceService(repository, menus, tenants);

        var result = service.update(17L, new UserExperiencePreferenceRequest(List.of(), false, null));

        assertTrue(result.keyboardShortcutsEnabled());
    }
}
