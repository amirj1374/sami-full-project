package com.sami.app.dashboard.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.dashboard.dto.DashboardDtos.RefreshContext;
import com.sami.app.dashboard.dto.KpiDtos.KpiCalculationResult;
import com.sami.app.dashboard.dto.KpiDtos.KpiRequest;
import com.sami.app.dashboard.dto.KpiDtos.KpiResponse;
import com.sami.app.dashboard.dto.KpiDtos.KpiValueResponse;
import com.sami.app.dashboard.dto.KpiDtos.ValidationResult;
import com.sami.app.dashboard.service.KpiService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** KPI definitions: CRUD, formula validation, calculation and history. */
@RestController
@RequestMapping("/api/v1/kpis")
@RequiredArgsConstructor
@Tag(name = "KPIs", description = "KPI definitions, calculation and thresholds")
public class KpiController {

    private final KpiService kpiService;

    @GetMapping
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "List KPI definitions")
    public ApiResponse<PageResponse<KpiResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long statusId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(kpiService.list(search, statusId, pageable)));
    }

    @GetMapping("/export")
    @PreAuthorize("@authz.has('dashboards:export')")
    @Operation(summary = "Export the executive KPI summary as CSV")
    public org.springframework.http.ResponseEntity<String> export() {
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=\"kpi-report.csv\"")
                .body(kpiService.exportCsv());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "Get a KPI with its thresholds and latest value")
    public ApiResponse<KpiResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(kpiService.getDetail(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "Historical values of a KPI (trend analysis)")
    public ApiResponse<List<KpiValueResponse>> history(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "30") int limit) {
        return ApiResponse.ok(kpiService.history(id, limit));
    }

    @GetMapping("/{id}/validate")
    @PreAuthorize("@authz.has('dashboards:manage-kpis')")
    @Operation(summary = "Validate a KPI's formula")
    public ApiResponse<ValidationResult> validate(@PathVariable Long id) {
        return ApiResponse.ok(kpiService.validate(id));
    }

    @PostMapping("/{id}/calculate")
    @PreAuthorize("@authz.has('dashboards:view')")
    @Operation(summary = "Calculate and store a KPI's current value (optionally a manual value)")
    public ApiResponse<KpiCalculationResult> calculate(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal value,
            @RequestBody(required = false) RefreshContext context) {
        return ApiResponse.ok(kpiService.calculate(id, context, value));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('dashboards:manage-kpis')")
    @Operation(summary = "Create a KPI definition")
    public ApiResponse<KpiResponse> create(@Valid @RequestBody KpiRequest request) {
        return ApiResponse.ok(kpiService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('dashboards:manage-kpis')")
    @Operation(summary = "Update a KPI definition")
    public ApiResponse<KpiResponse> update(@PathVariable Long id,
                                           @Valid @RequestBody KpiRequest request) {
        return ApiResponse.ok(kpiService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('dashboards:manage-kpis')")
    @Operation(summary = "Delete a KPI definition")
    public void delete(@PathVariable Long id) {
        kpiService.delete(id);
    }
}
