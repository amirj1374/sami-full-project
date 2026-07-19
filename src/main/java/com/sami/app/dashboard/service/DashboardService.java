package com.sami.app.dashboard.service;

import com.sami.app.authz.repository.RoleRepository;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.dashboard.domain.Dashboard;
import com.sami.app.dashboard.domain.DashboardFavorite;
import com.sami.app.dashboard.domain.DashboardShare;
import com.sami.app.dashboard.domain.DashboardWidget;
import com.sami.app.dashboard.dto.DashboardDtos.DashboardDetailResponse;
import com.sami.app.dashboard.dto.DashboardDtos.DashboardFilter;
import com.sami.app.dashboard.dto.DashboardDtos.DashboardRequest;
import com.sami.app.dashboard.dto.DashboardDtos.DashboardRowResponse;
import com.sami.app.dashboard.dto.DashboardDtos.RefreshContext;
import com.sami.app.dashboard.dto.DashboardDtos.ShareRequest;
import com.sami.app.dashboard.dto.DashboardDtos.ShareResponse;
import com.sami.app.dashboard.dto.DashboardDtos.WidgetDataResponse;
import com.sami.app.dashboard.dto.DashboardDtos.WidgetResponse;
import com.sami.app.dashboard.event.DashboardDomainEvent;
import com.sami.app.dashboard.repository.DashStatusRepository;
import com.sami.app.dashboard.repository.DashVisibilityRepository;
import com.sami.app.dashboard.repository.DashboardFavoriteRepository;
import com.sami.app.dashboard.repository.DashboardRepository;
import com.sami.app.dashboard.repository.DashboardShareRepository;
import com.sami.app.security.CurrentActor;
import com.sami.app.user.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dashboard management: CRUD, visibility-aware listing, sharing, favorites,
 * whole-dashboard refresh, and configuration import/export. Widget-level CRUD is
 * in {@link WidgetService}; widget data resolution in {@link WidgetDataService}.
 *
 * <p>Access model: a user sees a dashboard if they own it, it is public or
 * executive, it targets their role, or it is shared with them (directly or via
 * their role). Super admins see everything. Widgets whose
 * {@code requiredPermission} the user lacks are hidden.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Set<String> OPEN_VISIBILITIES = Set.of("public", "executive");

    private final DashboardRepository dashboardRepository;
    private final DashboardShareRepository shareRepository;
    private final DashboardFavoriteRepository favoriteRepository;
    private final DashStatusRepository statusRepository;
    private final DashVisibilityRepository visibilityRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final WidgetDataService widgetDataService;
    private final WidgetService widgetService;
    private final DashboardAuditService audit;
    private final DashboardAccessGuard guard;

    // ----------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public Page<DashboardRowResponse> list(DashboardFilter filter, Pageable pageable) {
        DashboardAccess access = DashboardAccess.current();
        List<Long> sharedIds = sharedIds(access);
        Set<Long> favorites = favoriteIds(access);
        List<Long> editableShared = access.superAdmin() ? List.of()
                : shareRepository.findEditableDashboardIdsSharedWith(access.userId(), access.roleId());

        Specification<Dashboard> spec = Specification.allOf(
                accessibleSpec(access, sharedIds),
                searchSpec(filter.search()),
                idFieldSpec("status", filter.statusId()),
                idFieldSpec("visibility", filter.visibilityId()),
                Boolean.TRUE.equals(filter.favoritesOnly()) ? idInSpec(favorites) : null);

        return dashboardRepository.findAll(spec, pageable)
                .map(d -> DashboardRowResponse.from(d, favorites.contains(d.getId()),
                        canEdit(d, access, editableShared)));
    }

    @Transactional(readOnly = true)
    public DashboardDetailResponse getDetail(Long id) {
        DashboardAccess access = DashboardAccess.current();
        Dashboard dashboard = dashboardRepository.findWithWidgetsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Dashboard", id));
        requireView(dashboard, access);

        List<WidgetResponse> widgets = dashboard.getWidgets().stream()
                .filter(this::canSeeWidget)
                .map(WidgetResponse::from)
                .toList();
        boolean favorite = favoriteRepository.existsByUserIdAndDashboardId(access.userId(), id);
        return DashboardDetailResponse.from(dashboard, widgets, favorite, canEdit(dashboard, access,
                editableShared(access)));
    }

    /** Resolves every visible widget's live data (the "Refresh Dashboard" service). */
    @Transactional
    public List<WidgetDataResponse> refresh(Long id, RefreshContext context) {
        DashboardAccess access = DashboardAccess.current();
        Dashboard dashboard = dashboardRepository.findWithWidgetsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Dashboard", id));
        requireView(dashboard, access);
        return dashboard.getWidgets().stream()
                .filter(this::canSeeWidget)
                .map(w -> widgetDataService.resolve(w, context == null ? RefreshContext.empty() : context))
                .toList();
    }

    // ---------------------------------------------------------------- writes

    @Transactional
    public DashboardDetailResponse create(DashboardRequest request) {
        if (dashboardRepository.existsByCodeIgnoreCase(request.code())) {
            throw duplicateCode(request.code());
        }
        Dashboard dashboard = new Dashboard();
        dashboard.setCode(request.code());
        applyFields(dashboard, request, true);
        dashboardRepository.save(dashboard);

        audit.record(DashboardAuditService.DASHBOARD, dashboard.getId(), "CREATED",
                DashboardDomainEvent.DASHBOARD_CREATED, null,
                Map.of("code", dashboard.getCode(), "name", dashboard.getName()));
        return getDetail(dashboard.getId());
    }

    @Transactional
    public DashboardDetailResponse update(Long id, DashboardRequest request) {
        DashboardAccess access = DashboardAccess.current();
        Dashboard dashboard = dashboardRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Dashboard", id));
        requireEdit(dashboard, access);
        if (request.expectedVersion() != null && !request.expectedVersion().equals(dashboard.getVersion())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "The dashboard was modified by someone else; reload and retry");
        }
        if (dashboardRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
            throw duplicateCode(request.code());
        }
        dashboard.setCode(request.code());
        applyFields(dashboard, request, false);

        audit.record(DashboardAuditService.DASHBOARD, id, "UPDATED",
                DashboardDomainEvent.DASHBOARD_UPDATED, null, Map.of("code", dashboard.getCode()));
        return getDetail(id);
    }

    @Transactional
    public void delete(Long id) {
        DashboardAccess access = DashboardAccess.current();
        Dashboard dashboard = dashboardRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Dashboard", id));
        requireEdit(dashboard, access);
        if (dashboard.getStatus().isArchivedState()) {
            // already archived; allow hard delete
        }
        dashboardRepository.delete(dashboard);
        audit.record(DashboardAuditService.DASHBOARD, id, "DELETED",
                DashboardDomainEvent.DASHBOARD_DELETED, Map.of("code", dashboard.getCode()), null);
    }

    // --------------------------------------------------------------- sharing

    @Transactional(readOnly = true)
    public List<ShareResponse> listShares(Long id) {
        requireView(loadOrThrow(id), DashboardAccess.current());
        return shareRepository.findByDashboardId(id).stream().map(ShareResponse::from).toList();
    }

    @Transactional
    public ShareResponse share(Long id, ShareRequest request) {
        DashboardAccess access = DashboardAccess.current();
        Dashboard dashboard = loadOrThrow(id);
        requireEdit(dashboard, access);

        DashboardShare.DashboardShareBuilder builder = DashboardShare.builder()
                .dashboard(dashboard).targetType(request.targetType())
                .canEdit(request.canEdit()).createdBy(access.userId());

        if (request.targetType() == DashboardShare.TargetType.USER) {
            if (request.targetUserId() == null) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "A target user is required");
            }
            if (shareRepository.existsByDashboardIdAndTargetUserId(id, request.targetUserId())) {
                throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Already shared with this user");
            }
            builder.targetUser(userRepository.findById(request.targetUserId())
                    .orElseThrow(() -> ResourceNotFoundException.of("User", request.targetUserId())));
        } else {
            if (request.targetRoleId() == null) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "A target role is required");
            }
            if (shareRepository.existsByDashboardIdAndTargetRoleId(id, request.targetRoleId())) {
                throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Already shared with this role");
            }
            builder.targetRole(roleRepository.findById(request.targetRoleId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Role", request.targetRoleId())));
        }

        DashboardShare share = shareRepository.save(builder.build());
        audit.record(DashboardAuditService.DASHBOARD, id, "SHARED",
                DashboardDomainEvent.DASHBOARD_SHARED, null,
                Map.of("targetType", request.targetType().name(), "canEdit", request.canEdit()));
        return ShareResponse.from(share);
    }

    @Transactional
    public void unshare(Long id, Long shareId) {
        DashboardAccess access = DashboardAccess.current();
        requireEdit(loadOrThrow(id), access);
        DashboardShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> ResourceNotFoundException.of("Share", shareId));
        if (!share.getDashboard().getId().equals(id)) {
            throw ResourceNotFoundException.of("Share", shareId);
        }
        shareRepository.delete(share);
    }

    // ------------------------------------------------------------- favorites

    @Transactional
    public void addFavorite(Long id) {
        DashboardAccess access = DashboardAccess.current();
        Dashboard dashboard = loadOrThrow(id);
        requireView(dashboard, access);
        if (!favoriteRepository.existsByUserIdAndDashboardId(access.userId(), id)) {
            favoriteRepository.save(DashboardFavorite.builder()
                    .userId(access.userId()).dashboardId(id).build());
        }
    }

    @Transactional
    public void removeFavorite(Long id) {
        Long userId = CurrentActor.id();
        favoriteRepository.findById(new DashboardFavorite.Key(userId, id))
                .ifPresent(favoriteRepository::delete);
    }

    // ---------------------------------------------------------- import/export

    /** Portable dashboard snapshot (codes, not ids) for export/backup/templates. */
    @Transactional(readOnly = true)
    public Map<String, Object> export(Long id) {
        DashboardAccess access = DashboardAccess.current();
        Dashboard dashboard = dashboardRepository.findWithWidgetsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Dashboard", id));
        requireView(dashboard, access);

        List<Map<String, Object>> widgets = dashboard.getWidgets().stream()
                .map(this::exportWidget).toList();
        Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("code", dashboard.getCode());
        snapshot.put("name", dashboard.getName());
        snapshot.put("description", dashboard.getDescription());
        snapshot.put("visibility", dashboard.getVisibility().getCode());
        snapshot.put("widgets", widgets);
        return snapshot;
    }

    private Map<String, Object> exportWidget(DashboardWidget w) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("code", w.getCode());
        m.put("widgetType", w.getWidgetType().getCode());
        m.put("chartType", w.getChartType() != null ? w.getChartType().getCode() : null);
        m.put("title", w.getTitle());
        m.put("description", w.getDescription());
        m.put("dataSource", w.getDataSource() != null ? w.getDataSource().getCode() : null);
        m.put("refreshPolicy", w.getRefreshPolicy() != null ? w.getRefreshPolicy().getCode() : null);
        m.put("positionX", w.getPositionX());
        m.put("positionY", w.getPositionY());
        m.put("width", w.getWidth());
        m.put("height", w.getHeight());
        m.put("requiredPermission", w.getRequiredPermission());
        m.put("config", w.getConfig());
        return m;
    }

    /**
     * Creates a dashboard (and its widgets) from an exported snapshot. Widget
     * templates are resolved by their configuration codes, so a snapshot is
     * portable across environments. The dashboard code must be unique.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public DashboardDetailResponse importDashboard(Map<String, Object> snapshot) {
        String code = String.valueOf(snapshot.getOrDefault("code", ""));
        if (code.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Snapshot is missing a dashboard code");
        }
        if (dashboardRepository.existsByCodeIgnoreCase(code)) {
            throw duplicateCode(code);
        }
        Long visibilityId = snapshot.get("visibility") != null
                ? visibilityRepository.findByCodeIgnoreCase(String.valueOf(snapshot.get("visibility")))
                        .map(v -> v.getId()).orElse(null)
                : null;

        DashboardRequest request = new DashboardRequest(
                code,
                String.valueOf(snapshot.getOrDefault("name", code)),
                snapshot.get("description") != null ? String.valueOf(snapshot.get("description")) : null,
                null, null, null, visibilityId, null, null, null);
        DashboardDetailResponse created = create(request);

        Object widgets = snapshot.get("widgets");
        if (widgets instanceof List<?> list) {
            for (Object w : list) {
                if (w instanceof Map<?, ?> map) {
                    widgetService.addFromSnapshot(created.dashboard().id(), (Map<String, Object>) map);
                }
            }
        }
        return getDetail(created.dashboard().id());
    }

    // -------------------------------------------------------------- internals

    private void applyFields(Dashboard dashboard, DashboardRequest r, boolean creating) {
        dashboard.setName(r.name());
        dashboard.setDescription(r.description());
        dashboard.setStatus(r.statusId() != null
                ? statusRepository.findById(r.statusId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Dashboard status", r.statusId()))
                : statusRepository.findByIsDefaultTrue()
                        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                                "No default dashboard status is configured")));
        dashboard.setVisibility(r.visibilityId() != null
                ? visibilityRepository.findById(r.visibilityId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Visibility", r.visibilityId()))
                : visibilityRepository.findByIsDefaultTrue()
                        .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                                "No default visibility is configured")));
        dashboard.setRole(r.roleId() == null ? null : roleRepository.findById(r.roleId())
                .orElseThrow(() -> ResourceNotFoundException.of("Role", r.roleId())));
        dashboard.setCompanyId(r.companyId());
        dashboard.setBranchId(r.branchId());
        if (creating) {
            Long ownerId = r.ownerId() != null ? r.ownerId() : CurrentActor.id();
            dashboard.setOwner(ownerId == null ? null : userRepository.findById(ownerId)
                    .orElseThrow(() -> ResourceNotFoundException.of("User", ownerId)));
            dashboard.setCreatedBy(CurrentActor.id());
            dashboard.setCreatedByEmail(CurrentActor.email());
        } else if (r.ownerId() != null) {
            dashboard.setOwner(userRepository.findById(r.ownerId())
                    .orElseThrow(() -> ResourceNotFoundException.of("User", r.ownerId())));
        }
    }

    private List<Long> sharedIds(DashboardAccess access) {
        return guard.sharedIds(access);
    }

    private List<Long> editableShared(DashboardAccess access) {
        return guard.editableShared(access);
    }

    private Set<Long> favoriteIds(DashboardAccess access) {
        if (access.userId() == null) {
            return Set.of();
        }
        return favoriteRepository.findByUserId(access.userId()).stream()
                .map(DashboardFavorite::getDashboardId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean canEdit(Dashboard d, DashboardAccess access, List<Long> editableShared) {
        return guard.canEdit(d, access, editableShared);
    }

    private boolean canSeeWidget(DashboardWidget widget) {
        return guard.canSeeWidget(widget);
    }

    private void requireView(Dashboard d, DashboardAccess access) {
        guard.requireView(d, access);
    }

    private void requireEdit(Dashboard d, DashboardAccess access) {
        guard.requireEdit(d, access);
    }

    private Dashboard loadOrThrow(Long id) {
        return dashboardRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Dashboard", id));
    }

    private ApiException duplicateCode(String code) {
        return new ApiException(ErrorCode.RESOURCE_CONFLICT,
                "A dashboard with code '%s' already exists".formatted(code));
    }

    // --- specifications ------------------------------------------------------

    private Specification<Dashboard> accessibleSpec(DashboardAccess access, List<Long> sharedIds) {
        if (access.superAdmin()) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> ors = new ArrayList<>();
            // LEFT joins so nullable owner/role never exclude rows from the OR.
            if (access.userId() != null) {
                ors.add(cb.equal(root.join("owner", JoinType.LEFT).get("id"), access.userId()));
            }
            ors.add(root.join("visibility", JoinType.LEFT).get("code").in(OPEN_VISIBILITIES));
            if (access.roleId() != null) {
                ors.add(cb.equal(root.join("role", JoinType.LEFT).get("id"), access.roleId()));
            }
            if (!sharedIds.isEmpty()) {
                ors.add(root.get("id").in(sharedIds));
            }
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    private Specification<Dashboard> searchSpec(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern));
    }

    private Specification<Dashboard> idFieldSpec(String field, Long id) {
        return id == null ? null : (root, query, cb) -> cb.equal(root.get(field).get("id"), id);
    }

    private Specification<Dashboard> idInSpec(Set<Long> ids) {
        if (ids.isEmpty()) {
            return (root, query, cb) -> cb.disjunction();
        }
        return (root, query, cb) -> root.get("id").in(ids);
    }
}
