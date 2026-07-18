package com.sami.app.authz.web;

import com.sami.app.authz.dto.MenuItemResponse;
import com.sami.app.authz.service.MenuService;
import com.sami.app.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The current user's navigation menu. Requires authentication only (enforced by
 * the security filter chain) — per-module visibility is decided by the service
 * from the user's {@code <module>:view} permissions.
 */
@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
@Tag(name = "Menu", description = "Navigation menu for the current user")
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    @Operation(summary = "List the enabled modules the current user may navigate to")
    public ApiResponse<List<MenuItemResponse>> menu() {
        return ApiResponse.ok(menuService.menuForCurrentUser());
    }
}
