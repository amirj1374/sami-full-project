package com.sami.app.authz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload to update a permission (PUT semantics). Only name and description are
 * editable — action and code are immutable because granted authorities reference
 * the code.
 */
public record UpdatePermissionRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 150)
        String name,

        @Size(max = 255)
        String description
) {
}
