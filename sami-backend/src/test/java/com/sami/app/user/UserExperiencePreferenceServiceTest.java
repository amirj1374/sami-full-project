package com.sami.app.user;

import com.sami.app.authz.service.MenuService;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.user.repository.UserExperiencePreferenceRepository;
import com.sami.app.user.service.UserExperiencePreferenceService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
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
        assertFalse(result.mobileNavigationConfigured());
    }
}
