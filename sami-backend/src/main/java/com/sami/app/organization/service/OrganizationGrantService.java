package com.sami.app.organization.service;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.organization.dto.OrganizationGrantDtos.*;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class OrganizationGrantService {
 private final TenantContext tenants; private final JdbcTemplate jdbc; private final FoundationAuditService audit;
 @Transactional public AssignmentResponse assign(AssignmentRequest request) {
  Long tenant=tenants.requireTenantId(); require("users",request.userId(),tenant); require("companies",request.companyId(),tenant);
  if (!Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from roles where id=?)",Boolean.class,request.roleId()))) deny("Role is unavailable");
  Long id=jdbc.queryForObject("insert into user_company_roles(tenant_id,user_id,company_id,role_id,is_active) values(?,?,?,?,true) on conflict(tenant_id,user_id,company_id) do update set role_id=excluded.role_id,is_active=true,updated_at=now(),version=user_company_roles.version+1 returning id",Long.class,tenant,request.userId(),request.companyId(),request.roleId());
  audit.record(tenant,request.companyId(),null,"USER_COMPANY_ROLE",id,"ASSIGNED",null,Map.of("userId",request.userId(),"roleId",request.roleId()));
  return response(tenant,id);
 }
 @Transactional public AssignmentResponse grant(Long assignmentId,BranchGrantRequest request) {
  Long tenant=tenants.requireTenantId(); AssignmentResponse assignment=response(tenant,assignmentId);
  Integer valid=jdbc.queryForObject("select count(*) from branches where id=? and tenant_id=? and company_id=? and is_active",Integer.class,request.branchId(),tenant,assignment.companyId());
  if(valid==null||valid==0) deny("Branch is outside the assigned Company");
  Long id=jdbc.queryForObject("insert into user_branch_grants(tenant_id,assignment_id,branch_id,is_active) values(?,?,?,true) on conflict(assignment_id,branch_id) do update set is_active=true,updated_at=now(),version=user_branch_grants.version+1 returning id",Long.class,tenant,assignmentId,request.branchId());
  audit.record(tenant,assignment.companyId(),request.branchId(),"USER_BRANCH_GRANT",id,"GRANTED",null,Map.of("assignmentId",assignmentId));
  return response(tenant,assignmentId);
 }
 @Transactional public void revokeBranch(Long assignmentId,Long branchId) { Long tenant=tenants.requireTenantId(); AssignmentResponse a=response(tenant,assignmentId); int n=jdbc.update("update user_branch_grants set is_active=false,updated_at=now(),version=version+1 where tenant_id=? and assignment_id=? and branch_id=? and is_active",tenant,assignmentId,branchId); if(n==0) deny("Active Branch grant was not found"); audit.record(tenant,a.companyId(),branchId,"USER_BRANCH_GRANT",null,"REVOKED",null,Map.of("assignmentId",assignmentId)); }
 @Transactional public void revokeAssignment(Long assignmentId) { Long tenant=tenants.requireTenantId(); AssignmentResponse a=response(tenant,assignmentId); jdbc.update("update user_company_roles set is_active=false,updated_at=now(),version=version+1 where tenant_id=? and id=?",tenant,assignmentId); jdbc.update("update user_branch_grants set is_active=false,updated_at=now(),version=version+1 where tenant_id=? and assignment_id=?",tenant,assignmentId); audit.record(tenant,a.companyId(),null,"USER_COMPANY_ROLE",assignmentId,"REVOKED",null,Map.of("userId",a.userId())); }
 @Transactional(readOnly=true) public List<AssignmentResponse> list(Long userId) { Long tenant=tenants.requireTenantId(); require("users",userId,tenant); return jdbc.query("select id from user_company_roles where tenant_id=? and user_id=? order by company_id",(rs,n)->response(tenant,rs.getLong(1)),tenant,userId); }
 private AssignmentResponse response(Long tenant,Long id) { return jdbc.queryForObject("select id,user_id,company_id,role_id,is_active from user_company_roles where tenant_id=? and id=?",(rs,n)->new AssignmentResponse(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getBoolean(5),jdbc.queryForList("select branch_id from user_branch_grants where tenant_id=? and assignment_id=? and is_active",Long.class,tenant,rs.getLong(1))),tenant,id); }
 private void require(String table,Long id,Long tenant) { Integer n=jdbc.queryForObject("select count(*) from "+table+" where id=? and tenant_id=?",Integer.class,id,tenant); if(n==null||n==0) deny("Resource is outside this tenant"); }
 private static void deny(String m){throw new ApiException(ErrorCode.ACCESS_DENIED,m);}
}
