package com.sami.app.authz.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Payload to replace a role's permission set in full. An empty list revokes
 * every permission.
 */
public record AssignPermissionsRequest(

        @NotNull(message = "Permission ids are required")
        List<@NotNull Long> permissionIds
) {
}
