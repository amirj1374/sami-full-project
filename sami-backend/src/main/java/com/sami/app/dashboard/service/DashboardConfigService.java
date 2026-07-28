package com.sami.app.dashboard.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.dashboard.domain.DashChartType;
import com.sami.app.dashboard.domain.DashDataSource;
import com.sami.app.dashboard.domain.DashRefreshPolicy;
import com.sami.app.dashboard.domain.DashWidgetType;
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
import com.sami.app.dashboard.repository.DashChartTypeRepository;
import com.sami.app.dashboard.repository.DashDataSourceRepository;
import com.sami.app.dashboard.repository.DashKpiStatusRepository;
import com.sami.app.dashboard.repository.DashRefreshPolicyRepository;
import com.sami.app.dashboard.repository.DashStatusRepository;
import com.sami.app.dashboard.repository.DashVisibilityRepository;
import com.sami.app.dashboard.repository.DashWidgetTypeRepository;
import com.sami.app.dashboard.repository.DashboardWidgetRepository;
import com.sami.app.dashboard.repository.KpiDefinitionRepository;
import com.sami.app.dashboard.spi.ReportingProviderRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD and listing for the dashboard module's configurable lookups. System rows
 * keep their code and cannot be deleted; rows still referenced cannot be
 * deleted. Data-source availability reflects whether a matching
 * {@link ReportingProviderRegistry} bean is registered.
 */
@Service
@RequiredArgsConstructor
public class DashboardConfigService {

    private final DashStatusRepository statusRepository;
    private final DashVisibilityRepository visibilityRepository;
    private final DashKpiStatusRepository kpiStatusRepository;
    private final DashWidgetTypeRepository widgetTypeRepository;
    private final DashChartTypeRepository chartTypeRepository;
    private final DashDataSourceRepository dataSourceRepository;
    private final DashRefreshPolicyRepository refreshPolicyRepository;
    private final DashboardWidgetRepository widgetRepository;
    private final KpiDefinitionRepository kpiRepository;
    private final ReportingProviderRegistry providerRegistry;

