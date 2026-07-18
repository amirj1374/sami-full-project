package com.sami.app.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload to fully update a user (PUT semantics): name, role assignment, status
 * and profile.
 *
 * <p>Email is intentionally omitted: it is the login identifier set at creation
 * and stays immutable. Passwords are never changed through this endpoint — the
 * owner uses the change-password flow instead.
 *
 * <p>{@code expectedVersion} is optional optimistic concurrency: when present it
 * must match the user's current version or the update fails with a conflict,
 * protecting concurrent editors from overwriting each other.
 */
public record UpdateUserRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must be at most 120 characters")
        String fullName,

        @NotNull(message = "Role is required")
        Long roleId,

        @NotNull(message = "Status is required")
        Long statusId,

        @Valid
        UserProfileRequest profile,

        Long expectedVersion
) {
}
