package com.sami.app.accounting.service;

import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Tenant-scoped, effective-dated tax policy configuration. */
@Service
@RequiredArgsConstructor
public class TaxPolicyService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenants;

    @Transactional
    public long define(Long companyId, Long branchId, String code, String name,
                       String policyType, BigDecimal rate, boolean exempt,
                       LocalDate effectiveFrom, LocalDate effectiveTo, Long createdBy) {
        if (code == null || code.isBlank() || name == null || name.isBlank()
                || policyType == null || effectiveFrom == null
                || (effectiveTo != null && effectiveTo.isBefore(effectiveFrom))) {
            throw new IllegalArgumentException("Tax policy identity and effective dates are required");
        }
        BigDecimal actualRate = rate == null ? BigDecimal.ZERO : rate;
        if (actualRate.signum() < 0 || (exempt && !"EXEMPTION".equals(policyType))) {
            throw new IllegalArgumentException("Invalid tax policy configuration");
        }
        Long tenant = tenants.requireTenantId();
        return jdbc.queryForObject("insert into accounting_tax_policies(tenant_id,company_id,branch_id,code,name,policy_type,rate,exempt,effective_from,effective_to,created_by) values(?,?,?,?,?,?,?,?,?,?,?) returning id",
                Long.class, tenant, companyId, branchId, code.trim(), name.trim(), policyType,
                actualRate, exempt, effectiveFrom, effectiveTo, createdBy);
    }

    /** Resolves the policy active on a document date without rewriting history. */
    @Transactional(readOnly = true)
    public Policy resolve(Long companyId, Long branchId, String code, LocalDate date) {
        Long tenant = tenants.requireTenantId();
        return jdbc.query("select id,policy_type,rate,exempt,effective_from,effective_to from accounting_tax_policies where tenant_id=? and company_id is not distinct from ? and branch_id is not distinct from ? and code=? and effective_from<=? and (effective_to is null or effective_to>=?) order by effective_from desc limit 1",
                rs -> rs.next() ? new Policy(rs.getLong("id"), rs.getString("policy_type"), rs.getBigDecimal("rate"), rs.getBoolean("exempt"), rs.getObject("effective_from", LocalDate.class), rs.getObject("effective_to", LocalDate.class)) : null,
                tenant, companyId, branchId, code, date, date);
    }

    public record Policy(long id, String policyType, BigDecimal rate, boolean exempt,
                         LocalDate effectiveFrom, LocalDate effectiveTo) {}
}
