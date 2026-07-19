package com.sami.app.licensing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.licensing.domain.ExpiryBehavior;
import com.sami.app.licensing.domain.Feature;
import com.sami.app.licensing.domain.License;
import com.sami.app.licensing.domain.LicenseFeature;
import com.sami.app.licensing.domain.LicenseType;
import com.sami.app.licensing.domain.LicenseTransfer;
import com.sami.app.licensing.domain.LicensingStatus;
import com.sami.app.licensing.domain.PaymentStatus;
import com.sami.app.licensing.domain.SubscriptionPlan;
import com.sami.app.licensing.domain.Tenant;
import com.sami.app.licensing.event.LicenseDomainEvent;
import com.sami.app.licensing.repository.ExpiryBehaviorRepository;
import com.sami.app.licensing.repository.FeatureRepository;
import com.sami.app.licensing.repository.LicenseRepository;
import com.sami.app.licensing.repository.LicenseTypeRepository;
import com.sami.app.licensing.repository.LicenseTransferRepository;
import com.sami.app.licensing.repository.LicensingStatusRepository;
import com.sami.app.licensing.repository.PaymentStatusRepository;
import com.sami.app.licensing.repository.SubscriptionPlanRepository;
import com.sami.app.licensing.repository.TenantRepository;
import com.sami.app.licensing.spi.LicenseActivationProvider;
import com.sami.app.licensing.spi.LicenseActivationRegistry;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Licence lifecycle and the public validation surface. Activation is idempotent
 * for the same installation and refuses a second, different one (duplicate
 * activation); expiry only changes what is permitted and never touches business
 * data.
 */