    // ----------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public List<StatusResponse> statuses() {
        return statusRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(StatusResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<VisibilityResponse> visibilities() {
        return visibilityRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(VisibilityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<KpiStatusResponse> kpiStatuses() {
        return kpiStatusRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(KpiStatusResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WidgetTypeResponse> widgetTypes() {
        return widgetTypeRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(WidgetTypeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ChartTypeResponse> chartTypes() {
        return chartTypeRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(ChartTypeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DataSourceResponse> dataSources() {
        return dataSourceRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(d -> DataSourceResponse.from(d, providerRegistry.isAvailable(d.getProviderKey())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RefreshPolicyResponse> refreshPolicies() {
        return refreshPolicyRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(RefreshPolicyResponse::from).toList();
    }

    // --------------------------------------------------------- widget types

    @Transactional
    public WidgetTypeResponse createWidgetType(WidgetTypeRequest r) {
        if (widgetTypeRepository.existsByCodeIgnoreCase(r.code())) {
            throw conflict("widget type", r.code());
        }
        return WidgetTypeResponse.from(widgetTypeRepository.save(DashWidgetType.builder()
                .code(r.code()).name(r.name()).icon(r.icon()).chartCapable(r.chartCapable())
                .active(r.active()).displayOrder(r.displayOrder()).build()));
    }

    @Transactional
    public WidgetTypeResponse updateWidgetType(Long id, WidgetTypeRequest r) {
        DashWidgetType t = widgetTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Widget type", id));
        guardCode(t.isSystem(), t.getCode(), r.code());
        if (widgetTypeRepository.existsByCodeIgnoreCaseAndIdNot(r.code(), id)) {
            throw conflict("widget type", r.code());
        }
        t.setCode(r.code());
        t.setName(r.name());
        t.setIcon(r.icon());
        t.setChartCapable(r.chartCapable());
        t.setActive(r.active());
        t.setDisplayOrder(r.displayOrder());
        return WidgetTypeResponse.from(t);
    }

    @Transactional
    public void deleteWidgetType(Long id) {
        DashWidgetType t = widgetTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Widget type", id));
        guardSystem(t.isSystem());
        guardInUse(widgetRepository.countByWidgetTypeId(id));
        widgetTypeRepository.delete(t);
    }

    // ----------------------------------------------------------- chart types

    @Transactional
    public ChartTypeResponse createChartType(ChartTypeRequest r) {
        if (chartTypeRepository.existsByCodeIgnoreCase(r.code())) {
            throw conflict("chart type", r.code());
        }
        return ChartTypeResponse.from(chartTypeRepository.save(DashChartType.builder()
                .code(r.code()).name(r.name()).active(r.active())
                .displayOrder(r.displayOrder()).build()));
    }

    @Transactional
    public ChartTypeResponse updateChartType(Long id, ChartTypeRequest r) {
        DashChartType t = chartTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Chart type", id));
        guardCode(t.isSystem(), t.getCode(), r.code());
        if (chartTypeRepository.existsByCodeIgnoreCaseAndIdNot(r.code(), id)) {
            throw conflict("chart type", r.code());
        }
        t.setCode(r.code());
        t.setName(r.name());
        t.setActive(r.active());
        t.setDisplayOrder(r.displayOrder());
        return ChartTypeResponse.from(t);
    }

    @Transactional
    public void deleteChartType(Long id) {
        DashChartType t = chartTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Chart type", id));
        guardSystem(t.isSystem());
        chartTypeRepository.delete(t);
    }

    // ---------------------------------------------------------- data sources

    @Transactional
    public DataSourceResponse createDataSource(DataSourceRequest r) {
        if (dataSourceRepository.existsByCodeIgnoreCase(r.code())) {
            throw conflict("data source", r.code());
        }
        DashDataSource d = dataSourceRepository.save(DashDataSource.builder()
                .code(r.code()).name(r.name()).providerKey(r.providerKey())
                .active(r.active()).displayOrder(r.displayOrder()).build());
        return DataSourceResponse.from(d, providerRegistry.isAvailable(d.getProviderKey()));
    }

    @Transactional
    public DataSourceResponse updateDataSource(Long id, DataSourceRequest r) {
        DashDataSource d = dataSourceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Data source", id));
        guardCode(d.isSystem(), d.getCode(), r.code());
        if (dataSourceRepository.existsByCodeIgnoreCaseAndIdNot(r.code(), id)) {
            throw conflict("data source", r.code());
        }
        d.setCode(r.code());
        d.setName(r.name());
        d.setProviderKey(r.providerKey());
        d.setActive(r.active());
        d.setDisplayOrder(r.displayOrder());
        return DataSourceResponse.from(d, providerRegistry.isAvailable(d.getProviderKey()));
    }

    @Transactional
    public void deleteDataSource(Long id) {
        DashDataSource d = dataSourceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Data source", id));
        guardSystem(d.isSystem());
        guardInUse(kpiRepository.countByDataSourceId(id));
        dataSourceRepository.delete(d);
    }

    // ------------------------------------------------------- refresh policies

    @Transactional
    public RefreshPolicyResponse createRefreshPolicy(RefreshPolicyRequest r) {
        if (refreshPolicyRepository.existsByCodeIgnoreCase(r.code())) {
            throw conflict("refresh policy", r.code());
        }
        return RefreshPolicyResponse.from(refreshPolicyRepository.save(DashRefreshPolicy.builder()
                .code(r.code()).name(r.name()).intervalSeconds(r.intervalSeconds())
                .displayOrder(r.displayOrder()).build()));
    }

    @Transactional
    public RefreshPolicyResponse updateRefreshPolicy(Long id, RefreshPolicyRequest r) {
        DashRefreshPolicy p = refreshPolicyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Refresh policy", id));
        guardCode(p.isSystem(), p.getCode(), r.code());
        if (refreshPolicyRepository.existsByCodeIgnoreCaseAndIdNot(r.code(), id)) {
            throw conflict("refresh policy", r.code());
        }
        p.setCode(r.code());
        p.setName(r.name());
        p.setIntervalSeconds(r.intervalSeconds());
        p.setDisplayOrder(r.displayOrder());
        return RefreshPolicyResponse.from(p);
    }

    @Transactional
    public void deleteRefreshPolicy(Long id) {
        DashRefreshPolicy p = refreshPolicyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Refresh policy", id));
        guardSystem(p.isSystem());
        refreshPolicyRepository.delete(p);
    }

    // -------------------------------------------------------------- internals

    private ApiException conflict(String kind, String code) {
        return new ApiException(ErrorCode.RESOURCE_CONFLICT,
                "A %s with code '%s' already exists".formatted(kind, code));
    }

    private void guardCode(boolean isSystem, String current, String next) {
        if (isSystem && !current.equals(next)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System entries keep their code");
        }
    }

    private void guardSystem(boolean isSystem) {
        if (isSystem) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System entries cannot be deleted");
        }
    }

    private void guardInUse(long count) {
        if (count > 0) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Cannot delete an entry that is still in use");
        }
    }
}
