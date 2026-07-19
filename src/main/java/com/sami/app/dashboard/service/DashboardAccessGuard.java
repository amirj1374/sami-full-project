package com.sami.app.dashboard.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.dashboard.domain.Dashboard;
import com.sami.app.dashboard.domain.DashboardWidget;
import com.sami.app.dashboard.repository.DashboardShareRepository;
import com.sami.app.security.Authz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Central dashboard access decisions, shared by the dashboard and widget
 * services (keeps the rule in one place and avoids a service cycle).
 *
 * <p>View: owner, public/executive visibility, matching role, or an active
 * share. Edit: owner or an editable share. Super admins bypass both. A widget is
 * visible only if the caller holds its {@code requiredPermission}.
 */
@Component
@RequiredArgsConstructor
public class DashboardAccessGuard {

    private static final Set<String> OPEN_VISIBILITIES = Set.of("public", "executive");

    private final DashboardShareRepository shareRepository;
    private final Authz authz;

    public List<Long> sharedIds(DashboardAccess access) {
        if (access.superAdmin() || access.userId() == null) {
            return List.of();
        }
        return shareRepository.findDashboardIdsSharedWith(access.userId(), access.roleId());
    }

    public List<Long> editableShared(DashboardAccess access) {
        if (access.superAdmin() || access.userId() == null) {
            return List.of();
        }
        return shareRepository.findEditableDashboardIdsSharedWith(access.userId(), access.roleId());
    }

    public boolean canView(Dashboard d, DashboardAccess access, List<Long> sharedIds) {
        if (access.superAdmin()) {
            return true;
        }
        if (d.getOwner() != null && d.getOwner().getId().equals(access.userId())) {
            return true;
        }
        if (OPEN_VISIBILITIES.contains(d.getVisibility().getCode())) {
            return true;
        }
        if (d.getRole() != null && d.getRole().getId().equals(access.roleId())) {
            return true;
        }
        return sharedIds.contains(d.getId());
    }

    public boolean canEdit(Dashboard d, DashboardAccess access, List<Long> editableShared) {
        if (access.superAdmin()) {
            return true;
        }
        if (d.getOwner() != null && d.getOwner().getId().equals(access.userId())) {
            return true;
        }
        return editableShared.contains(d.getId());
    }

    public boolean canSeeWidget(DashboardWidget widget) {
        String permission = widget.getRequiredPermission();
        return permission == null || permission.isBlank() || authz.has(permission);
    }

    public void requireView(Dashboard d, DashboardAccess access) {
        if (!canView(d, access, sharedIds(access))) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "You cannot access this dashboard");
        }
    }

    public void requireEdit(Dashboard d, DashboardAccess access) {
        if (!canEdit(d, access, editableShared(access))) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "You cannot modify this dashboard");
        }
    }
}
