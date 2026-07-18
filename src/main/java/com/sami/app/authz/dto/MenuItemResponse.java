package com.sami.app.authz.dto;

import com.sami.app.authz.domain.AppModule;

/** A single navigation entry: an enabled module the current user may view. */
public record MenuItemResponse(
        String code,
        String name,
        String icon,
        String path,
        int displayOrder
) {

    public static MenuItemResponse from(AppModule module) {
        return new MenuItemResponse(
                module.getCode(),
                module.getName(),
                module.getIcon(),
                module.getPath(),
                module.getDisplayOrder()
        );
    }
}
