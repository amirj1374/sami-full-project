package com.sami.app.inventory.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.domain.InventoryWarehouse;
import com.sami.app.inventory.publicapi.InventoryStockOperations;
import com.sami.app.product.event.ProductStockChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Canonical transactional adapter for Product, Purchasing and Sales stock effects. */
@Service
@RequiredArgsConstructor
public class InventoryStockService implements InventoryStockOperations {

    private final TenantContext tenantContext;
    private final InventoryLedgerService ledger;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void receivePurchase(PurchaseReceiptCommand command) {
        Long tenantId = tenantContext.requireTenantId();
        InventoryWarehouse warehouse = command.warehouseId() == null
                ? ledger.salesWarehouse(tenantId, null)
                : ledger.requireWarehouse(tenantId, command.warehouseId());
        Long locationId = ledger.requireLocation(tenantId, warehouse.getId(), null);
        int index = 0;
        for (ReceiptLine line : requireLines(command.lines())) {
            index++;
            String operationKey = "PURCHASE-RECEIPT-" + command.receiptId() + "-" + index;
            boolean posted = ledger.increase(tenantId, line.productId(), warehouse.getId(),
                    locationId, line.quantity(), line.unitCost(), "RECEIPT", "PURCHASE",
                    command.purchaseId(), line.sourceLineId(), operationKey,
                    "Purchase receipt " + command.receiptId());
            if (posted && line.serials() != null) {
                for (SerialIdentity serial : line.serials()) {
                    ledger.createSerial(tenantId, line.productId(), warehouse.getId(), locationId,
                            serial.serialNumber(), serial.imei(), "PURCHASE", command.purchaseId(),
                            line.sourceLineId());
                }
            }
        }
        ledger.audit(tenantId, "PURCHASE_RECEIPT", command.receiptId(), "POSTED", null,
                Map.of("purchaseId", command.purchaseId(), "warehouseId", warehouse.getId(),
                        "lines", command.lines().size()));
        ledger.publish(tenantId, "PurchaseReceiptPosted", "PURCHASE_RECEIPT",
                command.receiptId(), Map.of("purchaseId", command.purchaseId(),
                        "warehouseId", warehouse.getId()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void returnToSupplier(SupplierReturnCommand command) {
        Long tenantId = tenantContext.requireTenantId();
        InventoryWarehouse warehouse = command.warehouseId() == null
                ? ledger.salesWarehouse(tenantId, null)
                : ledger.requireWarehouse(tenantId, command.warehouseId());
        Long locationId = ledger.requireLocation(tenantId, warehouse.getId(), null);
        int index = 0;
        for (StockLine line : requireLines(command.lines())) {
            index++;
            ledger.decrease(tenantId, line.productId(), warehouse.getId(), locationId,
                    line.quantity(), "SUPPLIER_RETURN", "PURCHASE_RETURN", command.returnId(),
                    line.sourceLineId(), "PURCHASE-RETURN-" + command.returnId() + "-" + index,
                    "Returned to supplier");
            if (whole(line.quantity())) {
                ledger.markSupplierReturnedSerials(tenantId, line.productId(), warehouse.getId(),
                        line.sourceLineId(), line.quantity().intValueExact());
            }
        }
        ledger.audit(tenantId, "PURCHASE_RETURN", command.returnId(), "POSTED", null,
                Map.of("purchaseId", command.purchaseId(), "warehouseId", warehouse.getId(),
                        "lines", command.lines().size()));
        ledger.publish(tenantId, "SupplierReturnPosted", "PURCHASE_RETURN",
                command.returnId(), Map.of("purchaseId", command.purchaseId()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(ReservationCommand command) {
        Long tenantId = tenantContext.requireTenantId();
        InventoryWarehouse warehouse = ledger.salesWarehouse(tenantId, command.branchId());
        Long locationId = ledger.requireLocation(tenantId, warehouse.getId(), null);
        for (StockLine line : requireLines(command.lines())) {
            ledger.reserve(tenantId, line.productId(), warehouse.getId(), locationId,
                    line.quantity(), normalize(command.sourceType()), command.sourceId(),
                    line.sourceLineId(), line.serialNumber(), line.imei());
        }
        ledger.audit(tenantId, "RESERVATION", command.sourceId(), "CREATED", null,
                Map.of("sourceType", normalize(command.sourceType()),
                        "warehouseId", warehouse.getId(), "lines", command.lines().size()));
        ledger.publish(tenantId, "StockReserved", "RESERVATION", command.sourceId(),
                Map.of("sourceType", normalize(command.sourceType())));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void release(String sourceType, Long sourceId, String reason) {
        Long tenantId = tenantContext.requireTenantId();
        List<InventoryLedgerService.ReservationState> reservations =
                ledger.activeReservations(tenantId, sourceType, sourceId);
        reservations.forEach(reservation -> ledger.releaseReservation(tenantId, reservation, reason));
        if (!reservations.isEmpty()) {
            ledger.audit(tenantId, "RESERVATION", sourceId, "RELEASED", null,
                    Map.of("sourceType", normalize(sourceType), "reason", safe(reason)));
            ledger.publish(tenantId, "StockReleased", "RESERVATION", sourceId,
                    Map.of("sourceType", normalize(sourceType)));
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void issue(String sourceType, Long sourceId) {
        Long tenantId = tenantContext.requireTenantId();
        List<InventoryLedgerService.ReservationState> reservations =
                ledger.activeReservations(tenantId, sourceType, sourceId);
        if (reservations.isEmpty()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "No active stock reservation exists for this source");
        }
        reservations.forEach(reservation -> ledger.issueReservation(tenantId, reservation));
        ledger.audit(tenantId, "RESERVATION", sourceId, "FULFILLED", null,
                Map.of("sourceType", normalize(sourceType)));
        ledger.publish(tenantId, "StockIssued", "RESERVATION", sourceId,
                Map.of("sourceType", normalize(sourceType)));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void customerReturn(CustomerReturnCommand command) {
        Long tenantId = tenantContext.requireTenantId();
        InventoryWarehouse warehouse = ledger.salesWarehouse(tenantId, command.branchId());
        Long locationId = ledger.requireLocation(tenantId, warehouse.getId(), null);
        int index = 0;
        for (StockLine line : requireLines(command.lines())) {
            index++;
            ledger.increase(tenantId, line.productId(), warehouse.getId(), locationId,
                    line.quantity(), BigDecimal.ZERO, "CUSTOMER_RETURN", command.sourceType(),
                    command.sourceId(), line.sourceLineId(),
                    "CUSTOMER-RETURN-" + command.sourceId() + "-" + index,
                    "Customer return");
            ledger.restoreIssuedSerial(tenantId, line.productId(), warehouse.getId(), locationId,
                    line.serialNumber(), line.imei());
        }
        ledger.audit(tenantId, "CUSTOMER_RETURN", command.sourceId(), "POSTED", null,
                Map.of("warehouseId", warehouse.getId(), "lines", command.lines().size()));
        ledger.publish(tenantId, "CustomerReturnPosted", "CUSTOMER_RETURN",
                command.sourceId(), Map.of("warehouseId", warehouse.getId()));
    }

    /** Product stock compatibility writes become real Inventory adjustments. */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onProductStockChanged(ProductStockChangedEvent event) {
        tenantContext.requireAccessTo(event.tenantId());
        int delta = event.newQuantity() - event.previousQuantity();
        if (delta == 0) {
            return;
        }
        InventoryWarehouse warehouse = ledger.salesWarehouse(event.tenantId(), null);
        Long locationId = ledger.requireLocation(event.tenantId(), warehouse.getId(), null);
        String key = "PRODUCT-STOCK-" + event.productId() + "-"
                + event.occurredAt().toEpochMilli() + "-" + event.newQuantity();
        if (delta > 0) {
            ledger.increase(event.tenantId(), event.productId(), warehouse.getId(), locationId,
                    BigDecimal.valueOf(delta), BigDecimal.ZERO, "ADJUSTMENT_IN", "PRODUCT",
                    event.productId(), null, key, "Product stock compatibility adjustment");
        } else {
            ledger.decrease(event.tenantId(), event.productId(), warehouse.getId(), locationId,
                    BigDecimal.valueOf(-delta), "ADJUSTMENT_OUT", "PRODUCT", event.productId(),
                    null, key, "Product stock compatibility adjustment");
        }
        ledger.audit(event.tenantId(), "PRODUCT", event.productId(), "STOCK_ADJUSTED",
                Map.of("quantity", event.previousQuantity()), Map.of("quantity", event.newQuantity()));
        ledger.publish(event.tenantId(), "ProductStockAdjusted", "PRODUCT", event.productId(),
                Map.of("previous", event.previousQuantity(), "current", event.newQuantity()));
    }

    private <T> List<T> requireLines(List<T> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "At least one stock line is required");
        }
        return lines;
    }

    private boolean whole(BigDecimal quantity) {
        return quantity.stripTrailingZeros().scale() <= 0;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Source type is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
