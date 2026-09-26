package com.sami.app.inventory.publicapi;

import java.math.BigDecimal;
import java.util.List;

/**
 * Transactional stock commands consumed by Product, Purchasing and Sales.
 * Callers never write Inventory tables or the compatibility projection.
 */
public interface InventoryStockOperations {

    void receivePurchase(PurchaseReceiptCommand command);
    void receiveVariantPurchase(VariantPurchaseReceiptCommand command);

    void returnToSupplier(SupplierReturnCommand command);

    void reserve(ReservationCommand command);

    ReservationResult reserveAvailable(ReservationCommand command);

    void release(String sourceType, Long sourceId, String reason);

    void issue(String sourceType, Long sourceId);

    void issuePartial(PartialIssueCommand command);

    BigDecimal fulfillBackorder(String sourceType, Long sourceId, BigDecimal quantity);

    void cancelBackorder(String sourceType, Long sourceId);

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
    record VariantPurchaseReceiptCommand(Long warehouseId, Long purchaseId, Long receiptId,
                                         Long sourceLineId, Long productId, Long variantId, BigDecimal enteredQuantity,
                                         Long enteredUomId, Long baseUomId, BigDecimal conversionFactor,
                                         BigDecimal unitCost, List<SerialIdentity> serials) {
        public VariantPurchaseReceiptCommand(Long warehouseId, Long purchaseId, Long receiptId,
                                             Long productId, Long variantId, BigDecimal enteredQuantity,
                                             Long enteredUomId, Long baseUomId, BigDecimal conversionFactor,
                                             BigDecimal unitCost) {
            this(warehouseId, purchaseId, receiptId, null, productId, variantId, enteredQuantity,
                    enteredUomId, baseUomId, conversionFactor, unitCost, List.of());
        }

        public VariantPurchaseReceiptCommand(Long warehouseId, Long purchaseId, Long receiptId,
                                             Long productId, Long variantId, BigDecimal enteredQuantity,
                                             Long enteredUomId, Long baseUomId, BigDecimal conversionFactor,
                                             BigDecimal unitCost, List<SerialIdentity> serials) {
            this(warehouseId, purchaseId, receiptId, null, productId, variantId, enteredQuantity,
                    enteredUomId, baseUomId, conversionFactor, unitCost, serials);
        }
    }

    record StockLine(Long sourceLineId, Long productId, BigDecimal quantity,
                     String serialNumber, String imei, Long variantId) {
        public StockLine(Long sourceLineId, Long productId, BigDecimal quantity,
                         String serialNumber, String imei) {
            this(sourceLineId, productId, quantity, serialNumber, imei, null);
        }
    }

    record SupplierReturnCommand(Long warehouseId, Long purchaseId, Long returnId,
                                 List<StockLine> lines) {
    }

    record ReservationCommand(Long companyId, Long branchId, String sourceType,
                              Long sourceId, List<StockLine> lines) {
    }

    record ReservationResult(List<ReservationAllocation> allocations) {}

    record ReservationAllocation(Long sourceLineId, BigDecimal orderedQuantity,
                                  BigDecimal reservedQuantity, BigDecimal backorderedQuantity) {}

    record PartialIssueCommand(String reservationSourceType, Long reservationSourceId,
                               String deliverySourceType, Long deliverySourceId,
                               List<StockLine> lines) {}

    record CustomerReturnCommand(Long companyId, Long branchId, String sourceType,
                                 Long sourceId, List<StockLine> lines) {
    }
}
