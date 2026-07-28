package com.sami.app;

import com.sami.app.authz.domain.Permission;
import com.sami.app.authz.domain.Role;
import com.sami.app.security.Authz;
import com.sami.app.security.SecurityUser;
import com.sami.app.user.domain.User;
import com.sami.app.user.domain.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Authz}. Pure (no Spring context, no database): the bean
 * only reads the {@link SecurityContextHolder}, so we populate it directly.
 */
class AuthzTest {

    private final Authz authz = new Authz();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void superAdminBypassesEveryCheck() {
        authenticate(role(true));

        assertThat(authz.has("users:view")).isTrue();
        assertThat(authz.has("anything:whatsoever")).isTrue();
        assertThat(authz.hasAny("roles:edit", "modules:delete")).isTrue();
    }

    @Test
    void grantedPermissionPasses() {
        authenticate(role(false, "users:view", "products:view"));

        assertThat(authz.has("users:view")).isTrue();
        assertThat(authz.hasAny("users:delete", "products:view")).isTrue();
    }

    @Test
    void missingPermissionFails() {
        authenticate(role(false, "users:view"));

        assertThat(authz.has("users:delete")).isFalse();
        assertThat(authz.hasAny("users:delete", "roles:view")).isFalse();
    }

    @Test
    void anonymousRequestFails() {
        // No authentication in the context at all.
        assertThat(authz.has("users:view")).isFalse();
        assertThat(authz.hasAny("users:view", "products:view")).isFalse();
    }

    private Role role(boolean superAdmin, String... codes) {
        Set<Permission> permissions = Arrays.stream(codes)
                .map(code -> Permission.builder().code(code).build())
                .collect(Collectors.toSet());
        return Role.builder()
                .name("Test Role")
                .isSuperAdmin(superAdmin)
                .permissions(permissions)
                .build();
    }

    private void authenticate(Role role) {
        User user = User.builder()
                .email("user@example.com")
                .passwordHash("hash")
                .fullName("Test User")
                .role(role)
                .status(UserStatus.builder().code("active").name("Active").allowsLogin(true).build())
                .build();
        SecurityUser principal = new SecurityUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
