package com.sami.app.dashboard.dto;

import com.sami.app.dashboard.domain.DashChartType;
import com.sami.app.dashboard.domain.DashDataSource;
import com.sami.app.dashboard.domain.DashKpiStatus;
import com.sami.app.dashboard.domain.DashRefreshPolicy;
import com.sami.app.dashboard.domain.DashStatus;
import com.sami.app.dashboard.domain.DashVisibility;
import com.sami.app.dashboard.domain.DashWidgetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Responses and requests for the dashboard module's configurable lookups. */
public final class DashLookupDtos {

    private static final String SLUG = "^[a-z][a-z0-9-]{1,63}$";
    private static final String SLUG_MSG = "Code must be a lowercase slug (letters, digits, dashes)";

    private DashLookupDtos() {
    }

    public record StatusResponse(Long id, String code, String name, boolean isDefault,
                                 boolean isActiveState, boolean isArchivedState,
                                 boolean isSystem, int displayOrder) {
        public static StatusResponse from(DashStatus s) {
            return new StatusResponse(s.getId(), s.getCode(), s.getName(), s.isDefault(),
                    s.isActiveState(), s.isArchivedState(), s.isSystem(), s.getDisplayOrder());
        }
    }

    public record VisibilityResponse(Long id, String code, String name, boolean isDefault,
                                     boolean isSystem, int displayOrder) {
        public static VisibilityResponse from(DashVisibility v) {
            return new VisibilityResponse(v.getId(), v.getCode(), v.getName(), v.isDefault(),
                    v.isSystem(), v.getDisplayOrder());
        }
    }

    public record KpiStatusResponse(Long id, String code, String name, boolean isDefault,
                                    boolean isActiveState, boolean isSystem, int displayOrder) {
        public static KpiStatusResponse from(DashKpiStatus s) {
            return new KpiStatusResponse(s.getId(), s.getCode(), s.getName(), s.isDefault(),
                    s.isActiveState(), s.isSystem(), s.getDisplayOrder());
        }
    }

    public record WidgetTypeResponse(Long id, String code, String name, String icon,
                                     boolean chartCapable, boolean active, boolean isSystem,
                                     int displayOrder) {
        public static WidgetTypeResponse from(DashWidgetType t) {
            return new WidgetTypeResponse(t.getId(), t.getCode(), t.getName(), t.getIcon(),
                    t.isChartCapable(), t.isActive(), t.isSystem(), t.getDisplayOrder());
        }
    }

    public record ChartTypeResponse(Long id, String code, String name, boolean active,
                                    boolean isSystem, int displayOrder) {
        public static ChartTypeResponse from(DashChartType t) {
            return new ChartTypeResponse(t.getId(), t.getCode(), t.getName(), t.isActive(),
                    t.isSystem(), t.getDisplayOrder());
        }
    }

    public record DataSourceResponse(Long id, String code, String name, String providerKey,
                                     boolean available, boolean active, boolean isSystem,
                                     int displayOrder) {
        public static DataSourceResponse from(DashDataSource d, boolean available) {
            return new DataSourceResponse(d.getId(), d.getCode(), d.getName(), d.getProviderKey(),
                    available, d.isActive(), d.isSystem(), d.getDisplayOrder());
        }
    }

    public record RefreshPolicyResponse(Long id, String code, String name, int intervalSeconds,
                                        boolean isDefault, boolean isSystem, int displayOrder) {
        public static RefreshPolicyResponse from(DashRefreshPolicy p) {
            return new RefreshPolicyResponse(p.getId(), p.getCode(), p.getName(),
                    p.getIntervalSeconds(), p.isDefault(), p.isSystem(), p.getDisplayOrder());
        }
    }

    // --- Requests for admin-creatable lookups --------------------------------

    public record WidgetTypeRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MSG) String code,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 64) String icon,
            boolean chartCapable,
            boolean active,
            int displayOrder) {
    }

    public record ChartTypeRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MSG) String code,
            @NotBlank @Size(max = 100) String name,
            boolean active,
            int displayOrder) {
    }

    public record DataSourceRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MSG) String code,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 64) String providerKey,
            boolean active,
            int displayOrder) {
    }

    public record RefreshPolicyRequest(
            @NotBlank @Pattern(regexp = SLUG, message = SLUG_MSG) String code,
            @NotBlank @Size(max = 100) String name,
            @PositiveOrZero int intervalSeconds,
            int displayOrder) {
    }
}
