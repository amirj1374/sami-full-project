package com.sami.app.licensing.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.licensing.dto.LicensingDtos.ActivateRequest;
import com.sami.app.licensing.dto.LicensingDtos.CatalogResponse;
import com.sami.app.licensing.dto.LicensingDtos.FeatureResponse;
import com.sami.app.licensing.dto.LicensingDtos.FeatureToggleRequest;
import com.sami.app.licensing.dto.LicensingDtos.LicenseRequest;
import com.sami.app.licensing.dto.LicensingDtos.LicenseResponse;
import com.sami.app.licensing.dto.LicensingDtos.LookupResponse;
import com.sami.app.licensing.dto.LicensingDtos.PlanResponse;
import com.sami.app.licensing.dto.LicensingDtos.RenewRequest;
import com.sami.app.licensing.dto.LicensingDtos.StatusChangeRequest;
import com.sami.app.licensing.dto.LicensingDtos.TenantRequest;
import com.sami.app.licensing.dto.LicensingDtos.TenantResponse;
import com.sami.app.licensing.domain.LicensingStatus;
import com.sami.app.licensing.repository.ExpiryBehaviorRepository;
import com.sami.app.licensing.repository.FeatureRepository;
import com.sami.app.licensing.repository.LicenseTypeRepository;
import com.sami.app.licensing.repository.LicensingStatusRepository;
import com.sami.app.licensing.repository.SubscriptionPlanRepository;
import com.sami.app.licensing.repository.UsageLimitTypeRepository;
import com.sami.app.licensing.service.LicenseReportService;
import com.sami.app.licensing.service.LicenseService;
import com.sami.app.licensing.service.LicenseValidation;
import com.sami.app.licensing.service.TenantService;
import com.sami.app.licensing.service.UsageCheck;
import com.sami.app.licensing.service.UsageService;
import com.sami.app.licensing.spi.ExpiryBehaviorRegistry;
import com.sami.app.licensing.spi.UsageMeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Licensing, tenant, subscription and feature-flag API. Business modules do not
 * call these endpoints to gate themselves — they use the {@code @features} bean.
 */
@RestController
@RequestMapping("/api/v1/licensing")
@RequiredArgsConstructor
@Tag(name = "Licensing", description = "Licences, tenants, subscription plans, features and usage limits")
public class LicensingController {

    private final LicenseService licenseService;
    private final LicenseReportService reportService;
    private final TenantService tenantService;
    private final UsageService usageService;
    private final LicensingStatusRepository statusRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final ExpiryBehaviorRepository expiryBehaviorRepository;
    private final UsageLimitTypeRepository limitTypeRepository;
    private final SubscriptionPlanRepository planRepository;
    private final FeatureRepository featureRepository;
    private final UsageMeterRegistry meterRegistry;
    private final ExpiryBehaviorRegistry expiryRegistry;

    // ---- Licences -----------------------------------------------------------

    @GetMapping("/licenses")
    @PreAuthorize("@authz.has('licensing:view')")
    @Operation(summary = "List licences")
    public ApiResponse<List<LicenseResponse>> licenses() {
        return ApiResponse.ok(licenseService.list().stream().map(LicenseResponse::from).toList());
    }

    @GetMapping("/licenses/{id}")
    @PreAuthorize("@authz.has('licensing:view')")
    @Operation(summary = "Get a licence")
    public ApiResponse<LicenseResponse> license(@PathVariable Long id) {
        return ApiResponse.ok(LicenseResponse.from(licenseService.get(id)));
    }

    @PostMapping("/licenses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('licensing:create')")
    @Operation(summary = "Create a licence")
    public ApiResponse<LicenseResponse> createLicense(@Valid @RequestBody LicenseRequest request) {
        return ApiResponse.ok(LicenseResponse.from(licenseService.create(
                request.code(), request.licenseKey(), request.typeCode(), request.tenantId(),
                request.companyId(), request.planCode(), request.expiryBehaviorCode(),
                request.graceDays(), request.expirationDate(), request.limitOverrides())));
    }

    @PostMapping("/licenses/{id}/activate")
    @PreAuthorize("@authz.has('licensing:activate')")
    @Operation(summary = "Activate a licence for this installation")
    public ApiResponse<LicenseResponse> activate(@PathVariable Long id,
                                                 @RequestBody(required = false) ActivateRequest request) {
        return ApiResponse.ok(LicenseResponse.from(
                licenseService.activate(id, request == null ? null : request.fingerprint())));
    }

    @PostMapping("/licenses/{id}/renew")
    @PreAuthorize("@authz.has('licensing:renew')")
    @Operation(summary = "Renew / extend a subscription")
    public ApiResponse<LicenseResponse> renew(@PathVariable Long id,
                                              @RequestBody(required = false) RenewRequest request) {
        return ApiResponse.ok(LicenseResponse.from(licenseService.renew(id,
                request == null ? null : request.days(), request == null ? null : request.planCode())));
    }

