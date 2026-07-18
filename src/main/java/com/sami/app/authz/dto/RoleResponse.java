package com.sami.app.authz.dto;

import com.sami.app.authz.domain.Permission;
import com.sami.app.authz.domain.Role;

import java.util.List;

/**
 * Full administrative representation of a {@link Role}.
 *
 * <p>{@code userCount} tells the admin UI whether the role is deletable;
 * {@code permissionIds} (sorted for stable payloads) feeds the permission matrix.
 */
public record RoleResponse(
        Long id,
        String name,
        String description,
        boolean isSystem,
        boolean isSuperAdmin,
        boolean isDefault,
        long userCount,
        List<Long> permissionIds
) {

    /** Maps a role whose permission collection is already initialized. */
    public static RoleResponse from(Role role, long userCount) {
        List<Long> permissionIds = role.getPermissions().stream()
                .map(Permission::getId)
                .sorted()
                .toList();
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isSystem(),
                role.isSuperAdmin(),
                role.isDefault(),
                userCount,
                permissionIds
        );
    }
}
