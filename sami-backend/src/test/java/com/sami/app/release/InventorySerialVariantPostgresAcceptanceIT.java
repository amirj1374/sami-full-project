package com.sami.app.release;

import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.dto.InventoryDtos.TransferLineRequest;
import com.sami.app.inventory.dto.InventoryDtos.TransferRequest;
import com.sami.app.inventory.service.InventoryLedgerService;
import com.sami.app.inventory.service.InventoryWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;

/** PostgreSQL custody identity acceptance for legacy and Variant-aware serial units. */
class InventorySerialVariantPostgresAcceptanceIT extends PostgresApplicationFixture {
    @Autowired JdbcTemplate jdbc;
    @Autowired InventoryLedgerService ledger;
    @Autowired InventoryWorkflowService workflows;
    @Autowired TenantContext tenants;
    @Autowired TransactionTemplate transactions;

    @Test
    void legacyAndVariantSerialCustodyRemainTenantAndProductSafe() {
        long tenant = 1L;
        long warehouse = jdbc.queryForObject("select id from pur_warehouses where tenant_id=? order by id limit 1", Long.class, tenant);
        long location = jdbc.queryForObject("select id from inventory_locations where tenant_id=? and warehouse_id=? order by id limit 1", Long.class, tenant, warehouse);
        long product = jdbc.queryForObject("insert into products(name,sku,price,stock_quantity,active,tenant_id,created_at,updated_at) values(?, ?,0,0,true,?,now(),now()) returning id", Long.class, "step6-product", "step6-product", tenant);
        long variant = jdbc.queryForObject("insert into product_variants(tenant_id,product_id,variant_code,name,status) values(?,?,?,?,'ACTIVE') returning id", Long.class, tenant, product, "A", "A");

        long legacy = serial(tenant, product, null, warehouse, location, "STEP6-LEGACY", null);
        long variantSerial = serial(tenant, product, variant, warehouse, location, "STEP6-VARIANT", "490154203237518");

        assertNull(jdbc.queryForObject("select variant_id from inventory_serial_units where id=?", Long.class, legacy));
        assertEquals(variant, jdbc.queryForObject("select variant_id from inventory_serial_units where id=?", Long.class, variantSerial));
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from inventory_serial_units where id=?", String.class, variantSerial));
        assertThrows(RuntimeException.class, () -> serial(tenant, product, variant, warehouse, location, "STEP6-VARIANT", null));
        assertThrows(RuntimeException.class, () -> transactions.executeWithoutResult(s -> ledger.createSerial(tenant, product, 999999L, warehouse, location, "STEP6-WRONG", null, "TEST", 1L, 1L)));
    }

    @Test
    void variantTransferMovesCustodyAndPreservesIdentity() {
        long tenant = 1L;
        long sourceWarehouse = warehouse(tenant, "STEP6-SOURCE");
        long destinationWarehouse = warehouse(tenant, "STEP6-DEST");
        long sourceLocation = location(tenant, sourceWarehouse, "STEP6-SRC");
        location(tenant, destinationWarehouse, "STEP6-DEST-LOC");
        long product = product(tenant, "step6-transfer-product");
        long variant = variant(tenant, product, "STEP6-TRANSFER-A");
        long serial = serial(tenant, product, variant, sourceWarehouse, sourceLocation, "STEP6-TRANSFER-S", null);
        balance(tenant, sourceWarehouse, sourceLocation, product, variant, "1");

        TransferRequest request = new TransferRequest(sourceWarehouse, destinationWarehouse, "step6", List.of(
                new TransferLineRequest(product, variant, BigDecimal.ONE, List.of("STEP6-TRANSFER-S"))));
        long transferId = tenants.callAsTenant(tenant, () -> transactions.execute(s -> workflows.createTransfer(request).id()));
        tenants.callAsTenant(tenant, () -> transactions.execute(s -> { workflows.shipTransfer(transferId); return null; }));
        tenants.callAsTenant(tenant, () -> transactions.execute(s -> { workflows.receiveTransfer(transferId); return null; }));

        MapRow row = jdbc.queryForObject("select product_id,variant_id,warehouse_id,location_id,status from inventory_serial_units where id=?",
                (rs, n) -> new MapRow(rs.getLong(1), (Long) rs.getObject(2), rs.getLong(3), rs.getLong(4), rs.getString(5)), serial);
        assertEquals(product, row.productId());
        assertEquals(variant, row.variantId());
        assertEquals(destinationWarehouse, row.warehouseId());
        assertEquals("AVAILABLE", row.status());
        assertEquals(1, jdbc.queryForObject("select count(*) from inventory_serial_units where tenant_id=? and serial_number=?", Integer.class, tenant, "STEP6-TRANSFER-S"));
        assertEquals(0, jdbc.queryForObject("select count(*) from inventory_serial_units where tenant_id=? and serial_number=? and warehouse_id=? and status='AVAILABLE'", Integer.class, tenant, "STEP6-TRANSFER-S", sourceWarehouse));
    }