    @PatchMapping("/licenses/{id}/status")
    @PreAuthorize("@authz.has('licensing:edit')")
    @Operation(summary = "Change a licence status")
    public ApiResponse<LicenseResponse> changeStatus(@PathVariable Long id,
                                                     @Valid @RequestBody StatusChangeRequest request) {
        return ApiResponse.ok(LicenseResponse.from(licenseService.changeStatus(id, request.statusCode())));
    }

    @PostMapping("/licenses/{id}/features")
    @PreAuthorize("@authz.has('licensing:manage-features')")
    @Operation(summary = "Enable or disable a feature for a licence (takes effect immediately)")
    public ApiResponse<LicenseResponse> toggleFeature(@PathVariable Long id,
                                                      @Valid @RequestBody FeatureToggleRequest request) {
        return ApiResponse.ok(LicenseResponse.from(
                licenseService.setFeature(id, request.featureCode(), request.enabled())));
    }

    @GetMapping("/licenses/validate")
    @PreAuthorize("@authz.has('licensing:view')")
    @Operation(summary = "Validate a licence key")
    public ApiResponse<LicenseValidation> validate(@RequestParam String key) {
        return ApiResponse.ok(licenseService.validate(key));
    }

    @PostMapping("/licenses/expire-lapsed")
    @PreAuthorize("@authz.has('licensing:edit')")
    @Operation(summary = "Mark lapsed licences expired (sweep)")
    public ApiResponse<Integer> expireLapsed() {
        return ApiResponse.ok(licenseService.expireLapsed());
    }

    // ---- Tenants ------------------------------------------------------------

    @GetMapping("/tenants")
    @PreAuthorize("@authz.has('licensing:manage-tenants')")
    @Operation(summary = "List tenants")
    public ApiResponse<List<TenantResponse>> tenants() {
        return ApiResponse.ok(tenantService.list().stream().map(TenantResponse::from).toList());
    }

    @PostMapping("/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('licensing:manage-tenants')")
    @Operation(summary = "Create a tenant")
    public ApiResponse<TenantResponse> createTenant(@Valid @RequestBody TenantRequest request) {
        return ApiResponse.ok(TenantResponse.from(tenantService.create(
                request.code(), request.name(), request.description(),
                request.contactEmail(), request.config())));
    }

    @PostMapping("/tenants/{id}/activate")
    @PreAuthorize("@authz.has('licensing:manage-tenants')")
    @Operation(summary = "Activate a tenant")
    public ApiResponse<TenantResponse> activateTenant(@PathVariable Long id) {
        return ApiResponse.ok(TenantResponse.from(tenantService.activate(id)));
    }

    @PostMapping("/tenants/{id}/suspend")
    @PreAuthorize("@authz.has('licensing:manage-tenants')")
    @Operation(summary = "Suspend a tenant")
    public ApiResponse<TenantResponse> suspendTenant(@PathVariable Long id) {
        return ApiResponse.ok(TenantResponse.from(tenantService.suspend(id)));
    }

    // ---- Plans, features, usage, catalog ------------------------------------

    @GetMapping("/plans")
    @PreAuthorize("@authz.has('licensing:view')")
    @Operation(summary = "List subscription plans")
    public ApiResponse<List<PlanResponse>> plans() {
        return ApiResponse.ok(planRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(PlanResponse::from).toList());
    }

    @GetMapping("/features")
    @PreAuthorize("@authz.has('licensing:view')")
    @Operation(summary = "List the feature catalogue")
    public ApiResponse<List<FeatureResponse>> features() {
        return ApiResponse.ok(featureRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(FeatureResponse::from).toList());
    }

    @GetMapping("/usage")
    @PreAuthorize("@authz.has('licensing:view-usage')")
    @Operation(summary = "Usage against licensed limits")
    public ApiResponse<List<UsageCheck>> usage(@RequestParam(required = false) Long tenantId) {
        return ApiResponse.ok(usageService.checkAll(tenantId));
    }

    @GetMapping("/catalog")
    @PreAuthorize("@authz.has('licensing:view')")
    @Operation(summary = "Configurable catalogues and registered plugins")
    public ApiResponse<CatalogResponse> catalog() {
        return ApiResponse.ok(new CatalogResponse(
                statusRepository.findByScopeOrderByDisplayOrderAsc(LicensingStatus.SCOPE_LICENSE)
                        .stream().map(LookupResponse::from).toList(),
                statusRepository.findByScopeOrderByDisplayOrderAsc(LicensingStatus.SCOPE_TENANT)
                        .stream().map(LookupResponse::from).toList(),
                licenseTypeRepository.findAllByOrderByDisplayOrderAsc()
                        .stream().map(LookupResponse::from).toList(),
                expiryBehaviorRepository.findAllByOrderByDisplayOrderAsc()
                        .stream().map(LookupResponse::from).toList(),
                limitTypeRepository.findAllByOrderByDisplayOrderAsc()
                        .stream().map(LookupResponse::from).toList(),
                meterRegistry.meteredTypes(),
                expiryRegistry.codes()));
    }

    // ---- Lifecycle: modes, transfer, billing, auto-renew ---------------------

