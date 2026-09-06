package com.sami.app.contact.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.contact.dto.ContactDtos.MergeResponse;
import com.sami.app.contact.dto.ContactDtos.RoleRequest;
import com.sami.app.organization.service.FoundationAuditService;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Controlled Contact role links and merge; legacy rows are never deleted. */
@Service
@RequiredArgsConstructor
public class ContactWriteService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenantContext;
    private final FoundationAuditService audit;

    @Transactional
    public void addRole(Long contactId, RoleRequest request) {
        Long tenant = tenantContext.requireTenantId();
        requireContact(contactId, tenant);
        String role = request.role() == null ? "" : request.role().trim().toUpperCase(Locale.ROOT);
        if (request.legacyId() == null || !(role.equals("CUSTOMER") || role.equals("SUPPLIER"))) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A Customer or Supplier legacy role is required");
        }
        if (role.equals("CUSTOMER")) {
            requireLegacy("customers", request.legacyId(), tenant);
            jdbc.update("insert into contact_customer_roles(tenant_id,contact_id,customer_id) values(?,?,?) on conflict(customer_id) do update set contact_id=excluded.contact_id,updated_at=now(),version=contact_customer_roles.version+1", tenant, contactId, request.legacyId());
        } else {
            requireLegacy("suppliers", request.legacyId(), tenant);
            jdbc.update("insert into contact_supplier_roles(tenant_id,contact_id,supplier_id) values(?,?,?) on conflict(supplier_id) do update set contact_id=excluded.contact_id,updated_at=now(),version=contact_supplier_roles.version+1", tenant, contactId, request.legacyId());
        }
        audit.record(tenant, null, null, "CONTACT_ROLE", contactId, "ROLE_ADDED", null, Map.of("role", role, "legacyId", request.legacyId()));
    }

    @Transactional
    public MergeResponse merge(Long sourceId, Long targetId) {
        if (sourceId.equals(targetId)) throw new ApiException(ErrorCode.VALIDATION_FAILED, "A Contact cannot be merged into itself");
        Long tenant = tenantContext.requireTenantId();
        requireContact(sourceId, tenant); requireContact(targetId, tenant);
        Boolean sourceMerged = jdbc.queryForObject("select merged_into_id is not null from contacts where tenant_id=? and id=?", Boolean.class, tenant, sourceId);
        Boolean targetMerged = jdbc.queryForObject("select merged_into_id is not null from contacts where tenant_id=? and id=?", Boolean.class, tenant, targetId);
        if (Boolean.TRUE.equals(sourceMerged) || Boolean.TRUE.equals(targetMerged)) throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "Merged Contacts cannot participate in another merge");
        Integer customerConflict = jdbc.queryForObject("select count(*) from contact_customer_roles s join contact_customer_roles t on t.tenant_id=s.tenant_id and t.contact_id=? where s.tenant_id=? and s.contact_id=?", Integer.class, targetId, tenant, sourceId);
        Integer supplierConflict = jdbc.queryForObject("select count(*) from contact_supplier_roles s join contact_supplier_roles t on t.tenant_id=s.tenant_id and t.contact_id=? where s.tenant_id=? and s.contact_id=?", Integer.class, targetId, tenant, sourceId);
        if ((customerConflict != null && customerConflict > 0) || (supplierConflict != null && supplierConflict > 0)) throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "The target already owns one of the source Contact roles");
        int mappings = jdbc.update("update legacy_contact_mappings set contact_id=?,updated_at=now(),version=version+1 where tenant_id=? and contact_id=?", targetId, tenant, sourceId);
        jdbc.update("update contact_customer_roles set contact_id=?,updated_at=now(),version=version+1 where tenant_id=? and contact_id=?", targetId, tenant, sourceId);
        jdbc.update("update contact_supplier_roles set contact_id=?,updated_at=now(),version=version+1 where tenant_id=? and contact_id=?", targetId, tenant, sourceId);
        jdbc.update("update contacts set merged_into_id=?,is_active=false,updated_at=now(),version=version+1 where tenant_id=? and id=?", targetId, tenant, sourceId);
        audit.record(tenant, null, null, "CONTACT", targetId, "MERGED", "CONTROLLED_CONTACT_MERGE", Map.of("sourceId", sourceId, "targetId", targetId, "mappingsMoved", mappings, "sourcePreserved", true));
        return new MergeResponse(sourceId, targetId, mappings, true, "MERGED");
    }

    private void requireContact(Long id, Long tenant) {
        Integer count = jdbc.queryForObject("select count(*) from contacts where tenant_id=? and id=?", Integer.class, tenant, id);
        if (count == null || count == 0) throw ResourceNotFoundException.of("Contact", id);
    }
    private void requireLegacy(String table, Long id, Long tenant) {
        Integer count = jdbc.queryForObject("select count(*) from " + table + " where tenant_id=? and id=?", Integer.class, tenant, id);
        if (count == null || count == 0) throw ResourceNotFoundException.of("Legacy role", id);
    }
}
