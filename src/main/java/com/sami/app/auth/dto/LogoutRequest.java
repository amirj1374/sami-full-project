package com.sami.app.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Payload to revoke a refresh token on logout. */
public record LogoutRequest(

        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
