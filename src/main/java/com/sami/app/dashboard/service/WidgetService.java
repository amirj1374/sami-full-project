package com.sami.app.dashboard.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.dashboard.domain.Dashboard;
import com.sami.app.dashboard.domain.DashboardWidget;
import com.sami.app.dashboard.dto.DashboardDtos.WidgetRequest;
import com.sami.app.dashboard.dto.DashboardDtos.WidgetResponse;
import com.sami.app.dashboard.event.DashboardDomainEvent;
import com.sami.app.dashboard.repository.DashChartTypeRepository;
import com.sami.app.dashboard.repository.DashDataSourceRepository;
import com.sami.app.dashboard.repository.DashRefreshPolicyRepository;
import com.sami.app.dashboard.repository.DashWidgetTypeRepository;
import com.sami.app.dashboard.repository.DashboardRepository;
import com.sami.app.dashboard.repository.DashboardWidgetRepository;
import com.sami.app.dashboard.repository.KpiDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Widget CRUD on a dashboard. Editing requires edit access to the parent
 * dashboard (via {@link DashboardAccessGuard}); every change is audited and
 * published as a {@code WidgetAdded/Updated/Removed} event.
 */
@Service
@RequiredArgsConstructor
public class WidgetService {

    private final DashboardRepository dashboardRepository;
    private final DashboardWidgetRepository widgetRepository;
    private final DashWidgetTypeRepository widgetTypeRepository;
    private final DashChartTypeRepository chartTypeRepository;
    private final DashDataSourceRepository dataSourceRepository;
    private final DashRefreshPolicyRepository refreshPolicyRepository;
    private final KpiDefinitionRepository kpiRepository;
    private final DashboardAuditService audit;
    private final DashboardAccessGuard guard;

    @Transactional
    public WidgetResponse add(Long dashboardId, WidgetRequest request) {
        Dashboard dashboard = loadDashboard(dashboardId);
        guard.requireEdit(dashboard, DashboardAccess.current());
        if (widgetRepository.existsByDashboardIdAndCodeIgnoreCase(dashboardId, request.code())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A widget with code '%s' already exists on this dashboard".formatted(request.code()));
        }

        DashboardWidget widget = new DashboardWidget();
        widget.setDashboard(dashboard);
        widget.setCode(request.code());
        apply(widget, request);
        widgetRepository.save(widget);

        audit.record(DashboardAuditService.WIDGET, widget.getId(), "ADDED",
                DashboardDomainEvent.WIDGET_ADDED, null,
                Map.of("dashboardId", dashboardId, "code", widget.getCode(),
                        "type", widget.getWidgetType().getCode()));
        return WidgetResponse.from(widget);
    }

    @Transactional
    public WidgetResponse update(Long widgetId, WidgetRequest request) {
        DashboardWidget widget = widgetRepository.findWithDetailsById(widgetId)
                .orElseThrow(() -> ResourceNotFoundException.of("Widget", widgetId));
        guard.requireEdit(widget.getDashboard(), DashboardAccess.current());
        if (request.expectedVersion() != null && !request.expectedVersion().equals(widget.getVersion())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "The widget was modified by someone else; reload and retry");
        }
        Long dashboardId = widget.getDashboard().getId();
        if (widgetRepository.existsByDashboardIdAndCodeIgnoreCaseAndIdNot(dashboardId, request.code(), widgetId)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A widget with code '%s' already exists on this dashboard".formatted(request.code()));
        }
        widget.setCode(request.code());
        apply(widget, request);

