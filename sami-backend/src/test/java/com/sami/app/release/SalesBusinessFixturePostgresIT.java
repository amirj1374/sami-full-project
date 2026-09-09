package com.sami.app.release;

import com.sami.app.licensing.domain.Tenant;
import com.sami.app.licensing.service.TenantService;
import com.sami.app.organization.service.CompanyService;
import com.sami.app.organization.service.BranchService;
import com.sami.app.organization.service.OrganizationGrantService;
import com.sami.app.organization.dto.OrganizationGrantDtos.AssignmentRequest;
import com.sami.app.organization.dto.OrganizationGrantDtos.BranchGrantRequest;
import com.sami.app.organization.dto.CompanyDtos.CompanyRequest;
import com.sami.app.organization.dto.BranchDtos;
import com.sami.app.crm.dto.CustomerRequest;
import com.sami.app.crm.dto.LookupRequests.TypeRequest;
import com.sami.app.crm.service.CrmConfigService;
import com.sami.app.crm.service.CustomerService;
import com.sami.app.contact.service.ContactWriteService;
import com.sami.app.inventory.dto.InventoryDtos.WarehouseRequest;
import com.sami.app.inventory.service.InventoryWarehouseService;
import com.sami.app.product.dto.CreateProductRequest;
import com.sami.app.product.service.ProductService;
import com.sami.app.treasury.dto.TreasuryDtos.AccountRequest;
import com.sami.app.treasury.service.TreasuryService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** First real lifecycle proof: tenant creation and activation through application services. */
public class SalesBusinessFixturePostgresIT extends PostgresApplicationFixture {
    public record Data(Long tenantId, Long companyId, Long branchId, Long userId, Long customerId,
                       Long productId, Long warehouseId, Long locationId, Long treasuryAccountId) { }
    @Autowired TenantService tenants;
    @Autowired CompanyService companies;
    @Autowired BranchService branches;
    @Autowired JdbcTemplate jdbc;
    @Autowired OrganizationGrantService grants;
    @Autowired CrmConfigService crmConfig;
    @Autowired CustomerService customers;
    @Autowired ContactWriteService contactWrites;
    @Autowired ProductService products;
    @Autowired InventoryWarehouseService warehouses;
    @Autowired TreasuryService treasury;

    @AfterEach void clearSecurity() { SalesSecurityTestSupport.clear(); }

    /** Reusable real-service bootstrap for downstream PostgreSQL acceptance tests. */
    public Data bootstrap() {
        Tenant tenant = createTenant();
        SalesSecurityTestSupport.authenticateTenantUser(1L, tenant.getId(), "admin@sami.local",
                "organization:create", "organization:edit", "crm:create", "product:create",
                "inventory:manage", "treasury:manage", "sales:create", "sales:confirm", "sales:payment");
        var company = companies.create(new CompanyRequest("FIX-COMPANY-" + tenant.getId(), "Fixture Company", null, null, null, "IRR", "Asia/Tehran", "fa", 1, null, null, null, null, null, null, null, null, true, 0, null));
        Long branchType = jdbc.queryForObject("select id from branch_types where is_default and tenant_id is null limit 1", Long.class);
        var branch = branches.create(company.id(), new BranchDtos.Request("FIX-BRANCH-" + tenant.getId(), "Fixture Branch", branchType, true, 1, null));
        Long role = jdbc.queryForObject("select id from roles order by id limit 1", Long.class);
        var assignment = grants.assign( new AssignmentRequest(1L, company.id(), role)); grants.grant(assignment.id(), new BranchGrantRequest(branch.id()));
        var type = crmConfig.createType(new TypeRequest("fixture-customer-" + tenant.getId(), "Fixture Customer", null, true, 1));
        var customer = customers.create(new CustomerRequest(null, null, "Fixture Customer", null, null, null, null, null, null, null, type.id(), null, null, null, null, null, false, null));
        contactWrites.createCustomerContact(customer.customer().id(), customer.customer().displayName());
        var warehouse = warehouses.create(new WarehouseRequest(null, null, "FIX-WH-" + tenant.getId(), "Fixture Warehouse", null, "STANDARD", true, true, 1));
        var product = products.create(new CreateProductRequest("Fixture Product", "FIX-" + tenant.getId(), null, new BigDecimal("100.00"), 1, true, false));
        var location = warehouses.locations(warehouse.id()).getFirst();
        Long accountType = treasury.accountTypes().stream().filter(x -> x.active()).findFirst().orElseThrow().id();
        var account = treasury.createAccount(new AccountRequest(company.id(), branch.id(), accountType, "FIX-" + tenant.getId(), "Fixture Cash", "IRR", BigDecimal.ZERO, true, 1L, null, null, null, null, null, null, null, true));
        return new Data(tenant.getId(), company.id(), branch.id(), 1L, customer.customer().id(), product.id(), warehouse.id(), location.id(), account.id());
    }

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
        // The V16 bootstrap user is seeded for the initial tenant. Re-home that
        // disposable test actor to the tenant created by this test so the real
        // organization grant foreign-key/scope checks can execute.
        jdbc.update("delete from user_branch_grants where assignment_id in (select id from user_company_roles where user_id=?)", 1L);
        jdbc.update("delete from user_company_roles where user_id=?", 1L);
        jdbc.update("delete from user_organization_contexts where user_id=?", 1L);
        jdbc.update("update users set tenant_id=? where id=?", activated.getId(), 1L);
        return activated;
    }
}
