package com.sami.app.authz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Payload to create a module.
 *
 * <p>{@code code} becomes the immutable permission-code prefix. With
 * {@code createDefaultPermissions} set, the six standard actions (view, create,
 * edit, delete, export, import) are created alongside the module so a new
 * functional area is grantable immediately.
 */
public record CreateModuleRequest(

        @NotBlank(message = "Code is required")
        @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$",
                message = "Code must be a lowercase slug (letters, digits, hyphens), 2-64 characters")
        String code,

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description,

        @Size(max = 64)
        String icon,

        @Size(max = 255)
        String path,

        @PositiveOrZero(message = "Display order must be zero or positive")
        int displayOrder,

        boolean enabled,

        boolean createDefaultPermissions
) {
}
