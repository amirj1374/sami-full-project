package com.sami.app.licensing.service;

import com.sami.app.licensing.LicensingProperties;
import com.sami.app.licensing.domain.Feature;
import com.sami.app.licensing.domain.FeatureState;
import com.sami.app.licensing.domain.License;
import com.sami.app.licensing.domain.LicenseFeature;
import com.sami.app.licensing.domain.SubscriptionPlan;
import com.sami.app.licensing.domain.Tenant;
import com.sami.app.licensing.repository.FeatureRepository;
import com.sami.app.licensing.repository.LicenseRepository;
import com.sami.app.licensing.repository.SubscriptionPlanRepository;
import com.sami.app.licensing.repository.TenantRepository;
import com.sami.app.licensing.spi.ExpiryBehaviorHandler;
import com.sami.app.licensing.spi.ExpiryBehaviorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the effective entitlement for a tenant: the licence that applies
 * (a company-scoped licence outranks the tenant-wide one), the feature set from
 * its plan with per-licence overrides applied, and — once lapsed — what the
 * configured {@link ExpiryBehaviorHandler} still permits.
 *
 * <p>Snapshots are cached for a short, configurable window so feature toggles
 * take effect without a restart while checks stay cheap.
 */
@Service
@RequiredArgsConstructor
public class EntitlementService {

    private final LicenseRepository licenseRepository;
    private final TenantRepository tenantRepository;
    private final FeatureRepository featureRepository;
    private final SubscriptionPlanRepository planRepository;
    private final ExpiryBehaviorRegistry expiryRegistry;
    private final LicensingProperties properties;

    private final Map<Long, CachedEntitlement> cache = new ConcurrentHashMap<>();
    private static final Long SINGLE_TENANT_KEY = -1L;

    /** Drops cached snapshots so licence/feature changes apply immediately. */
    public void invalidate() {
        cache.clear();
    }

