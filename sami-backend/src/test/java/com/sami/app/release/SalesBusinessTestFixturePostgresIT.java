package com.sami.app.release;

import com.sami.app.crm.dto.CustomerRequest;
import com.sami.app.crm.dto.LookupRequests.TypeRequest;
import com.sami.app.crm.service.CrmConfigService;
import com.sami.app.crm.service.CustomerService;
import com.sami.app.inventory.dto.InventoryDtos.WarehouseRequest;
import com.sami.app.inventory.service.InventoryWarehouseService;
import com.sami.app.licensing.domain.Tenant;
import com.sami.app.licensing.service.TenantService;
import com.sami.app.organization.dto.BranchDtos;
import com.sami.app.organization.dto.CompanyDtos.CompanyRequest;
import com.sami.app.organization.dto.OrganizationGrantDtos.AssignmentRequest;
import com.sami.app.organization.dto.OrganizationGrantDtos.BranchGrantRequest;
import com.sami.app.organization.service.BranchService;
import com.sami.app.organization.service.CompanyService;
import com.sami.app.organization.service.OrganizationGrantService;
import com.sami.app.product.dto.CreateProductRequest;
import com.sami.app.product.service.ProductService;
import com.sami.app.treasury.dto.TreasuryDtos.AccountRequest;
import com.sami.app.treasury.service.TreasuryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SalesBusinessTestFixturePostgresIT extends PostgresApplicationFixture {
    @Autowired TenantService tenants;
    @Autowired CompanyService companies;
    @Autowired BranchService branches;
    @Autowired OrganizationGrantService grants;
    @Autowired CrmConfigService crmConfig;
    @Autowired CustomerService customers;
    @Autowired ProductService products;
    @Autowired InventoryWarehouseService warehouses;
    @Autowired TreasuryService treasury;
    @Autowired JdbcTemplate jdbc;

    @AfterEach void clearSecurity() { SalesSecurityTestSupport.clear(); }

    @Test
    void createsCompleteBusinessFixtureWithOwnership() {
        Tenant tenant = createTenant();
        SalesSecurityTestSupport.authenticateTenantUser(1L, tenant.getId(), "admin@sami.local",
                "organization:create", "organization:edit", "crm:create", "product:create",
                "inventory:manage", "treasury:manage");

        var company = companies.create(new CompanyRequest("FIX-COMPANY-" + tenant.getId(),
                "Fixture Company", null, null, null, "IRR", "Asia/Tehran", "fa", 1,
                null, null, null, null, null, null, null, null, true, 0, null));
        Long branchType = jdbc.queryForObject("select id from branch_types where is_default and tenant_id is null limit 1", Long.class);
        var branch = branches.create(company.id(), new BranchDtos.Request("FIX-BRANCH-" + tenant.getId(),
                "Fixture Branch", branchType, true, 1, null));
        Long role = jdbc.queryForObject("select id from roles order by id limit 1", Long.class);
        var assignment = grants.assign(new AssignmentRequest(1L, company.id(), role));
        grants.grant(assignment.id(), new BranchGrantRequest(branch.id()));

        var type = crmConfig.createType(new TypeRequest("fixture-customer", "Fixture Customer", null, true, 1));
        var customer = customers.create(new CustomerRequest(null, null, "Fixture Customer", null, null,
                null, null, null, null, null, type.id(), null, null, null, null, null, false, null));
        var warehouse = warehouses.create(new WarehouseRequest(null, null,
                "FIX-WH-" + tenant.getId(), "Fixture Warehouse", null, "STANDARD", true, true, 1));
        var location = warehouses.locations(warehouse.id()).getFirst();
        var product = products.create(new CreateProductRequest("Fixture Product", "FIX-" + tenant.getId(),
                null, new BigDecimal("10.00"), 3, true, false));
        Long accountType = treasury.accountTypes().stream().filter(x -> x.active()).findFirst().orElseThrow().id();
        var account = treasury.createAccount(new AccountRequest(accountType, "FIX-" + tenant.getId(),
                "Fixture Cash", "IRR", BigDecimal.ZERO, true, 1L,
                null, null, null, null, null, null, null, true));

        assertEquals(tenant.getId(), jdbc.queryForObject("select tenant_id from companies where id=?", Long.class, company.id()));
        assertEquals(company.id(), jdbc.queryForObject("select company_id from branches where id=?", Long.class, branch.id()));
        assertEquals(tenant.getId(), jdbc.queryForObject("select tenant_id from customers where id=?", Long.class, customer.customer().id()));
        assertEquals(tenant.getId(), jdbc.queryForObject("select tenant_id from products where id=?", Long.class, product.id()));
        assertEquals(tenant.getId(), jdbc.queryForObject("select tenant_id from pur_warehouses where id=?", Long.class, warehouse.id()));
        assertEquals(tenant.getId(), jdbc.queryForObject("select tenant_id from inventory_locations where id=?", Long.class, location.id()));
        assertEquals(tenant.getId(), jdbc.queryForObject("select tenant_id from treasury_accounts where id=?", Long.class, account.id()));
        assertTrue(jdbc.queryForObject("select count(*) from inventory_balances where tenant_id=? and product_id=?", Integer.class, tenant.getId(), product.id()) > 0);
    }

    private Tenant createTenant() {
        SalesSecurityTestSupport.authenticatePlatformTenantUser(9001L, 1L, "platform@sami.test");
        Tenant tenant = tenants.create("it-" + UUID.randomUUID().toString().replace("-", ""),
                "SAMI Integration Tenant", "", "platform@sami.test", Map.of());
        tenant = tenants.activate(tenant.getId());
        jdbc.update("delete from user_branch_grants where assignment_id in (select id from user_company_roles where user_id=?)", 1L);
        jdbc.update("delete from user_company_roles where user_id=?", 1L);
        jdbc.update("update users set tenant_id=? where id=?", tenant.getId(), 1L);
        return tenant;
    }
}
