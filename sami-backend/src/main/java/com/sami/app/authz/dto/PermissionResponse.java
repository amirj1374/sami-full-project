package com.sami.app.authz.dto;

import com.sami.app.authz.domain.Permission;

/** Administrative representation of a {@link Permission} with its owning module. */
public record PermissionResponse(
        Long id,
        Long moduleId,
        String moduleCode,
        String moduleName,
        String action,
        String code,
        String name,
        String description,
        boolean isSystem
) {

    /** Maps a permission whose owning module is already initialized. */
    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getModule().getId(),
                permission.getModule().getCode(),
                permission.getModule().getName(),
                permission.getAction(),
                permission.getCode(),
                permission.getName(),
                permission.getDescription(),
                permission.isSystem()
        );
    }
}
