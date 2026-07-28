package com.sami.app.authz.dto;

import com.sami.app.authz.domain.AppModule;
import com.sami.app.authz.service.ModuleLifecycle;

/**
 * Administrative representation of an {@link AppModule}.
 *
 * <p>The original fields keep their names, types and order; the lifecycle block
 * is additive, so existing admin clients continue to deserialise this response.
 */
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
        long permissionCount,

        // --- lifecycle (V25, additive) ---
        ModuleStatusResponse backendStatus,
        ModuleStatusResponse frontendStatus,
        ModuleStatusResponse overallStatus,
        boolean overallStatusDerived,
        short progressPercentage,
        String releaseVersion,
        String developmentNotes,
        boolean available,
        boolean productionReady,
        boolean showPlaceholder
) {

    public static ModuleResponse from(AppModule module, long permissionCount,
                                      ModuleLifecycle lifecycle) {
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
                permissionCount,
                ModuleStatusResponse.from(module.getBackendStatus()),
                ModuleStatusResponse.from(module.getFrontendStatus()),
                ModuleStatusResponse.from(lifecycle.overallStatus(module)),
                lifecycle.isDerived(module),
                module.getProgressPercentage(),
                module.getReleaseVersion(),
                module.getDevelopmentNotes(),
                module.isAvailable(),
                module.isProductionReady(),
                lifecycle.showsPlaceholder(module));
    }
}
