package com.sami.app.release;

import com.sami.app.licensing.domain.Tenant;
import com.sami.app.licensing.service.TenantService;
import com.sami.app.organization.service.CompanyService;
import com.sami.app.organization.service.BranchService;
import com.sami.app.organization.service.OrganizationGrantService;
import com.sami.app.organization.dto.OrganizationGrantDtos.AssignmentRequest;
import com.sami.app.organization.dto.OrganizationGrantDtos.BranchGrantRequest;
import com.sami.app.organization.dto.CompanyDtos.CompanyRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** First real lifecycle proof: tenant creation and activation through application services. */
class SalesBusinessFixturePostgresIT extends PostgresApplicationFixture {
    @Autowired TenantService tenants;
    @Autowired CompanyService companies;
    @Autowired BranchService branches;
    @Autowired JdbcTemplate jdbc;
    @Autowired OrganizationGrantService grants;

    @AfterEach void clearSecurity() { SalesSecurityTestSupport.clear(); }

    @Test void shouldCreateAndActivateTenant() {
        Tenant tenant = createTenant();
        assertEquals("active", tenant.getStatus().getCode());
    }

    @Test void shouldBootstrapUserCompanyAndBranchGrants() {
        Tenant tenant = createTenant();
        SalesSecurityTestSupport.authenticateTenantUser(1L, tenant.getId(), "admin@sami.local", "organization:create", "organization:edit");
        CompanyRequest request = new CompanyRequest("GRANT-COMPANY-" + tenant.getId(), "Grant Company", null, null, null, "IRR", "Asia/Tehran", "fa", 1, null, null, null, null, null, null, null, null, true, 0, null);
        var company = companies.create(request);
        Long branchTypeId = jdbc.queryForObject("select id from branch_types where is_default and tenant_id is null limit 1", Long.class);
        var branch = branches.create(company.id(), new com.sami.app.organization.dto.BranchDtos.Request("GRANT-BRANCH-" + tenant.getId(), "Grant Branch", branchTypeId, true, 1, null));
        Long roleId = jdbc.queryForObject("select id from roles order by id limit 1", Long.class);
        var assignment = grants.assign(new AssignmentRequest(1L, company.id(), roleId));
        var withBranch = grants.grant(assignment.id(), new BranchGrantRequest(branch.id()));
        assertEquals(tenant.getId(), jdbc.queryForObject("select tenant_id from user_company_roles where id=?", Long.class, assignment.id()));
        assertEquals(1, withBranch.branchIds().stream().filter(branch.id()::equals).count());
    }

    @Test void shouldCreateTenantAndCompany() {
        Tenant tenant = createTenant();
        // V4 bootstrap seeds the admin user with id 1; organization audit rows
        // enforce a foreign key to users, so use that real persisted actor.
        SalesSecurityTestSupport.authenticateTenantUser(1L, tenant.getId(), "admin@sami.local", "organization:create", "organization:edit");
        CompanyRequest request = new CompanyRequest("IT-COMPANY-" + tenant.getId(), "SAMI Integration Company", null, null, null, "IRR", "Asia/Tehran", "fa", 1, null, null, null, null, null, null, null, null, true, 0, null);
        var company = companies.create(request);
        assertNotNull(company.id());
        assertEquals(tenant.getId(), jdbc.queryForObject("select tenant_id from companies where id=?", Long.class, company.id()));
        assertEquals(1, companies.list().stream().filter(c -> c.id().equals(company.id())).count());
        Long branchTypeId = jdbc.queryForObject("select id from branch_types where is_default and tenant_id is null limit 1", Long.class);
        var branch = branches.create(company.id(), new com.sami.app.organization.dto.BranchDtos.Request(
                "IT-BRANCH-" + tenant.getId(), "SAMI Integration Branch", branchTypeId, true, 1, null));
        assertNotNull(branch.id());
        assertEquals(tenant.getId(), jdbc.queryForObject("select tenant_id from branches where id=?", Long.class, branch.id()));
        assertEquals(company.id(), jdbc.queryForObject("select company_id from branches where id=?", Long.class, branch.id()));
        assertEquals(1, branches.list(company.id()).stream().filter(b -> b.id().equals(branch.id())).count());
    }

    private Tenant createTenant() {
        SalesSecurityTestSupport.authenticatePlatformTenantUser(9001L, 1L, "platform@sami.test");
        String code = "it-" + UUID.randomUUID().toString().replace("-", "");
        Tenant created = tenants.create(code, "SAMI Integration Tenant", "", "platform@sami.test", Map.of());
        assertNotNull(created.getId());
        Tenant activated = tenants.activate(created.getId());
        return activated;
    }
}
