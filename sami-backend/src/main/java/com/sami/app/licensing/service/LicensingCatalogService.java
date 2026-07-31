package com.sami.app.licensing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.licensing.domain.Feature;
import com.sami.app.licensing.domain.PlanLimit;
import com.sami.app.licensing.domain.SubscriptionPlan;
import com.sami.app.licensing.dto.LicensingDtos.FeatureRequest;
import com.sami.app.licensing.dto.LicensingDtos.PlanRequest;
import com.sami.app.licensing.repository.BillingCycleRepository;
import com.sami.app.licensing.repository.FeatureRepository;
import com.sami.app.licensing.repository.FeatureStateRepository;
import com.sami.app.licensing.repository.LicensingStatusRepository;
import com.sami.app.licensing.repository.SubscriptionPlanRepository;
import com.sami.app.licensing.repository.UsageLimitTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Platform-global administration for the existing plan/feature catalogue. */
@Service
@RequiredArgsConstructor
public class LicensingCatalogService {

    private final SubscriptionPlanRepository planRepository;
    private final FeatureRepository featureRepository;
    private final LicensingStatusRepository statusRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final FeatureStateRepository featureStateRepository;
    private final UsageLimitTypeRepository limitTypeRepository;
    private final LicensingScope scope;
    private final LicenseAuditService audit;
    private final EntitlementService entitlements;

    @Transactional(readOnly = true)
    public List<SubscriptionPlan> plans() {
        scope.currentTenantId();
        return planRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<Feature> features() {
        scope.currentTenantId();
        return featureRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional
    public SubscriptionPlan createPlan(PlanRequest request) {
        scope.requirePlatform();
        if (planRepository.existsByCode(request.code())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Plan code already exists: " + request.code());
        }
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .code(request.code())
                .features(new HashSet<>())
                .limits(new ArrayList<>())
                .displayOrder(planRepository.findAllByOrderByDisplayOrderAsc().size() * 10 + 10)
                .isSystem(false)
                .build();
        applyPlan(plan, request);
        SubscriptionPlan saved = planRepository.save(plan);
        audit.record("PLAN", saved.getId(), "CREATED", null, planSnapshot(saved));
        entitlements.invalidate();
        return planRepository.findWithDetailsById(saved.getId()).orElse(saved);
    }

    @Transactional
    public SubscriptionPlan updatePlan(Long id, PlanRequest request) {
        scope.requirePlatform();
        SubscriptionPlan plan = planRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + id));
        checkVersion(plan.getVersion(), request.expectedVersion(), "Plan");
        Map<String, Object> before = planSnapshot(plan);
        applyPlan(plan, request);
        SubscriptionPlan saved = planRepository.save(plan);
        audit.record("PLAN", saved.getId(), "UPDATED", before, planSnapshot(saved));
        entitlements.invalidate();
        return planRepository.findWithDetailsById(saved.getId()).orElse(saved);
    }

    @Transactional
    public Feature createFeature(FeatureRequest request) {
        scope.requirePlatform();
        if (featureRepository.existsByCode(request.code())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Feature code already exists: " + request.code());
        }
        Feature feature = Feature.builder()
                .code(request.code())
                .dependencies(new HashSet<>())
                .displayOrder(featureRepository.findAllByOrderByDisplayOrderAsc().size() * 10 + 10)
                .isSystem(false)
                .build();
        applyFeature(feature, request);
        Feature saved = featureRepository.save(feature);
        audit.record("FEATURE", saved.getId(), "CREATED", null, featureSnapshot(saved));
        entitlements.invalidate();
        return featureRepository.findByCode(saved.getCode()).orElse(saved);
    }

    @Transactional
    public Feature updateFeature(Long id, FeatureRequest request) {
        scope.requirePlatform();
        Feature feature = featureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature not found: " + id));
        checkVersion(feature.getVersion(), request.expectedVersion(), "Feature");
        Map<String, Object> before = featureSnapshot(feature);
        applyFeature(feature, request);
        Feature saved = featureRepository.save(feature);
        audit.record("FEATURE", saved.getId(), "UPDATED", before, featureSnapshot(saved));
        entitlements.invalidate();
        return featureRepository.findByCode(saved.getCode()).orElse(saved);
    }

