package com.sami.app.licensing;

import com.sami.app.authz.domain.Role;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.licensing.service.LicensingScope;
import com.sami.app.security.SecurityUser;
import com.sami.app.user.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LicensingScopeTest {

    private final LicensingScope scope = new LicensingScope(new TenantContext());

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantActorIsRestrictedToAuthenticatedTenant() {
        authenticate(41L, false);

        assertThat(scope.resolveTenant(null)).isEqualTo(41L);
        assertThat(scope.resolveTenant(41L)).isEqualTo(41L);
        assertThat(scope.tenantFilter()).contains(41L);
        assertApiError(ErrorCode.ACCESS_DENIED, () -> scope.resolveTenant(42L));
    }

    @Test
    void platformActorMaySelectAnExplicitTenant() {
        authenticate(41L, true);

        assertThat(scope.resolveTenant(42L)).isEqualTo(42L);
        assertThat(scope.tenantFilter()).isEmpty();
        scope.requirePlatform();
    }

    @Test
    void nonPlatformActorCannotUsePlatformAdministration() {
        authenticate(41L, false);

        assertApiError(ErrorCode.ACCESS_DENIED, scope::requirePlatform);
    }

    @Test
    void missingTrustedContextFailsClosed() {
        assertApiError(ErrorCode.UNAUTHENTICATED, scope::currentTenantId);
        assertApiError(ErrorCode.UNAUTHENTICATED, () -> scope.resolveTenant(41L));
    }

    private void authenticate(Long tenantId, boolean platform) {
        Role role = Role.builder().isPlatform(platform).build();
        User user = User.builder()
                .tenantId(tenantId)
                .email("licensing@example.com")
                .passwordHash("hash")
                .fullName("Licensing User")
                .role(role)
                .build();
        SecurityUser principal = new SecurityUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void assertApiError(ErrorCode expected, Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
