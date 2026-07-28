package com.sami.app.licensing.dto;

import com.sami.app.licensing.domain.ExpiryBehavior;
import com.sami.app.licensing.domain.Feature;
import com.sami.app.licensing.domain.License;
import com.sami.app.licensing.domain.LicenseType;
import com.sami.app.licensing.domain.LicensingStatus;
import com.sami.app.licensing.domain.SubscriptionPlan;
import com.sami.app.licensing.domain.Tenant;
import com.sami.app.licensing.domain.UsageLimitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
                                  Map<String, Object> limitOverrides, Instant createdAt, long version) {
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
                    l.getLimitOverrides(), l.getCreatedAt(), l.getVersion());
        }
    }

    public record FeatureResponse(Long id, String code, String name, String description, String moduleCode,
                                  boolean licenseRequired, boolean core, boolean active) {
        public static FeatureResponse from(Feature f) {
            return new FeatureResponse(f.getId(), f.getCode(), f.getName(), f.getDescription(),
                    f.getModuleCode(), f.isLicenseRequired(), f.isCore(), f.isActive());
        }
    }

    public record PlanResponse(Long id, String code, String name, String description, String statusCode,
                               int durationDays, String renewalPolicy, boolean isDefault) {
        public static PlanResponse from(SubscriptionPlan p) {
            return new PlanResponse(p.getId(), p.getCode(), p.getName(), p.getDescription(),
                    p.getStatus().getCode(), p.getDurationDays(), p.getRenewalPolicy(), p.isDefault());
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
    }

    public record CatalogResponse(List<LookupResponse> licenseStatuses, List<LookupResponse> tenantStatuses,
                                  List<LookupResponse> licenseTypes, List<LookupResponse> expiryBehaviors,
                                  List<LookupResponse> limitTypes, List<String> meteredLimitTypes,
                                  List<String> expiryHandlers) {
    }
}
