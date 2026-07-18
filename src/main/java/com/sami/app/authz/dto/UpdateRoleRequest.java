package com.sami.app.authz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload to update a role (PUT semantics). System roles accept description
 * changes only — a changed name is rejected by the service.
 */
public record UpdateRoleRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description
) {
}
