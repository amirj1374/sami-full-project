package com.sami.app.release;

import com.sami.app.inventory.service.InventoryLedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** PostgreSQL acceptance for reservation allocation and V70 persistence. */
class InventoryReservationBackorderPostgresAcceptanceIT extends PostgresApplicationFixture {
    @Autowired JdbcTemplate jdbc;
    @Autowired InventoryLedgerService ledger;
    @Autowired TransactionTemplate tx;

    @Test
    void reservationTimeoutExpiresAndReleasesProjection() {
        long tenant = 1L;
        long warehouse = jdbc.queryForObject("select id from pur_warehouses where tenant_id=? order by id limit 1", Long.class, tenant);
        long location = jdbc.queryForObject("select id from inventory_locations where tenant_id=? and warehouse_id=? order by id limit 1", Long.class, tenant, warehouse);
        long product = jdbc.queryForObject("insert into products(name,sku,price,stock_quantity,active,created_at,updated_at,tenant_id) values(?,?,0,0,true,?,?,?) returning id", Long.class, "Timeout Product", "timeout-" + System.nanoTime(), new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()), tenant);
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,on_hand) values(?,?,?,?,?)", tenant, warehouse, location, product, new BigDecimal("4"));
        tx.execute(status -> ledger.reserveAvailable(tenant, product, null, warehouse, location, new BigDecimal("2"), "TIMEOUT", 8801L, 1L, null, null, new BigDecimal("2"), null, BigDecimal.ONE, new BigDecimal("2"), null));
        Timestamp expires = jdbc.queryForObject("select expires_at from inventory_reservations where tenant_id=? and source_type='TIMEOUT' and source_id=8801", Timestamp.class, tenant);
        assertNotNull(expires);
        assertTrue(expires.toInstant().isAfter(Instant.now().plusSeconds(29 * 60)));
        assertTrue(expires.toInstant().isBefore(Instant.now().plusSeconds(31 * 60)));
        int expired = tx.execute(status -> ledger.expireReservations(tenant, Instant.now().plus(Duration.ofHours(1))));
        assertTrue(expired >= 1);
        assertEquals("EXPIRED", jdbc.queryForObject("select status from inventory_reservations where tenant_id=? and source_type='TIMEOUT' and source_id=8801", String.class, tenant));
        assertEquals(new BigDecimal("0.000"), jdbc.queryForObject("select reserved from inventory_balances where tenant_id=? and product_id=?", BigDecimal.class, tenant, product));
        assertEquals(1, jdbc.queryForObject("select count(*) from inventory_movements where tenant_id=? and movement_type='RELEASE' and source_id=8801", Integer.class, tenant));
        assertEquals(0, tx.execute(status -> ledger.expireReservations(tenant, Instant.now().plus(Duration.ofHours(2)))).intValue());
    }

    @Test
    void productAndVariantReservationsAllocateWithoutNegativeStock() {
        long tenant = 1L;
        long warehouse = jdbc.queryForObject("select id from pur_warehouses where tenant_id=? order by id limit 1", Long.class, tenant);
        long location = jdbc.queryForObject("select id from inventory_locations where tenant_id=? and warehouse_id=? order by id limit 1", Long.class, tenant, warehouse);
        long product = jdbc.queryForObject("insert into products(name,sku,price,stock_quantity,active,created_at,updated_at,tenant_id) values(?,?,0,0,true,?,?,?) returning id", Long.class, "Reservation Product", "reservation-" + System.nanoTime(), new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()), tenant);
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,on_hand) values(?,?,?,?,?)", tenant, warehouse, location, product, new BigDecimal("10"));
        BigDecimal first = tx.execute(status -> ledger.reserveAvailable(tenant, product, null, warehouse, location, new BigDecimal("7"), "STEP5", 1L, 1L, null, null, new BigDecimal("7"), null, BigDecimal.ONE, new BigDecimal("7"), null));
        BigDecimal second = tx.execute(status -> ledger.reserveAvailable(tenant, product, null, warehouse, location, new BigDecimal("7"), "STEP5", 2L, 2L, null, null, new BigDecimal("7"), null, BigDecimal.ONE, new BigDecimal("7"), null));
        assertEquals(0, first.compareTo(new BigDecimal("7")));
        assertEquals(0, second.compareTo(new BigDecimal("3")));
        assertEquals(new BigDecimal("10.000"), jdbc.queryForObject("select reserved from inventory_balances where tenant_id=? and product_id=?", BigDecimal.class, tenant, product));
        assertEquals(2, jdbc.queryForObject("select count(*) from inventory_reservations where tenant_id=? and product_id=? and status='ACTIVE'", Integer.class, tenant, product));
        assertEquals(new BigDecimal("4.000"), jdbc.queryForObject("select backordered_quantity from inventory_reservations where tenant_id=? and source_id=2", BigDecimal.class, tenant));
        assertTrue(jdbc.queryForObject("select on_hand-reserved from inventory_balances where tenant_id=? and product_id=?", BigDecimal.class, tenant, product).compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void backorderLifecycleAndSnapshotPersistInPostgres() {
        long tenant = 1L;
        long warehouse = jdbc.queryForObject("select id from pur_warehouses where tenant_id=? order by id limit 1", Long.class, tenant);
        long location = jdbc.queryForObject("select id from inventory_locations where tenant_id=? and warehouse_id=? order by id limit 1", Long.class, tenant, warehouse);
        long product = jdbc.queryForObject("insert into products(name,sku,price,stock_quantity,active,created_at,updated_at,tenant_id) values(?,?,0,0,true,?,?,?) returning id", Long.class, "Backorder Product", "backorder-" + System.nanoTime(), new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()), tenant);
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,on_hand) values(?,?,?,?,?)", tenant, warehouse, location, product, new BigDecimal("5"));
        tx.execute(status -> ledger.reserveAvailable(tenant, product, null, warehouse, location, new BigDecimal("8"), "BO", 90L, 1L, null, null, new BigDecimal("8"), null, BigDecimal.ONE, new BigDecimal("8"), null));
        long reservation = jdbc.queryForObject("select id from inventory_reservations where tenant_id=? and source_type='BO' and source_id=90", Long.class, tenant);
        assertEquals(new BigDecimal("8.000"), jdbc.queryForObject("select requested_quantity from inventory_reservations where id=?", BigDecimal.class, reservation));
        assertEquals(new BigDecimal("5.000"), jdbc.queryForObject("select reserved_quantity from inventory_reservations where id=?", BigDecimal.class, reservation));
        assertEquals(new BigDecimal("3.000"), jdbc.queryForObject("select backordered_quantity from inventory_reservations where id=?", BigDecimal.class, reservation));
        assertEquals("OPEN", jdbc.queryForObject("select backorder_status from inventory_reservations where id=?", String.class, reservation));
        jdbc.update("update inventory_balances set on_hand=on_hand+2 where tenant_id=? and product_id=?", tenant, product);
        tx.execute(status -> ledger.fulfillBackorder(tenant, reservation, new BigDecimal("2")));
        assertEquals(new BigDecimal("1.000"), jdbc.queryForObject("select backordered_quantity from inventory_reservations where id=?", BigDecimal.class, reservation));
        assertEquals("PARTIALLY_FULFILLED", jdbc.queryForObject("select backorder_status from inventory_reservations where id=?", String.class, reservation));
        jdbc.update("update inventory_balances set on_hand=on_hand+1 where tenant_id=? and product_id=?", tenant, product);
        tx.execute(status -> ledger.fulfillBackorder(tenant, reservation, new BigDecimal("1")));
        assertEquals(0, BigDecimal.ZERO.compareTo(jdbc.queryForObject("select backordered_quantity from inventory_reservations where id=?", BigDecimal.class, reservation)));
        assertEquals("FULFILLED", jdbc.queryForObject("select backorder_status from inventory_reservations where id=?", String.class, reservation));
    }

    @Test
    void backorderCancellationIsPersisted() {
        long tenant = 1L;
        long warehouse = jdbc.queryForObject("select id from pur_warehouses where tenant_id=? order by id limit 1", Long.class, tenant);
        long location = jdbc.queryForObject("select id from inventory_locations where tenant_id=? and warehouse_id=? order by id limit 1", Long.class, tenant, warehouse);
        long product = jdbc.queryForObject("insert into products(name,sku,price,stock_quantity,active,created_at,updated_at,tenant_id) values(?,?,0,0,true,?,?,?) returning id", Long.class, "Cancelled Backorder", "cancel-bo-" + System.nanoTime(), new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()), tenant);
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,on_hand) values(?,?,?,?,?)", tenant, warehouse, location, product, new BigDecimal("1"));
        tx.execute(status -> ledger.reserveAvailable(tenant, product, null, warehouse, location, new BigDecimal("4"), "CBO", 91L, 1L, null, null, new BigDecimal("4"), null, BigDecimal.ONE, new BigDecimal("4"), null));
        tx.execute(s->{ledger.cancelBackorders(tenant, "CBO", 91L); return null;});
        assertEquals("CANCELLED", jdbc.queryForObject("select backorder_status from inventory_reservations where tenant_id=? and source_type='CBO' and source_id=91", String.class, tenant));
        assertEquals(new BigDecimal("0.000"), jdbc.queryForObject("select backordered_quantity from inventory_reservations where tenant_id=? and source_type='CBO' and source_id=91", BigDecimal.class, tenant));
    }

    @Test
    void rejectsVariantBelongingToAnotherProductWithoutPersistence() {
        long tenant = 1L;
        long warehouse = jdbc.queryForObject("select id from pur_warehouses where tenant_id=? order by id limit 1", Long.class, tenant);
        long location = jdbc.queryForObject("select id from inventory_locations where tenant_id=? and warehouse_id=? order by id limit 1", Long.class, tenant, warehouse);
        long productA = createProduct(tenant, "Mismatch A");
        long productB = createProduct(tenant, "Mismatch B");
        long variantB = jdbc.queryForObject("insert into product_variants(tenant_id,product_id,variant_code,name) values(?,?,?,?) returning id", Long.class, tenant, productB, "M-" + System.nanoTime(), "Mismatch variant");
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,on_hand) values(?,?,?,?,?)", tenant, warehouse, location, productA, new BigDecimal("10"));
        int before = jdbc.queryForObject("select count(*) from inventory_reservations where tenant_id=? and product_id=?", Integer.class, tenant, productA);
        assertThrows(RuntimeException.class, () -> tx.execute(status -> ledger.reserveAvailable(tenant, productA, variantB, warehouse, location, new BigDecimal("1"), "MISMATCH", 1L, 1L, null, null, new BigDecimal("1"), null, BigDecimal.ONE, new BigDecimal("1"), null)));
        assertEquals(before, jdbc.queryForObject("select count(*) from inventory_reservations where tenant_id=? and product_id=?", Integer.class, tenant, productA));
        assertEquals(new BigDecimal("0.000"), jdbc.queryForObject("select reserved from inventory_balances where tenant_id=? and warehouse_id=? and location_id=? and product_id=?", BigDecimal.class, tenant, warehouse, location, productA));
        assertEquals(0, jdbc.queryForObject("select count(*) from inventory_movements where tenant_id=? and product_id=? and source_type='MISMATCH'", Integer.class, tenant, productA));
    }

    private long createProduct(long tenant, String name) {
        return jdbc.queryForObject("insert into products(name,sku,price,stock_quantity,active,created_at,updated_at,tenant_id) values(?,?,0,0,true,?,?,?) returning id", Long.class, name, "step5-" + System.nanoTime(), new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()), tenant);
    }

    @Test
    void crossTenantVariantReservationIsRejected() {
        long tenantA = 1L;
        Long tenantB = jdbc.queryForObject("insert into tenants(code,name,status_id) values(?,?,(select id from licensing_statuses order by id limit 1)) returning id", Long.class, "STEP5-T-"+System.nanoTime(), "Step5 Tenant B");
        long productA = createProduct(tenantA, "Tenant A product");
        long productB = createProduct(tenantB, "Tenant B product");
        long variantB = jdbc.queryForObject("insert into product_variants(tenant_id,product_id,variant_code,name) values(?,?,?,?) returning id", Long.class, tenantB, productB, "TB-" + System.nanoTime(), "Tenant B variant");
        long warehouse = jdbc.queryForObject("select id from pur_warehouses where tenant_id=? order by id limit 1", Long.class, tenantA);
        long location = jdbc.queryForObject("select id from inventory_locations where tenant_id=? and warehouse_id=? order by id limit 1", Long.class, tenantA, warehouse);
        int before = jdbc.queryForObject("select count(*) from inventory_reservations where tenant_id=? and product_id=?", Integer.class, tenantA, productA);
        assertThrows(RuntimeException.class, () -> tx.execute(s -> ledger.reserveAvailable(tenantA, productA, variantB, warehouse, location, BigDecimal.ONE, "CROSS", 1L, 1L, null, null, BigDecimal.ONE, null, BigDecimal.ONE, BigDecimal.ONE, null)));
        assertEquals(before, jdbc.queryForObject("select count(*) from inventory_reservations where tenant_id=? and product_id=?", Integer.class, tenantA, productA));
    }

    @Test
    void productOnlyReleaseAndDuplicateReleaseAreSafe() {
        long tenant=1L, warehouse=jdbc.queryForObject("select id from pur_warehouses where tenant_id=1 limit 1",Long.class), location=jdbc.queryForObject("select id from inventory_locations where warehouse_id=? limit 1",Long.class,warehouse), product=createProduct(tenant,"Release product");
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,on_hand) values(?,?,?,?,10)",tenant,warehouse,location,product);
        tx.execute(s->ledger.reserveAvailable(tenant,product,null,warehouse,location,new BigDecimal("7"),"REL",700L,1L,null,null,new BigDecimal("7"),null,BigDecimal.ONE,new BigDecimal("7"),null));
        var reservation=tx.execute(s->ledger.activeReservations(tenant,"REL",700L).get(0));
        tx.execute(s->{ledger.releaseReservation(tenant,reservation,"test");return null;});
        assertEquals(new BigDecimal("10.000"),jdbc.queryForObject("select on_hand from inventory_balances where product_id=?",BigDecimal.class,product));
        assertEquals(new BigDecimal("0.000"),jdbc.queryForObject("select reserved from inventory_balances where product_id=?",BigDecimal.class,product));
        assertThrows(RuntimeException.class,()->tx.execute(s->{ledger.releaseReservation(tenant,reservation,"duplicate");return null;}));
        assertEquals(new BigDecimal("0.000"),jdbc.queryForObject("select reserved from inventory_balances where product_id=?",BigDecimal.class,product));
    }

    @Test
    void productOnlyPartialAndFullIssueAreSafe() {
        long tenant=1L, warehouse=jdbc.queryForObject("select id from pur_warehouses where tenant_id=1 limit 1",Long.class), location=jdbc.queryForObject("select id from inventory_locations where warehouse_id=? limit 1",Long.class,warehouse), product=createProduct(tenant,"Issue product");
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,on_hand) values(?,?,?,?,10)",tenant,warehouse,location,product);
        tx.execute(s->ledger.reserveAvailable(tenant,product,null,warehouse,location,new BigDecimal("10"),"ISS",701L,1L,null,null,new BigDecimal("10"),null,BigDecimal.ONE,new BigDecimal("10"),null));
        var reservation=tx.execute(s->ledger.activeReservations(tenant,"ISS",701L).get(0));
        tx.execute(s->{ledger.issueReservation(tenant,reservation,new BigDecimal("4"),"TEST",701L,"ISS-701-4");return null;});
        assertEquals(new BigDecimal("6.000"),jdbc.queryForObject("select on_hand from inventory_balances where product_id=?",BigDecimal.class,product));
        var refreshed=tx.execute(s->ledger.activeReservations(tenant,"ISS",701L).get(0));
        tx.execute(s->{ledger.issueReservation(tenant,refreshed,new BigDecimal("6"),"TEST",701L,"ISS-701-6");return null;});
        assertEquals(new BigDecimal("0.000"),jdbc.queryForObject("select on_hand from inventory_balances where product_id=?",BigDecimal.class,product));
        assertThrows(RuntimeException.class,()->tx.execute(s->{ledger.issueReservation(tenant,refreshed,new BigDecimal("1"),"TEST",701L,"ISS-701-DUP");return null;}));
        assertEquals(new BigDecimal("0.000"),jdbc.queryForObject("select on_hand from inventory_balances where product_id=?",BigDecimal.class,product));
    }

    @Test
    void sameIdentityConcurrentReservationsNeverOverReserve() throws Exception {
        long tenant=1L, warehouse=jdbc.queryForObject("select id from pur_warehouses where tenant_id=1 limit 1",Long.class), location=jdbc.queryForObject("select id from inventory_locations where warehouse_id=? limit 1",Long.class,warehouse), product=createProduct(tenant,"Concurrent product");
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,on_hand) values(?,?,?,?,10)",tenant,warehouse,location,product);
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1); List<Future<BigDecimal>> futures=new ArrayList<>();
        for(int i=0;i<2;i++){ final long source=800+i; futures.add(pool.submit(()->{start.await(); return tx.execute(s->ledger.reserveAvailable(tenant,product,null,warehouse,location,new BigDecimal("7"),"CONC",source,source,null,null,new BigDecimal("7"),null,BigDecimal.ONE,new BigDecimal("7"),null));})); }
        start.countDown(); futures.forEach(f->{try{f.get(30,TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}}); pool.shutdownNow();
        BigDecimal reserved=jdbc.queryForObject("select reserved from inventory_balances where tenant_id=? and product_id=?",BigDecimal.class,tenant,product);
        assertTrue(reserved.compareTo(new BigDecimal("10"))<=0); assertTrue(jdbc.queryForObject("select on_hand-reserved from inventory_balances where product_id=?",BigDecimal.class,product).signum()>=0);
    }

    @Test
    void variantIdentitiesRemainIndependentUnderConcurrentReservations() throws Exception {
        long tenant=1L, warehouse=jdbc.queryForObject("select id from pur_warehouses where tenant_id=1 limit 1",Long.class), location=jdbc.queryForObject("select id from inventory_locations where warehouse_id=? limit 1",Long.class,warehouse), product=createProduct(tenant,"Variant concurrent product");
        long a=jdbc.queryForObject("insert into product_variants(tenant_id,product_id,variant_code,name) values(?,?,?,?) returning id",Long.class,tenant,product,"A-"+System.nanoTime(),"A");
        long b=jdbc.queryForObject("insert into product_variants(tenant_id,product_id,variant_code,name) values(?,?,?,?) returning id",Long.class,tenant,product,"B-"+System.nanoTime(),"B");
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,variant_id,on_hand) values(?,?,?,?,?,10),(?,?,?,?,?,10)",tenant,warehouse,location,product,a,tenant,warehouse,location,product,b);
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1); Future<?> fa=pool.submit(()->{try{start.await();tx.execute(s->ledger.reserveAvailable(tenant,product,a,warehouse,location,new BigDecimal("7"),"VA",1L,1L,null,null,new BigDecimal("7"),null,BigDecimal.ONE,new BigDecimal("7"),null));}catch(Exception e){throw new RuntimeException(e);}}); Future<?> fb=pool.submit(()->{try{start.await();tx.execute(s->ledger.reserveAvailable(tenant,product,b,warehouse,location,new BigDecimal("7"),"VB",1L,1L,null,null,new BigDecimal("7"),null,BigDecimal.ONE,new BigDecimal("7"),null));}catch(Exception e){throw new RuntimeException(e);}}); start.countDown(); fa.get(30,TimeUnit.SECONDS);fb.get(30,TimeUnit.SECONDS);pool.shutdownNow();
        assertEquals(new BigDecimal("7.000"),jdbc.queryForObject("select reserved from inventory_balances where variant_id=?",BigDecimal.class,a)); assertEquals(new BigDecimal("7.000"),jdbc.queryForObject("select reserved from inventory_balances where variant_id=?",BigDecimal.class,b));
    }

    @Test
    void reservationUomSnapshotIsImmutable() {
        long tenant=1L, warehouse=jdbc.queryForObject("select id from pur_warehouses where tenant_id=1 limit 1",Long.class), location=jdbc.queryForObject("select id from inventory_locations where warehouse_id=? limit 1",Long.class,warehouse), product=createProduct(tenant,"UOM product");
        Long pcs=jdbc.queryForObject("insert into units_of_measure(tenant_id,code,name) values(?,?,?) returning id",Long.class,tenant,"PCS-"+System.nanoTime(),"PCS");
        Long box=jdbc.queryForObject("insert into units_of_measure(tenant_id,code,name) values(?,?,?) returning id",Long.class,tenant,"BOX-"+System.nanoTime(),"BOX");
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,on_hand) values(?,?,?,?,30)",tenant,warehouse,location,product);
        tx.execute(s->ledger.reserveAvailable(tenant,product,null,warehouse,location,new BigDecimal("24"),"UOM",900L,1L,null,null,new BigDecimal("2"),box,new BigDecimal("12"),new BigDecimal("24"),pcs));
        long id=jdbc.queryForObject("select id from inventory_reservations where source_type='UOM' and source_id=900",Long.class);
        assertEquals(0,new BigDecimal("2").compareTo(jdbc.queryForObject("select entered_quantity from inventory_reservations where id=?",BigDecimal.class,id)));
        assertEquals(new BigDecimal("12.000000000000"),jdbc.queryForObject("select conversion_factor from inventory_reservations where id=?",BigDecimal.class,id));
        assertEquals(0,new BigDecimal("24").compareTo(jdbc.queryForObject("select base_quantity from inventory_reservations where id=?",BigDecimal.class,id)));
        jdbc.update("insert into uom_conversions(tenant_id,from_uom_id,to_uom_id,factor) values(?,?,?,?)",tenant,box,pcs,new BigDecimal("10"));
        assertEquals(0,new BigDecimal("24").compareTo(jdbc.queryForObject("select base_quantity from inventory_reservations where id=?",BigDecimal.class,id)));
    }

    @Test
    void variantReleasePreservesVariantIdentity() {
        long tenant=1L, warehouse=jdbc.queryForObject("select id from pur_warehouses where tenant_id=1 limit 1",Long.class), location=jdbc.queryForObject("select id from inventory_locations where warehouse_id=? limit 1",Long.class,warehouse), product=createProduct(tenant,"Variant release product");
        long a=jdbc.queryForObject("insert into product_variants(tenant_id,product_id,variant_code,name) values(?,?,?,?) returning id",Long.class,tenant,product,"REL-A-"+System.nanoTime(),"A");
        long b=jdbc.queryForObject("insert into product_variants(tenant_id,product_id,variant_code,name) values(?,?,?,?) returning id",Long.class,tenant,product,"REL-B-"+System.nanoTime(),"B");
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,variant_id,on_hand) values(?,?,?,?,?,10),(?,?,?,?,?,10)",tenant,warehouse,location,product,a,tenant,warehouse,location,product,b);
        tx.execute(s->ledger.reserveAvailable(tenant,product,a,warehouse,location,new BigDecimal("7"),"VREL",910L,1L,null,null,new BigDecimal("7"),null,BigDecimal.ONE,new BigDecimal("7"),null));
        assertEquals(new BigDecimal("7.000"),jdbc.queryForObject("select reserved from inventory_balances where variant_id=?",BigDecimal.class,a));
        assertEquals(new BigDecimal("0.000"),jdbc.queryForObject("select reserved from inventory_balances where variant_id=?",BigDecimal.class,b));
        var reservation=tx.execute(s->ledger.activeReservations(tenant,"VREL",910L).get(0));
        tx.execute(s->{ledger.releaseReservation(tenant,reservation,"variant release");return null;});
        assertEquals(new BigDecimal("10.000"),jdbc.queryForObject("select on_hand from inventory_balances where variant_id=?",BigDecimal.class,a));
        assertEquals(new BigDecimal("0.000"),jdbc.queryForObject("select reserved from inventory_balances where variant_id=?",BigDecimal.class,a));
        assertEquals(new BigDecimal("10.000"),jdbc.queryForObject("select on_hand from inventory_balances where variant_id=?",BigDecimal.class,b));
        assertEquals(new BigDecimal("0.000"),jdbc.queryForObject("select reserved from inventory_balances where variant_id=?",BigDecimal.class,b));
        assertEquals("RELEASED",jdbc.queryForObject("select status from inventory_reservations where tenant_id=? and source_type='VREL' and source_id=910",String.class,tenant));
    }

    @Test
    void variantPartialIssuePreservesVariantIdentity() {
        long tenant=1L, warehouse=jdbc.queryForObject("select id from pur_warehouses where tenant_id=1 limit 1",Long.class), location=jdbc.queryForObject("select id from inventory_locations where warehouse_id=? limit 1",Long.class,warehouse), product=createProduct(tenant,"Variant issue product");
        long a=jdbc.queryForObject("insert into product_variants(tenant_id,product_id,variant_code,name) values(?,?,?,?) returning id",Long.class,tenant,product,"ISS-A-"+System.nanoTime(),"A");
        long b=jdbc.queryForObject("insert into product_variants(tenant_id,product_id,variant_code,name) values(?,?,?,?) returning id",Long.class,tenant,product,"ISS-B-"+System.nanoTime(),"B");
        jdbc.update("insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id,variant_id,on_hand) values(?,?,?,?,?,10),(?,?,?,?,?,10)",tenant,warehouse,location,product,a,tenant,warehouse,location,product,b);
        tx.execute(s->ledger.reserveAvailable(tenant,product,a,warehouse,location,new BigDecimal("10"),"VISS",911L,1L,null,null,new BigDecimal("10"),null,BigDecimal.ONE,new BigDecimal("10"),null));
        var reservation=tx.execute(s->ledger.activeReservations(tenant,"VISS",911L).get(0));
        tx.execute(s->{ledger.issueReservation(tenant,reservation,new BigDecimal("4"),"VISS-ISSUE",911L,"VISS-ISSUE-4");return null;});
        assertEquals(new BigDecimal("6.000"),jdbc.queryForObject("select on_hand from inventory_balances where variant_id=?",BigDecimal.class,a));
        assertEquals(new BigDecimal("6.000"),jdbc.queryForObject("select reserved from inventory_balances where variant_id=?",BigDecimal.class,a));
        assertEquals(new BigDecimal("10.000"),jdbc.queryForObject("select on_hand from inventory_balances where variant_id=?",BigDecimal.class,b));
        assertEquals(new BigDecimal("0.000"),jdbc.queryForObject("select reserved from inventory_balances where variant_id=?",BigDecimal.class,b));
        assertEquals(1,jdbc.queryForObject("select count(*) from inventory_movements where tenant_id=? and source_type='VISS-ISSUE' and source_id=911",Integer.class,tenant));
    }
}
