package com.sami.app.authz.dto;

import com.sami.app.authz.domain.ModuleStatus;

/**
 * A lifecycle stage as the API exposes it.
 *
 * <p>The behavioural flags travel with the status so the frontend renders from
 * data rather than matching on {@code code} — the mistake this whole change
 * exists to remove.
 */
public record ModuleStatusResponse(
        String code,
        String name,
        String description,
        String color,
        String icon,
        int lifecycleRank,
        /**
         * Whether this stage is a legal value for the backend / frontend axis.
         * Exposed so the admin UI can filter each dropdown from data instead of
         * excluding stage codes by name.
         */
        boolean appliesToBackend,
        boolean appliesToFrontend,
        boolean navigable,
        boolean showsPlaceholder,
        boolean productionReady,
        boolean terminal
) {

    public static ModuleStatusResponse from(ModuleStatus status) {
        if (status == null) {
            return null;
        }
        return new ModuleStatusResponse(
                status.getCode(),
                status.getName(),
                status.getDescription(),
                status.getColor(),
                status.getIcon(),
                status.getLifecycleRank(),
                status.isAppliesToBackend(),
                status.isAppliesToFrontend(),
                status.isNavigable(),
                status.isShowsPlaceholder(),
                status.isProductionReady(),
                status.isTerminal());
    }
}
