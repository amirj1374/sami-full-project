package com.sami.app.dashboard.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.dashboard.dto.DashboardDtos.RefreshContext;
import com.sami.app.dashboard.dto.DashboardDtos.WidgetDataResponse;
import com.sami.app.dashboard.dto.DashboardDtos.WidgetRequest;
import com.sami.app.dashboard.dto.DashboardDtos.WidgetResponse;
import com.sami.app.dashboard.service.WidgetDataService;
import com.sami.app.dashboard.service.WidgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Widget management and single-widget data resolution (the per-widget refresh
 * path, polled at the widget's refresh interval).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Dashboard widgets", description = "Widget CRUD and live data")
public class WidgetController {

    private final WidgetService widgetService;
    private final WidgetDataService widgetDataService;

    @PostMapping("/dashboards/{dashboardId}/widgets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('dashboards:manage-widgets')")
    @Operation(summary = "Add a widget to a dashboard")
    public ApiResponse<WidgetResponse> add(@PathVariable Long dashboardId,
                                           @Valid @RequestBody WidgetRequest request) {
        return ApiResponse.ok(widgetService.add(dashboardId, request));
    }

    @PutMapping("/widgets/{id}")
    @PreAuthorize("@authz.has('dashboards:manage-widgets')")
    @Operation(summary = "Update a widget")
    public ApiResponse<WidgetResponse> update(@PathVariable Long id,
                                              @Valid @RequestBody WidgetRequest request) {
        return ApiResponse.ok(widgetService.update(id, request));
    }

    @DeleteMapping("/widgets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('dashboards:manage-widgets')")
    @Operation(summary = "Remove a widget")
    public void remove(@PathVariable Long id) {
        widgetService.remove(id);
    }

    @PutMapping("/dashboards/{dashboardId}/layout")
    @PreAuthorize("@authz.has('dashboards:manage-widgets')")
    @Operation(summary = "Save the builder layout (positions/sizes of all widgets at once)")
    public ApiResponse<Void> saveLayout(
            @PathVariable Long dashboardId,
            @Valid @RequestBody com.sami.app.dashboard.dto.DashboardDtos.LayoutRequest request) {
        widgetService.saveLayout(dashboardId, request);
        return ApiResponse.ok();
    }

    @PostMapping("/widgets/{id}/data")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "Resolve a single widget's live data (applies the filter context)")
    public ApiResponse<WidgetDataResponse> data(@PathVariable Long id,
                                                @RequestBody(required = false) RefreshContext context) {
        return ApiResponse.ok(widgetDataService.resolveById(id,
                context == null ? RefreshContext.empty() : context));
    }
}
