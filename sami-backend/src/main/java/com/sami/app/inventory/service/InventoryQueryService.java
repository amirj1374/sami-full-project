package com.sami.app.inventory.service;

import com.sami.app.common.api.PageResponse;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.dto.InventoryDtos.BalanceResponse;
import com.sami.app.inventory.dto.InventoryDtos.DashboardResponse;
import com.sami.app.inventory.dto.InventoryDtos.MetricPoint;
import com.sami.app.inventory.dto.InventoryDtos.MovementResponse;
import com.sami.app.inventory.dto.InventoryDtos.ReportResponse;
import com.sami.app.inventory.dto.InventoryDtos.ReservationResponse;
import com.sami.app.inventory.dto.InventoryDtos.SerialResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tenant-scoped Inventory monitoring, reporting and export queries. */
@Service
@RequiredArgsConstructor
public class InventoryQueryService {

    private final TenantContext tenantContext;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public PageResponse<BalanceResponse> balances(String search, Long warehouseId,
                                                   Boolean lowStock, int page, int size) {
        Long tenantId = tenantContext.requireTenantId();
        SqlFilter filter = balanceFilter(tenantId, search, warehouseId, lowStock);
        long total = count("select count(*) " + balanceFrom() + filter.sql(), filter.args());
        List<Object> args = new ArrayList<>(filter.args());
        args.add(limit(size));
        args.add(offset(page, size));
        List<BalanceResponse> rows = jdbc.query("select " + balanceColumns() + " "
                        + balanceFrom() + filter.sql()
                        + " order by low_stock desc,p.name,w.display_order,l.code limit ? offset ?",
                (rs, row) -> balance(rs), args.toArray());
        return page(rows, page, size, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementResponse> movements(String search, Long warehouseId,
                                                     String movementType, String sourceType,
                                                     Instant from, Instant to, int page, int size) {
        Long tenantId = tenantContext.requireTenantId();
        StringBuilder where = new StringBuilder(" where m.tenant_id=?");
        ArrayList<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (search != null && !search.isBlank()) {
            where.append(" and (lower(p.name) like ? or lower(p.sku) like ?)");
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(pattern);
            args.add(pattern);
        }
        if (warehouseId != null) {
            where.append(" and (m.from_warehouse_id=? or m.to_warehouse_id=?)");
            args.add(warehouseId);
            args.add(warehouseId);
        }
        if (movementType != null && !movementType.isBlank()) {
            where.append(" and m.movement_type=?");
            args.add(movementType.trim().toUpperCase(Locale.ROOT));
        }
        if (sourceType != null && !sourceType.isBlank()) {
            where.append(" and m.source_type=?");
            args.add(sourceType.trim().toUpperCase(Locale.ROOT));
        }
        if (from != null) {
            where.append(" and m.occurred_at>=?");
            args.add(java.sql.Timestamp.from(from));
        }
        if (to != null) {
            where.append(" and m.occurred_at<?");
            args.add(java.sql.Timestamp.from(to));
        }
        String fromSql = " from inventory_movements m join products p on p.id=m.product_id "
                + "left join pur_warehouses fw on fw.id=m.from_warehouse_id "
                + "left join pur_warehouses tw on tw.id=m.to_warehouse_id ";
        long total = count("select count(*)" + fromSql + where, args);
        ArrayList<Object> paged = new ArrayList<>(args);
        paged.add(limit(size));
        paged.add(offset(page, size));
        List<MovementResponse> rows = jdbc.query("""
                select m.id,m.product_id,p.sku,p.name,
                       m.from_warehouse_id,fw.name from_warehouse_name,
                       m.to_warehouse_id,tw.name to_warehouse_name,
                       m.movement_type,m.quantity,m.unit_cost,m.source_type,m.source_id,
                       m.source_line_id,m.reason,m.actor_id,m.actor_email,m.occurred_at
                """ + fromSql + where + " order by m.occurred_at desc,m.id desc limit ? offset ?",
                (rs, row) -> movement(rs), paged.toArray());
        return page(rows, page, size, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<SerialResponse> serials(String search, Long warehouseId, String status,
                                                 int page, int size) {
        Long tenantId = tenantContext.requireTenantId();
        StringBuilder where = new StringBuilder(" where s.tenant_id=?");
        ArrayList<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (search != null && !search.isBlank()) {
            where.append(" and (lower(p.name) like ? or lower(p.sku) like ? or lower(coalesce(s.serial_number,'')) like ? or lower(coalesce(s.imei,'')) like ?)");
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(pattern); args.add(pattern); args.add(pattern); args.add(pattern);
        }
        if (warehouseId != null) {
            where.append(" and s.warehouse_id=?");
            args.add(warehouseId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" and s.status=?");
            args.add(status.trim().toUpperCase(Locale.ROOT));
        }
        String fromSql = " from inventory_serial_units s join products p on p.id=s.product_id "
                + "join pur_warehouses w on w.id=s.warehouse_id "
                + "join inventory_locations l on l.id=s.location_id ";
        long total = count("select count(*)" + fromSql + where, args);
        ArrayList<Object> paged = new ArrayList<>(args);
        paged.add(limit(size)); paged.add(offset(page, size));
        List<SerialResponse> rows = jdbc.query("""
                select s.id,s.product_id,p.sku,p.name,s.warehouse_id,w.name warehouse_name,
                       s.location_id,l.name location_name,s.serial_number,s.imei,s.status,
                       s.source_type,s.source_id,s.received_at,s.issued_at,s.version
                """ + fromSql + where + " order by s.received_at desc,s.id desc limit ? offset ?",
                (rs, row) -> serial(rs), paged.toArray());
        return page(rows, page, size, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> reservations(String sourceType, String status,
                                                           int page, int size) {
        Long tenantId = tenantContext.requireTenantId();
        StringBuilder where = new StringBuilder(" where r.tenant_id=?");
        ArrayList<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (sourceType != null && !sourceType.isBlank()) {
            where.append(" and r.source_type=?");
            args.add(sourceType.trim().toUpperCase(Locale.ROOT));
        }
        if (status != null && !status.isBlank()) {
            where.append(" and r.status=?");
            args.add(status.trim().toUpperCase(Locale.ROOT));
        }
        String fromSql = " from inventory_reservations r join products p on p.id=r.product_id "
                + "join pur_warehouses w on w.id=r.warehouse_id "
                + "left join inventory_serial_units s on s.id=r.serial_unit_id ";
        long total = count("select count(*)" + fromSql + where, args);
        ArrayList<Object> paged = new ArrayList<>(args);
        paged.add(limit(size)); paged.add(offset(page, size));
        List<ReservationResponse> rows = jdbc.query("""
                select r.id,r.product_id,p.sku,p.name,r.warehouse_id,w.name warehouse_name,
                       r.source_type,r.source_id,r.source_line_id,r.quantity,r.fulfilled_quantity,
                       r.status,s.serial_number,s.imei,r.expires_at,r.created_at,r.updated_at,r.version
                """ + fromSql + where + " order by r.created_at desc,r.id desc limit ? offset ?",
                (rs, row) -> reservation(rs), paged.toArray());
        return page(rows, page, size, total);
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        Long tenantId = tenantContext.requireTenantId();
        return jdbc.queryForObject("""
                select
                  (select count(*) from pur_warehouses where tenant_id=? and active) warehouse_count,
                  (select count(distinct product_id) from inventory_balances where tenant_id=? and on_hand>0) product_count,
                  (select count(*) from inventory_balances where tenant_id=? and on_hand-reserved<=reorder_point) low_stock_count,
                  (select count(*) from inventory_reservations where tenant_id=? and status='ACTIVE') reservation_count,
                  (select count(*) from inventory_transfers where tenant_id=? and status in ('DRAFT','SHIPPED')) transfer_count,
                  (select count(*) from inventory_counts where tenant_id=? and status in ('DRAFT','COUNTED')) count_count,
                  (select coalesce(sum(on_hand),0) from inventory_balances where tenant_id=?) on_hand,
                  (select coalesce(sum(on_hand-reserved),0) from inventory_balances where tenant_id=?) available,
                  (select coalesce(sum(on_hand*average_unit_cost),0) from inventory_balances where tenant_id=?) inventory_value
                """, (rs, row) -> new DashboardResponse(rs.getLong("warehouse_count"),
                        rs.getLong("product_count"), rs.getLong("low_stock_count"),
                        rs.getLong("reservation_count"), rs.getLong("transfer_count"),
                        rs.getLong("count_count"), rs.getBigDecimal("on_hand"),
                        rs.getBigDecimal("available"), rs.getBigDecimal("inventory_value")),
                tenantId, tenantId, tenantId, tenantId, tenantId, tenantId,
                tenantId, tenantId, tenantId);
    }

    @Transactional(readOnly = true)
    public ReportResponse report(Instant from, Instant to) {
        Long tenantId = tenantContext.requireTenantId();
        List<MetricPoint> warehouseValues = jdbc.query("""
                select w.name label,coalesce(sum(b.on_hand*b.average_unit_cost),0) value,
                       count(distinct b.product_id) count
                from pur_warehouses w left join inventory_balances b
                  on b.warehouse_id=w.id and b.tenant_id=w.tenant_id
                where w.tenant_id=? group by w.id,w.name,w.display_order order by w.display_order,w.name
                """, (rs, row) -> metric(rs), tenantId);
        StringBuilder movementSql = new StringBuilder("""
                select movement_type label,coalesce(sum(quantity),0) value,count(*) count
                from inventory_movements where tenant_id=?
                """);
        ArrayList<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (from != null) {
            movementSql.append(" and occurred_at>=?");
            args.add(java.sql.Timestamp.from(from));
        }
        if (to != null) {
            movementSql.append(" and occurred_at<?");
            args.add(java.sql.Timestamp.from(to));
        }
        movementSql.append(" group by movement_type order by count desc,movement_type");
        List<MetricPoint> movements = jdbc.query(movementSql.toString(),
                (rs, row) -> metric(rs), args.toArray());
        List<BalanceResponse> lowStock = balances(null, null, true, 0, 100).content();
        return new ReportResponse(dashboard(), warehouseValues, movements, lowStock);
    }

    @Transactional(readOnly = true)
    public byte[] exportBalances(String search, Long warehouseId, Boolean lowStock) {
        PageResponse<BalanceResponse> page = balances(search, warehouseId, lowStock, 0, 10_000);
        StringBuilder csv = new StringBuilder("\uFEFFWarehouse,Location,SKU,Product,On Hand,Reserved,Available,Average Cost,Value,Reorder Point\r\n");
        for (BalanceResponse row : page.content()) {
            csv.append(csv(row.warehouseName())).append(',')
                    .append(csv(row.locationName())).append(',')
                    .append(csv(row.sku())).append(',')
                    .append(csv(row.productName())).append(',')
                    .append(row.onHand()).append(',').append(row.reserved()).append(',')
                    .append(row.available()).append(',').append(row.averageUnitCost()).append(',')
                    .append(row.inventoryValue()).append(',').append(row.reorderPoint()).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private SqlFilter balanceFilter(Long tenantId, String search, Long warehouseId, Boolean lowStock) {
        StringBuilder where = new StringBuilder(" where b.tenant_id=?");
        ArrayList<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (search != null && !search.isBlank()) {
            where.append(" and (lower(p.name) like ? or lower(p.sku) like ?)");
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(pattern); args.add(pattern);
        }
        if (warehouseId != null) {
            where.append(" and b.warehouse_id=?");
            args.add(warehouseId);
        }
        if (Boolean.TRUE.equals(lowStock)) {
            where.append(" and b.on_hand-b.reserved<=b.reorder_point");
        }
        return new SqlFilter(where.toString(), args);
    }

    private String balanceFrom() {
        return " from inventory_balances b join products p on p.id=b.product_id "
                + "join pur_warehouses w on w.id=b.warehouse_id "
                + "join inventory_locations l on l.id=b.location_id ";
    }

    private String balanceColumns() {
        return "b.id,b.warehouse_id,w.code warehouse_code,w.name warehouse_name,"
                + "b.location_id,l.code location_code,l.name location_name,b.product_id,p.sku,p.name,"
                + "b.on_hand,b.reserved,(b.on_hand-b.reserved) available,b.average_unit_cost,"
                + "(b.on_hand*b.average_unit_cost) inventory_value,b.reorder_point,"
                + "(b.on_hand-b.reserved<=b.reorder_point) low_stock,b.last_movement_at,b.version";
    }

    private BalanceResponse balance(ResultSet rs) throws SQLException {
        return new BalanceResponse(rs.getLong("id"), rs.getLong("warehouse_id"),
                rs.getString("warehouse_code"), rs.getString("warehouse_name"),
                rs.getLong("location_id"), rs.getString("location_code"),
                rs.getString("location_name"), rs.getLong("product_id"),
                rs.getString("sku"), rs.getString("name"), rs.getBigDecimal("on_hand"),
                rs.getBigDecimal("reserved"), rs.getBigDecimal("available"),
                rs.getBigDecimal("average_unit_cost"), rs.getBigDecimal("inventory_value"),
                rs.getBigDecimal("reorder_point"), rs.getBoolean("low_stock"),
                instant(rs, "last_movement_at"), rs.getLong("version"));
    }

    private MovementResponse movement(ResultSet rs) throws SQLException {
        return new MovementResponse(rs.getLong("id"), rs.getLong("product_id"),
                rs.getString("sku"), rs.getString("name"),
                (Long) rs.getObject("from_warehouse_id"), rs.getString("from_warehouse_name"),
                (Long) rs.getObject("to_warehouse_id"), rs.getString("to_warehouse_name"),
                rs.getString("movement_type"), rs.getBigDecimal("quantity"),
                rs.getBigDecimal("unit_cost"), rs.getString("source_type"),
                (Long) rs.getObject("source_id"), (Long) rs.getObject("source_line_id"),
                rs.getString("reason"), (Long) rs.getObject("actor_id"),
                rs.getString("actor_email"), rs.getTimestamp("occurred_at").toInstant());
    }

    private SerialResponse serial(ResultSet rs) throws SQLException {
        return new SerialResponse(rs.getLong("id"), rs.getLong("product_id"),
                rs.getString("sku"), rs.getString("name"), rs.getLong("warehouse_id"),
                rs.getString("warehouse_name"), rs.getLong("location_id"),
                rs.getString("location_name"), rs.getString("serial_number"),
                rs.getString("imei"), rs.getString("status"), rs.getString("source_type"),
                (Long) rs.getObject("source_id"), rs.getTimestamp("received_at").toInstant(),
                instant(rs, "issued_at"), rs.getLong("version"));
    }

    private ReservationResponse reservation(ResultSet rs) throws SQLException {
        return new ReservationResponse(rs.getLong("id"), rs.getLong("product_id"),
                rs.getString("sku"), rs.getString("name"), rs.getLong("warehouse_id"),
                rs.getString("warehouse_name"), rs.getString("source_type"),
                rs.getLong("source_id"), (Long) rs.getObject("source_line_id"),
                rs.getBigDecimal("quantity"), rs.getBigDecimal("fulfilled_quantity"),
                rs.getString("status"), rs.getString("serial_number"), rs.getString("imei"),
                instant(rs, "expires_at"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(), rs.getLong("version"));
    }

    private MetricPoint metric(ResultSet rs) throws SQLException {
        return new MetricPoint(rs.getString("label"), rs.getBigDecimal("value"),
                rs.getLong("count"));
    }

    private long count(String sql, List<Object> args) {
        Long value = jdbc.queryForObject(sql, Long.class, args.toArray());
        return value == null ? 0 : value;
    }

    private <T> PageResponse<T> page(List<T> content, int requestedPage, int requestedSize, long total) {
        int page = Math.max(requestedPage, 0);
        int size = limit(requestedSize);
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, pages, page == 0,
                pages == 0 || page >= pages - 1);
    }

    private int limit(int size) {
        return Math.min(Math.max(size, 1), 10_000);
    }

    private int offset(int page, int size) {
        return Math.max(page, 0) * limit(size);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }

    private record SqlFilter(String sql, List<Object> args) {
    }
}
