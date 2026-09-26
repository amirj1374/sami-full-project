package com.sami.app.release;

import com.sami.app.organization.dto.OrganizationContextDtos;
import com.sami.app.organization.service.OrganizationScopeService;
import com.sami.app.product.dto.ProductVariantDtos;
import com.sami.app.product.service.ProductVariantService;
import com.sami.app.purchasing.service.GoodsReceiptService;
import com.sami.app.purchasing.service.PurchaseOrderService;
import com.sami.app.sales.delivery.DeliveryDtos;
import com.sami.app.sales.delivery.DeliveryService;
import com.sami.app.sales.invoice.SalesInvoiceDtos;
import com.sami.app.sales.invoice.SalesInvoiceService;
import com.sami.app.sales.order.SalesOrderDtos;
import com.sami.app.sales.order.SalesOrderService;
import com.sami.app.supplier.dto.SupplierDtos;
import com.sami.app.supplier.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Real PostgreSQL regression for the phone-store purchase, receipt, sale and issue boundary. */
class PhonePurchaseToSalePostgresAcceptanceIT extends SalesBusinessFixturePostgresIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired OrganizationScopeService organizationScope;
    @Autowired ProductVariantService variants;
    @Autowired SupplierService suppliers;
    @Autowired PurchaseOrderService purchaseOrders;
    @Autowired GoodsReceiptService receipts;
    @Autowired SalesOrderService orders;
    @Autowired DeliveryService deliveries;
    @Autowired SalesInvoiceService invoices;

    @Override @Disabled("The cross-module scenario owns its isolated bootstrap")
    void shouldCreateAndActivateTenant() { }

    @Override @Disabled("The cross-module scenario owns its isolated bootstrap")
    void shouldBootstrapUserCompanyAndBranchGrants() { }

    @Override @Disabled("The cross-module scenario owns its isolated bootstrap")
    void shouldCreateTenantAndCompany() { }

    @Test
    void phoneVariantImeiFlowsFromPurchaseReceiptToIssuedSaleAndReceivable() {
        var data = bootstrap();
        SalesSecurityTestSupport.authenticateTenantUser(1L, data.tenantId(), "admin@sami.local",
                "organization:create", "organization:edit", "crm:create", "product:create",
                "inventory:manage", "purchasing:create", "purchasing:edit", "purchasing:approve",
                "sales:create", "sales:confirm", "sales:payment");
        organizationScope.select(new OrganizationContextDtos.SelectRequest(data.companyId(), data.branchId()));

        var variant = variants.create(data.productId(), new ProductVariantDtos.Request(
                "IPH16P-256-BLK", "256GB / Black", "IPH16P-256-BLK", "ACTIVE"));
        Long supplierType = jdbc.queryForObject("select id from sup_types order by id limit 1", Long.class);
        var supplier = suppliers.create(new SupplierDtos.SupplierRequest("Phone Supplier", "Phone Supplier", null,
                null, null, null, null, null, null, null, null, null, null, null, supplierType,
                null, null, BigDecimal.ZERO, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), true, null));

        var purchase = purchaseOrders.create(data.companyId(), data.branchId(), supplier.supplier().id(), List.of(Map.of(
                "productId", data.productId(), "variantId", variant.id(), "quantity", BigDecimal.ONE,
                "unitPrice", new BigDecimal("100000.00"))), "Phone lifecycle acceptance");
        Long purchaseId = ((Number) purchase.get("id")).longValue();
        purchaseOrders.submit(purchaseId);
        purchaseOrders.approve(purchaseId);
        Map<?, ?> purchaseLineState = (Map<?, ?>) ((List<?>) purchaseOrders.get(purchaseId).get("lines")).getFirst();
        Long purchaseLine = ((Number) purchaseLineState.get("id")).longValue();

        String imei = "359881234567890";
        Map<String, Object> receipt = receipts.create(data.companyId(), data.branchId(), purchaseId, data.warehouseId(), List.of(Map.of(
                "purchaseOrderLineId", purchaseLine, "receivedQuantity", BigDecimal.ONE, "unitCost", new BigDecimal("100000.00"),
                "serials", List.of(Map.of("serialNumber", "PHONE-IMEI-001", "imei", imei)))), "Phone received");
        Long receiptId = ((Number) receipt.get("id")).longValue();
        assertEquals(1, jdbc.queryForObject("select count(*) from inventory_serial_units where tenant_id=? and product_id=? and variant_id=? and imei=? and status='AVAILABLE'", Integer.class, data.tenantId(), data.productId(), variant.id(), imei));
        assertEquals(0, jdbc.queryForObject("select on_hand from inventory_balances where tenant_id=? and warehouse_id=? and location_id=? and product_id=? and variant_id=?", BigDecimal.class, data.tenantId(), data.warehouseId(), data.locationId(), data.productId(), variant.id()).compareTo(BigDecimal.ONE));

        var order = orders.create(new SalesOrderDtos.Request(data.companyId(), data.branchId(), data.customerId(), "IRR", "Phone sale", List.of(
                new SalesOrderDtos.LineRequest(data.productId(), variant.id(), "PHONE-IMEI-001", imei, BigDecimal.ONE, new BigDecimal("125000.00"), BigDecimal.ZERO)), null));
        orders.confirm(order.id());
        assertEquals(0, jdbc.queryForObject("select reserved from inventory_balances where tenant_id=? and warehouse_id=? and location_id=? and product_id=? and variant_id=?", BigDecimal.class, data.tenantId(), data.warehouseId(), data.locationId(), data.productId(), variant.id()).compareTo(BigDecimal.ONE));
        assertEquals(1, jdbc.queryForObject("select count(*) from inventory_movements where tenant_id=? and product_id=? and variant_id=? and movement_type='RESERVE'", Integer.class, data.tenantId(), data.productId(), variant.id()));

        var delivery = deliveries.create(order.id(), new DeliveryDtos.Request(data.companyId(), data.branchId(), "Phone delivery", List.of(new DeliveryDtos.LineRequest(order.lines().getFirst().id(), BigDecimal.ONE))));
        deliveries.confirm(delivery.id());
        assertEquals("ISSUED", jdbc.queryForObject("select status from inventory_serial_units where tenant_id=? and imei=?", String.class, data.tenantId(), imei));
        assertEquals(0, jdbc.queryForObject("select on_hand from inventory_balances where tenant_id=? and warehouse_id=? and location_id=? and product_id=? and variant_id=?", BigDecimal.class, data.tenantId(), data.warehouseId(), data.locationId(), data.productId(), variant.id()).compareTo(BigDecimal.ZERO));
        assertEquals(1, jdbc.queryForObject("select count(*) from inventory_movements where tenant_id=? and product_id=? and variant_id=? and movement_type='ISSUE'", Integer.class, data.tenantId(), data.productId(), variant.id()));

        var invoice = invoices.create(new SalesInvoiceDtos.Request(order.id(), data.companyId(), data.branchId(), "IRR", "Phone invoice", List.of(new SalesInvoiceDtos.LineRequest(delivery.lines().getFirst().id(), BigDecimal.ONE))));
        invoices.issue(invoice.id());
        invoices.issue(invoice.id());
        assertEquals(1, jdbc.queryForObject("select count(*) from accounting_receivables where tenant_id=? and sales_invoice_id=?", Integer.class, data.tenantId(), invoice.id()));
        assertEquals(variant.id(), jdbc.queryForObject("select variant_id from sales_invoice_lines where invoice_id=?", Long.class, invoice.id()));
        assertEquals(receiptId, jdbc.queryForObject("select source_id from inventory_serial_units where tenant_id=? and imei=?", Long.class, data.tenantId(), imei));
    }
}
