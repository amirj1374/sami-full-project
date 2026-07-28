package com.sami.app.dashboard.service;

import com.sami.app.security.CurrentActor;
import com.sami.app.security.SecurityUser;

/**
 * The current caller's identity as the dashboard module needs it: user id, role
 * id and super-admin flag. Resolved from the security context; used for
 * visibility and edit-permission decisions.
 */
public record DashboardAccess(Long userId, Long roleId, boolean superAdmin) {

    public static DashboardAccess current() {
        SecurityUser principal = CurrentActor.get().orElse(null);
        if (principal == null) {
            return new DashboardAccess(null, null, false);
        }
        Long roleId = principal.user().getRole() != null ? principal.user().getRole().getId() : null;
        return new DashboardAccess(principal.getId(), roleId, principal.isSuperAdmin());
    }
}
