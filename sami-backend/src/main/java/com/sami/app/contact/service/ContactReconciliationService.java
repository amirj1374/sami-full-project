package com.sami.app.contact.service;

import com.sami.app.common.tenancy.TenantContext;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Tenant-scoped, read-only reconciliation summary for the shared Contact foundation. */
@Service
@RequiredArgsConstructor
public class ContactReconciliationService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenantContext;

    public Map<String, Object> summary() {
        Long tenant = tenantContext.requireTenantId();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contacts", count("select count(*) from contacts where tenant_id=?", tenant));
        result.put("activeContacts", count("select count(*) from contacts where tenant_id=? and is_active=true", tenant));
        result.put("mergedContacts", count("select count(*) from contacts where tenant_id=? and merged_into_id is not null", tenant));
        result.put("customerRoles", count("select count(*) from contact_customer_roles where tenant_id=?", tenant));
        result.put("supplierRoles", count("select count(*) from contact_supplier_roles where tenant_id=?", tenant));
        result.put("legacyMappings", count("select count(*) from legacy_contact_mappings where tenant_id=?", tenant));
        result.put("reviewRequiredMappings", count("select count(*) from legacy_contact_mappings where tenant_id=? and review_required=true", tenant));
        result.put("unmappedCustomers", count("select count(*) from customers c where c.tenant_id=? and c.merged_into_id is null and not exists (select 1 from legacy_contact_mappings m where m.tenant_id=c.tenant_id and m.legacy_type='CUSTOMER' and m.legacy_id=c.id)", tenant));
        result.put("unmappedSuppliers", count("select count(*) from suppliers s where s.tenant_id=? and not exists (select 1 from legacy_contact_mappings m where m.tenant_id=s.tenant_id and m.legacy_type='SUPPLIER' and m.legacy_id=s.id)", tenant));
        result.put("candidateMerges", count("select count(*) from contact_merge_candidates where tenant_id=? and status='OPEN'", tenant));
        result.put("matchingPolicy", "EXACT_IDENTITY_ONLY");
        return result;
    }

    private long count(String sql, Long tenant) {
        Long value = jdbc.queryForObject(sql, Long.class, tenant);
        return value == null ? 0L : value;
    }
}
