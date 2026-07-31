package com.sami.app.licensing.dto;

import com.sami.app.licensing.domain.ExpiryBehavior;
import com.sami.app.licensing.domain.BillingCycle;
import com.sami.app.licensing.domain.Feature;
import com.sami.app.licensing.domain.FeatureState;
import com.sami.app.licensing.domain.License;
import com.sami.app.licensing.domain.LicenseAuditLog;
import com.sami.app.licensing.domain.LicenseType;
import com.sami.app.licensing.domain.LicensingStatus;
import com.sami.app.licensing.domain.SubscriptionPlan;
import com.sami.app.licensing.domain.Tenant;
import com.sami.app.licensing.domain.UsageLimitType;
import com.sami.app.licensing.domain.PaymentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Request/response records for the licensing module (all wrapped in ApiResponse). */
public final class LicensingDtos {

    private LicensingDtos() {
    }

    // ---- Requests -----------------------------------------------------------

    public record TenantRequest(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$", message = "code must be a lowercase slug") String code,
            @NotBlank String name,
            String description,
            String contactEmail,
            Map<String, Object> config
    ) {
    }

    public record TenantUpdateRequest(
            @NotBlank String name,
            String description,
            String contactEmail,
            Map<String, Object> config,
            Long expectedVersion
    ) {
    }

