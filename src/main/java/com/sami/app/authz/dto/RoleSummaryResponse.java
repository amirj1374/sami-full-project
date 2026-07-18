package com.sami.app.authz.dto;

import com.sami.app.authz.domain.Role;

/** Compact role representation embedded in user payloads. */
public record RoleSummaryResponse(
        Long id,
        String name,
        boolean isSuperAdmin
) {

    public static RoleSummaryResponse from(Role role) {
        return new RoleSummaryResponse(role.getId(), role.getName(), role.isSuperAdmin());
    }
}
