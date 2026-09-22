package com.sami.app.release;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import com.sami.app.inventory.service.InventoryStockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL regression coverage for V69 product/variant balance identity. */
class InventoryVariantUomPostgresAcceptanceIT extends PostgresApplicationFixture {
    @Autowired JdbcTemplate jdbc;
    @Autowired InventoryStockService stock;
    @Autowired TenantContext tenants;
    @Autowired TransactionTemplate transactions;

    @Test
    void productAndVariantBalancesPersistSeparatelyWithUomProvenance() {
        long tenant = 1L;
        long warehouse = jdbc.queryForObject("select id from pur_warehouses where tenant_id=? order by id limit 1", Long.class, tenant);
        long location = jdbc.queryForObject("select id from inventory_locations where tenant_id=? and warehouse_id=? order by id limit 1", Long.class, tenant, warehouse);
        long product = product(tenant, "step4-product-a");
        long variantA = variant(tenant, product, "A");
        long variantB = variant(tenant, product, "B");
        long pcs = uom(tenant, "PCS");
        long box = uom(tenant, "BOX");
        jdbc.update("insert into uom_conversions(tenant_id,from_uom_id,to_uom_id,factor) values(?,?,?,?)", tenant, box, pcs, new BigDecimal("12"));

        asTenant(tenant, () -> stock.receivePurchase(new InventoryStockOperations.PurchaseReceiptCommand(
                warehouse, 9101L, 9101L, List.of(new InventoryStockOperations.ReceiptLine(null, product, new BigDecimal("5"), BigDecimal.ONE, List.of())))));
        asTenant(tenant, () -> stock.receiveVariantPurchase(new InventoryStockOperations.VariantPurchaseReceiptCommand(
                warehouse, 9102L, 9102L, product, variantA, new BigDecimal("2"), box, pcs, new BigDecimal("12"), BigDecimal.ONE)));
        asTenant(tenant, () -> stock.receiveVariantPurchase(new InventoryStockOperations.VariantPurchaseReceiptCommand(
                warehouse, 9103L, 9103L, product, variantB, new BigDecimal("12"), pcs, pcs, BigDecimal.ONE, BigDecimal.ONE)));
        asTenant(tenant, () -> stock.receiveVariantPurchase(new InventoryStockOperations.VariantPurchaseReceiptCommand(
                warehouse, 9104L, 9104L, product, variantA, new BigDecimal("1"), box, pcs, new BigDecimal("12"), BigDecimal.ONE)));

        assertEquals(3, jdbc.queryForObject("select count(*) from inventory_balances where tenant_id=? and warehouse_id=? and location_id=? and product_id=?", Integer.class, tenant, warehouse, location, product));
        assertEquals(new BigDecimal("5.000"), jdbc.queryForObject("select on_hand from inventory_balances where tenant_id=? and warehouse_id=? and location_id=? and product_id=? and variant_id is null", BigDecimal.class, tenant, warehouse, location, product));
        assertEquals(new BigDecimal("36.000"), jdbc.queryForObject("select on_hand from inventory_balances where tenant_id=? and warehouse_id=? and location_id=? and product_id=? and variant_id=?", BigDecimal.class, tenant, warehouse, location, product, variantA));
        assertEquals(new BigDecimal("12.000"), jdbc.queryForObject("select on_hand from inventory_balances where tenant_id=? and warehouse_id=? and location_id=? and product_id=? and variant_id=?", BigDecimal.class, tenant, warehouse, location, product, variantB));
        var movement = jdbc.queryForMap("select product_id,variant_id,entered_quantity,entered_uom_id,conversion_factor,base_quantity,base_uom_id from inventory_movements where operation_key=?", "PURCHASE-RECEIPT-9102");
        assertEquals(product, ((Number) movement.get("product_id")).longValue());
        assertEquals(variantA, ((Number) movement.get("variant_id")).longValue());
        assertEquals(new BigDecimal("2.000000"), movement.get("entered_quantity"));
        assertEquals(box, ((Number) movement.get("entered_uom_id")).longValue());
        assertEquals(new BigDecimal("12.000000000000"), movement.get("conversion_factor"));
        assertEquals(new BigDecimal("24.000000"), movement.get("base_quantity"));
        assertEquals(pcs, ((Number) movement.get("base_uom_id")).longValue());
        assertEquals(2, jdbc.queryForObject("select count(*) from inventory_movements where product_id=? and variant_id=?", Integer.class, product, variantA));
        jdbc.update("update uom_conversions set factor=10 where tenant_id=? and from_uom_id=? and to_uom_id=?", tenant, box, pcs);
        var historical = jdbc.queryForMap("select entered_quantity,conversion_factor,base_quantity,entered_uom_id,base_uom_id from inventory_movements where operation_key=?", "PURCHASE-RECEIPT-9102");
        assertEquals(new BigDecimal("12.000000000000"), historical.get("conversion_factor"));
        assertEquals(new BigDecimal("24.000000"), historical.get("base_quantity"));

        long productB = product(tenant, "step4-product-b");
        long variantB2 = variant(tenant, productB, "B2");
        assertThrows(RuntimeException.class, () -> asTenant(tenant, () -> stock.receiveVariantPurchase(new InventoryStockOperations.VariantPurchaseReceiptCommand(
                warehouse, 9199L, 9199L, product, variantB2, BigDecimal.ONE, pcs, pcs, BigDecimal.ONE, BigDecimal.ONE))));
        assertEquals(0, jdbc.queryForObject("select count(*) from inventory_movements where operation_key=?", Integer.class, "PURCHASE-RECEIPT-9199"));

        long tenantB = jdbc.queryForObject("insert into tenants(code,name,status_id,config,activated_at) values(?,?,?,'{}'::jsonb,now()) returning id", Long.class,
                "STEP4-TENANT-B", "Step 4 Tenant B", jdbc.queryForObject("select id from licensing_statuses where scope='TENANT' and grants_access order by display_order limit 1", Long.class));
        long tenantBProduct = product(tenantB, "step4-tenant-b-product");
        long tenantBVariant = variant(tenantB, tenantBProduct, "TB");
        assertThrows(RuntimeException.class, () -> asTenant(tenant, () -> stock.receiveVariantPurchase(new InventoryStockOperations.VariantPurchaseReceiptCommand(
                warehouse, 9299L, 9299L, product, tenantBVariant, BigDecimal.ONE, pcs, pcs, BigDecimal.ONE, BigDecimal.ONE))));
        assertEquals(0, jdbc.queryForObject("select count(*) from inventory_movements where operation_key=?", Integer.class, "PURCHASE-RECEIPT-9299"));
    }

    private void asTenant(long tenant, Runnable action) {
        tenants.callAsTenant(tenant, () -> { transactions.executeWithoutResult(status -> action.run()); return null; });
    }
    private long product(long tenant, String sku) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return jdbc.queryForObject("insert into products(name,sku,price,stock_quantity,active,created_at,updated_at,tenant_id) values(?,?,0,0,true,?,?,?) returning id", Long.class, sku, sku, now, now, tenant);
    }
    private long variant(long tenant, long product, String code) {
        return jdbc.queryForObject("insert into product_variants(tenant_id,product_id,variant_code,name,status) values(?,?,?,?,'ACTIVE') returning id", Long.class, tenant, product, code, code);
    }
    private long uom(long tenant, String code) {
        return jdbc.queryForObject("insert into units_of_measure(tenant_id,code,name) values(?,?,?) returning id", Long.class, tenant, code, code);
    }
}
