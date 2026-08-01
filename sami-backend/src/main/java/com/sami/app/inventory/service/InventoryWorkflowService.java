package com.sami.app.inventory.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.common.api.PageResponse;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.dto.InventoryDtos.AdjustmentRequest;
import com.sami.app.inventory.dto.InventoryDtos.AdjustmentResponse;
import com.sami.app.inventory.dto.InventoryDtos.CountLineResponse;
import com.sami.app.inventory.dto.InventoryDtos.CountRequest;
import com.sami.app.inventory.dto.InventoryDtos.CountResponse;
import com.sami.app.inventory.dto.InventoryDtos.CountResultRequest;
import com.sami.app.inventory.dto.InventoryDtos.ImportResult;
import com.sami.app.inventory.dto.InventoryDtos.SerialStatusRequest;
import com.sami.app.inventory.dto.InventoryDtos.TransferLineRequest;
import com.sami.app.inventory.dto.InventoryDtos.TransferLineResponse;
import com.sami.app.inventory.dto.InventoryDtos.TransferRequest;
import com.sami.app.inventory.dto.InventoryDtos.TransferResponse;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Adjustments, transfers, counts, serial lifecycle and CSV import workflows. */
@Service
@RequiredArgsConstructor
public class InventoryWorkflowService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };
    private static final Set<String> SERIAL_STATUSES = Set.of("AVAILABLE", "QUARANTINED");

    private final TenantContext tenantContext;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final InventoryLedgerService ledger;
    private final InventoryStockOperations stockOperations;

    @Transactional
    public AdjustmentResponse adjust(AdjustmentRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        ledger.requireWarehouse(tenantId, request.warehouseId());
        Long locationId = ledger.requireLocation(tenantId, request.warehouseId(), request.locationId());
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "At least one adjustment line is required");
        }
        Set<Long> products = new HashSet<>();
        String baseKey = request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                ? UUID.randomUUID().toString() : request.idempotencyKey().trim();
        BigDecimal increase = BigDecimal.ZERO;
        BigDecimal decrease = BigDecimal.ZERO;
        int index = 0;
        for (AdjustmentRequest.AdjustmentLine line : request.lines()) {
            index++;
            if (!products.add(line.productId())) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Each product may appear only once in an adjustment");
            }
            if (line.quantity() == null || line.quantity().signum() == 0) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Adjustment quantity cannot be zero");
            }
            String operationKey = "ADJUSTMENT-" + baseKey + "-" + index;
            if (line.quantity().signum() > 0) {
                ledger.increase(tenantId, line.productId(), request.warehouseId(), locationId,
                        line.quantity(), line.unitCost(), "ADJUSTMENT_IN", "ADJUSTMENT", null,
                        line.productId(), operationKey, request.reason());
                increase = increase.add(line.quantity());
            } else {
                BigDecimal quantity = line.quantity().abs();
                ledger.decrease(tenantId, line.productId(), request.warehouseId(), locationId,
                        quantity, "ADJUSTMENT_OUT", "ADJUSTMENT", null, line.productId(),
                        operationKey, request.reason());
                decrease = decrease.add(quantity);
            }
        }
        ledger.audit(tenantId, "ADJUSTMENT", null, "POSTED", null,
                Map.of("warehouseId", request.warehouseId(), "locationId", locationId,
                        "reason", request.reason(), "lines", request.lines().size()));
        ledger.publish(tenantId, "StockAdjusted", "ADJUSTMENT", null,
                Map.of("warehouseId", request.warehouseId(), "lines", request.lines().size()));
        return new AdjustmentResponse(request.lines().size(), increase, decrease, Instant.now());
    }

    @Transactional
    public TransferResponse createTransfer(TransferRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        if (request.fromWarehouseId().equals(request.toWarehouseId())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Source and destination warehouses must be different");
        }
        ledger.requireWarehouse(tenantId, request.fromWarehouseId());
        ledger.requireWarehouse(tenantId, request.toWarehouseId());
        Set<Long> products = new HashSet<>();
        request.lines().forEach(line -> {
            ledger.requireProduct(tenantId, line.productId());
            if (!products.add(line.productId())) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Each product may appear only once in a transfer");
            }
            validateSerialQuantity(line);
        });
        String number = nextNumber(tenantId, "TRANSFER", "TRF");
        Long id = jdbc.queryForObject("""
                insert into inventory_transfers(
                    tenant_id,transfer_number,from_warehouse_id,to_warehouse_id,notes,created_by)
                values(?,?,?,?,?,?) returning id
                """, Long.class, tenantId, number, request.fromWarehouseId(),
                request.toWarehouseId(), blank(request.notes()), com.sami.app.security.CurrentActor.id());
        for (TransferLineRequest line : request.lines()) {
            jdbc.update("""
                    insert into inventory_transfer_items(transfer_id,product_id,quantity,serial_numbers)
                    values(?,?,?,cast(? as jsonb))
                    """, id, line.productId(), line.quantity(), json(line.serialNumbers()));
        }
        ledger.audit(tenantId, "TRANSFER", id, "CREATED", null,
                Map.of("transferNumber", number, "lines", request.lines().size()));
        ledger.publish(tenantId, "TransferCreated", "TRANSFER", id,
                Map.of("transferNumber", number));
        return getTransfer(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> transfers(String status, int page, int size) {
        Long tenantId = tenantContext.requireTenantId();
        String where = " where tenant_id=?";
        ArrayList<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (status != null && !status.isBlank()) {
            where += " and status=?";
            args.add(status.trim().toUpperCase(Locale.ROOT));
        }
        Long totalValue = jdbc.queryForObject("select count(*) from inventory_transfers" + where,
                Long.class, args.toArray());
        int actualSize = Math.min(Math.max(size, 1), 100);
        args.add(actualSize);
        args.add(Math.max(page, 0) * actualSize);
        List<Long> ids = jdbc.query("select id from inventory_transfers" + where
                        + " order by created_at desc,id desc limit ? offset ?",
                (rs, row) -> rs.getLong(1), args.toArray());
        List<TransferResponse> rows = ids.stream().map(this::getTransfer).toList();
        return page(rows, page, actualSize, totalValue == null ? 0 : totalValue);
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(Long id) {
        Long tenantId = tenantContext.requireTenantId();
        List<TransferResponse> rows = jdbc.query("""
                select t.id,t.transfer_number,t.from_warehouse_id,fw.name from_name,
                       t.to_warehouse_id,tw.name to_name,t.status,t.notes,t.created_by,
                       t.shipped_by,t.received_by,t.shipped_at,t.received_at,t.cancelled_at,
                       t.created_at,t.updated_at,t.version
                from inventory_transfers t
                join pur_warehouses fw on fw.id=t.from_warehouse_id
                join pur_warehouses tw on tw.id=t.to_warehouse_id
                where t.id=? and t.tenant_id=?
                """, (rs, row) -> transfer(rs, transferItems(id)), id, tenantId);
        if (rows.isEmpty()) {
            throw ResourceNotFoundException.of("Inventory transfer", id);
        }
        return rows.getFirst();
    }

    @Transactional
    public TransferResponse shipTransfer(Long id) {
        Long tenantId = tenantContext.requireTenantId();
        TransferState state = lockTransfer(id, tenantId);
        requireStatus("Transfer", state.status(), "DRAFT");
        Long sourceLocation = ledger.requireLocation(tenantId, state.fromWarehouseId(), null);
        for (TransferItemState item : transferItemStates(id)) {
            validateTransferSerials(tenantId, state.fromWarehouseId(), item);
            BigDecimal unitCost = ledger.decrease(tenantId, item.productId(),
                    state.fromWarehouseId(), sourceLocation, item.quantity(), "TRANSFER_OUT",
                    "TRANSFER", id, item.id(), "TRANSFER-OUT-" + id + "-" + item.id(),
                    "Transfer " + state.number());
            if (!item.serialNumbers().isEmpty()) {
                item.serialNumbers().forEach(serial -> jdbc.update("""
                        update inventory_serial_units set status='IN_TRANSIT',updated_at=now(),version=version+1
                        where tenant_id=? and product_id=? and warehouse_id=? and serial_number=? and status='AVAILABLE'
                        """, tenantId, item.productId(), state.fromWarehouseId(), serial));
            }
            jdbc.update("""
                    update inventory_movements set unit_cost=?
                    where tenant_id=? and operation_key=?
                    """, unitCost, tenantId, "TRANSFER-OUT-" + id + "-" + item.id());
        }
        jdbc.update("""
                update inventory_transfers set status='SHIPPED',shipped_by=?,shipped_at=now(),
                    updated_at=now(),version=version+1 where id=? and tenant_id=?
                """, com.sami.app.security.CurrentActor.id(), id, tenantId);
        ledger.audit(tenantId, "TRANSFER", id, "SHIPPED", Map.of("status", "DRAFT"),
                Map.of("status", "SHIPPED"));
        ledger.publish(tenantId, "TransferShipped", "TRANSFER", id,
                Map.of("transferNumber", state.number()));
        return getTransfer(id);
    }

    @Transactional
    public TransferResponse receiveTransfer(Long id) {
        Long tenantId = tenantContext.requireTenantId();
        TransferState state = lockTransfer(id, tenantId);
        requireStatus("Transfer", state.status(), "SHIPPED");
        Long destinationLocation = ledger.requireLocation(tenantId, state.toWarehouseId(), null);
        for (TransferItemState item : transferItemStates(id)) {
            BigDecimal unitCost = jdbc.queryForObject("""
                    select unit_cost from inventory_movements where tenant_id=? and operation_key=?
                    """, BigDecimal.class, tenantId, "TRANSFER-OUT-" + id + "-" + item.id());
            ledger.increase(tenantId, item.productId(), state.toWarehouseId(), destinationLocation,
                    item.quantity(), unitCost, "TRANSFER_IN", "TRANSFER", id, item.id(),
                    "TRANSFER-IN-" + id + "-" + item.id(), "Transfer " + state.number());
            for (String serial : item.serialNumbers()) {
                int changed = jdbc.update("""
                        update inventory_serial_units
                        set warehouse_id=?,location_id=?,status='AVAILABLE',updated_at=now(),version=version+1
                        where tenant_id=? and product_id=? and serial_number=? and status='IN_TRANSIT'
                        """, state.toWarehouseId(), destinationLocation, tenantId,
                        item.productId(), serial);
                if (changed != 1) {
                    throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                            "Serialized transfer state is inconsistent");
                }
            }
        }
        jdbc.update("""
                update inventory_transfers set status='RECEIVED',received_by=?,received_at=now(),
                    updated_at=now(),version=version+1 where id=? and tenant_id=?
                """, com.sami.app.security.CurrentActor.id(), id, tenantId);
        ledger.audit(tenantId, "TRANSFER", id, "RECEIVED", Map.of("status", "SHIPPED"),
                Map.of("status", "RECEIVED"));
        ledger.publish(tenantId, "TransferReceived", "TRANSFER", id,
                Map.of("transferNumber", state.number()));
        return getTransfer(id);
    }

    @Transactional
    public TransferResponse cancelTransfer(Long id) {
        Long tenantId = tenantContext.requireTenantId();
        TransferState state = lockTransfer(id, tenantId);
        requireStatus("Transfer", state.status(), "DRAFT");
        jdbc.update("""
                update inventory_transfers set status='CANCELLED',cancelled_at=now(),
                    updated_at=now(),version=version+1 where id=? and tenant_id=?
                """, id, tenantId);
        ledger.audit(tenantId, "TRANSFER", id, "CANCELLED", Map.of("status", "DRAFT"),
                Map.of("status", "CANCELLED"));
        ledger.publish(tenantId, "TransferCancelled", "TRANSFER", id, Map.of());
        return getTransfer(id);
    }

    @Transactional
    public CountResponse createCount(CountRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        ledger.requireWarehouse(tenantId, request.warehouseId());
        Long locationId = request.locationId() == null ? null
                : ledger.requireLocation(tenantId, request.warehouseId(), request.locationId());
        String number = nextNumber(tenantId, "COUNT", "CNT");
        Long id = jdbc.queryForObject("""
                insert into inventory_counts(
                    tenant_id,count_number,warehouse_id,location_id,notes,created_by)
                values(?,?,?,?,?,?) returning id
                """, Long.class, tenantId, number, request.warehouseId(), locationId,
                blank(request.notes()), com.sami.app.security.CurrentActor.id());
        if (locationId == null) {
            jdbc.update("""
                    insert into inventory_count_items(count_id,product_id,expected_quantity)
                    select ?,product_id,sum(on_hand) from inventory_balances
                    where tenant_id=? and warehouse_id=? group by product_id
                    """, id, tenantId, request.warehouseId());
        } else {
            jdbc.update("""
                    insert into inventory_count_items(count_id,product_id,expected_quantity)
                    select ?,product_id,on_hand from inventory_balances
                    where tenant_id=? and warehouse_id=? and location_id=?
                    """, id, tenantId, request.warehouseId(), locationId);
        }
        ledger.audit(tenantId, "COUNT", id, "CREATED", null,
                Map.of("countNumber", number, "warehouseId", request.warehouseId()));
        ledger.publish(tenantId, "StockCountCreated", "COUNT", id,
                Map.of("countNumber", number));
        return getCount(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<CountResponse> counts(String status, int page, int size) {
        Long tenantId = tenantContext.requireTenantId();
        String where = " where tenant_id=?";
        ArrayList<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (status != null && !status.isBlank()) {
            where += " and status=?";
            args.add(status.trim().toUpperCase(Locale.ROOT));
        }
        Long total = jdbc.queryForObject("select count(*) from inventory_counts" + where,
                Long.class, args.toArray());
        int actualSize = Math.min(Math.max(size, 1), 100);
        args.add(actualSize); args.add(Math.max(page, 0) * actualSize);
        List<Long> ids = jdbc.query("select id from inventory_counts" + where
                        + " order by created_at desc,id desc limit ? offset ?",
                (rs, row) -> rs.getLong(1), args.toArray());
        return page(ids.stream().map(this::getCount).toList(), page, actualSize,
                total == null ? 0 : total);
    }

    @Transactional(readOnly = true)
    public CountResponse getCount(Long id) {
        Long tenantId = tenantContext.requireTenantId();
        List<CountResponse> rows = jdbc.query("""
                select c.id,c.count_number,c.warehouse_id,w.name warehouse_name,
                       c.location_id,l.name location_name,c.status,c.notes,c.created_by,
                       c.posted_by,c.posted_at,c.cancelled_at,c.created_at,c.updated_at,c.version
                from inventory_counts c join pur_warehouses w on w.id=c.warehouse_id
                left join inventory_locations l on l.id=c.location_id
                where c.id=? and c.tenant_id=?
                """, (rs, row) -> count(rs, countItems(id)), id, tenantId);
        if (rows.isEmpty()) {
            throw ResourceNotFoundException.of("Inventory count", id);
        }
        return rows.getFirst();
    }

    @Transactional
    public CountResponse submitCount(Long id, CountResultRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        CountState state = lockCount(id, tenantId);
        requireStatus("Count", state.status(), "DRAFT");
        Set<Long> seen = new HashSet<>();
        for (CountResultRequest.CountedLine line : request.lines()) {
            if (!seen.add(line.productId())) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Each product may appear only once in count results");
            }
            ledger.requireProduct(tenantId, line.productId());
            jdbc.update("""
                    insert into inventory_count_items(
                        count_id,product_id,expected_quantity,counted_quantity,variance)
                    values(?,?,0,?,?)
                    on conflict(count_id,product_id) do update
                    set counted_quantity=excluded.counted_quantity,
                        variance=excluded.counted_quantity-inventory_count_items.expected_quantity,
                        updated_at=now(),version=inventory_count_items.version+1
                    """, id, line.productId(), line.countedQuantity(), line.countedQuantity());
        }
        Integer missing = jdbc.queryForObject("""
                select count(*) from inventory_count_items where count_id=? and counted_quantity is null
                """, Integer.class, id);
        if (missing != null && missing > 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "A counted quantity is required for every snapshot product");
        }
        jdbc.update("""
                update inventory_counts set status='COUNTED',updated_at=now(),version=version+1
                where id=? and tenant_id=?
                """, id, tenantId);
        ledger.audit(tenantId, "COUNT", id, "COUNTED", Map.of("status", "DRAFT"),
                Map.of("status", "COUNTED"));
        ledger.publish(tenantId, "StockCountSubmitted", "COUNT", id, Map.of());
        return getCount(id);
    }

    @Transactional
    public CountResponse postCount(Long id) {
        Long tenantId = tenantContext.requireTenantId();
        CountState state = lockCount(id, tenantId);
        requireStatus("Count", state.status(), "COUNTED");
        Long locationId = ledger.requireLocation(tenantId, state.warehouseId(), state.locationId());
        for (CountItemState item : countItemStates(id)) {
            BigDecimal variance = item.variance();
            if (variance == null || variance.signum() == 0) {
                continue;
            }
            if (variance.signum() > 0) {
                ledger.increase(tenantId, item.productId(), state.warehouseId(), locationId,
                        variance, BigDecimal.ZERO, "COUNT_GAIN", "COUNT", id, item.id(),
                        "COUNT-" + id + "-" + item.id(), "Stock count variance");
            } else {
                ledger.decrease(tenantId, item.productId(), state.warehouseId(), locationId,
                        variance.abs(), "COUNT_LOSS", "COUNT", id, item.id(),
                        "COUNT-" + id + "-" + item.id(), "Stock count variance");
            }
        }
        jdbc.update("""
                update inventory_counts set status='POSTED',posted_by=?,posted_at=now(),
                    updated_at=now(),version=version+1 where id=? and tenant_id=?
                """, com.sami.app.security.CurrentActor.id(), id, tenantId);
        ledger.audit(tenantId, "COUNT", id, "POSTED", Map.of("status", "COUNTED"),
                Map.of("status", "POSTED"));
        ledger.publish(tenantId, "StockCountPosted", "COUNT", id, Map.of());
        return getCount(id);
    }

    @Transactional
    public CountResponse cancelCount(Long id) {
        Long tenantId = tenantContext.requireTenantId();
        CountState state = lockCount(id, tenantId);
        if (!Set.of("DRAFT", "COUNTED").contains(state.status())) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only draft or counted stock counts can be cancelled");
        }
        jdbc.update("""
                update inventory_counts set status='CANCELLED',cancelled_at=now(),
                    updated_at=now(),version=version+1 where id=? and tenant_id=?
                """, id, tenantId);
        ledger.audit(tenantId, "COUNT", id, "CANCELLED", Map.of("status", state.status()),
                Map.of("status", "CANCELLED"));
        ledger.publish(tenantId, "StockCountCancelled", "COUNT", id, Map.of());
        return getCount(id);
    }

    @Transactional
    public void updateSerialStatus(Long id, SerialStatusRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        String status = request.status().trim().toUpperCase(Locale.ROOT);
        if (!SERIAL_STATUSES.contains(status)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Only AVAILABLE and QUARANTINED are manually assignable");
        }
        List<String> current = jdbc.query("""
                select status from inventory_serial_units where id=? and tenant_id=? for update
                """, (rs, row) -> rs.getString(1), id, tenantId);
        if (current.isEmpty()) {
            throw ResourceNotFoundException.of("Serialized inventory unit", id);
        }
        if (!SERIAL_STATUSES.contains(current.getFirst())) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Reserved, issued or in-transit units cannot be manually reclassified");
        }
        jdbc.update("""
                update inventory_serial_units set status=?,updated_at=now(),version=version+1
                where id=? and tenant_id=?
                """, status, id, tenantId);
        ledger.audit(tenantId, "SERIAL_UNIT", id, "STATUS_CHANGED",
                Map.of("status", current.getFirst()),
                Map.of("status", status, "reason", request.reason() == null ? "" : request.reason()));
        ledger.publish(tenantId, "SerialStatusChanged", "SERIAL_UNIT", id,
                Map.of("status", status));
    }

    @Transactional
    public void releaseReservation(Long reservationId, String reason) {
        Long tenantId = tenantContext.requireTenantId();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select source_type,source_id from inventory_reservations
                where id=? and tenant_id=? and status='ACTIVE'
                """, reservationId, tenantId);
        if (rows.isEmpty()) {
            throw ResourceNotFoundException.of("Active inventory reservation", reservationId);
        }
        stockOperations.release((String) rows.getFirst().get("source_type"),
                ((Number) rows.getFirst().get("source_id")).longValue(), reason);
    }

    @Transactional
    public ImportResult importAdjustments(MultipartFile file) {
        Long tenantId = tenantContext.requireTenantId();
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A CSV file is required");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "CSV file exceeds 5 MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Could not read the CSV file");
        }
        String content = new String(bytes, StandardCharsets.UTF_8).replace("\uFEFF", "");
        String[] lines = content.split("\\R");
        if (lines.length < 2) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "CSV has no data rows");
        }
        List<String> header = csvLine(lines[0]).stream().map(v -> v.trim().toLowerCase(Locale.ROOT)).toList();
        List<String> expected = List.of("warehousecode", "locationcode", "sku", "quantity", "unitcost", "reason");
        if (!header.equals(expected)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "CSV header must be warehouseCode,locationCode,sku,quantity,unitCost,reason");
        }
        List<ImportRow> parsed = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            List<String> values = csvLine(lines[i]);
            if (values.size() != 6) {
                errors.add("Row " + (i + 1) + ": expected 6 columns");
                continue;
            }
            try {
                Long warehouseId = findId("""
                        select id from pur_warehouses where tenant_id=? and lower(code)=lower(?) and active
                        """, tenantId, values.get(0));
                Long locationId = findId("""
                        select id from inventory_locations
                        where tenant_id=? and warehouse_id=? and lower(code)=lower(?) and active
                        """, tenantId, warehouseId, values.get(1));
                Long productId = findId("""
                        select id from products where tenant_id=? and lower(sku)=lower(?) and active
                        """, tenantId, values.get(2));
                BigDecimal quantity = new BigDecimal(values.get(3));
                BigDecimal cost = values.get(4).isBlank() ? BigDecimal.ZERO : new BigDecimal(values.get(4));
                if (quantity.signum() == 0 || cost.signum() < 0 || values.get(5).isBlank()) {
                    throw new IllegalArgumentException("quantity/reason/cost is invalid");
                }
                parsed.add(new ImportRow(i + 1, warehouseId, locationId, productId,
                        quantity, cost, values.get(5).trim()));
            } catch (RuntimeException ex) {
                errors.add("Row " + (i + 1) + ": " + ex.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            return new ImportResult(lines.length - 1, 0, parsed.size(), errors);
        }
        String hash = sha256(bytes);
        int successful = 0;
        for (ImportRow row : parsed) {
            String key = "IMPORT-" + hash + "-" + row.rowNumber();
            if (row.quantity().signum() > 0) {
                ledger.increase(tenantId, row.productId(), row.warehouseId(), row.locationId(),
                        row.quantity(), row.unitCost(), "ADJUSTMENT_IN", "IMPORT", null,
                        row.productId(), key, row.reason());
            } else {
                ledger.decrease(tenantId, row.productId(), row.warehouseId(), row.locationId(),
                        row.quantity().abs(), "ADJUSTMENT_OUT", "IMPORT", null,
                        row.productId(), key, row.reason());
            }
            successful++;
        }
        ledger.audit(tenantId, "IMPORT", null, "POSTED", null,
                Map.of("fileName", file.getOriginalFilename() == null ? "inventory.csv" : file.getOriginalFilename(),
                        "rows", successful, "hash", hash));
        ledger.publish(tenantId, "InventoryImported", "IMPORT", null,
                Map.of("rows", successful));
        return new ImportResult(parsed.size(), successful, 0, List.of());
    }

    private List<TransferLineResponse> transferItems(Long transferId) {
        return jdbc.query("""
                select i.id,i.product_id,p.sku,p.name,i.quantity,i.serial_numbers::text
                from inventory_transfer_items i join products p on p.id=i.product_id
                where i.transfer_id=? order by i.id
                """, (rs, row) -> new TransferLineResponse(rs.getLong("id"),
                rs.getLong("product_id"), rs.getString("sku"), rs.getString("name"),
                rs.getBigDecimal("quantity"), strings(rs.getString("serial_numbers"))), transferId);
    }

    private List<TransferItemState> transferItemStates(Long transferId) {
        return jdbc.query("""
                select id,product_id,quantity,serial_numbers::text
                from inventory_transfer_items where transfer_id=? order by id
                """, (rs, row) -> new TransferItemState(rs.getLong("id"),
                rs.getLong("product_id"), rs.getBigDecimal("quantity"),
                strings(rs.getString("serial_numbers"))), transferId);
    }

    private TransferResponse transfer(ResultSet rs, List<TransferLineResponse> lines) throws SQLException {
        return new TransferResponse(rs.getLong("id"), rs.getString("transfer_number"),
                rs.getLong("from_warehouse_id"), rs.getString("from_name"),
                rs.getLong("to_warehouse_id"), rs.getString("to_name"),
                rs.getString("status"), rs.getString("notes"),
                (Long) rs.getObject("created_by"), (Long) rs.getObject("shipped_by"),
                (Long) rs.getObject("received_by"), instant(rs, "shipped_at"),
                instant(rs, "received_at"), instant(rs, "cancelled_at"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                rs.getLong("version"), lines);
    }

    private TransferState lockTransfer(Long id, Long tenantId) {
        List<TransferState> rows = jdbc.query("""
                select transfer_number,from_warehouse_id,to_warehouse_id,status
                from inventory_transfers where id=? and tenant_id=? for update
                """, (rs, row) -> new TransferState(rs.getString("transfer_number"),
                rs.getLong("from_warehouse_id"), rs.getLong("to_warehouse_id"),
                rs.getString("status")), id, tenantId);
        if (rows.isEmpty()) throw ResourceNotFoundException.of("Inventory transfer", id);
        return rows.getFirst();
    }

    private void validateTransferSerials(Long tenantId, Long warehouseId, TransferItemState item) {
        if (item.serialNumbers().isEmpty()) return;
        for (String serial : item.serialNumbers()) {
            Integer count = jdbc.queryForObject("""
                    select count(*) from inventory_serial_units
                    where tenant_id=? and warehouse_id=? and product_id=? and serial_number=? and status='AVAILABLE'
                    """, Integer.class, tenantId, warehouseId, item.productId(), serial);
            if (count == null || count != 1) {
                throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                        "Serialized unit " + serial + " is not available in the source warehouse");
            }
        }
    }

    private void validateSerialQuantity(TransferLineRequest line) {
        if (line.serialNumbers() == null || line.serialNumbers().isEmpty()) return;
        if (line.quantity().stripTrailingZeros().scale() > 0
                || line.serialNumbers().size() != line.quantity().intValueExact()
                || new HashSet<>(line.serialNumbers()).size() != line.serialNumbers().size()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Serialized transfer lines require one unique serial per whole unit");
        }
    }

    private List<CountLineResponse> countItems(Long countId) {
        return jdbc.query("""
                select i.id,i.product_id,p.sku,p.name,i.expected_quantity,
                       i.counted_quantity,i.variance
                from inventory_count_items i join products p on p.id=i.product_id
                where i.count_id=? order by p.name,i.id
                """, (rs, row) -> new CountLineResponse(rs.getLong("id"),
                rs.getLong("product_id"), rs.getString("sku"), rs.getString("name"),
                rs.getBigDecimal("expected_quantity"), rs.getBigDecimal("counted_quantity"),
                rs.getBigDecimal("variance")), countId);
    }

    private List<CountItemState> countItemStates(Long countId) {
        return jdbc.query("""
                select id,product_id,variance from inventory_count_items where count_id=? order by id
                """, (rs, row) -> new CountItemState(rs.getLong("id"),
                rs.getLong("product_id"), rs.getBigDecimal("variance")), countId);
    }

    private CountResponse count(ResultSet rs, List<CountLineResponse> lines) throws SQLException {
        return new CountResponse(rs.getLong("id"), rs.getString("count_number"),
                rs.getLong("warehouse_id"), rs.getString("warehouse_name"),
                (Long) rs.getObject("location_id"), rs.getString("location_name"),
                rs.getString("status"), rs.getString("notes"),
                (Long) rs.getObject("created_by"), (Long) rs.getObject("posted_by"),
                instant(rs, "posted_at"), instant(rs, "cancelled_at"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                rs.getLong("version"), lines);
    }

    private CountState lockCount(Long id, Long tenantId) {
        List<CountState> rows = jdbc.query("""
                select warehouse_id,location_id,status from inventory_counts
                where id=? and tenant_id=? for update
                """, (rs, row) -> new CountState(rs.getLong("warehouse_id"),
                (Long) rs.getObject("location_id"), rs.getString("status")), id, tenantId);
        if (rows.isEmpty()) throw ResourceNotFoundException.of("Inventory count", id);
        return rows.getFirst();
    }

    private String nextNumber(Long tenantId, String type, String prefix) {
        int year = Year.now(ZoneOffset.UTC).getValue();
        jdbc.update("""
                insert into inventory_document_numbers(tenant_id,document_type,sequence_year,next_value)
                values(?,?,?,1) on conflict(tenant_id,document_type,sequence_year) do nothing
                """, tenantId, type, year);
        Long next = jdbc.queryForObject("""
                select next_value from inventory_document_numbers
                where tenant_id=? and document_type=? and sequence_year=? for update
                """, Long.class, tenantId, type, year);
        if (next == null) throw new ApiException(ErrorCode.INTERNAL_ERROR, "Inventory numbering failed");
        jdbc.update("""
                update inventory_document_numbers set next_value=?,updated_at=now(),version=version+1
                where tenant_id=? and document_type=? and sequence_year=?
                """, next + 1, tenantId, type, year);
        return "%s-%d-%06d".formatted(prefix, year, next);
    }

    private void requireStatus(String entity, String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    entity + " must be " + expected);
        }
    }

    private Long findId(String sql, Object... args) {
        List<Long> ids = jdbc.query(sql, (rs, row) -> rs.getLong(1), args);
        if (ids.size() != 1) throw new IllegalArgumentException("referenced warehouse, location or SKU was not found");
        return ids.getFirst();
    }

    private List<String> csvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"'); i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(value.toString().trim()); value.setLength(0);
            } else {
                value.append(ch);
            }
        }
        values.add(value.toString().trim());
        return values;
    }

    private String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String json(List<String> value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Serial list is invalid");
        }
    }

    private List<String> strings(String json) {
        if (json == null) return List.of();
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Stored transfer serial list is invalid", ex);
        }
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> PageResponse<T> page(List<T> rows, int requestedPage, int size, long total) {
        int page = Math.max(requestedPage, 0);
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(rows, page, size, total, pages, page == 0,
                pages == 0 || page >= pages - 1);
    }

    private record TransferState(String number, Long fromWarehouseId,
                                 Long toWarehouseId, String status) { }
    private record TransferItemState(Long id, Long productId, BigDecimal quantity,
                                     List<String> serialNumbers) { }
    private record CountState(Long warehouseId, Long locationId, String status) { }
    private record CountItemState(Long id, Long productId, BigDecimal variance) { }
    private record ImportRow(int rowNumber, Long warehouseId, Long locationId,
                             Long productId, BigDecimal quantity, BigDecimal unitCost,
                             String reason) { }
}
