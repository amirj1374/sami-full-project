package com.sami.app.authz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload to update a module's lifecycle record.
 *
 * <p>Deliberately separate from {@code UpdateModuleStatusRequest}, which
 * toggles {@code enabled} and predates this. The two words "status" mean
 * different things in this domain — one is an administrator switching a module
 * off, the other is where the module sits in its development lifecycle — so
 * they get different endpoints rather than one overloaded payload.
 *
 * <p>Statuses are supplied by CODE, not id: codes are stable across
 * environments, so the same request body works against dev and production.
 */
public record UpdateModuleLifecycleRequest(

        @NotBlank(message = "Backend status is required")
        @Size(max = 64)
        String backendStatusCode,

        @NotBlank(message = "Frontend status is required")
        @Size(max = 64)
        String frontendStatusCode,

        /** Null clears the override and returns the module to derivation. */
        @Size(max = 64)
        String overallStatusCode,

        @Min(value = 0, message = "Progress cannot be negative")
        @Max(value = 100, message = "Progress cannot exceed 100")
        short progressPercentage,

        @Size(max = 32)
        String releaseVersion,

        @Size(max = 2000)
        String developmentNotes,

        boolean available,

        boolean productionReady
) {
}