        audit.record(DashboardAuditService.WIDGET, widgetId, "UPDATED",
                DashboardDomainEvent.WIDGET_UPDATED, null, Map.of("code", widget.getCode()));
        return WidgetResponse.from(widget);
    }

    @Transactional
    public void remove(Long widgetId) {
        DashboardWidget widget = widgetRepository.findWithDetailsById(widgetId)
                .orElseThrow(() -> ResourceNotFoundException.of("Widget", widgetId));
        guard.requireEdit(widget.getDashboard(), DashboardAccess.current());
        Long dashboardId = widget.getDashboard().getId();
        widgetRepository.delete(widget);
        audit.record(DashboardAuditService.WIDGET, widgetId, "REMOVED",
                DashboardDomainEvent.WIDGET_REMOVED, Map.of("code", widget.getCode()),
                Map.of("dashboardId", dashboardId));
    }

    /**
     * Batch layout save from the dashboard builder: positions and sizes of many
     * widgets in one transaction. Widgets not belonging to the dashboard are
     * rejected; a single audit entry records the change.
     */
    @Transactional
    public void saveLayout(Long dashboardId, com.sami.app.dashboard.dto.DashboardDtos.LayoutRequest request) {
        Dashboard dashboard = loadDashboard(dashboardId);
        guard.requireEdit(dashboard, DashboardAccess.current());
        if (request.items() == null || request.items().isEmpty()) {
            return;
        }
        for (var item : request.items()) {
            DashboardWidget widget = widgetRepository.findById(item.widgetId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Widget", item.widgetId()));
            if (!widget.getDashboard().getId().equals(dashboardId)) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Widget %d does not belong to this dashboard".formatted(item.widgetId()));
            }
            widget.setPositionX(item.positionX());
            widget.setPositionY(item.positionY());
            widget.setWidth(item.width());
            widget.setHeight(item.height());
        }
        audit.record(DashboardAuditService.DASHBOARD, dashboardId, "LAYOUT_UPDATED",
                DashboardDomainEvent.DASHBOARD_UPDATED, null,
                Map.of("widgets", request.items().size()));
    }

    /** Creates a widget from an exported snapshot (codes resolved to entities). */
    @Transactional
    public void addFromSnapshot(Long dashboardId, Map<String, Object> snapshot) {
        Dashboard dashboard = loadDashboard(dashboardId);
        String code = String.valueOf(snapshot.getOrDefault("code", ""));
        if (code.isBlank() || widgetRepository.existsByDashboardIdAndCodeIgnoreCase(dashboardId, code)) {
            return; // skip invalid / duplicate widgets rather than aborting the import
        }
        DashboardWidget widget = new DashboardWidget();
        widget.setDashboard(dashboard);
        widget.setCode(code);
        widget.setTitle(String.valueOf(snapshot.getOrDefault("title", code)));
        widget.setDescription(str(snapshot.get("description")));
        widget.setWidgetType(widgetTypeRepository.findByCodeIgnoreCase(str(snapshot.get("widgetType")))
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Unknown widget type '%s'".formatted(snapshot.get("widgetType")))));
        if (snapshot.get("chartType") != null) {
            chartTypeRepository.findByCodeIgnoreCase(str(snapshot.get("chartType")))
                    .ifPresent(widget::setChartType);
        }
        if (snapshot.get("dataSource") != null) {
            dataSourceRepository.findByCodeIgnoreCase(str(snapshot.get("dataSource")))
                    .ifPresent(widget::setDataSource);
        }
        if (snapshot.get("refreshPolicy") != null) {
            refreshPolicyRepository.findByCodeIgnoreCase(str(snapshot.get("refreshPolicy")))
                    .ifPresent(widget::setRefreshPolicy);
        }
        widget.setPositionX(intOf(snapshot.get("positionX"), 0));
        widget.setPositionY(intOf(snapshot.get("positionY"), 0));
        widget.setWidth(intOf(snapshot.get("width"), 3));
        widget.setHeight(intOf(snapshot.get("height"), 2));
        widget.setRequiredPermission(str(snapshot.get("requiredPermission")));
        widget.setConfig(asMap(snapshot.get("config")));
        widgetRepository.save(widget);
    }

    // -------------------------------------------------------------- internals

    private void apply(DashboardWidget widget, WidgetRequest r) {
        widget.setWidgetType(widgetTypeRepository.findById(r.widgetTypeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Widget type", r.widgetTypeId())));
        widget.setChartType(r.chartTypeId() == null ? null : chartTypeRepository.findById(r.chartTypeId())
                .orElseThrow(() -> ResourceNotFoundException.of("Chart type", r.chartTypeId())));
        widget.setKpi(r.kpiId() == null ? null : kpiRepository.findById(r.kpiId())
                .orElseThrow(() -> ResourceNotFoundException.of("KPI", r.kpiId())));
        widget.setTitle(r.title());
        widget.setDescription(r.description());
        widget.setDataSource(r.dataSourceId() == null ? null : dataSourceRepository.findById(r.dataSourceId())
                .orElseThrow(() -> ResourceNotFoundException.of("Data source", r.dataSourceId())));
        widget.setRefreshPolicy(r.refreshPolicyId() == null ? null
                : refreshPolicyRepository.findById(r.refreshPolicyId())
                        .orElseThrow(() -> ResourceNotFoundException.of("Refresh policy", r.refreshPolicyId())));
        widget.setPositionX(r.positionX());
        widget.setPositionY(r.positionY());
        widget.setWidth(r.width());
        widget.setHeight(r.height());
        widget.setRequiredPermission(r.requiredPermission());
        widget.setConfig(r.config() == null ? new HashMap<>() : r.config());
    }

    private Dashboard loadDashboard(Long id) {
        return dashboardRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Dashboard", id));
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private int intOf(Object o, int fallback) {
        return o instanceof Number n ? n.intValue() : fallback;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : new HashMap<>();
    }
}
