package com.sami.app.inventory.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.inventory.domain.InventoryWarehouse;
import com.sami.app.inventory.event.InventoryDomainEvent;
import com.sami.app.inventory.repository.InventoryWarehouseRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Concurrency-safe balance projection and immutable movement writer. All
 * physical stock mutations pass through this service inside a caller-owned
 * transaction.
 */
@Service
@RequiredArgsConstructor
public class InventoryLedgerService {

    private final JdbcTemplate jdbc;
    private final InventoryWarehouseRepository warehouses;
    private final InventoryAuditService audit;
    private final ApplicationEventPublisher events;

    @Transactional(propagation = Propagation.MANDATORY)
    public InventoryWarehouse requireWarehouse(Long tenantId, Long warehouseId) {
        return warehouses.findByIdAndTenantId(warehouseId, tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Warehouse", warehouseId));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public InventoryWarehouse salesWarehouse(Long tenantId, Long branchId) {
        return warehouses
                .findFirstByTenantIdAndBranchIdAndActiveTrueOrderByDefaultWarehouseDescDisplayOrderAsc(
                        tenantId, branchId)
                .or(() -> warehouses.findByTenantIdAndDefaultWarehouseTrueAndActiveTrue(tenantId))
                .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "No active Inventory warehouse is configured for this branch"));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long requireLocation(Long tenantId, Long warehouseId, Long locationId) {
        if (locationId == null) {
            List<Long> result = jdbc.query("""
                    select id from inventory_locations
                    where tenant_id=? and warehouse_id=? and is_default and active
                    order by id limit 1
                    """, (rs, row) -> rs.getLong(1), tenantId, warehouseId);
            if (result.isEmpty()) {
                throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "Warehouse has no active default location");
            }
            return result.getFirst();
        }
        Integer count = jdbc.queryForObject("""
                select count(*) from inventory_locations
                where id=? and tenant_id=? and warehouse_id=? and active
                """, Integer.class, locationId, tenantId, warehouseId);
        if (count == null || count != 1) {
            throw new ApiException(ErrorCode.ACCESS_DENIED,
                    "Location is outside the trusted warehouse scope");
        }
        return locationId;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ProductInfo requireProduct(Long tenantId, Long productId) {
        List<ProductInfo> rows = jdbc.query("""
                select id,sku,name from products where id=? and tenant_id=? and active
                """, (rs, row) -> new ProductInfo(rs.getLong("id"), rs.getString("sku"),
                rs.getString("name")), productId, tenantId);
        if (rows.isEmpty()) {
            throw ResourceNotFoundException.of("Product", productId);
        }
        return rows.getFirst();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean increase(Long tenantId, Long productId, Long warehouseId, Long locationId,
                            BigDecimal quantity, BigDecimal unitCost, String movementType,
                            String sourceType, Long sourceId, Long sourceLineId,
                            String operationKey, String reason) {
        requirePositive(quantity);
        requireProduct(tenantId, productId);
        requireWarehouse(tenantId, warehouseId);
        Long location = requireLocation(tenantId, warehouseId, locationId);
        if (movementExists(tenantId, operationKey)) {
            return false;
        }
        BalanceState state = lockBalance(tenantId, warehouseId, location, productId);
        BigDecimal cost = nonNegative(unitCost);
        BigDecimal newOnHand = state.onHand().add(quantity);
        BigDecimal newAverage = state.averageCost();
        if (cost.signum() > 0 && newOnHand.signum() > 0) {
            newAverage = state.onHand().multiply(state.averageCost())
                    .add(quantity.multiply(cost))
                    .divide(newOnHand, 4, RoundingMode.HALF_UP);
        }
        updateBalance(state.id(), newOnHand, state.reserved(), newAverage);
        recordMovement(tenantId, productId, null, null, warehouseId, location,
                movementType, quantity, cost, sourceType, sourceId, sourceLineId,
                operationKey, reason);
        syncProductProjection(tenantId, productId);
        return true;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public BigDecimal decrease(Long tenantId, Long productId, Long warehouseId, Long locationId,
                               BigDecimal quantity, String movementType, String sourceType,
                               Long sourceId, Long sourceLineId, String operationKey, String reason) {
        requirePositive(quantity);
        requireProduct(tenantId, productId);
        requireWarehouse(tenantId, warehouseId);
        Long location = requireLocation(tenantId, warehouseId, locationId);
        if (movementExists(tenantId, operationKey)) {
            return currentAverage(tenantId, warehouseId, location, productId);
        }
        BalanceState state = lockBalance(tenantId, warehouseId, location, productId);
        BigDecimal newOnHand = state.onHand().subtract(quantity);
        if (newOnHand.compareTo(BigDecimal.ZERO) < 0
                || newOnHand.compareTo(state.reserved()) < 0) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Insufficient available stock for product " + productId);
        }
        updateBalance(state.id(), newOnHand, state.reserved(), state.averageCost());
        recordMovement(tenantId, productId, warehouseId, location, null, null,
                movementType, quantity, state.averageCost(), sourceType, sourceId,
                sourceLineId, operationKey, reason);
        syncProductProjection(tenantId, productId);
        return state.averageCost();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long reserve(Long tenantId, Long productId, Long warehouseId, Long locationId,
                        BigDecimal quantity, String sourceType, Long sourceId, Long sourceLineId,
                        String serialNumber, String imei) {
        requirePositive(quantity);
        requireProduct(tenantId, productId);
        Long location = requireLocation(tenantId, warehouseId, locationId);
        BalanceState state = lockBalance(tenantId, warehouseId, location, productId);
        if (state.onHand().subtract(state.reserved()).compareTo(quantity) < 0) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Insufficient available stock for product " + productId);
        }
        Long serialId = findSerialForUpdate(tenantId, productId, warehouseId,
                serialNumber, imei, "AVAILABLE");
        if (serialId != null && quantity.compareTo(BigDecimal.ONE) != 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Serialized reservations must have quantity 1");
        }
        updateBalance(state.id(), state.onHand(), state.reserved().add(quantity), state.averageCost());
        Long reservationId = jdbc.queryForObject("""
                insert into inventory_reservations(
                    tenant_id,product_id,warehouse_id,location_id,source_type,source_id,
                    source_line_id,quantity,serial_unit_id,created_by)
                values(?,?,?,?,?,?,?,?,?,?) returning id
                """, Long.class, tenantId, productId, warehouseId, location,
                normalize(sourceType), sourceId, sourceLineId, quantity, serialId, CurrentActor.id());
        if (serialId != null) {
            jdbc.update("update inventory_serial_units set status='RESERVED',updated_at=now(),version=version+1 where id=?",
                    serialId);
        }
        recordMovement(tenantId, productId, warehouseId, location, null, null,
                "RESERVE", quantity, state.averageCost(), sourceType, sourceId,
                sourceLineId, "RESERVE-" + normalize(sourceType) + "-" + sourceId + "-" + sourceLineId,
                null);
        return reservationId;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseReservation(Long tenantId, ReservationState reservation, String reason) {
        BalanceState state = lockBalance(tenantId, reservation.warehouseId(),
                reservation.locationId(), reservation.productId());
        BigDecimal remaining = reservation.quantity().subtract(reservation.fulfilledQuantity());
        if (remaining.signum() <= 0) {
            return;
        }
        if (state.reserved().compareTo(remaining) < 0) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Reservation projection is inconsistent");
        }
        updateBalance(state.id(), state.onHand(), state.reserved().subtract(remaining),
                state.averageCost());
        jdbc.update("""
                update inventory_reservations set status='RELEASED',updated_at=now(),version=version+1
                where id=? and tenant_id=? and status='ACTIVE'
                """, reservation.id(), tenantId);
        if (reservation.serialUnitId() != null) {
            jdbc.update("""
                    update inventory_serial_units set status='AVAILABLE',updated_at=now(),version=version+1
                    where id=? and tenant_id=? and status='RESERVED'
                    """, reservation.serialUnitId(), tenantId);
        }
        recordMovement(tenantId, reservation.productId(), reservation.warehouseId(),
                reservation.locationId(), null, null, "RELEASE", remaining,
                state.averageCost(), reservation.sourceType(), reservation.sourceId(),
                reservation.sourceLineId(), "RELEASE-" + reservation.id(), reason);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void issueReservation(Long tenantId, ReservationState reservation) {
        BalanceState state = lockBalance(tenantId, reservation.warehouseId(),
                reservation.locationId(), reservation.productId());
        BigDecimal remaining = reservation.quantity().subtract(reservation.fulfilledQuantity());
        if (remaining.signum() <= 0 || state.onHand().compareTo(remaining) < 0
                || state.reserved().compareTo(remaining) < 0) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Reserved stock is no longer available");
        }
        updateBalance(state.id(), state.onHand().subtract(remaining),
                state.reserved().subtract(remaining), state.averageCost());
        jdbc.update("""
                update inventory_reservations
                set fulfilled_quantity=quantity,status='FULFILLED',updated_at=now(),version=version+1
                where id=? and tenant_id=? and status='ACTIVE'
                """, reservation.id(), tenantId);
        if (reservation.serialUnitId() != null) {
            jdbc.update("""
                    update inventory_serial_units
                    set status='ISSUED',issued_at=now(),updated_at=now(),version=version+1
                    where id=? and tenant_id=? and status='RESERVED'
                    """, reservation.serialUnitId(), tenantId);
        }
        recordMovement(tenantId, reservation.productId(), reservation.warehouseId(),
                reservation.locationId(), null, null, "ISSUE", remaining,
                state.averageCost(), reservation.sourceType(), reservation.sourceId(),
                reservation.sourceLineId(), "ISSUE-" + reservation.id(), null);
        syncProductProjection(tenantId, reservation.productId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<ReservationState> activeReservations(Long tenantId, String sourceType, Long sourceId) {
        return jdbc.query("""
                select id,product_id,warehouse_id,location_id,source_type,source_id,source_line_id,
                       quantity,fulfilled_quantity,serial_unit_id
                from inventory_reservations
                where tenant_id=? and source_type=? and source_id=? and status='ACTIVE'
                order by id for update
                """, (rs, row) -> new ReservationState(rs.getLong("id"),
                rs.getLong("product_id"), rs.getLong("warehouse_id"),
                rs.getLong("location_id"), rs.getString("source_type"),
                rs.getLong("source_id"), (Long) rs.getObject("source_line_id"),
                rs.getBigDecimal("quantity"), rs.getBigDecimal("fulfilled_quantity"),
                (Long) rs.getObject("serial_unit_id")), tenantId, normalize(sourceType), sourceId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long createSerial(Long tenantId, Long productId, Long warehouseId, Long locationId,
                             String serialNumber, String imei, String sourceType,
                             Long sourceId, Long sourceLineId) {
        String serial = blank(serialNumber);
        String normalizedImei = blank(imei);
        if (serial == null && normalizedImei == null) {
            return null;
        }
        try {
            return jdbc.queryForObject("""
                    insert into inventory_serial_units(
                        tenant_id,product_id,warehouse_id,location_id,serial_number,imei,
                        status,source_type,source_id,source_line_id)
                    values(?,?,?,?,?,?,'AVAILABLE',?,?,?)
                    returning id
                    """, Long.class, tenantId, productId, warehouseId, locationId, serial, normalizedImei,
                    normalize(sourceType), sourceId, sourceLineId);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Serial number or IMEI already exists in this tenant");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markSupplierReturnedSerials(Long tenantId, Long productId, Long warehouseId,
                                            Long purchaseLineId, int quantity) {
        List<Long> ids = jdbc.query("""
                select id from inventory_serial_units
                where tenant_id=? and product_id=? and warehouse_id=? and source_type='PURCHASE'
                  and source_line_id=? and status='AVAILABLE'
                order by received_at,id limit ? for update
                """, (rs, row) -> rs.getLong(1), tenantId, productId, warehouseId,
                purchaseLineId, quantity);
        if (!ids.isEmpty() && ids.size() != quantity) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Not enough matching serialized units are available for supplier return");
        }
        ids.forEach(id -> jdbc.update("""
                update inventory_serial_units
                set status='RETURNED_TO_SUPPLIER',updated_at=now(),version=version+1 where id=?
                """, id));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void restoreIssuedSerial(Long tenantId, Long productId, Long warehouseId,
                                    Long locationId, String serial, String imei) {
        if (blank(serial) == null && blank(imei) == null) {
            return;
        }
        int changed = jdbc.update("""
                update inventory_serial_units
                set warehouse_id=?,location_id=?,status='AVAILABLE',issued_at=null,
                    updated_at=now(),version=version+1
                where tenant_id=? and product_id=? and status='ISSUED'
                  and (? is null or serial_number=?) and (? is null or imei=?)
                """, warehouseId, locationId, tenantId, productId,
                blank(serial), blank(serial), blank(imei), blank(imei));
        if (changed != 1) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Issued serialized unit was not found for return");
        }
    }

    public void audit(Long tenantId, String entityType, Long entityId, String action,
                      Map<String, Object> oldValues, Map<String, Object> newValues) {
        audit.record(tenantId, entityType, entityId, action, oldValues, newValues);
    }

    public void publish(Long tenantId, String eventType, String entityType,
                        Long entityId, Map<String, Object> detail) {
        events.publishEvent(new InventoryDomainEvent(tenantId, eventType, entityType,
                entityId, detail == null ? Map.of() : detail, CurrentActor.id(), Instant.now()));
    }

    public BigDecimal onHand(Long tenantId, Long warehouseId, Long locationId, Long productId) {
        return lockBalance(tenantId, warehouseId, locationId, productId).onHand();
    }

    private BalanceState lockBalance(Long tenantId, Long warehouseId, Long locationId, Long productId) {
        jdbc.update("""
                insert into inventory_balances(tenant_id,warehouse_id,location_id,product_id)
                values(?,?,?,?) on conflict(tenant_id,warehouse_id,location_id,product_id) do nothing
                """, tenantId, warehouseId, locationId, productId);
        return jdbc.query("""
                select id,on_hand,reserved,average_unit_cost from inventory_balances
                where tenant_id=? and warehouse_id=? and location_id=? and product_id=? for update
                """, (rs, row) -> new BalanceState(rs.getLong("id"),
                rs.getBigDecimal("on_hand"), rs.getBigDecimal("reserved"),
                rs.getBigDecimal("average_unit_cost")), tenantId, warehouseId,
                locationId, productId).getFirst();
    }

    private void updateBalance(Long id, BigDecimal onHand, BigDecimal reserved, BigDecimal averageCost) {
        jdbc.update("""
                update inventory_balances
                set on_hand=?,reserved=?,average_unit_cost=?,last_movement_at=now(),
                    updated_at=now(),version=version+1 where id=?
                """, onHand, reserved, averageCost, id);
    }

    private void recordMovement(Long tenantId, Long productId,
                                Long fromWarehouseId, Long fromLocationId,
                                Long toWarehouseId, Long toLocationId,
                                String movementType, BigDecimal quantity, BigDecimal unitCost,
                                String sourceType, Long sourceId, Long sourceLineId,
                                String operationKey, String reason) {
        jdbc.update("""
                insert into inventory_movements(
                    tenant_id,product_id,from_warehouse_id,from_location_id,
                    to_warehouse_id,to_location_id,movement_type,quantity,unit_cost,
                    source_type,source_id,source_line_id,operation_key,reason,actor_id,actor_email)
                values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, tenantId, productId, fromWarehouseId, fromLocationId,
                toWarehouseId, toLocationId, normalize(movementType), quantity,
                nonNegative(unitCost), normalize(sourceType), sourceId, sourceLineId,
                operationKey, reason, CurrentActor.id(), CurrentActor.email());
    }

    private boolean movementExists(Long tenantId, String operationKey) {
        if (operationKey == null || operationKey.isBlank()) {
            return false;
        }
        Integer count = jdbc.queryForObject("""
                select count(*) from inventory_movements where tenant_id=? and operation_key=?
                """, Integer.class, tenantId, operationKey);
        return count != null && count > 0;
    }

    private Long findSerialForUpdate(Long tenantId, Long productId, Long warehouseId,
                                     String serialNumber, String imei, String status) {
        String serial = blank(serialNumber);
        String normalizedImei = blank(imei);
        if (serial == null && normalizedImei == null) {
            return null;
        }
        List<Long> result = jdbc.query("""
                select id from inventory_serial_units
                where tenant_id=? and product_id=? and warehouse_id=? and status=?
                  and (? is null or serial_number=?) and (? is null or imei=?)
                for update
                """, (rs, row) -> rs.getLong(1), tenantId, productId, warehouseId,
                status, serial, serial, normalizedImei, normalizedImei);
        if (result.size() != 1) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "Requested serialized unit is not available");
        }
        return result.getFirst();
    }

    private BigDecimal currentAverage(Long tenantId, Long warehouseId, Long locationId, Long productId) {
        BigDecimal value = jdbc.queryForObject("""
                select average_unit_cost from inventory_balances
                where tenant_id=? and warehouse_id=? and location_id=? and product_id=?
                """, BigDecimal.class, tenantId, warehouseId, locationId, productId);
        return value == null ? BigDecimal.ZERO : value;
    }

    private void syncProductProjection(Long tenantId, Long productId) {
        jdbc.update("""
                update products p set stock_quantity=coalesce((
                    select floor(sum(b.on_hand))::integer from inventory_balances b
                    where b.tenant_id=p.tenant_id and b.product_id=p.id),0),
                    updated_at=now(),version=version+1
                where p.id=? and p.tenant_id=?
                """, productId, tenantId);
    }

    private void requirePositive(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Quantity must be positive");
        }
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.signum() < 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unit cost cannot be negative");
        }
        return value;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record BalanceState(Long id, BigDecimal onHand, BigDecimal reserved,
                                BigDecimal averageCost) {
    }

    public record ProductInfo(Long id, String sku, String name) {
    }

    public record ReservationState(Long id, Long productId, Long warehouseId,
                                   Long locationId, String sourceType, Long sourceId,
                                   Long sourceLineId, BigDecimal quantity,
                                   BigDecimal fulfilledQuantity, Long serialUnitId) {
    }
}
