package com.sami.app.dashboard.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.dashboard.dto.DashLookupDtos.ChartTypeRequest;
import com.sami.app.dashboard.dto.DashLookupDtos.ChartTypeResponse;
import com.sami.app.dashboard.dto.DashLookupDtos.DataSourceRequest;
import com.sami.app.dashboard.dto.DashLookupDtos.DataSourceResponse;
import com.sami.app.dashboard.dto.DashLookupDtos.KpiStatusResponse;
import com.sami.app.dashboard.dto.DashLookupDtos.RefreshPolicyRequest;
import com.sami.app.dashboard.dto.DashLookupDtos.RefreshPolicyResponse;
import com.sami.app.dashboard.dto.DashLookupDtos.StatusResponse;
import com.sami.app.dashboard.dto.DashLookupDtos.VisibilityResponse;
import com.sami.app.dashboard.dto.DashLookupDtos.WidgetTypeRequest;
import com.sami.app.dashboard.dto.DashLookupDtos.WidgetTypeResponse;
import com.sami.app.dashboard.service.DashboardConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/**
 * The dashboard module's configuration surface: statuses, visibilities, widget
 * types, chart types, data sources and refresh policies. Reads need
 * {@code dashboards:view}; management needs {@code dashboards:manage-config}.
 */
@RestController
@RequestMapping("/api/v1/dashboard-config")
@RequiredArgsConstructor
@Tag(name = "Dashboard configuration", description = "Configurable dashboard lookups")
public class DashboardConfigController {

    private static final String MANAGE = "@authz.hasAny('dashboards:manage-config','dashboards:edit')";

    private final DashboardConfigService config;

    // reads
    @GetMapping("/statuses")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "List dashboard statuses")
    public ApiResponse<List<StatusResponse>> statuses() {
        return ApiResponse.ok(config.statuses());
    }

    @GetMapping("/visibilities")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "List visibilities")
    public ApiResponse<List<VisibilityResponse>> visibilities() {
        return ApiResponse.ok(config.visibilities());
    }

    @GetMapping("/kpi-statuses")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "List KPI statuses")
    public ApiResponse<List<KpiStatusResponse>> kpiStatuses() {
        return ApiResponse.ok(config.kpiStatuses());
    }

    @GetMapping("/widget-types")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "List widget types")
    public ApiResponse<List<WidgetTypeResponse>> widgetTypes() {
        return ApiResponse.ok(config.widgetTypes());
    }

    @GetMapping("/chart-types")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "List chart types")
    public ApiResponse<List<ChartTypeResponse>> chartTypes() {
        return ApiResponse.ok(config.chartTypes());
    }

    @GetMapping("/data-sources")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "List data sources (with provider availability)")
    public ApiResponse<List<DataSourceResponse>> dataSources() {
        return ApiResponse.ok(config.dataSources());
    }

    @GetMapping("/refresh-policies")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "List refresh policies")
    public ApiResponse<List<RefreshPolicyResponse>> refreshPolicies() {
        return ApiResponse.ok(config.refreshPolicies());
    }

    // widget types
    @PostMapping("/widget-types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a widget type")
    public ApiResponse<WidgetTypeResponse> createWidgetType(@Valid @RequestBody WidgetTypeRequest r) {
        return ApiResponse.ok(config.createWidgetType(r));
    }

    @PutMapping("/widget-types/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a widget type")
    public ApiResponse<WidgetTypeResponse> updateWidgetType(@PathVariable Long id,
                                                            @Valid @RequestBody WidgetTypeRequest r) {
        return ApiResponse.ok(config.updateWidgetType(id, r));
    }

    @DeleteMapping("/widget-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a widget type")
    public void deleteWidgetType(@PathVariable Long id) {
        config.deleteWidgetType(id);
    }

    // chart types
    @PostMapping("/chart-types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a chart type")
    public ApiResponse<ChartTypeResponse> createChartType(@Valid @RequestBody ChartTypeRequest r) {
        return ApiResponse.ok(config.createChartType(r));
    }

    @PutMapping("/chart-types/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a chart type")
    public ApiResponse<ChartTypeResponse> updateChartType(@PathVariable Long id,
                                                          @Valid @RequestBody ChartTypeRequest r) {
        return ApiResponse.ok(config.updateChartType(id, r));
    }

    @DeleteMapping("/chart-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a chart type")
    public void deleteChartType(@PathVariable Long id) {
        config.deleteChartType(id);
    }

    // data sources
    @PostMapping("/data-sources")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a data source")
    public ApiResponse<DataSourceResponse> createDataSource(@Valid @RequestBody DataSourceRequest r) {
        return ApiResponse.ok(config.createDataSource(r));
    }

    @PutMapping("/data-sources/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a data source")
    public ApiResponse<DataSourceResponse> updateDataSource(@PathVariable Long id,
                                                            @Valid @RequestBody DataSourceRequest r) {
        return ApiResponse.ok(config.updateDataSource(id, r));
    }

    @DeleteMapping("/data-sources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a data source")
    public void deleteDataSource(@PathVariable Long id) {
        config.deleteDataSource(id);
    }

    // refresh policies
    @PostMapping("/refresh-policies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a refresh policy")
    public ApiResponse<RefreshPolicyResponse> createRefreshPolicy(@Valid @RequestBody RefreshPolicyRequest r) {
        return ApiResponse.ok(config.createRefreshPolicy(r));
    }

    @PutMapping("/refresh-policies/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a refresh policy")
    public ApiResponse<RefreshPolicyResponse> updateRefreshPolicy(@PathVariable Long id,
                                                                  @Valid @RequestBody RefreshPolicyRequest r) {
        return ApiResponse.ok(config.updateRefreshPolicy(id, r));
    }

    @DeleteMapping("/refresh-policies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a refresh policy")
    public void deleteRefreshPolicy(@PathVariable Long id) {
        config.deleteRefreshPolicy(id);
    }
}
