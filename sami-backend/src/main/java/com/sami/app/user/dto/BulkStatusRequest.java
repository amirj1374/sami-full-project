package com.sami.app.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Bulk status transition: move every listed user to the given status. */
public record BulkStatusRequest(

        @NotEmpty(message = "At least one user id is required")
        @Size(max = 500, message = "At most 500 users per bulk operation")
        List<Long> ids,

        @NotNull(message = "Status is required")
        Long statusId
) {
}
