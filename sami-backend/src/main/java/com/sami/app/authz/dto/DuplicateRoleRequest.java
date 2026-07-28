package com.sami.app.authz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload to duplicate a role under a new name, copying its permission set. */
public record DuplicateRoleRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name
) {
}