    @Transactional(readOnly = true)
    public Entitlement resolve(Long tenantId) {
        Long key = tenantId == null ? SINGLE_TENANT_KEY : tenantId;
        CachedEntitlement cached = cache.get(key);
        Instant now = Instant.now();
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.entitlement();
        }
        Entitlement resolved = compute(tenantId, now);
        cache.put(key, new CachedEntitlement(resolved,
                now.plusSeconds(properties.cacheSecondsOrDefault())));
        return resolved;
    }

    /**
     * True when the feature is granted right now. Unknown/inactive features are
     * refused; features not requiring a licence always pass; an installation with
     * no licence passes unless {@code app.licensing.enforce} is on.
     */
    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(String featureCode, Long tenantId) {
        if (featureCode == null || featureCode.isBlank()) {
            return false;
        }
        Optional<Feature> feature = featureRepository.findByCode(featureCode);
        if (feature.isEmpty() || !feature.get().isActive()) {
            return false;
        }
        if (!stateAllows(feature.get(), tenantId)) {
            return false;
        }
        if (!feature.get().isLicenseRequired()) {
            return true;
        }
        Entitlement ent = resolve(tenantId);
        if (!ent.licensed()) {
            return !properties.enforce();
        }
        if (!ent.enabledFeatures().contains(featureCode)) {
            return false;
        }
        if (ent.accessGranted()) {
            return true;
        }
        return expiryRegistry.find(ent.expiryBehavior())
                .map(h -> h.permits(featureCode, feature.get().isCore(), ent.withinGrace()))
                .orElse(false);
    }

    /**
     * Applies the feature's lifecycle state: hidden/disabled never pass, beta
     * requires a tenant opt-in, and a trial only passes inside its window
     * (measured from licence activation). Deprecated and premium features pass
     * here — premium is still subject to the plan bundle check that follows.
     */
    private boolean stateAllows(Feature feature, Long tenantId) {
        FeatureState state = feature.getState();
        if (state == null) {
            return true;
        }
        if (!state.isGrantsAccess() || state.isHidden()) {
            return false;
        }
        if (state.isRequiresOptin() && !hasOptedIn(feature.getCode(), tenantId)) {
            return false;
        }
        if (state.isTrial()) {
            return withinTrial(feature, tenantId);
        }
        return true;
    }

    /** Beta opt-in lives in the tenant's configuration: {@code betaFeatures: [...]}. */
    private boolean hasOptedIn(String featureCode, Long tenantId) {
        Long id = tenantId != null ? tenantId : singleTenantId();
        if (id == null) {
            return false;
        }
        return tenantRepository.findWithStatusById(id)
                .map(t -> t.getConfig().get("betaFeatures"))
                .filter(v -> v instanceof java.util.List<?>)
                .map(v -> ((java.util.List<?>) v).stream()
                        .anyMatch(f -> featureCode.equals(String.valueOf(f))))
                .orElse(false);
    }

    /** Trial window runs trialDays from the licence's activation moment. */
    private boolean withinTrial(Feature feature, Long tenantId) {
        if (feature.getTrialDays() <= 0) {
            return false;
        }
        Long id = tenantId != null ? tenantId : singleTenantId();
        if (id == null) {
            return false;
        }
        List<License> licenses = licenseRepository.findForTenant(id);
        if (licenses.isEmpty()) {
            return false;
        }
        Instant start = licenses.get(0).getActivatedAt();
        if (start == null) {
            return false;
        }
        return start.plusSeconds((long) feature.getTrialDays() * 86_400L).isAfter(Instant.now());
    }

    private Entitlement compute(Long tenantId, Instant now) {
        Long effectiveTenantId = tenantId != null ? tenantId : singleTenantId();
        if (effectiveTenantId == null) {
            return Entitlement.unlicensed(null);
        }
        List<License> licenses = licenseRepository.findForTenant(effectiveTenantId);
        if (licenses.isEmpty()) {
            return Entitlement.unlicensed(effectiveTenantId);
        }
        License license = licenses.get(0);

        boolean withinTerm = license.withinTerm(now);
        boolean withinGrace = license.withinGrace(now);
        boolean paymentBlocks = license.getPaymentStatus() != null
                && license.getPaymentStatus().isBlocksAccess();
        boolean accessGranted = (license.getStatus().isGrantsAccess() && withinTerm && !paymentBlocks)
                || license.withinEmergency(now);
        String behavior = license.getExpiryBehavior() != null
                ? license.getExpiryBehavior().getCode() : "blocked";
        boolean writesAllowed = accessGranted
                || expiryRegistry.find(behavior).map(h -> h.permitsWrites(withinGrace)).orElse(false);

        return new Entitlement(effectiveTenantId, license.getId(), license.getCode(),
                effectiveFeatures(license), true, accessGranted, withinGrace, writesAllowed, behavior);
    }

    /** Plan bundle ∪ per-licence enables − per-licence disables. */
    private Set<String> effectiveFeatures(License license) {
        Set<String> codes = new HashSet<>();
        SubscriptionPlan plan = planRepository.findWithDetailsById(license.getPlan().getId()).orElse(null);
        if (plan != null) {
            plan.getFeatures().stream().filter(Feature::isActive).map(Feature::getCode).forEach(codes::add);
        }
        License detailed = licenseRepository.findWithDetailsById(license.getId()).orElse(license);
        for (LicenseFeature override : detailed.getFeatureOverrides()) {
            String code = override.getFeature().getCode();
            if (override.isEnabled()) {
                codes.add(code);
            } else {
                codes.remove(code);
            }
        }
        return Set.copyOf(codes);
    }

    /** On-premise installs run one tenant; that tenant is implicit. */
    private Long singleTenantId() {
        if (tenantRepository.count() != 1) {
            return null;
        }
        return tenantRepository.findAll().stream().findFirst().map(Tenant::getId).orElse(null);
    }

    private record CachedEntitlement(Entitlement entitlement, Instant expiresAt) {
    }
}
