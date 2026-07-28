package com.sami.app.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload to create or update a configurable user status.
 *
 * <p>The structural flags (default / archived-state / deleted-state) are managed
 * exclusively through the seeded system rows — exactly one of each exists
 * (DB-enforced) — so they are not part of this payload. Admin-created statuses
 * configure name, login behavior, visibility and ordering.
 */
public record UserStatusRequest(

        @NotBlank(message = "Code is required")
        @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$",
                message = "Code must be a lowercase slug (letters, digits, dashes)")
        String code,

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description,

        boolean allowsLogin,

        boolean hiddenByDefault,

        int displayOrder
) {
}
