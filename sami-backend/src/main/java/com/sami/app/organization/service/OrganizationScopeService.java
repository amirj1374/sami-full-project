package com.sami.app.organization.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.organization.dto.OrganizationContextDtos.*;
import com.sami.app.security.CurrentActor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationScopeService {
    private final TenantContext tenantContext;
    private final JdbcTemplate jdbc;
    private final FoundationAuditService audit;

    @Transactional(readOnly = true)
    public ContextResponse current() {
        Long tenant = tenantContext.requireTenantId(); Long user = requireActor();
        Long[] saved = jdbc.query("select company_id, branch_id from user_organization_contexts where tenant_id=? and user_id=?",
                rs -> rs.next() ? new Long[]{rs.getObject(1, Long.class), rs.getObject(2, Long.class)} : new Long[]{null, null}, tenant, user);
        List<CompanyChoice> companies = companies(tenant, user);
        Long companyId = saved[0] != null && companies.stream().anyMatch(c -> c.id().equals(saved[0])) ? saved[0] : null;
        List<BranchChoice> branches = companyId == null ? List.of() : branches(tenant, user, companyId);
        Long branchId = saved[1] != null && branches.stream().anyMatch(b -> b.id().equals(saved[1])) ? saved[1] : null;
        return new ContextResponse(companyId, branchId, companies, branches);
    }
    @Transactional
    public ContextResponse select(SelectRequest request) {
        Long tenant = tenantContext.requireTenantId(); Long user = requireActor();
        requireCompanyGrant(tenant, user, request.companyId());
        if (request.branchId() != null) requireBranchGrant(tenant, user, request.companyId(), request.branchId());
        jdbc.update("insert into user_organization_contexts(tenant_id,user_id,company_id,branch_id) values(?,?,?,?) on conflict(tenant_id,user_id) do update set company_id=excluded.company_id,branch_id=excluded.branch_id,updated_at=now(),version=user_organization_contexts.version+1",
                tenant, user, request.companyId(), request.branchId());
        audit.record(tenant, request.companyId(), request.branchId(), "ORGANIZATION_CONTEXT", user, "CONTEXT_SELECTED", null, java.util.Map.of("userId", user));
        return current();
    }
    public void requireScope(Long companyId, Long branchId) {
        Long tenant = tenantContext.requireTenantId(); Long user = requireActor();
        requireCompanyGrant(tenant, user, companyId);
        if (branchId != null) requireBranchGrant(tenant, user, companyId, branchId);
    }
    private List<CompanyChoice> companies(Long tenant, Long user) { return jdbc.query("select c.id,c.code,c.name from user_company_roles a join companies c on c.id=a.company_id and c.tenant_id=a.tenant_id where a.tenant_id=? and a.user_id=? and a.is_active and c.is_active order by c.display_order,c.name", (rs,n) -> new CompanyChoice(rs.getLong(1),rs.getString(2),rs.getString(3)), tenant,user); }
    private List<BranchChoice> branches(Long tenant, Long user, Long company) { return jdbc.query("select b.id,b.company_id,b.code,b.name from user_company_roles a join user_branch_grants g on g.assignment_id=a.id and g.tenant_id=a.tenant_id join branches b on b.id=g.branch_id and b.tenant_id=g.tenant_id where a.tenant_id=? and a.user_id=? and a.company_id=? and a.is_active and g.is_active and b.is_active order by b.display_order,b.name", (rs,n) -> new BranchChoice(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4)), tenant,user,company); }
    private void requireCompanyGrant(Long tenant, Long user, Long company) { if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from user_company_roles a join companies c on c.id=a.company_id and c.tenant_id=a.tenant_id where a.tenant_id=? and a.user_id=? and a.company_id=? and a.is_active and c.is_active)", Boolean.class, tenant,user,company))) deny(); }
    private void requireBranchGrant(Long tenant, Long user, Long company, Long branch) { if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from user_company_roles a join user_branch_grants g on g.assignment_id=a.id and g.tenant_id=a.tenant_id join branches b on b.id=g.branch_id and b.tenant_id=g.tenant_id where a.tenant_id=? and a.user_id=? and a.company_id=? and g.branch_id=? and a.is_active and g.is_active and b.is_active and b.company_id=a.company_id)", Boolean.class, tenant,user,company,branch))) deny(); }
    private Long requireActor() { Long id=CurrentActor.id(); if(id==null) deny(); return id; }
    private static void deny() { throw new ApiException(ErrorCode.ACCESS_DENIED, "Organization scope is not granted"); }
}
