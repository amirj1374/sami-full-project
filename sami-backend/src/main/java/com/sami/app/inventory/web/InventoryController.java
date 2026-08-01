package com.sami.app.inventory.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.dto.InventoryDtos.AdjustmentRequest;
import com.sami.app.inventory.dto.InventoryDtos.AdjustmentResponse;
import com.sami.app.inventory.dto.InventoryDtos.AuditResponse;
import com.sami.app.inventory.dto.InventoryDtos.BalanceResponse;
import com.sami.app.inventory.dto.InventoryDtos.CountRequest;
import com.sami.app.inventory.dto.InventoryDtos.CountResponse;
import com.sami.app.inventory.dto.InventoryDtos.CountResultRequest;
import com.sami.app.inventory.dto.InventoryDtos.DashboardResponse;
import com.sami.app.inventory.dto.InventoryDtos.ImportResult;
import com.sami.app.inventory.dto.InventoryDtos.LocationRequest;
import com.sami.app.inventory.dto.InventoryDtos.LocationResponse;
import com.sami.app.inventory.dto.InventoryDtos.MovementResponse;
import com.sami.app.inventory.dto.InventoryDtos.ReportResponse;
import com.sami.app.inventory.dto.InventoryDtos.ReservationResponse;
import com.sami.app.inventory.dto.InventoryDtos.SerialResponse;
import com.sami.app.inventory.dto.InventoryDtos.SerialStatusRequest;
import com.sami.app.inventory.dto.InventoryDtos.TransferRequest;
import com.sami.app.inventory.dto.InventoryDtos.TransferResponse;
import com.sami.app.inventory.dto.InventoryDtos.WarehouseRequest;
import com.sami.app.inventory.dto.InventoryDtos.WarehouseResponse;
import com.sami.app.inventory.service.InventoryAuditService;
import com.sami.app.inventory.service.InventoryQueryService;
import com.sami.app.inventory.service.InventoryWarehouseService;
import com.sami.app.inventory.service.InventoryWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/** Complete tenant-scoped Inventory operational and reporting API. */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Warehouses, stock, serials, transfers, counts and reporting")
public class InventoryController {

    private final InventoryWarehouseService warehouses;
    private final InventoryQueryService queries;
    private final InventoryWorkflowService workflows;
    private final InventoryAuditService audit;
    private final TenantContext tenantContext;

