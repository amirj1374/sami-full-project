package com.sami.app.contact.service;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.contact.dto.ContactDtos.ContactResponse;
import com.sami.app.contact.dto.ContactDtos.LegacyMappingResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactQueryService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<ContactResponse> list() {
        return jdbc.query("""
                select c.id,c.identity_type,c.display_name,c.first_name,c.last_name,c.company_name,
                       c.national_code,c.legal_identifier,c.tax_number,c.email,c.phone,c.is_active,
                       c.merged_into_id,c.created_at,c.updated_at,
                       exists(select 1 from contact_customer_roles r where r.tenant_id=c.tenant_id and r.contact_id=c.id),
                       exists(select 1 from contact_supplier_roles r where r.tenant_id=c.tenant_id and r.contact_id=c.id)
                  from contacts c where c.tenant_id=? and c.merged_into_id is null order by c.display_name,c.id
                """, (rs, row) -> mapContact(rs, row), tenantContext.requireTenantId());
    }

    @Transactional(readOnly = true)
    public ContactResponse get(Long id) {
        Long tenant = tenantContext.requireTenantId();
        List<ContactResponse> matches = jdbc.query("""
                select c.id,c.identity_type,c.display_name,c.first_name,c.last_name,c.company_name,
                       c.national_code,c.legal_identifier,c.tax_number,c.email,c.phone,c.is_active,
                       c.merged_into_id,c.created_at,c.updated_at,
                       exists(select 1 from contact_customer_roles r where r.tenant_id=c.tenant_id and r.contact_id=c.id),
                       exists(select 1 from contact_supplier_roles r where r.tenant_id=c.tenant_id and r.contact_id=c.id)
                  from contacts c where c.tenant_id=? and c.id=?
                """, (rs, row) -> mapContact(rs, row), tenant, id);
        return matches.stream().findFirst().orElseThrow(() -> ResourceNotFoundException.of("Contact", id));
    }

    @Transactional(readOnly = true)
    public List<LegacyMappingResponse> mappings(Long contactId) {
        Long tenant = tenantContext.requireTenantId();
        // The tenant predicate is repeated on both sides of the lookup so a
        // caller can never use an id from another tenant as an oracle.
        return jdbc.query("""
                select m.id,m.legacy_type,m.legacy_id,m.match_method,m.review_required,m.contact_id
                  from legacy_contact_mappings m join contacts c on c.id=m.contact_id and c.tenant_id=m.tenant_id
                 where m.tenant_id=? and m.contact_id=? order by m.legacy_type,m.legacy_id
                """, (rs, row) -> new LegacyMappingResponse(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getBoolean(5), rs.getLong(6)), tenant, contactId);
    }

    private ContactResponse mapContact(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        List<String> roles = new java.util.ArrayList<>();
        if (rs.getBoolean(16)) roles.add("CUSTOMER");
        if (rs.getBoolean(17)) roles.add("SUPPLIER");
        return new ContactResponse(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                rs.getString(10), rs.getString(11), rs.getBoolean(12), rs.getObject(13, Long.class),
                roles, rs.getObject(14, java.time.Instant.class), rs.getObject(15, java.time.Instant.class));
    }
}
