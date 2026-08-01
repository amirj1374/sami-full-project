package com.sami.app.crm.service;

import com.sami.app.crm.dto.CrmStatsResponse;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Aggregate customer counts for dashboards and reports (new customers,
 * customers by status/type/source). List-shaped reports — VIPs, top buyers,
 * inactive customers — are served by segments over the same filter engine.
 */
@Service
@RequiredArgsConstructor
public class CrmStatsService {

    private final CustomerRepository customerRepository;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public CrmStatsResponse stats() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        Long tenantId = tenantContext.requireTenantId();
        return new CrmStatsResponse(
                customerRepository.countByTenantIdAndMergedIntoIsNull(tenantId),
                customerRepository.countByTenantIdAndCreatedAtAfterAndMergedIntoIsNull(tenantId, thirtyDaysAgo),
                buckets(customerRepository.countByStatus(tenantId)),
                buckets(customerRepository.countByType(tenantId)),
                buckets(customerRepository.countBySource(tenantId)));
    }

    private List<CrmStatsResponse.Bucket> buckets(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new CrmStatsResponse.Bucket(
                        row[0] != null ? String.valueOf(row[0]) : "Unknown",
                        ((Number) row[1]).longValue()))
                .sorted((a, b) -> Long.compare(b.count(), a.count()))
                .toList();
    }
}
