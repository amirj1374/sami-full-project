package com.sami.app.common.tenancy;

import com.sami.app.authz.domain.Role;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.security.SecurityUser;
import com.sami.app.user.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    private final TenantContext context = new TenantContext();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesTenantFromAuthenticatedServerSidePrincipal() {
        authenticate(41L, false);

        assertThat(context.requireTenantId()).isEqualTo(41L);
        assertThat(context.currentTenantId()).contains(41L);
    }

    @Test
    void missingAuthenticationFailsClosed() {
        assertApiError(ErrorCode.UNAUTHENTICATED, context::requireTenantId);
    }

    @Test
    void authenticatedPrincipalWithoutTenantFailsClosed() {
        authenticate(null, false);

        assertApiError(ErrorCode.ACCESS_DENIED, context::requireTenantId);
    }

    @Test
    void mismatchedTenantAccessIsDenied() {
        authenticate(41L, false);

        context.requireAccessTo(41L);
        assertApiError(ErrorCode.ACCESS_DENIED, () -> context.requireAccessTo(42L));
        assertApiError(ErrorCode.ACCESS_DENIED, () -> context.requireAccessTo(null));
    }

    @Test
    void platformRoleDoesNotImplicitlyBypassTenantBoundary() {
        authenticate(41L, true);

        assertThat(context.isPlatformActor()).isTrue();
        assertApiError(ErrorCode.ACCESS_DENIED, () -> context.requireAccessTo(42L));
    }

    @Test
    void trustedBackgroundScopeResolvesAndIsCleared() {
        Long tenant = context.callAsTenant(77L, context::requireTenantId);

        assertThat(tenant).isEqualTo(77L);
        assertApiError(ErrorCode.UNAUTHENTICATED, context::requireTenantId);
    }

    @Test
    void authenticatedTenantCannotBeOverridden() {
        authenticate(41L, false);

        assertApiError(ErrorCode.ACCESS_DENIED,
                () -> context.callAsTenant(42L, context::requireTenantId));
    }

    private void authenticate(Long tenantId, boolean platform) {
        Role role = Role.builder()
                .isPlatform(platform)
                .build();
        User user = User.builder()
                .tenantId(tenantId)
                .email("user@example.com")
                .passwordHash("hash")
                .fullName("User")
                .role(role)
                .build();
        SecurityUser principal = new SecurityUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
    }

    private void assertApiError(ErrorCode expected, Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