    private void applyPlan(SubscriptionPlan plan, PlanRequest request) {
        if (!plan.getCode().equals(request.code())) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "Plan code cannot be changed");
        }
        if (!Set.of("MANUAL", "AUTO", "NONE").contains(request.renewalPolicy().toUpperCase())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown renewal policy: " + request.renewalPolicy());
        }
        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setStatus(statusRepository.findByScopeAndCode("PLAN", request.statusCode())
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST,
                        "Unknown plan status: " + request.statusCode())));
        plan.setDurationDays(request.durationDays());
        plan.setRenewalPolicy(request.renewalPolicy().toUpperCase());
        plan.setBillingCycle(request.billingCycleCode() == null ? null
                : billingCycleRepository.findByCode(request.billingCycleCode())
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST,
                        "Unknown billing cycle: " + request.billingCycleCode())));
        plan.setPriceConfig(request.priceConfig() == null ? new HashMap<>() : new HashMap<>(request.priceConfig()));
        if (request.defaultPlan()) {
            planRepository.findByIsDefaultTrue().filter(existing -> !existing.getId().equals(plan.getId()))
                    .ifPresent(existing -> {
                        existing.setDefault(false);
                        planRepository.save(existing);
                    });
        }
        plan.setDefault(request.defaultPlan());
        plan.getFeatures().clear();
        for (String code : nullSafe(request.featureCodes())) {
            plan.getFeatures().add(featureRepository.findByCode(code)
                    .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown feature: " + code)));
        }
        Map<String, Long> requestedLimits = nullSafeMap(request.limits());
        plan.getLimits().removeIf(limit -> !requestedLimits.containsKey(limit.getLimitType().getCode()));
        for (Map.Entry<String, Long> entry : requestedLimits.entrySet()) {
            if (entry.getValue() == null || entry.getValue() < PlanLimit.UNLIMITED) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Limit must be -1 (unlimited) or non-negative: " + entry.getKey());
            }
            PlanLimit existing = plan.getLimits().stream()
                    .filter(limit -> entry.getKey().equals(limit.getLimitType().getCode()))
                    .findFirst().orElse(null);
            if (existing != null) {
                existing.setLimitValue(entry.getValue());
            } else {
                plan.getLimits().add(PlanLimit.builder()
                        .plan(plan)
                        .limitType(limitTypeRepository.findByCode(entry.getKey())
                                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST,
                                        "Unknown limit type: " + entry.getKey())))
                        .limitValue(entry.getValue())
                        .build());
            }
        }
    }

    private void applyFeature(Feature feature, FeatureRequest request) {
        if (!feature.getCode().equals(request.code())) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "Feature code cannot be changed");
        }
        feature.setName(request.name());
        feature.setDescription(request.description());
        feature.setModuleCode(request.moduleCode());
        feature.setLicenseRequired(request.licenseRequired());
        feature.setCore(request.core());
        feature.setActive(request.active());
        feature.setState(featureStateRepository.findByCode(request.stateCode())
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST,
                        "Unknown feature state: " + request.stateCode())));
        feature.setTrialDays(request.trialDays());
        Set<Feature> dependencies = new HashSet<>();
        for (String code : nullSafe(request.dependencyCodes())) {
            Feature dependency = featureRepository.findByCode(code)
                    .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown feature: " + code));
            if (dependency.getId().equals(feature.getId()) || dependsOn(dependency, feature.getId(), new HashSet<>())) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Feature dependency would create a cycle: " + code);
            }
            dependencies.add(dependency);
        }
        feature.setDependencies(dependencies);
    }

    private boolean dependsOn(Feature feature, Long targetId, Set<Long> visited) {
        if (targetId == null || feature.getId() == null || !visited.add(feature.getId())) {
            return false;
        }
        if (feature.getId().equals(targetId)) {
            return true;
        }
        return feature.getDependencies().stream().anyMatch(dependency -> dependsOn(dependency, targetId, visited));
    }

    private void checkVersion(long actual, Long expected, String name) {
        if (expected != null && expected != actual) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    name + " was modified by another user; reload and retry");
        }
    }

    private Set<String> nullSafe(Set<String> values) {
        return values == null ? Set.of() : values;
    }

    private Map<String, Long> nullSafeMap(Map<String, Long> values) {
        return values == null ? Map.of() : values;
    }

    private Map<String, Object> planSnapshot(SubscriptionPlan plan) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("code", plan.getCode());
        snapshot.put("name", plan.getName());
        snapshot.put("status", plan.getStatus().getCode());
        snapshot.put("features", plan.getFeatures().stream().map(Feature::getCode).sorted().toList());
        snapshot.put("limits", plan.getLimits().size());
        return snapshot;
    }

    private Map<String, Object> featureSnapshot(Feature feature) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("code", feature.getCode());
        snapshot.put("name", feature.getName());
        snapshot.put("state", feature.getState().getCode());
        snapshot.put("active", feature.isActive());
        snapshot.put("dependencies", feature.getDependencies().stream().map(Feature::getCode).sorted().toList());
        return snapshot;
    }
}
