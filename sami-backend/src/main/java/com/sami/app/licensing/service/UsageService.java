package com.sami.app.licensing.service;

import com.sami.app.licensing.domain.License;
import com.sami.app.licensing.domain.PlanLimit;
import com.sami.app.licensing.domain.SubscriptionPlan;
import com.sami.app.licensing.domain.UsageCounter;
import com.sami.app.licensing.domain.UsageLimitType;
import com.sami.app.licensing.event.LicenseDomainEvent;
import com.sami.app.licensing.repository.LicenseRepository;
import com.sami.app.licensing.repository.SubscriptionPlanRepository;
import com.sami.app.licensing.repository.TenantRepository;
import com.sami.app.licensing.repository.UsageCounterRepository;
import com.sami.app.licensing.repository.UsageLimitTypeRepository;
import com.sami.app.licensing.spi.UsageMeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Enforces configurable usage ceilings. The licensing core never counts business
 * data: it reads the ceiling from the plan (with per-licence overrides) and asks
 * the module-supplied {@code UsageMeterProvider} for the current value.
 */
@Service
@RequiredArgsConstructor
public class UsageService {

    private final LicenseRepository licenseRepository;
    private final SubscriptionPlanRepository planRepository;
    private final UsageLimitTypeRepository limitTypeRepository;
    private final UsageCounterRepository counterRepository;
    private final TenantRepository tenantRepository;
    private final UsageMeterRegistry meterRegistry;
    private final EntitlementService entitlements;
    private final ApplicationEventPublisher eventPublisher;
    private final LicensingScope scope;

    /** Checks one limit; publishes {@code UsageLimitExceeded} when breached. */
    @Transactional(readOnly = true)
    public UsageCheck check(String limitTypeCode, Long tenantId) {
        Long trustedTenantId = scope.resolveTenant(tenantId);
        Entitlement ent = entitlements.resolve(trustedTenantId);
        long current = measure(limitTypeCode, ent.tenantId());
        Long limit = resolveLimit(limitTypeCode, ent.licenseId());

        if (limit == null || limit == PlanLimit.UNLIMITED) {
            return new UsageCheck(limitTypeCode, PlanLimit.UNLIMITED, current, true, true);
        }
        boolean allowed = current < limit;
        if (!allowed) {
            eventPublisher.publishEvent(new LicenseDomainEvent(
                    "usage-" + limitTypeCode, LicenseDomainEvent.USAGE_LIMIT_EXCEEDED,
                    ent.tenantId(), ent.licenseId(), ent.licenseCode(),
                    Map.of("limitType", limitTypeCode, "limit", limit, "current", current),
                    Instant.now()));
        }
        return new UsageCheck(limitTypeCode, limit, current, false, allowed);
    }

    /** All limits for the tenant, for the usage report. */
    @Transactional(readOnly = true)
    public List<UsageCheck> checkAll(Long tenantId) {
        List<UsageCheck> results = new ArrayList<>();
        for (UsageLimitType type : limitTypeRepository.findAllByOrderByDisplayOrderAsc()) {
            results.add(check(type.getCode(), tenantId));
        }
        return results;
    }

    /** Persists a measured value (for metrics the meters cannot compute live). */
    @Transactional
    public void record(Long tenantId, String limitTypeCode, long value, String periodKey) {
        UsageLimitType type = limitTypeRepository.findByCode(limitTypeCode).orElse(null);
        if (type == null || tenantId == null) {
            return;
        }
        String period = periodKey == null ? "" : periodKey;
        UsageCounter counter = counterRepository
                .findByTenantIdAndLimitTypeIdAndPeriodKey(tenantId, type.getId(), period)
                .orElseGet(() -> UsageCounter.builder()
                        .tenant(tenantRepository.getReferenceById(tenantId))
                        .limitType(type)
                        .periodKey(period)
                        .build());
        counter.setCurrentValue(value);
        counter.setRecordedAt(Instant.now());
        counterRepository.save(counter);
    }

    /** Live meter when a module supplies one, else the last recorded counter. */
    private long measure(String limitTypeCode, Long tenantId) {
        Optional<Long> live = meterRegistry.find(limitTypeCode)
                .map(meter -> meter.currentUsage(tenantId));
        if (live.isPresent()) {
            return live.get();
        }
        if (tenantId == null) {
            return 0;
        }
        return limitTypeRepository.findByCode(limitTypeCode)
                .flatMap(type -> counterRepository
                        .findByTenantIdAndLimitTypeIdAndPeriodKey(tenantId, type.getId(), ""))
                .map(UsageCounter::getCurrentValue)
                .orElse(0L);
    }

    /** Per-licence override wins over the plan ceiling. */
    private Long resolveLimit(String limitTypeCode, Long licenseId) {
        if (licenseId == null) {
            return null;
        }
        License license = licenseRepository.findWithDetailsById(licenseId).orElse(null);
        if (license == null) {
            return null;
        }
        Object override = license.getLimitOverrides().get(limitTypeCode);
        if (override instanceof Number n) {
            return n.longValue();
        }
        if (override instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        SubscriptionPlan plan = planRepository.findWithDetailsById(license.getPlan().getId()).orElse(null);
        if (plan == null) {
            return null;
        }
        return plan.getLimits().stream()
                .filter(l -> l.getLimitType().getCode().equals(limitTypeCode))
                .map(PlanLimit::getLimitValue)
                .findFirst()
                .orElse(null);
    }
}
