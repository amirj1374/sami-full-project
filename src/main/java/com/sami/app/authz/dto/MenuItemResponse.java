package com.sami.app.authz.dto;

import com.sami.app.authz.domain.AppModule;
import com.sami.app.authz.service.ModuleLifecycle;

/**
 * A single navigation entry: an enabled module the current user may view.
 *
 * <p><b>Backward compatibility.</b> The original five fields — {@code code},
 * {@code name}, {@code icon}, {@code path}, {@code displayOrder} — keep their
 * names, types and order, so an existing client deserialising this response is
 * unaffected. Everything below them is additive.
 */
public record MenuItemResponse(
        String code,
        String name,
        String icon,
        String path,
        int displayOrder,

        // --- lifecycle (V25, additive) ---
        ModuleStatusResponse backendStatus,
        ModuleStatusResponse frontendStatus,
        ModuleStatusResponse overallStatus,
        /** True when the overall status was calculated rather than pinned. */
        boolean overallStatusDerived,
        short progressPercentage,
        String releaseVersion,
        /** Whether the UI should render the placeholder rather than real content. */
        boolean showPlaceholder,
        boolean navigable
) {

    public static MenuItemResponse from(AppModule module, ModuleLifecycle lifecycle) {
        return new MenuItemResponse(
                module.getCode(),
                module.getName(),
                module.getIcon(),
                module.getPath(),
                module.getDisplayOrder(),
                ModuleStatusResponse.from(module.getBackendStatus()),
                ModuleStatusResponse.from(module.getFrontendStatus()),
                ModuleStatusResponse.from(lifecycle.overallStatus(module)),
                lifecycle.isDerived(module),
                module.getProgressPercentage(),
                module.getReleaseVersion(),
                lifecycle.showsPlaceholder(module),
                lifecycle.isNavigable(module));
    }
}
