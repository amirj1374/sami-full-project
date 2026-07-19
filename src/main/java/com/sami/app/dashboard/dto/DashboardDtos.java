package com.sami.app.dashboard.dto;

import com.sami.app.dashboard.domain.Dashboard;
import com.sami.app.dashboard.domain.DashboardShare;
import com.sami.app.dashboard.domain.DashboardWidget;
import com.sami.app.dashboard.spi.WidgetData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request/response payloads for dashboards, widgets and sharing. */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    // ------------------------------------------------------------- responses

    public record DashboardRowResponse(
            Long id, String code, String name, String description,
            Long ownerId, Long roleId, String roleName,
            Long statusId, String statusCode, String statusName,
            Long visibilityId, String visibilityCode, String visibilityName,
            boolean isDefault, boolean favorite, boolean canEdit,
            Instant createdAt, Instant updatedAt, Long version) {

        public static DashboardRowResponse from(Dashboard d, boolean favorite, boolean canEdit) {
            return new DashboardRowResponse(
                    d.getId(), d.getCode(), d.getName(), d.getDescription(),
                    d.getOwner() != null ? d.getOwner().getId() : null,
                    d.getRole() != null ? d.getRole().getId() : null,
                    d.getRole() != null ? d.getRole().getName() : null,
                    d.getStatus().getId(), d.getStatus().getCode(), d.getStatus().getName(),
                    d.getVisibility().getId(), d.getVisibility().getCode(), d.getVisibility().getName(),
                    d.isDefault(), favorite, canEdit,
                    d.getCreatedAt(), d.getUpdatedAt(), d.getVersion());
        }
    }

    public record WidgetResponse(
            Long id, String code, Long widgetTypeId, String widgetTypeCode,
            Long chartTypeId, String chartTypeCode, Long kpiId,
            String title, String description,
            Long dataSourceId, String dataSourceCode,
            Long refreshPolicyId, String refreshPolicyCode, int refreshIntervalSeconds,
            int positionX, int positionY, int width, int height,
            String requiredPermission, Map<String, Object> config, Long version) {

        public static WidgetResponse from(DashboardWidget w) {
            return new WidgetResponse(
                    w.getId(), w.getCode(),
                    w.getWidgetType().getId(), w.getWidgetType().getCode(),
                    w.getChartType() != null ? w.getChartType().getId() : null,
                    w.getChartType() != null ? w.getChartType().getCode() : null,
                    w.getKpi() != null ? w.getKpi().getId() : null,
                    w.getTitle(), w.getDescription(),
                    w.getDataSource() != null ? w.getDataSource().getId() : null,
                    w.getDataSource() != null ? w.getDataSource().getCode() : null,
                    w.getRefreshPolicy() != null ? w.getRefreshPolicy().getId() : null,
                    w.getRefreshPolicy() != null ? w.getRefreshPolicy().getCode() : null,
                    w.getRefreshPolicy() != null ? w.getRefreshPolicy().getIntervalSeconds() : 0,
                    w.getPositionX(), w.getPositionY(), w.getWidth(), w.getHeight(),
                    w.getRequiredPermission(), w.getConfig(), w.getVersion());
        }
    }

    public record DashboardDetailResponse(
            DashboardRowResponse dashboard, List<WidgetResponse> widgets) {
        public static DashboardDetailResponse from(Dashboard d, List<WidgetResponse> widgets,
                                                   boolean favorite, boolean canEdit) {
            return new DashboardDetailResponse(DashboardRowResponse.from(d, favorite, canEdit), widgets);
        }
    }

    public record ShareResponse(Long id, String targetType, Long targetUserId,
                                String targetUserEmail, Long targetRoleId, String targetRoleName,
                                boolean canEdit, Instant createdAt) {
        public static ShareResponse from(DashboardShare s) {
            return new ShareResponse(s.getId(), s.getTargetType().name(),
                    s.getTargetUser() != null ? s.getTargetUser().getId() : null,
                    s.getTargetUser() != null ? s.getTargetUser().getEmail() : null,
                    s.getTargetRole() != null ? s.getTargetRole().getId() : null,
                    s.getTargetRole() != null ? s.getTargetRole().getName() : null,
                    s.isCanEdit(), s.getCreatedAt());
        }
    }

    /** A widget together with its resolved data — the runtime "refresh" payload. */
    public record WidgetDataResponse(
            Long widgetId, String code, String widgetTypeCode, String chartTypeCode,
            String title, int refreshIntervalSeconds, Map<String, Object> config,
            WidgetData data, String error) {
    }

    // -------------------------------------------------------------- requests

    public record DashboardRequest(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$",
                    message = "Code must be a lowercase slug") String code,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 500) String description,
            Long ownerId,
            Long roleId,
            Long statusId,
            Long visibilityId,
            Long companyId,
            Long branchId,
            Long expectedVersion) {
    }

    public record WidgetRequest(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$",
                    message = "Code must be a lowercase slug") String code,
            @NotNull Long widgetTypeId,
            Long chartTypeId,
            Long kpiId,
            @NotBlank @Size(max = 150) String title,
            @Size(max = 500) String description,
            Long dataSourceId,
            Long refreshPolicyId,
            @PositiveOrZero int positionX,
            @PositiveOrZero int positionY,
            @Positive int width,
            @Positive int height,
            @Size(max = 100) String requiredPermission,
            Map<String, Object> config,
            Long expectedVersion) {
    }

    public record ShareRequest(
            @NotNull DashboardShare.TargetType targetType,
            Long targetUserId,
            Long targetRoleId,
            boolean canEdit) {
    }

    /** Dashboard listing filters. */
    public record DashboardFilter(String search, Long statusId, Long visibilityId,
                                  Boolean favoritesOnly) {
    }

    /**
     * The runtime filter context applied to widget data (the filter engine).
     * All optional; providers apply what they understand.
     */
    public record RefreshContext(Long companyId, Long branchId, Long salespersonId,
                                 LocalDate from, LocalDate to, Map<String, Object> filters) {
        public static RefreshContext empty() {
            return new RefreshContext(null, null, null, null, null, Map.of());
        }
    }

    /** One widget's grid placement, used by the builder's batch layout save. */
    public record LayoutItem(
            @NotNull Long widgetId,
            @PositiveOrZero int positionX,
            @PositiveOrZero int positionY,
            @Positive int width,
            @Positive int height) {
    }

    public record LayoutRequest(@Valid List<LayoutItem> items) {
    }

    public record SavedFilterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull Map<String, Object> filter) {
    }

    public record SavedFilterResponse(Long id, String name, Map<String, Object> filter) {
    }
}