    @Test
    void hamtaRelationshipSurvivesVariantCustodyOperations() {
        long tenant = 1L;
        long warehouse = jdbc.queryForObject("select id from pur_warehouses where tenant_id=? order by id limit 1", Long.class, tenant);
        long location = jdbc.queryForObject("select id from inventory_locations where tenant_id=? and warehouse_id=? order by id limit 1", Long.class, tenant, warehouse);
        long product = product(tenant, "step6-hamta-product");
        long variant = variant(tenant, product, "STEP6-HAMTA-A");
        long serial = serial(tenant, product, variant, warehouse, location, "STEP6-HAMTA-S", null);
        jdbc.update("insert into hamta_activations(tenant_id,serial_unit_id,activation_code,created_by,created_by_email) values(?,?,?,?,?)", tenant, serial, "HAMTA-STEP6", 1L, "step6@test");
        Long activation = jdbc.queryForObject("select id from hamta_activations where tenant_id=? and serial_unit_id=?", Long.class, tenant, serial);
        tenants.callAsTenant(tenant, () -> transactions.execute(s -> { jdbc.update("update inventory_serial_units set status='RESERVED' where id=? and tenant_id=?", serial, tenant); return null; }));
        assertEquals(activation, jdbc.queryForObject("select id from hamta_activations where tenant_id=? and serial_unit_id=?", Long.class, tenant, serial));
        assertEquals("HAMTA-STEP6", jdbc.queryForObject("select activation_code from hamta_activations where id=?", String.class, activation));
        assertEquals(variant, jdbc.queryForObject("select variant_id from inventory_serial_units where id=?", Long.class, serial));
    }

    @Test
    void crossTenantMutationIsRejectedWithoutCustodyChange() {
        long owner = 1L;
        long other = jdbc.queryForObject("insert into tenants(code,name,status_id) select 'STEP6-OTHER','Step6 Other',id from licensing_statuses order by id limit 1 returning id", Long.class);
        long warehouse = jdbc.queryForObject("select id from pur_warehouses where tenant_id=? order by id limit 1", Long.class, owner);
        long location = jdbc.queryForObject("select id from inventory_locations where tenant_id=? and warehouse_id=? order by id limit 1", Long.class, owner, warehouse);
        long product = product(owner, "step6-cross-tenant-product");
        long variant = variant(owner, product, "STEP6-CROSS-A");
        long serial = serial(owner, product, variant, warehouse, location, "STEP6-CROSS-S", null);
        assertThrows(RuntimeException.class, () -> tenants.callAsTenant(other, () -> transactions.execute(s -> { ledger.createSerial(other, product, variant, warehouse, location, "STEP6-CROSS-MUTATION", null, "TEST", 2L, 2L); return null; })));
        assertEquals("AVAILABLE", jdbc.queryForObject("select status from inventory_serial_units where id=?", String.class, serial));
        assertEquals(1, jdbc.queryForObject("select count(*) from inventory_serial_units where id=?", Integer.class, serial));
    }

