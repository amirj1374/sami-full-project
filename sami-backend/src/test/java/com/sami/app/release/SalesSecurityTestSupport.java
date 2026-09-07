package com.sami.app.release;

import com.sami.app.authz.domain.Permission;
import com.sami.app.authz.domain.Role;
import com.sami.app.security.SecurityUser;
import com.sami.app.user.domain.User;
import com.sami.app.user.domain.UserStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Test-only security support; uses the same principal type as production. */
public final class SalesSecurityTestSupport {
    private SalesSecurityTestSupport() { }

    public static SecurityUser authenticateTenantUser(Long userId, Long tenantId, String email, String... permissions) {
        return authenticate(userId, tenantId, email, false, permissions);
    }

    public static SecurityUser authenticatePlatformUser(Long userId, String email) {
        return authenticate(userId, null, email, true);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }

    private static SecurityUser authenticate(Long userId, Long tenantId, String email, boolean platform, String... permissions) {
        Set<Permission> grants = Arrays.stream(permissions)
                .map(code -> Permission.builder().code(code).build())
                .collect(Collectors.toSet());
        Role role = Role.builder().name(platform ? "Test Platform" : "Test Tenant")
                .isPlatform(platform).isSuperAdmin(platform).permissions(grants).build();
        User user = User.builder().tenantId(tenantId).email(email).passwordHash("test-only")
                .fullName("SAMI Integration User").role(role)
                .status(UserStatus.builder().code("active").name("Active").allowsLogin(true).build()).build();
        try { var field = user.getClass().getSuperclass().getDeclaredField("id"); field.setAccessible(true); field.set(user, userId); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Unable to prepare test principal", e); }
        SecurityUser principal = new SecurityUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        return principal;
    }
}
