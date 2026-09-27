package com.sami.app.organization.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.tenancy.TenantContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organization/branch-types")
@RequiredArgsConstructor
public class BranchTypeController {
    private final JdbcTemplate jdbc;
    private final TenantContext tenantContext;

    public record Option(Long id, String code, String name, boolean allowsInventory, boolean allowsSales) {}

    @GetMapping
    @PreAuthorize("@authz.has('organization:view')")
    public ApiResponse<List<Option>> list() {
        Long tenantId = tenantContext.requireTenantId();
        return ApiResponse.ok(jdbc.query("select id, code, name, allows_inventory, allows_sales from branch_types where tenant_id is null or tenant_id=? order by is_default desc, name", (rs, i) ->
                new Option(rs.getLong("id"), rs.getString("code"), rs.getString("name"), rs.getBoolean("allows_inventory"), rs.getBoolean("allows_sales")), tenantId));
    }
}
