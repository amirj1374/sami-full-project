package com.sami.app.organization.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.organization.domain.Branch;
import com.sami.app.organization.dto.BranchDtos.Request;
import com.sami.app.organization.dto.BranchDtos.Response;
import com.sami.app.organization.repository.BranchRepository;
import com.sami.app.organization.repository.CompanyRepository;
import com.sami.app.security.CurrentActor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BranchService {
    private final BranchRepository branches;
    private final CompanyRepository companies;
    private final TenantContext tenantContext;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<Response> list(Long companyId) {
        Long tenantId = tenantContext.requireTenantId();
        requireCompany(companyId, tenantId);
        return branches.findByTenantIdAndCompanyIdOrderByDisplayOrderAscNameAsc(tenantId, companyId).stream().map(Response::from).toList();
    }
    @Transactional
    public Response create(Long companyId, Request request) {
        Long tenantId = tenantContext.requireTenantId();
        requireCompany(companyId, tenantId); requireType(request.branchTypeId(), tenantId);
        if (branches.existsByTenantIdAndCompanyIdAndCodeIgnoreCase(tenantId, companyId, request.code().trim())) conflict();
        Branch branch = branches.save(Branch.builder().tenantId(tenantId).companyId(companyId).branchTypeId(request.branchTypeId())
                .code(request.code().trim()).name(request.name().trim()).isActive(request.active() == null || request.active())
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder()).build());
        audit(tenantId, companyId, branch.getId(), "BRANCH_CREATED");
        return Response.from(branch);
    }
    @Transactional
    public Response update(Long companyId, Long id, Request request) {
        Long tenantId = tenantContext.requireTenantId();
        requireCompany(companyId, tenantId); requireType(request.branchTypeId(), tenantId);
        Branch branch = branches.findByIdAndTenantIdAndCompanyId(id, tenantId, companyId).orElseThrow(() -> ResourceNotFoundException.of("Branch", id));
        if (request.expectedVersion() == null || !request.expectedVersion().equals(branch.getVersion())) conflict();
        if (branches.existsByTenantIdAndCompanyIdAndCodeIgnoreCaseAndIdNot(tenantId, companyId, request.code().trim(), id)) conflict();
        branch.setCode(request.code().trim()); branch.setName(request.name().trim()); branch.setBranchTypeId(request.branchTypeId());
        if (request.active() != null) branch.setActive(request.active());
        if (request.displayOrder() != null) branch.setDisplayOrder(request.displayOrder());
        branch = branches.save(branch); audit(tenantId, companyId, id, "BRANCH_UPDATED");
        return Response.from(branch);
    }
    private void requireCompany(Long id, Long tenantId) { if (!companies.existsByIdAndTenantId(id, tenantId)) throw ResourceNotFoundException.of("Company", id); }
    private void requireType(Long id, Long tenantId) {
        Integer count = jdbc.queryForObject("select count(*) from branch_types where id=? and (tenant_id is null or tenant_id=?)", Integer.class, id, tenantId);
        if (count == null || count == 0) throw new ApiException(ErrorCode.VALIDATION_FAILED, "Branch type is unavailable for this tenant");
    }
    private void audit(Long tenant, Long company, Long branch, String action) {
        jdbc.update("insert into foundation_audit_logs(tenant_id,company_id,branch_id,actor_id,actor_email,subject_type,subject_id,action) values(?,?,?,?,?,?,?,?)",
                tenant, company, branch, CurrentActor.id(), CurrentActor.email(), "BRANCH", branch, action);
    }
    private static void conflict() { throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Branch changed or code already exists"); }
}
