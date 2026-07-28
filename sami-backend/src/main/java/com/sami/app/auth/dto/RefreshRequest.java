package com.sami.app.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Payload to exchange a refresh token for a new access token. */
public record RefreshRequest(

        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
