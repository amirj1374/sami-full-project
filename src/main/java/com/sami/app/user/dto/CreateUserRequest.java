package com.sami.app.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for an administrator to create a user.
 *
 * <p>Validation mirrors self-registration; role and status are chosen explicitly
 * instead of falling back to the database-configured defaults. The profile block
 * is optional.
 */
public record CreateUserRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must be at most 120 characters")
        String fullName,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotNull(message = "Role is required")
        Long roleId,

        @NotNull(message = "Status is required")
        Long statusId,

        @Valid
        UserProfileRequest profile
) {
}