    @PostMapping("/licenses/{id}/activate/{mode}")
    @PreAuthorize("@authz.has('licensing:activate')")
    @Operation(summary = "Activate through a pluggable mode (OFFLINE / EMERGENCY / ONLINE)")
    public ApiResponse<LicenseResponse> activateWithMode(
            @PathVariable Long id,
            @PathVariable String mode,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        String fingerprint = body == null ? null : (String) body.get("fingerprint");
        return ApiResponse.ok(LicenseResponse.from(
                licenseService.activateWithMode(id, mode.toUpperCase(), fingerprint, body)));
    }

    @PostMapping("/licenses/{id}/transfer")
    @PreAuthorize("@authz.has('licensing:transfer')")
    @Operation(summary = "Transfer a licence to another tenant")
    public ApiResponse<LicenseResponse> transfer(@PathVariable Long id,
                                                 @RequestBody java.util.Map<String, Object> body) {
        Object target = body.get("toTenantId");
        Long toTenantId = target instanceof Number n ? n.longValue() : Long.valueOf(String.valueOf(target));
        return ApiResponse.ok(LicenseResponse.from(
                licenseService.transfer(id, toTenantId, (String) body.get("reason"))));
    }

    @PatchMapping("/licenses/{id}/payment-status")
    @PreAuthorize("@authz.has('licensing:manage-billing')")
    @Operation(summary = "Set the payment status of a licence")
    public ApiResponse<LicenseResponse> paymentStatus(@PathVariable Long id,
                                                      @Valid @RequestBody StatusChangeRequest request) {
        return ApiResponse.ok(LicenseResponse.from(
                licenseService.setPaymentStatus(id, request.statusCode())));
    }

    @PostMapping("/licenses/auto-renew")
    @PreAuthorize("@authz.has('licensing:renew')")
    @Operation(summary = "Renew every lapsed licence flagged auto-renew")
    public ApiResponse<Integer> autoRenew() {
        return ApiResponse.ok(licenseService.processAutoRenewals());
    }

    @GetMapping("/licenses/{id}/transfers")
    @PreAuthorize("@authz.has('licensing:view')")
    @Operation(summary = "Transfer history of a licence")
    public ApiResponse<List<java.util.Map<String, Object>>> transfers(@PathVariable Long id) {
        return ApiResponse.ok(licenseService.transfers(id).stream()
                .map(t -> java.util.Map.<String, Object>of(
                        "fromTenantId", String.valueOf(t.getFromTenantId()),
                        "toTenantId", t.getToTenantId(),
                        "reason", String.valueOf(t.getReason()),
                        "transferredAt", t.getTransferredAt(),
                        "by", String.valueOf(t.getTransferredByEmail())))
                .toList());
    }

    // ---- Reports ------------------------------------------------------------

    @GetMapping("/reports/summary")
    @PreAuthorize("@authz.has('licensing:view')")
    @Operation(summary = "Licence summary report")
    public ApiResponse<java.util.Map<String, Object>> reportSummary() {
        return ApiResponse.ok(reportService.licenseSummary());
    }

    @GetMapping("/reports/tenants")
    @PreAuthorize("@authz.has('licensing:view')")
    @Operation(summary = "Active tenants report")
    public ApiResponse<List<java.util.Map<String, Object>>> reportTenants() {
        return ApiResponse.ok(reportService.activeTenants());
    }

    @GetMapping("/reports/expiring")
    @PreAuthorize("@authz.has('licensing:view')")
    @Operation(summary = "Expired / expiring licences")
    public ApiResponse<List<java.util.Map<String, Object>>> reportExpiring(
            @RequestParam(defaultValue = "30") int withinDays) {
        return ApiResponse.ok(reportService.expiring(withinDays));
    }

    @GetMapping("/reports/feature-usage")
    @PreAuthorize("@authz.has('licensing:view')")
    @Operation(summary = "Feature usage across licences")
    public ApiResponse<List<java.util.Map<String, Object>>> reportFeatureUsage() {
        return ApiResponse.ok(reportService.featureUsage());
    }

    @GetMapping("/reports/plan-comparison")
    @PreAuthorize("@authz.has('licensing:view')")
    @Operation(summary = "Plan comparison (features and limits)")
    public ApiResponse<List<java.util.Map<String, Object>>> reportPlanComparison() {
        return ApiResponse.ok(reportService.planComparison());
    }

    @GetMapping(value = "/reports/{report}/export.csv", produces = "text/csv")
    @PreAuthorize("@authz.has('licensing:export')")
    @Operation(summary = "Export a report as CSV")
    public org.springframework.http.ResponseEntity<String> exportCsv(
            @PathVariable String report,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "30") int withinDays) {
        List<java.util.Map<String, Object>> rows = switch (report) {
            case "tenants" -> reportService.activeTenants();
            case "expiring" -> reportService.expiring(withinDays);
            case "feature-usage" -> reportService.featureUsage();
            case "usage-limits" -> reportService.usageLimits(tenantId);
            case "plan-comparison" -> reportService.planComparison();
            default -> List.of();
        };
        return org.springframework.http.ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=\"" + report + ".csv\"")
                .body(reportService.toCsv(rows));
    }
}
