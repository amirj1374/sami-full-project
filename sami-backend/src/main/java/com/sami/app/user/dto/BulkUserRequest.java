package com.sami.app.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Ids targeted by a bulk lifecycle operation. */
public record BulkUserRequest(

        @NotEmpty(message = "At least one user id is required")
        @Size(max = 500, message = "At most 500 users per bulk operation")
        List<Long> ids
) {
}
