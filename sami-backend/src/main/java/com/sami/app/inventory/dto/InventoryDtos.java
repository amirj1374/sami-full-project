package com.sami.app.inventory.dto;

import com.sami.app.inventory.domain.InventoryWarehouse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Typed REST contracts for the Inventory bounded context. */
public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record WarehouseRequest(
            Long companyId,
            Long branchId,
            @NotBlank @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{1,63}$") String code,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 500) String description,
            @NotBlank String warehouseType,
            boolean defaultWarehouse,
            boolean active,
            int displayOrder
    ) {
    }

    public record WarehouseResponse(
            Long id, Long companyId, Long branchId, String code, String name,
            String description, String warehouseType, boolean defaultWarehouse,
            boolean active, int displayOrder, Instant createdAt, Instant updatedAt, Long version
    ) {
        public static WarehouseResponse from(InventoryWarehouse warehouse) {
            return new WarehouseResponse(warehouse.getId(), warehouse.getCompanyId(),
                    warehouse.getBranchId(), warehouse.getCode(), warehouse.getName(),
                    warehouse.getDescription(), warehouse.getWarehouseType(),
                    warehouse.isDefaultWarehouse(), warehouse.isActive(),
                    warehouse.getDisplayOrder(), warehouse.getCreatedAt(),
                    warehouse.getUpdatedAt(), warehouse.getVersion());
        }
    }

    public record LocationRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_-]{1,63}$") String code,
            @NotBlank @Size(max = 120) String name,
            @NotBlank String locationType,
            @Size(max = 500) String description,
            boolean defaultLocation,
            boolean active
    ) {
    }

    public record LocationResponse(
            Long id, Long warehouseId, String code, String name, String locationType,
            String description, boolean defaultLocation, boolean active,
            Instant createdAt, Instant updatedAt, Long version
    ) {
    }

    public record BalanceResponse(
            Long id, Long warehouseId, String warehouseCode, String warehouseName,
            Long locationId, String locationCode, String locationName,
            Long productId, String sku, String productName,
            BigDecimal onHand, BigDecimal reserved, BigDecimal available,
            BigDecimal averageUnitCost, BigDecimal inventoryValue,
            BigDecimal reorderPoint, boolean lowStock, Instant lastMovementAt, Long version
    ) {
    }

    public record MovementResponse(
            Long id, Long productId, String sku, String productName,
            Long fromWarehouseId, String fromWarehouseName,
            Long toWarehouseId, String toWarehouseName,
            String movementType, BigDecimal quantity, BigDecimal unitCost,
            String sourceType, Long sourceId, Long sourceLineId, String reason,
            Long actorId, String actorEmail, Instant occurredAt
    ) {
    }

    public record SerialResponse(
            Long id, Long productId, String sku, String productName,
            Long warehouseId, String warehouseName, Long locationId, String locationName,
            String serialNumber, String imei, String status,
            String sourceType, Long sourceId, Instant receivedAt, Instant issuedAt, Long version
    ) {
    }

    public record SerialStatusRequest(@NotBlank String status, @Size(max = 500) String reason) {
    }

    public record AdjustmentRequest(
            @NotNull Long warehouseId,
            Long locationId,
            @NotBlank @Size(max = 500) String reason,
            @Size(max = 160) String idempotencyKey,
            @NotEmpty @Valid List<AdjustmentLine> lines
    ) {
        public record AdjustmentLine(
                @NotNull Long productId,
                @NotNull BigDecimal quantity,
                @PositiveOrZero BigDecimal unitCost
        ) {
        }
    }

    public record AdjustmentResponse(int lines, BigDecimal totalIncrease,
                                     BigDecimal totalDecrease, Instant postedAt) {
    }

    public record TransferRequest(
            @NotNull Long fromWarehouseId,
            @NotNull Long toWarehouseId,
            @Size(max = 1000) String notes,
            @NotEmpty @Valid List<TransferLineRequest> lines
    ) {
    }

    public record TransferLineRequest(
            @NotNull Long productId,
            @NotNull @Positive BigDecimal quantity,
            List<@NotBlank String> serialNumbers
    ) {
    }

    public record TransferLineResponse(
            Long id, Long productId, String sku, String productName,
            BigDecimal quantity, List<String> serialNumbers
    ) {
    }

    public record TransferResponse(
            Long id, String transferNumber, Long fromWarehouseId, String fromWarehouseName,
            Long toWarehouseId, String toWarehouseName, String status, String notes,
            Long createdBy, Long shippedBy, Long receivedBy,
            Instant shippedAt, Instant receivedAt, Instant cancelledAt,
            Instant createdAt, Instant updatedAt, Long version,
            List<TransferLineResponse> lines
    ) {
    }

    public record CountRequest(
            @NotNull Long warehouseId,
            Long locationId,
            @Size(max = 1000) String notes
    ) {
    }

    public record CountResultRequest(@NotEmpty @Valid List<CountedLine> lines) {
        public record CountedLine(@NotNull Long productId,
                                  @NotNull @PositiveOrZero BigDecimal countedQuantity) {
        }
    }

    public record CountLineResponse(
            Long id, Long productId, String sku, String productName,
            BigDecimal expectedQuantity, BigDecimal countedQuantity, BigDecimal variance
    ) {
    }

    public record CountResponse(
            Long id, String countNumber, Long warehouseId, String warehouseName,
            Long locationId, String locationName, String status, String notes,
            Long createdBy, Long postedBy, Instant postedAt, Instant cancelledAt,
            Instant createdAt, Instant updatedAt, Long version, List<CountLineResponse> lines
    ) {
    }

    public record ReservationResponse(
            Long id, Long productId, String sku, String productName,
            Long warehouseId, String warehouseName, String sourceType, Long sourceId,
            Long sourceLineId, BigDecimal quantity, BigDecimal fulfilledQuantity,
            String status, String serialNumber, String imei, Instant expiresAt,
            Instant createdAt, Instant updatedAt, Long version
    ) {
    }

    public record DashboardResponse(
            long warehouseCount, long productCount, long lowStockCount,
            long activeReservationCount, long openTransferCount, long openCountCount,
            BigDecimal totalOnHand, BigDecimal totalAvailable, BigDecimal inventoryValue
    ) {
    }

    public record MetricPoint(String label, BigDecimal value, long count) {
    }

    public record ReportResponse(
            DashboardResponse summary,
            List<MetricPoint> warehouseValues,
            List<MetricPoint> movementTypes,
            List<BalanceResponse> lowStock
    ) {
    }

    public record AuditResponse(
            Long id, String entityType, Long entityId, String action,
            Map<String, Object> oldValues, Map<String, Object> newValues,
            Long actorId, String actorEmail, Instant createdAt
    ) {
    }

    public record ImportResult(int processedRows, int successfulRows,
                               int skippedRows, List<String> errors) {
    }
}
