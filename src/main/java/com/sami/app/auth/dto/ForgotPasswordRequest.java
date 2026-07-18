package com.sami.app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Payload to request a password-reset email. */
public record ForgotPasswordRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email
) {
}
