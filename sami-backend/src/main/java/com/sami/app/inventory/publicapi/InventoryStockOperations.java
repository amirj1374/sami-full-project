package com.sami.app.inventory.publicapi;

import java.math.BigDecimal;
import java.util.List;

/**
 * Transactional stock commands consumed by Product, Purchasing and Sales.
 * Callers never write Inventory tables or the compatibility projection.
 */
public interface InventoryStockOperations {

    void receivePurchase(PurchaseReceiptCommand command);

    void returnToSupplier(SupplierReturnCommand command);

    void reserve(ReservationCommand command);

    void release(String sourceType, Long sourceId, String reason);

    void issue(String sourceType, Long sourceId);

    void customerReturn(CustomerReturnCommand command);

    void setMarketAvailability(MarketAvailabilityCommand command);

    record MarketAvailabilityCommand(Long productId, Long sourceId, boolean available,
                                     BigDecimal unitCost, String operationKey) {}

    record SerialIdentity(String serialNumber, String imei, String hamtaActivationCode) {
    }

    record ReceiptLine(Long sourceLineId, Long productId, BigDecimal quantity,
                       BigDecimal unitCost, List<SerialIdentity> serials) {
    }

    record PurchaseReceiptCommand(Long warehouseId, Long purchaseId, Long receiptId,
                                  List<ReceiptLine> lines) {
    }

    record StockLine(Long sourceLineId, Long productId, BigDecimal quantity,
                     String serialNumber, String imei) {
    }

    record SupplierReturnCommand(Long warehouseId, Long purchaseId, Long returnId,
                                 List<StockLine> lines) {
    }

    record ReservationCommand(Long companyId, Long branchId, String sourceType,
                              Long sourceId, List<StockLine> lines) {
    }

    record CustomerReturnCommand(Long companyId, Long branchId, String sourceType,
                                 Long sourceId, List<StockLine> lines) {
    }
}