@Service
@RequiredArgsConstructor
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final LicensingStatusRepository statusRepository;
    private final SubscriptionPlanRepository planRepository;
    private final ExpiryBehaviorRepository expiryBehaviorRepository;
    private final FeatureRepository featureRepository;
    private final TenantRepository tenantRepository;
    private final LicenseTransferRepository transferRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final LicenseActivationRegistry activationRegistry;
    private final EntitlementService entitlements;
    private final LicenseAuditService audit;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<License> list() {
        return licenseRepository.findAllBy();
    }

    @Transactional(readOnly = true)
    public License get(Long id) {
        return licenseRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("License not found: " + id));
    }

    @Transactional
    public License create(String code, String licenseKey, String typeCode, Long tenantId, Long companyId,
                          String planCode, String expiryBehaviorCode, Integer graceDays,
                          Instant expirationDate, Map<String, Object> limitOverrides) {
        if (licenseRepository.existsByCode(code)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "License code already exists: " + code);
        }
        String key = (licenseKey == null || licenseKey.isBlank()) ? generateKey() : licenseKey;
        if (licenseRepository.existsByLicenseKey(key)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "License key already exists");
        }
        Tenant tenant = tenantRepository.findWithStatusById(tenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown tenant: " + tenantId));
        LicenseType type = licenseTypeRepository.findByCode(typeCode)
                .or(licenseTypeRepository::findByIsDefaultTrue)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown license type: " + typeCode));
        SubscriptionPlan plan = planRepository.findByCode(planCode)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown plan: " + planCode));
        ExpiryBehavior behavior = (expiryBehaviorCode == null
                ? expiryBehaviorRepository.findByIsDefaultTrue()
                : expiryBehaviorRepository.findByCode(expiryBehaviorCode))
                .orElse(null);

        License license = License.builder()
                .code(code)
                .licenseKey(key)
                .licenseType(type)
                .tenant(tenant)
                .companyId(companyId)
                .plan(plan)
                .status(licenseStatus("draft"))
                .expiryBehavior(behavior)
                .graceDays(graceDays == null ? 0 : graceDays)
                .expirationDate(expirationDate)
                .limitOverrides(limitOverrides == null ? new HashMap<>() : new HashMap<>(limitOverrides))
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .build();
        License saved = licenseRepository.save(license);
        audit.record("LICENSE", saved.getId(), "CREATED", null,
                Map.of("code", code, "plan", planCode, "tenantId", tenantId));
        entitlements.invalidate();
        return saved;
    }

    /**
     * Activates a licence for one installation. Re-activating with the same
     * fingerprint is idempotent; a different fingerprint is refused.
     */
    @Transactional
    public License activate(Long id, String fingerprint) {
        License license = get(id);
        if (license.getActivatedAt() != null
                && license.getActivationFingerprint() != null
                && fingerprint != null
                && !license.getActivationFingerprint().equals(fingerprint)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "License is already activated on another installation");
        }
        Instant now = Instant.now();
        if (license.getExpirationDate() == null) {
            license.setExpirationDate(now.plus(license.getPlan().getDurationDays(), ChronoUnit.DAYS));
        }
        license.setStatus(licenseStatus("active"));
        license.setActivationDate(license.getActivationDate() == null ? now : license.getActivationDate());
        license.setActivatedAt(now);
        if (fingerprint != null && !fingerprint.isBlank()) {
            license.setActivationFingerprint(fingerprint);
        }
        License saved = licenseRepository.save(license);

        audit.record("LICENSE", id, "ACTIVATED", null,
                Map.of("status", "active", "expiresAt", String.valueOf(saved.getExpirationDate())));
        publish(LicenseDomainEvent.LICENSE_ACTIVATED, saved, Map.of("plan", saved.getPlan().getCode()));
        entitlements.invalidate();
        return saved;
    }

    /** Extends the term (renewal during grace simply resumes an active licence). */
    @Transactional
    public License renew(Long id, Integer days, String planCode) {
        License license = get(id);
        Instant now = Instant.now();
        if (planCode != null && !planCode.isBlank()) {
            license.setPlan(planRepository.findByCode(planCode)
                    .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown plan: " + planCode)));
        }
        int extension = days != null ? days : license.getPlan().getDurationDays();
        Instant base = (license.getExpirationDate() == null || license.getExpirationDate().isBefore(now))
                ? now : license.getExpirationDate();
        license.setExpirationDate(base.plus(extension, ChronoUnit.DAYS));
        license.setStatus(licenseStatus("active"));
        License saved = licenseRepository.save(license);

        audit.record("LICENSE", id, "RENEWED", null,
                Map.of("expiresAt", String.valueOf(saved.getExpirationDate()), "days", extension));
        publish(LicenseDomainEvent.SUBSCRIPTION_RENEWED, saved,
                Map.of("expiresAt", String.valueOf(saved.getExpirationDate())));
        entitlements.invalidate();
        return saved;
    }

    @Transactional
    public License changeStatus(Long id, String statusCode) {
        License license = get(id);
        String from = license.getStatus().getCode();
        license.setStatus(licenseStatus(statusCode));
        License saved = licenseRepository.save(license);
        audit.record("LICENSE", id, "STATUS_CHANGED", Map.of("status", from), Map.of("status", statusCode));
        if (saved.getStatus().isBlockedState()) {
            publish(LicenseDomainEvent.LICENSE_SUSPENDED, saved, Map.of("status", statusCode));
        }
        entitlements.invalidate();
        return saved;
    }

    /** Enables/disables a single feature for one licence — no restart required. */
    @Transactional
    public License setFeature(Long licenseId, String featureCode, boolean enabled) {
        License license = get(licenseId);
        Feature feature = featureRepository.findByCode(featureCode)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown feature: " + featureCode));
        if (enabled) {
            requireDependencies(feature);
        }
        license.getFeatureOverrides().stream()
                .filter(o -> o.getFeature().getId().equals(feature.getId()))
                .findFirst()
                .ifPresentOrElse(o -> o.setEnabled(enabled),
                        () -> license.getFeatureOverrides().add(LicenseFeature.builder()
                                .license(license).feature(feature).enabled(enabled).build()));
        License saved = licenseRepository.save(license);

        audit.record("LICENSE", licenseId, enabled ? "FEATURE_ENABLED" : "FEATURE_DISABLED",
                null, Map.of("feature", featureCode));
        publish(enabled ? LicenseDomainEvent.FEATURE_ENABLED : LicenseDomainEvent.FEATURE_DISABLED,
                saved, Map.of("feature", featureCode));
        entitlements.invalidate();
        return saved;
    }

    /** Public validation used by other systems and the activation UI. */
    @Transactional(readOnly = true)
    public LicenseValidation validate(String licenseKey) {
        License license = licenseRepository.findByLicenseKey(licenseKey).orElse(null);
        if (license == null) {
            return LicenseValidation.invalid("Unknown license key");
        }
        Instant now = Instant.now();
        boolean withinTerm = license.withinTerm(now);
        boolean withinGrace = license.withinGrace(now);
        boolean withinEmergency = license.withinEmergency(now);
        boolean paymentBlocks = license.getPaymentStatus() != null
                && license.getPaymentStatus().isBlocksAccess();
        boolean granted = (license.getStatus().isGrantsAccess() && withinTerm && !paymentBlocks)
                || withinEmergency;
        String reason;
        if (granted) {
            reason = withinEmergency && paymentBlocks ? "Emergency activation window" : null;
        } else if (paymentBlocks) {
            reason = "Payment status blocks access: " + license.getPaymentStatus().getCode();
        } else if (!withinTerm) {
            reason = withinGrace ? "In grace period" : "Expired";
        } else {
            reason = "Status does not grant access";
        }
        Entitlement ent = entitlements.resolve(license.getTenant().getId());
        return new LicenseValidation(granted || withinGrace, reason, license.getCode(),
                license.getStatus().getCode(), license.getPlan().getCode(),
                license.getTenant().getId(), license.getExpirationDate(), withinGrace, ent.enabledFeatures());
    }

    /** Marks lapsed licences expired; publishes {@code SubscriptionExpired}. */
    @Transactional
    public int expireLapsed() {
        Instant now = Instant.now();
        List<License> lapsed = licenseRepository.findLapsed(now);
        LicensingStatus expired = licenseStatus("expired");
        for (License license : lapsed) {
            if (license.withinGrace(now)) {
                continue;
            }
            license.setStatus(expired);
            licenseRepository.save(license);
            audit.record("LICENSE", license.getId(), "EXPIRED", null, Map.of("status", "expired"));
            publish(LicenseDomainEvent.SUBSCRIPTION_EXPIRED, license, Map.of());
        }
        entitlements.invalidate();
        return lapsed.size();
    }

    /**
     * Activation through a pluggable mode (OFFLINE / EMERGENCY / a future ONLINE
     * provider). Emergency mode grants only a bounded window, so a failed or
     * impossible validation can never become permanent access.
     */
    @Transactional
    public License activateWithMode(Long id, String mode, String fingerprint, Map<String, Object> context) {
        License license = get(id);
        LicenseActivationProvider provider = activationRegistry.find(mode)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown activation mode: " + mode));
        LicenseActivationProvider.ActivationDecision decision =
                provider.verify(license.getLicenseKey(), fingerprint, context);
        if (!decision.allowed()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, decision.reason());
        }
        Instant now = Instant.now();
        license.setActivationMode(mode);
        if (decision.grantDays() != null) {
            license.setEmergencyUntil(now.plus(decision.grantDays(), ChronoUnit.DAYS));
        } else {
            if (license.getExpirationDate() == null) {
                license.setExpirationDate(now.plus(license.getPlan().getDurationDays(), ChronoUnit.DAYS));
            }
            license.setStatus(licenseStatus("active"));
        }
        license.setActivatedAt(now);
        if (license.getActivationDate() == null) {
            license.setActivationDate(now);
        }
        if (fingerprint != null && !fingerprint.isBlank()) {
            license.setActivationFingerprint(fingerprint);
        }
        License saved = licenseRepository.save(license);

        audit.record("LICENSE", id, "ACTIVATED_" + mode, null,
                Map.of("mode", mode, "emergencyUntil", String.valueOf(saved.getEmergencyUntil())));
        publish(LicenseDomainEvent.LICENSE_ACTIVATED, saved, Map.of("mode", mode));
        entitlements.invalidate();
        return saved;
    }

    /**
     * Moves a licence to another tenant/installation. The previous activation
     * fingerprint is cleared so the new host can activate, and the move is
     * recorded append-only for audit.
     */
    @Transactional
    public License transfer(Long id, Long toTenantId, String reason) {
        License license = get(id);
        Long fromTenantId = license.getTenant().getId();
        if (fromTenantId.equals(toTenantId)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "License already belongs to that tenant");
        }
        var target = tenantRepository.findWithStatusById(toTenantId)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown tenant: " + toTenantId));

        transferRepository.save(LicenseTransfer.builder()
                .licenseId(id)
                .fromTenantId(fromTenantId)
                .toTenantId(toTenantId)
                .fromFingerprint(license.getActivationFingerprint())
                .reason(reason)
                .transferredBy(CurrentActor.id())
                .transferredByEmail(CurrentActor.email())
                .build());

        license.setTenant(target);
        license.setActivationFingerprint(null);
        license.setActivatedAt(null);
        license.setTransferredAt(Instant.now());
        license.setTransferCount(license.getTransferCount() + 1);
        License saved = licenseRepository.save(license);

        audit.record("LICENSE", id, "TRANSFERRED",
                Map.of("tenantId", fromTenantId), Map.of("tenantId", toTenantId));
        entitlements.invalidate();
        return saved;
    }

    /** Sets the payment status (paid/overdue/…); overdue can gate access. */
    @Transactional
    public License setPaymentStatus(Long id, String statusCode) {
        License license = get(id);
        PaymentStatus status = paymentStatusRepository.findByCode(statusCode)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown payment status: " + statusCode));
        String from = license.getPaymentStatus() == null ? null : license.getPaymentStatus().getCode();
        license.setPaymentStatus(status);
        License saved = licenseRepository.save(license);
        audit.record("LICENSE", id, "PAYMENT_STATUS_CHANGED",
                Map.of("paymentStatus", String.valueOf(from)), Map.of("paymentStatus", statusCode));
        entitlements.invalidate();
        return saved;
    }

    /** Renews every auto-renew licence whose term has lapsed. */
    @Transactional
    public int processAutoRenewals() {
        Instant now = Instant.now();
        int renewed = 0;
        for (License license : licenseRepository.findLapsed(now)) {
            if (!license.isAutoRenew()) {
                continue;
            }
            renew(license.getId(), null, null);
            renewed++;
        }
        return renewed;
    }

    @Transactional(readOnly = true)
    public List<LicenseTransfer> transfers(Long licenseId) {
        return transferRepository.findByLicenseIdOrderByTransferredAtDesc(licenseId);
    }

    private void requireDependencies(Feature feature) {
        for (Feature dependency : feature.getDependencies()) {
            if (!dependency.isActive()) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Feature " + feature.getCode() + " requires inactive feature " + dependency.getCode());
            }
        }
    }

    private LicensingStatus licenseStatus(String code) {
        return statusRepository.findByScopeAndCode(LicensingStatus.SCOPE_LICENSE, code)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown license status: " + code));
    }

    private void publish(String type, License license, Map<String, Object> payload) {
        eventPublisher.publishEvent(new LicenseDomainEvent(
                "license-" + license.getId(), type, license.getTenant().getId(),
                license.getId(), license.getCode(), payload, Instant.now()));
    }

    private String generateKey() {
        String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "SAMI-" + raw.substring(0, 5) + "-" + raw.substring(5, 10)
                + "-" + raw.substring(10, 15) + "-" + raw.substring(15, 20);
    }
}
