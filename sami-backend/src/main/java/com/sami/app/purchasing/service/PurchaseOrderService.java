package com.sami.app.purchasing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.organization.service.FoundationAuditService;
import com.sami.app.organization.service.OrganizationScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {
    private final JdbcTemplate jdbc;
    private final TenantContext tenants;
    private final OrganizationScopeService scope;
    private final FoundationAuditService audit;

    @Transactional
    public Map<String,Object> create(Long company, Long branch, Long supplier, List<Map<String,Object>> lines, String notes) {
        Long tenant = tenants.requireTenantId();
        scope.requireScope(company, branch);
        if (supplier == null || jdbc.queryForObject("select count(*) from suppliers where id=?", Integer.class, supplier) == 0)
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Supplier not found");
        if (lines == null || lines.isEmpty()) throw new ApiException(ErrorCode.VALIDATION_FAILED, "Purchase order requires items");
        String number = "PO-" + jdbc.queryForObject("select nextval('purchase_order_number_seq')", Long.class);
        jdbc.update("insert into purchase_orders(tenant_id,company_id,branch_id,supplier_id,order_number,order_date,notes) values(?,?,?,?,?,?,?)", tenant, company, branch, supplier, number, LocalDate.now(), notes);
        Long id = jdbc.queryForObject("select id from purchase_orders where tenant_id=? and order_number=?", Long.class, tenant, number);
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String,Object> line : lines) {
            Object product = line.get("productId");
            BigDecimal quantity = decimal(line.get("quantity"));
            BigDecimal price = decimal(line.get("unitPrice"));
            if (product == null || quantity.signum() <= 0 || price.signum() < 0 || jdbc.queryForObject("select count(*) from products where id=? and active=true", Integer.class, product) == 0)
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid purchase order line");
            BigDecimal lineTotal = quantity.multiply(price); total = total.add(lineTotal);
            jdbc.update("insert into purchase_order_lines(tenant_id,purchase_order_id,product_id,quantity,unit_price,line_total) values(?,?,?,?,?,?)", tenant, id, product, quantity, price, lineTotal);
        }
        jdbc.update("update purchase_orders set subtotal=?,total=? where id=? and tenant_id=?", total, total, id, tenant);
        audit.record(tenant, company, branch, "PURCHASE_ORDER", id, "CREATED", "Purchase order created", Map.of("orderNumber", number));
        return get(id);
    }

    private BigDecimal decimal(Object value) { try { return new BigDecimal(Objects.requireNonNull(value).toString()); } catch (Exception e) { throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid numeric value"); } }

    @Transactional(readOnly = true)
    public Map<String,Object> get(Long id) {
        Long tenant = tenants.requireTenantId();
        Map<String,Object> order;
        try { order = jdbc.queryForMap("select * from purchase_orders where tenant_id=? and id=?", tenant, id); }
        catch (org.springframework.dao.EmptyResultDataAccessException e) { throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Purchase order not found"); }
        scope.requireScope(((Number) order.get("company_id")).longValue(), ((Number) order.get("branch_id")).longValue());
        order.put("lines", jdbc.queryForList("select * from purchase_order_lines where tenant_id=? and purchase_order_id=? order by id", tenant, id));
        return order;
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> list() {
        Long tenant = tenants.requireTenantId();
        var context = scope.current();
        if (context.companyId() == null) return List.of();
        if (context.branchId() == null)
            return jdbc.queryForList("select * from purchase_orders where tenant_id=? and company_id=? order by order_date desc,id desc", tenant, context.companyId());
        return jdbc.queryForList("select * from purchase_orders where tenant_id=? and company_id=? and branch_id=? order by order_date desc,id desc", tenant, context.companyId(), context.branchId());
    }

    @Transactional public void submit(Long id) { updateStatus(id, "DRAFT", "SUBMITTED"); }
    @Transactional public void approve(Long id) { updateStatus(id, "SUBMITTED", "APPROVED"); }
    @Transactional public void cancel(Long id) { updateStatus(id, "DRAFT", "CANCELLED"); }
    private void updateStatus(Long id, String from, String to) {
        Map<String,Object> order = get(id); Long tenant = tenants.requireTenantId();
        int changed = jdbc.update("update purchase_orders set status=?,updated_at=now(),version=version+1 where tenant_id=? and id=? and status=?", to, tenant, id, from);
        if (changed == 0) throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "Invalid Purchase Order status transition");
        audit.record(tenant, ((Number)order.get("company_id")).longValue(), ((Number)order.get("branch_id")).longValue(), "PURCHASE_ORDER", id, to, "Purchase order status changed", Map.of("from", from, "to", to));
    }
}
