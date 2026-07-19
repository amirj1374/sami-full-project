package com.sami.app.licensing.service;

import com.sami.app.licensing.domain.License;
import com.sami.app.licensing.domain.Tenant;
import com.sami.app.licensing.repository.FeatureRepository;
import com.sami.app.licensing.repository.LicenseRepository;
import com.sami.app.licensing.repository.SubscriptionPlanRepository;
import com.sami.app.licensing.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Licensing reports. Each report is a plain read model so it can be rendered as
 * JSON today and fed to CSV/Excel/PDF renderers without recomputation.
 */
@Service
@RequiredArgsConstructor
public class LicenseReportService {

    private final LicenseRepository licenseRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionPlanRepository planRepository;
    private final FeatureRepository featureRepository;
    private final UsageService usageService;

    /** Counts by licence status, plan and activation mode. */
    @Transactional(readOnly = true)
    public Map<String, Object> licenseSummary() {
        List<License> licenses = licenseRepository.findAllBy();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", licenses.size());
        summary.put("byStatus", licenses.stream().collect(Collectors.groupingBy(
                l -> l.getStatus().getCode(), Collectors.counting())));
        summary.put("byPlan", licenses.stream().collect(Collectors.groupingBy(
                l -> l.getPlan().getCode(), Collectors.counting())));
        summary.put("byActivationMode", licenses.stream().collect(Collectors.groupingBy(
                License::getActivationMode, Collectors.counting())));
        summary.put("autoRenew", licenses.stream().filter(License::isAutoRenew).count());
        return summary;
    }

    /** Tenants with their status and licence count. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> activeTenants() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Tenant tenant : tenantRepository.findAllBy()) {
            rows.add(new LinkedHashMap<>(Map.of(
                    "tenantCode", tenant.getCode(),
                    "name", tenant.getName(),
                    "status", tenant.getStatus().getCode(),
                    "licenses", licenseRepository.findForTenant(tenant.getId()).size())));
        }
        return rows;
    }

    /** Licences already lapsed, or lapsing inside the given horizon. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> expiring(int withinDays) {
        Instant horizon = Instant.now().plus(withinDays, ChronoUnit.DAYS);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (License license : licenseRepository.findAllBy()) {
            Instant expires = license.getExpirationDate();
            if (expires == null || expires.isAfter(horizon)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("licenseCode", license.getCode());
            row.put("tenant", license.getTenant().getCode());
            row.put("plan", license.getPlan().getCode());
            row.put("status", license.getStatus().getCode());
            row.put("expirationDate", String.valueOf(expires));
            row.put("expired", expires.isBefore(Instant.now()));
            row.put("autoRenew", license.isAutoRenew());
            rows.add(row);
        }
        return rows;
    }

    /** How many licences grant each feature (plan bundle ∪ overrides). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> featureUsage() {
        List<License> licenses = licenseRepository.findAllBy();
        List<Map<String, Object>> rows = new ArrayList<>();
        featureRepository.findAllByOrderByDisplayOrderAsc().forEach(feature -> {
            long granted = licenses.stream()
                    .filter(l -> planRepository.findWithDetailsById(l.getPlan().getId())
                            .map(p -> p.getFeatures().stream()
                                    .anyMatch(f -> f.getCode().equals(feature.getCode())))
                            .orElse(false))
                    .count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("featureCode", feature.getCode());
            row.put("name", feature.getName());
            row.put("state", feature.getState() == null ? null : feature.getState().getCode());
            row.put("module", feature.getModuleCode());
            row.put("licensesGranting", granted);
            rows.add(row);
        });
        return rows;
    }

    /** Current usage against every configured limit for a tenant. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> usageLimits(Long tenantId) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (UsageCheck check : usageService.checkAll(tenantId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("limitType", check.limitType());
            row.put("current", check.current());
            row.put("limit", check.unlimited() ? "unlimited" : check.limit());
            row.put("allowed", check.allowed());
            rows.add(row);
        }
        return rows;
    }

    /** Feature/limit comparison across plans. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> planComparison() {
        List<Map<String, Object>> rows = new ArrayList<>();
        planRepository.findAllByOrderByDisplayOrderAsc().forEach(plan ->
                planRepository.findWithDetailsById(plan.getId()).ifPresent(detail -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("planCode", detail.getCode());
                    row.put("name", detail.getName());
                    row.put("billingCycle", detail.getBillingCycle() == null
                            ? null : detail.getBillingCycle().getCode());
                    row.put("durationDays", detail.getDurationDays());
                    row.put("features", detail.getFeatures().stream()
                            .map(f -> f.getCode()).sorted().toList());
                    row.put("limits", detail.getLimits().stream().collect(Collectors.toMap(
                            l -> l.getLimitType().getCode(), l -> l.getLimitValue(), (a, b) -> a)));
                    rows.add(row);
                }));
        return rows;
    }

    /** Renders any of the row-shaped reports above as RFC-4180 CSV. */
    public String toCsv(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        StringBuilder csv = new StringBuilder(String.join(",", headers)).append("\n");
        for (Map<String, Object> row : rows) {
            csv.append(headers.stream()
                    .map(h -> escape(row.get(h)))
                    .collect(Collectors.joining(","))).append("\n");
        }
        return csv.toString();
    }

    private String escape(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
