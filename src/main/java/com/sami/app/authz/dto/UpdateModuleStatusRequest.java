package com.sami.app.authz.dto;

import jakarta.validation.constraints.NotNull;

/** Payload to enable or disable a module. */
public record UpdateModuleStatusRequest(

        @NotNull(message = "Enabled flag is required")
        Boolean enabled
) {
}
