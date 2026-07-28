package com.sami.app.authz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Payload to update a module (PUT semantics).
 *
 * <p>{@code code} is intentionally omitted: it prefixes every permission code and
 * is immutable once created. Enablement is toggled via the dedicated status
 * endpoint.
 */
public record UpdateModuleRequest(

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
        int displayOrder
) {
}