    public record LicenseRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{2,64}$") String code,
            String licenseKey,
            @NotBlank String typeCode,
            @NotNull Long tenantId,
            Long companyId,
            @NotBlank String planCode,
            String expiryBehaviorCode,
            Integer graceDays,
            Instant expirationDate,
            Map<String, Object> limitOverrides
    ) {
    }

    public record ActivateRequest(String fingerprint) {
    }

    public record RenewRequest(Integer days, String planCode) {
    }

    public record StatusChangeRequest(@NotBlank String statusCode) {
    }

    public record FeatureToggleRequest(@NotBlank String featureCode, boolean enabled) {
    }

    public record LicenseUpdateRequest(
            @NotBlank String planCode,
            String expiryBehaviorCode,
            @Min(0) Integer graceDays,
            Instant expirationDate,
            boolean autoRenew,
            Map<String, Object> limitOverrides,
            Long expectedVersion
    ) {
    }

    public record TransferRequest(@NotNull Long toTenantId, String reason) {
    }

    public record ActivationModeRequest(String fingerprint, @Min(1) Integer grantDays) {
    }

    public record PlanRequest(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$") String code,
            @NotBlank String name,
            String description,
            @NotBlank String statusCode,
            @Min(1) int durationDays,
            @NotBlank String renewalPolicy,
            String billingCycleCode,
            boolean defaultPlan,
            Map<String, Object> priceConfig,
            Set<String> featureCodes,
            Map<String, Long> limits,
            Long expectedVersion
    ) {
    }

    public record FeatureRequest(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9._-]{1,127}$") String code,
            @NotBlank String name,
            String description,
            String moduleCode,
            boolean licenseRequired,
            boolean core,
            boolean active,
            @NotBlank String stateCode,
            @Min(0) int trialDays,
            Set<String> dependencyCodes,
            Long expectedVersion
    ) {
    }

    // ---- Responses ----------------------------------------------------------

    public record TenantResponse(Long id, String code, String name, String description, String statusCode,
                                 String statusName, String contactEmail, Map<String, Object> config,
                                 Instant activatedAt, Instant suspendedAt, Instant createdAt, long version) {
        public static TenantResponse from(Tenant t) {
            return new TenantResponse(t.getId(), t.getCode(), t.getName(), t.getDescription(),
                    t.getStatus().getCode(), t.getStatus().getName(), t.getContactEmail(), t.getConfig(),
                    t.getActivatedAt(), t.getSuspendedAt(), t.getCreatedAt(), t.getVersion());
        }
    }

    public record LicenseResponse(Long id, String code, String typeCode, Long tenantId,
                                  String tenantCode, Long companyId, String statusCode, String statusName,
                                  String planCode, String expiryBehavior, Instant activationDate,
                                  Instant expirationDate, int graceDays, Instant activatedAt,
                                  String activationMode, String paymentStatus, boolean autoRenew,
                                  Instant emergencyUntil, int transferCount,
                                  Map<String, Object> limitOverrides, Map<String, Boolean> featureOverrides,
                                  Instant createdAt, long version) {
        public static LicenseResponse from(License l) {
            return new LicenseResponse(l.getId(), l.getCode(), l.getLicenseType().getCode(),
                    l.getTenant().getId(), l.getTenant().getCode(),
                    l.getCompanyId(), l.getStatus().getCode(), l.getStatus().getName(),
                    l.getPlan().getCode(),
                    l.getExpiryBehavior() != null ? l.getExpiryBehavior().getCode() : null,
                    l.getActivationDate(), l.getExpirationDate(), l.getGraceDays(), l.getActivatedAt(),
                    l.getActivationMode(),
                    l.getPaymentStatus() == null ? null : l.getPaymentStatus().getCode(),
                    l.isAutoRenew(), l.getEmergencyUntil(), l.getTransferCount(),
                    l.getLimitOverrides(), l.getFeatureOverrides().stream().collect(Collectors.toMap(
                    override -> override.getFeature().getCode(), override -> override.isEnabled())),
                    l.getCreatedAt(), l.getVersion());
        }
    }

    public record FeatureResponse(Long id, String code, String name, String description, String moduleCode,
                                  boolean licenseRequired, boolean core, boolean active, String stateCode,
                                  int trialDays, boolean system, Set<String> dependencyCodes, long version) {
        public static FeatureResponse from(Feature f) {
            return new FeatureResponse(f.getId(), f.getCode(), f.getName(), f.getDescription(),
                    f.getModuleCode(), f.isLicenseRequired(), f.isCore(), f.isActive(),
                    f.getState().getCode(), f.getTrialDays(), f.isSystem(), f.getDependencies().stream()
                    .map(Feature::getCode).collect(Collectors.toUnmodifiableSet()), f.getVersion());
        }
    }

    public record PlanResponse(Long id, String code, String name, String description, String statusCode,
                               int durationDays, String renewalPolicy, boolean isDefault, String billingCycleCode,
                               Map<String, Object> priceConfig, Set<String> featureCodes,
                               Map<String, Long> limits, boolean system, long version) {
        public static PlanResponse from(SubscriptionPlan p) {
            return new PlanResponse(p.getId(), p.getCode(), p.getName(), p.getDescription(),
                    p.getStatus().getCode(), p.getDurationDays(), p.getRenewalPolicy(), p.isDefault(),
                    p.getBillingCycle() == null ? null : p.getBillingCycle().getCode(), p.getPriceConfig(),
                    p.getFeatures().stream().map(Feature::getCode).collect(Collectors.toUnmodifiableSet()),
                    p.getLimits().stream().collect(Collectors.toMap(
                            limit -> limit.getLimitType().getCode(), limit -> limit.getLimitValue(),
                            (existing, duplicateJoinRow) -> existing)),
                    p.isSystem(), p.getVersion());
        }
    }

    public record AuditResponse(Long id, Long tenantId, String entityType, Long entityId, String action,
                                Map<String, Object> oldValues, Map<String, Object> newValues,
                                String actorEmail, Instant createdAt) {
        public static AuditResponse from(LicenseAuditLog log) {
            return new AuditResponse(log.getId(), log.getTenantId(), log.getEntityType(), log.getEntityId(),
                    log.getAction(), log.getOldValues(), log.getNewValues(), log.getActorEmail(), log.getCreatedAt());
        }
    }

    public record LookupResponse(Long id, String code, String name, String extra) {
        public static LookupResponse from(LicensingStatus s) {
            return new LookupResponse(s.getId(), s.getCode(), s.getName(), s.getScope());
        }

        public static LookupResponse from(LicenseType t) {
            return new LookupResponse(t.getId(), t.getCode(), t.getName(),
                    t.isRequiresOnlineValidation() ? "online" : "offline");
        }

        public static LookupResponse from(ExpiryBehavior b) {
            return new LookupResponse(b.getId(), b.getCode(), b.getName(), b.getDescription());
        }

        public static LookupResponse from(UsageLimitType u) {
            return new LookupResponse(u.getId(), u.getCode(), u.getName(), u.getUnit());
        }

        public static LookupResponse from(PaymentStatus status) {
            return new LookupResponse(status.getId(), status.getCode(), status.getName(),
                    status.isBlocksAccess() ? "blocks-access" : null);
        }

        public static LookupResponse from(BillingCycle cycle) {
            return new LookupResponse(cycle.getId(), cycle.getCode(), cycle.getName(),
                    String.valueOf(cycle.getMonths()));
        }

        public static LookupResponse from(FeatureState state) {
            return new LookupResponse(state.getId(), state.getCode(), state.getName(),
                    state.isHidden() ? "hidden" : state.isDeprecated() ? "deprecated" : null);
        }
    }

    public record CatalogResponse(List<LookupResponse> licenseStatuses, List<LookupResponse> tenantStatuses,
                                  List<LookupResponse> licenseTypes, List<LookupResponse> expiryBehaviors,
                                  List<LookupResponse> limitTypes, List<LookupResponse> paymentStatuses,
                                  List<LookupResponse> billingCycles, List<LookupResponse> featureStates,
                                  List<String> activationModes, List<String> meteredLimitTypes,
                                  List<String> expiryHandlers) {
    }
}
