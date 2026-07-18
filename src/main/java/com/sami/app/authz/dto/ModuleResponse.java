package com.sami.app.authz.dto;

import com.sami.app.authz.domain.AppModule;

/** Administrative representation of an {@link AppModule}. */
public record ModuleResponse(
        Long id,
        String code,
        String name,
        String description,
        String icon,
        String path,
        int displayOrder,
        boolean enabled,
        boolean isSystem,
        long permissionCount
) {

    public static ModuleResponse from(AppModule module, long permissionCount) {
        return new ModuleResponse(
                module.getId(),
                module.getCode(),
                module.getName(),
                module.getDescription(),
                module.getIcon(),
                module.getPath(),
                module.getDisplayOrder(),
                module.isEnabled(),
                module.isSystem(),
                permissionCount
        );
    }
}
