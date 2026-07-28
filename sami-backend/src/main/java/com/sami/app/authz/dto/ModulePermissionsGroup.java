package com.sami.app.authz.dto;

import com.sami.app.authz.domain.AppModule;

import java.util.List;

/**
 * One row of the role permission matrix: a module (including disabled ones) with
 * every permission it owns.
 */
public record ModulePermissionsGroup(
        Long moduleId,
        String moduleCode,
        String moduleName,
        List<PermissionResponse> permissions
) {

    public static ModulePermissionsGroup from(AppModule module, List<PermissionResponse> permissions) {
        return new ModulePermissionsGroup(
                module.getId(),
                module.getCode(),
                module.getName(),
                permissions
        );
    }
}
