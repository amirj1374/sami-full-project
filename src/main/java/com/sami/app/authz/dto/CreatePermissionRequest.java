package com.sami.app.authz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload to create a custom permission. The permission code is always derived
 * server-side as {@code <module.code>:<action>} — never accepted from the client.
 */
public record CreatePermissionRequest(

        @NotNull(message = "Module id is required")
        Long moduleId,

        @NotBlank(message = "Action is required")
        @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$",
                message = "Action must be a lowercase slug (letters, digits, hyphens), 2-64 characters")
        String action,

        @NotBlank(message = "Name is required")
        @Size(max = 150)
        String name,

        @Size(max = 255)
        String description
) {
}
