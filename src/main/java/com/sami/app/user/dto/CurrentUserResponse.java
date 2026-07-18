package com.sami.app.user.dto;

import com.sami.app.authz.domain.Permission;
import com.sami.app.authz.domain.Role;
import com.sami.app.authz.dto.RoleSummaryResponse;
import com.sami.app.user.domain.User;

import java.time.Instant;
import java.util.List;

/**
 * The authenticated user's own profile, including everything the frontend needs
 * for permission checks: {@code can(code) = isSuperAdmin || permissions.includes(code)}.
 *
 * <p>{@code permissions} is sorted for stable payloads and deliberately empty for
 * super admins — their role has no permission rows; the bypass flag covers all.
 */
public record CurrentUserResponse(
        Long id,
        String email,
        String fullName,
        Instant createdAt,
        RoleSummaryResponse role,
        List<String> permissions,
        boolean isSuperAdmin
) {

    public static CurrentUserResponse from(User user) {
        Role role = user.getRole();
        List<String> permissions = role.isSuperAdmin()
                ? List.of()
                : role.getPermissions().stream()
                        .map(Permission::getCode)
                        .sorted()
                        .toList();
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCreatedAt(),
                RoleSummaryResponse.from(role),
                permissions,
                role.isSuperAdmin()
        );
    }
}