    @Test
    void concurrentIssueOfOneVariantSerialConsumesItOnce() throws Exception {
        long tenant = 1L;
        long warehouse = jdbc.queryForObject("select id from pur_warehouses where tenant_id=? order by id limit 1", Long.class, tenant);
        long location = jdbc.queryForObject("select id from inventory_locations where tenant_id=? and warehouse_id=? order by id limit 1", Long.class, tenant, warehouse);
        long product = product(tenant, "step6-concurrent-product");
        long variant = variant(tenant, product, "STEP6-CONCURRENT-A");
        long serial = serial(tenant, product, variant, warehouse, location, "STEP6-CONCURRENT-S", null);
        balance(tenant, warehouse, location, product, variant, "1");
        long reservation = tenants.callAsTenant(tenant, () -> transactions.execute(s -> ledger.reserve(tenant, product, variant, warehouse, location, BigDecimal.ONE, "STEP6", 7001L, 1L, "STEP6-CONCURRENT-S", null)));
        var pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = pool.submit(() -> issueOnce(tenant, reservation));
            Future<Boolean> b = pool.submit(() -> issueOnce(tenant, reservation));
            assertTrue(a.get() ^ b.get());
        } finally { pool.shutdownNow(); }
        assertEquals("ISSUED", jdbc.queryForObject("select status from inventory_serial_units where id=?", String.class, serial));
        assertEquals(1, jdbc.queryForObject("select count(*) from inventory_movements where tenant_id=? and movement_type='ISSUE' and source_id=?", Integer.class, tenant, 7001L));
        assertEquals(0, jdbc.queryForObject("select on_hand from inventory_balances where tenant_id=? and warehouse_id=? and location_id=? and product_id=? and variant_id=?", BigDecimal.class, tenant, warehouse, location, product, variant).compareTo(BigDecimal.ZERO));
    }

    private boolean issueOnce(long tenant, long reservationId) {
        try {
            tenants.callAsTenant(tenant, () -> transactions.execute(s -> {
                var state = ledger.activeReservations(tenant, "STEP6", 7001L).stream().filter(r -> r.id().equals(reservationId)).findFirst().orElseThrow();
                ledger.issueReservation(tenant, state);
                return null;
            }));
            return true;
        } catch (RuntimeException ex) { return false; }
    }

    private long warehouse(long tenant, String code) {
        return jdbc.queryForObject("insert into pur_warehouses(tenant_id,code,name,active,display_order) values(?,?,?,true,0) returning id", Long.class, tenant, code, code);
    }
    private long location(long tenant, long warehouse, String code) {
        return jdbc.queryForObject("insert into inventory_locations(tenant_id,warehouse_id,code,name,is_default) values(?,?,?,?,true) returning id", Long.class, tenant, warehouse, code, code);
    }
    private long product(long tenant, String sku) {
        return jdbc.queryForObject("insert into products(name,sku,price,stock_quantity,active,tenant_id,created_at,updated_at) values(?, ?,0,0,true,?,now(),now()) returning id", Long.class, sku, sku, tenant);
    }
    private long variant(long tenant, long product, String code) {
        return jdbc.queryForObject("insert into product_variants(tenant_id,product_id,variant_code,name,status) values(?,?,?,?,'ACTIVE') returning id", Long.class, tenant, product, code, code);
    }
    private void balance(long tenant, long warehouse, long location, long product, long variant, String quantity) {
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,variant_id,on_hand,reserved) values(?,?,?,?,?,?,0)", tenant, warehouse, location, product, variant, new BigDecimal(quantity));
    }
    private record MapRow(long productId, Long variantId, long warehouseId, long locationId, String status) {}

    private long serial(long tenant, long product, Long variant, long warehouse, long location, String serial, String imei) {
        return tenants.callAsTenant(tenant, () -> transactions.execute(status -> ledger.createSerial(
                tenant, product, variant, warehouse, location, serial, imei, "TEST", 1L, 1L)));
    }
}