    @GetMapping("/dashboard")
    @PreAuthorize("@authz.has('inventory:view')")
    public ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.ok(queries.dashboard());
    }

    @GetMapping("/warehouses")
    @PreAuthorize("@authz.has('inventory:view')")
    public ApiResponse<List<WarehouseResponse>> warehouses() {
        return ApiResponse.ok(warehouses.list());
    }

    @PostMapping("/warehouses")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('inventory:manage-warehouses')")
    public ApiResponse<WarehouseResponse> createWarehouse(@Valid @RequestBody WarehouseRequest request) {
        return ApiResponse.ok(warehouses.create(request));
    }

    @PutMapping("/warehouses/{id}")
    @PreAuthorize("@authz.has('inventory:manage-warehouses')")
    public ApiResponse<WarehouseResponse> updateWarehouse(@PathVariable Long id,
                                                           @Valid @RequestBody WarehouseRequest request) {
        return ApiResponse.ok(warehouses.update(id, request));
    }

    @DeleteMapping("/warehouses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('inventory:manage-warehouses')")
    public void deleteWarehouse(@PathVariable Long id) {
        warehouses.delete(id);
    }

    @GetMapping("/warehouses/{warehouseId}/locations")
    @PreAuthorize("@authz.has('inventory:view')")
    public ApiResponse<List<LocationResponse>> locations(@PathVariable Long warehouseId) {
        return ApiResponse.ok(warehouses.locations(warehouseId));
    }

    @PostMapping("/warehouses/{warehouseId}/locations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('inventory:manage-warehouses')")
    public ApiResponse<LocationResponse> createLocation(@PathVariable Long warehouseId,
                                                         @Valid @RequestBody LocationRequest request) {
        return ApiResponse.ok(warehouses.createLocation(warehouseId, request));
    }

    @PutMapping("/warehouses/{warehouseId}/locations/{id}")
    @PreAuthorize("@authz.has('inventory:manage-warehouses')")
    public ApiResponse<LocationResponse> updateLocation(@PathVariable Long warehouseId,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody LocationRequest request) {
        return ApiResponse.ok(warehouses.updateLocation(warehouseId, id, request));
    }

    @DeleteMapping("/warehouses/{warehouseId}/locations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('inventory:manage-warehouses')")
    public void deleteLocation(@PathVariable Long warehouseId, @PathVariable Long id) {
        warehouses.deleteLocation(warehouseId, id);
    }

    @GetMapping("/balances")
    @PreAuthorize("@authz.has('inventory:view')")
    public ApiResponse<PageResponse<BalanceResponse>> balances(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(queries.balances(search, warehouseId, lowStock, page, size));
    }

    @GetMapping("/movements")
    @PreAuthorize("@authz.has('inventory:view')")
    public ApiResponse<PageResponse<MovementResponse>> movements(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(queries.movements(search, warehouseId, movementType,
                sourceType, from, to, page, size));
    }

    @GetMapping("/serials")
    @PreAuthorize("@authz.has('inventory:view')")
    public ApiResponse<PageResponse<SerialResponse>> serials(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(queries.serials(search, warehouseId, status, page, size));
    }

    @PutMapping("/serials/{id}/status")
    @PreAuthorize("@authz.has('inventory:adjust')")
    public ApiResponse<Void> updateSerialStatus(@PathVariable Long id,
                                                 @Valid @RequestBody SerialStatusRequest request) {
        workflows.updateSerialStatus(id, request);
        return ApiResponse.ok();
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('inventory:adjust')")
    public ApiResponse<AdjustmentResponse> adjust(@Valid @RequestBody AdjustmentRequest request) {
        return ApiResponse.ok(workflows.adjust(request));
    }

    @GetMapping("/transfers")
    @PreAuthorize("@authz.has('inventory:view')")
    public ApiResponse<PageResponse<TransferResponse>> transfers(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(workflows.transfers(status, page, size));
    }

    @GetMapping("/transfers/{id}")
    @PreAuthorize("@authz.has('inventory:view')")
    public ApiResponse<TransferResponse> transfer(@PathVariable Long id) {
        return ApiResponse.ok(workflows.getTransfer(id));
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('inventory:transfer')")
    public ApiResponse<TransferResponse> createTransfer(@Valid @RequestBody TransferRequest request) {
        return ApiResponse.ok(workflows.createTransfer(request));
    }

    @PostMapping("/transfers/{id}/ship")
    @PreAuthorize("@authz.has('inventory:transfer')")
    public ApiResponse<TransferResponse> shipTransfer(@PathVariable Long id) {
        return ApiResponse.ok(workflows.shipTransfer(id));
    }

    @PostMapping("/transfers/{id}/receive")
    @PreAuthorize("@authz.has('inventory:transfer')")
    public ApiResponse<TransferResponse> receiveTransfer(@PathVariable Long id) {
        return ApiResponse.ok(workflows.receiveTransfer(id));
    }

    @PostMapping("/transfers/{id}/cancel")
    @PreAuthorize("@authz.has('inventory:transfer')")
    public ApiResponse<TransferResponse> cancelTransfer(@PathVariable Long id) {
        return ApiResponse.ok(workflows.cancelTransfer(id));
    }

    @GetMapping("/counts")
    @PreAuthorize("@authz.has('inventory:view')")
    public ApiResponse<PageResponse<CountResponse>> counts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(workflows.counts(status, page, size));
    }

    @GetMapping("/counts/{id}")
    @PreAuthorize("@authz.has('inventory:view')")
    public ApiResponse<CountResponse> count(@PathVariable Long id) {
        return ApiResponse.ok(workflows.getCount(id));
    }

    @PostMapping("/counts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('inventory:count')")
    public ApiResponse<CountResponse> createCount(@Valid @RequestBody CountRequest request) {
        return ApiResponse.ok(workflows.createCount(request));
    }

    @PutMapping("/counts/{id}/results")
    @PreAuthorize("@authz.has('inventory:count')")
    public ApiResponse<CountResponse> submitCount(@PathVariable Long id,
                                                   @Valid @RequestBody CountResultRequest request) {
        return ApiResponse.ok(workflows.submitCount(id, request));
    }

    @PostMapping("/counts/{id}/post")
    @PreAuthorize("@authz.has('inventory:count')")
    public ApiResponse<CountResponse> postCount(@PathVariable Long id) {
        return ApiResponse.ok(workflows.postCount(id));
    }

    @PostMapping("/counts/{id}/cancel")
    @PreAuthorize("@authz.has('inventory:count')")
    public ApiResponse<CountResponse> cancelCount(@PathVariable Long id) {
        return ApiResponse.ok(workflows.cancelCount(id));
    }

    @GetMapping("/reservations")
    @PreAuthorize("@authz.has('inventory:view')")
    public ApiResponse<PageResponse<ReservationResponse>> reservations(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(queries.reservations(sourceType, status, page, size));
    }

    @PostMapping("/reservations/{id}/release")
    @PreAuthorize("@authz.has('inventory:reserve')")
    public ApiResponse<Void> releaseReservation(@PathVariable Long id,
                                                 @Valid @RequestBody ReasonRequest request) {
        workflows.releaseReservation(id, request.reason());
        return ApiResponse.ok();
    }

    @GetMapping("/reports")
    @PreAuthorize("@authz.has('inventory:report')")
    public ApiResponse<ReportResponse> report(@RequestParam(required = false) Instant from,
                                               @RequestParam(required = false) Instant to) {
        return ApiResponse.ok(queries.report(from, to));
    }

    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("@authz.has('inventory:export')")
    @Operation(summary = "Export the filtered balance report as Excel-compatible UTF-8 CSV")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String search,
                                          @RequestParam(required = false) Long warehouseId,
                                          @RequestParam(required = false) Boolean lowStock) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''inventory-balances.csv")
                .body(queries.exportBalances(search, warehouseId, lowStock));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.has('inventory:import')")
    public ApiResponse<ImportResult> importAdjustments(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(workflows.importAdjustments(file));
    }

    @GetMapping("/audit")
    @PreAuthorize("@authz.has('inventory:view-audit')")
    public ApiResponse<List<AuditResponse>> audit(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(audit.list(tenantContext.requireTenantId(), entityType, entityId, limit));
    }

    public record ReasonRequest(@Size(max = 500) String reason) {
    }
}
