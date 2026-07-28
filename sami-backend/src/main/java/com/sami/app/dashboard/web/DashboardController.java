package com.sami.app.dashboard.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.dashboard.dto.AuditEntryResponse;
import com.sami.app.dashboard.dto.DashboardDtos.DashboardDetailResponse;
import com.sami.app.dashboard.dto.DashboardDtos.DashboardFilter;
import com.sami.app.dashboard.dto.DashboardDtos.DashboardRequest;
import com.sami.app.dashboard.dto.DashboardDtos.DashboardRowResponse;
import com.sami.app.dashboard.dto.DashboardDtos.RefreshContext;
import com.sami.app.dashboard.dto.DashboardDtos.SavedFilterRequest;
import com.sami.app.dashboard.dto.DashboardDtos.SavedFilterResponse;
import com.sami.app.dashboard.dto.DashboardDtos.ShareRequest;
import com.sami.app.dashboard.dto.DashboardDtos.ShareResponse;
import com.sami.app.dashboard.dto.DashboardDtos.WidgetDataResponse;
import com.sami.app.dashboard.service.DashboardAuditService;
import com.sami.app.dashboard.service.DashboardService;
import com.sami.app.dashboard.service.SavedFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Dashboards: CRUD, visibility-aware listing, live refresh, sharing, favorites,
 * saved filters, configuration import/export and audit history. The runtime
 * data of a dashboard's widgets comes from {@code /refresh}.
 */
@RestController
@RequestMapping("/api/v1/dashboards")
@RequiredArgsConstructor
@Tag(name = "Dashboards", description = "Dashboards, executive cockpit and widget data")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SavedFilterService savedFilterService;
    private final DashboardAuditService auditService;

    // ----------------------------------------------------------------- reads

    @GetMapping
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "List dashboards visible to the current user")
    public ApiResponse<PageResponse<DashboardRowResponse>> list(
            DashboardFilter filter,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(dashboardService.list(filter, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "Get a dashboard with its (permission-filtered) widgets")
    public ApiResponse<DashboardDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(dashboardService.getDetail(id));
    }

    @PostMapping("/{id}/refresh")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "Resolve live data for every visible widget (applies the filter context)")
    public ApiResponse<List<WidgetDataResponse>> refresh(@PathVariable Long id,
                                                         @RequestBody(required = false) RefreshContext context) {
        return ApiResponse.ok(dashboardService.refresh(id, context));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "Audit history of a dashboard")
    public ApiResponse<PageResponse<AuditEntryResponse>> audit(
            @PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(
                auditService.history(DashboardAuditService.DASHBOARD, id, pageable)));
    }

    // ---------------------------------------------------------------- writes

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('dashboards:create')")
    @Operation(summary = "Create a dashboard")
    public ApiResponse<DashboardDetailResponse> create(@Valid @RequestBody DashboardRequest request) {
        return ApiResponse.ok(dashboardService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('dashboards:edit')")
    @Operation(summary = "Update a dashboard")
    public ApiResponse<DashboardDetailResponse> update(@PathVariable Long id,
                                                       @Valid @RequestBody DashboardRequest request) {
        return ApiResponse.ok(dashboardService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('dashboards:delete')")
    @Operation(summary = "Delete a dashboard")
    public void delete(@PathVariable Long id) {
        dashboardService.delete(id);
    }

    // --------------------------------------------------------------- sharing

    @GetMapping("/{id}/shares")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "List a dashboard's shares")
    public ApiResponse<List<ShareResponse>> shares(@PathVariable Long id) {
        return ApiResponse.ok(dashboardService.listShares(id));
    }

    @PostMapping("/{id}/shares")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('dashboards:share')")
    @Operation(summary = "Share a dashboard with a user or role")
    public ApiResponse<ShareResponse> share(@PathVariable Long id,
                                            @Valid @RequestBody ShareRequest request) {
        return ApiResponse.ok(dashboardService.share(id, request));
    }

    @DeleteMapping("/{id}/shares/{shareId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('dashboards:share')")
    @Operation(summary = "Revoke a share")
    public void unshare(@PathVariable Long id, @PathVariable Long shareId) {
        dashboardService.unshare(id, shareId);
    }

    // ------------------------------------------------------------- favorites

    @PostMapping("/{id}/favorite")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "Mark a dashboard as a favorite")
    public ApiResponse<Void> addFavorite(@PathVariable Long id) {
        dashboardService.addFavorite(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "Remove a favorite")
    public void removeFavorite(@PathVariable Long id) {
        dashboardService.removeFavorite(id);
    }

    // ---------------------------------------------------------- import/export

    @GetMapping("/{id}/export")
    @PreAuthorize("@authz.has('dashboards:export')")
    @Operation(summary = "Export a dashboard configuration snapshot (JSON)")
    public ApiResponse<Map<String, Object>> export(@PathVariable Long id) {
        return ApiResponse.ok(dashboardService.export(id));
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('dashboards:create')")
    @Operation(summary = "Import a dashboard configuration snapshot")
    public ApiResponse<DashboardDetailResponse> importDashboard(@RequestBody Map<String, Object> snapshot) {
        return ApiResponse.ok(dashboardService.importDashboard(snapshot));
    }

    // --------------------------------------------------------- saved filters

    @GetMapping("/saved-filters")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "List the current user's saved filters")
    public ApiResponse<List<SavedFilterResponse>> savedFilters() {
        return ApiResponse.ok(savedFilterService.list());
    }

    @PostMapping("/saved-filters")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "Save a filter")
    public ApiResponse<SavedFilterResponse> createSavedFilter(@Valid @RequestBody SavedFilterRequest request) {
        return ApiResponse.ok(savedFilterService.create(request));
    }

    @DeleteMapping("/saved-filters/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "Delete a saved filter")
    public void deleteSavedFilter(@PathVariable Long id) {
        savedFilterService.delete(id);
    }
}
