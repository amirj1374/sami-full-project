package com.sami.app.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payload to transition a user to another status (activate, deactivate, suspend,
 * or any custom status). Archive and soft-delete have dedicated endpoints because
 * they additionally stamp the lifecycle columns.
 */
public record UpdateUserStatusRequest(

        @NotNull(message = "Status is required")
        Long statusId
) {
}
