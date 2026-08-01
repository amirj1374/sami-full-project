package com.sami.app.purchasing.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.AttachmentResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.CancelRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.LogResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.DashboardResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.ImportResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseDetailResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseFilter;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.PurchaseRowResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.ReceiptResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.ReceiveRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.ReturnRequest;
import com.sami.app.purchasing.dto.PurchaseDtos.ReturnResponse;
import com.sami.app.purchasing.dto.PurchaseDtos.ReportResponse;
import com.sami.app.purchasing.service.PurchaseAttachmentService;
import com.sami.app.purchasing.service.PurchaseLogService;
import com.sami.app.purchasing.service.PurchaseImportService;
import com.sami.app.purchasing.service.PurchaseReportingService;
import com.sami.app.purchasing.service.PurchaseReceivingService;
import com.sami.app.purchasing.service.PurchaseReturnService;
import com.sami.app.purchasing.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
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

import java.util.List;
import java.nio.charset.StandardCharsets;

/** Purchase documents: drafts, workflow, receiving, returns, attachments, history. */
@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
@Tag(name = "Purchasing", description = "Purchase orders, receiving and supplier returns")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final PurchaseReceivingService receivingService;
    private final PurchaseReturnService returnService;
    private final PurchaseAttachmentService attachmentService;
    private final PurchaseLogService logService;
    private final PurchaseReportingService reportingService;
    private final PurchaseImportService importService;

    // ----------------------------------------------------------------- reads

    @GetMapping
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "List purchases (global search + combinable filters)")
    public ApiResponse<PageResponse<PurchaseRowResponse>> list(
            PurchaseFilter filter,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(purchaseService.list(filter, pageable)));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("@authz.hasAny('purchasing:report','purchasing:view')")
    @Operation(summary = "Purchasing operational dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.ok(reportingService.dashboard());
    }

    @GetMapping("/reports")
    @PreAuthorize("@authz.has('purchasing:report')")
    @Operation(summary = "Purchasing status, type, supplier and overdue reports")
    public ApiResponse<ReportResponse> reports(PurchaseFilter filter) {
        return ApiResponse.ok(reportingService.report(filter));
    }

    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("@authz.has('purchasing:export')")
    @Operation(summary = "Export filtered purchases as Excel-compatible UTF-8 CSV")
    public ResponseEntity<byte[]> export(PurchaseFilter filter) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("purchases.csv", StandardCharsets.UTF_8).build().toString())
                .body(reportingService.exportCsv(filter));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.has('purchasing:import')")
    @Operation(summary = "Import purchase drafts from a validated, idempotent CSV file")
    public ApiResponse<ImportResponse> importCsv(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(importService.importCsv(file));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "Full purchase detail with items and remaining quantities")
    public ApiResponse<PurchaseDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(purchaseService.getDetail(id));
    }

    @GetMapping("/{id}/receipts")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "Receiving history (with serialized identifiers)")
    public ApiResponse<List<ReceiptResponse>> receipts(@PathVariable Long id) {
        return ApiResponse.ok(receivingService.history(id));
    }

    @GetMapping("/{id}/returns")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "Supplier-return history")
    public ApiResponse<List<ReturnResponse>> returns(@PathVariable Long id) {
        return ApiResponse.ok(returnService.history(id));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "Chronological purchase history")
    public ApiResponse<PageResponse<LogResponse>> logs(
            @PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(logService.history(id, pageable)));
    }

    // ---------------------------------------------------------------- drafts

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('purchasing:create')")
    @Operation(summary = "Create a purchase draft (auto-numbered)")
    public ApiResponse<PurchaseDetailResponse> create(@Valid @RequestBody PurchaseRequest request) {
        return ApiResponse.ok(purchaseService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('purchasing:edit')")
    @Operation(summary = "Update a draft (items are a full replacement)")
    public ApiResponse<PurchaseDetailResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody PurchaseRequest request) {
        return ApiResponse.ok(purchaseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('purchasing:delete')")
    @Operation(summary = "Delete a draft (non-drafts must be cancelled)")
    public void deleteDraft(@PathVariable Long id) {
        purchaseService.deleteDraft(id);
    }

    // -------------------------------------------------------------- workflow

    @PostMapping("/{id}/submit")
    @PreAuthorize("@authz.has('purchasing:edit')")
    @Operation(summary = "Submit a draft (approval rules decide pending vs auto-approved; "
            + "blocked suppliers are refused)")
    public ApiResponse<PurchaseDetailResponse> submit(@PathVariable Long id) {
        return ApiResponse.ok(purchaseService.submit(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@authz.has('purchasing:approve')")
    @Operation(summary = "Approve a pending purchase")
    public ApiResponse<PurchaseDetailResponse> approve(@PathVariable Long id) {
        return ApiResponse.ok(purchaseService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@authz.has('purchasing:approve')")
    @Operation(summary = "Reject a pending purchase")
    public ApiResponse<PurchaseDetailResponse> reject(@PathVariable Long id,
                                                      @RequestParam(required = false) String note) {
        return ApiResponse.ok(purchaseService.reject(id, note));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@authz.has('purchasing:cancel')")
    @Operation(summary = "Cancel a purchase with a configurable reason")
    public ApiResponse<PurchaseDetailResponse> cancel(@PathVariable Long id,
                                                      @Valid @RequestBody CancelRequest request) {
        return ApiResponse.ok(purchaseService.cancel(id, request));
    }

    // ------------------------------------------------------------- receiving

    @PostMapping("/{id}/receive")
    @PreAuthorize("@authz.has('purchasing:receive')")
    @Operation(summary = "Receive goods (full/partial; serialized units carry identifiers)")
    public ApiResponse<ReceiptResponse> receive(@PathVariable Long id,
                                                @Valid @RequestBody ReceiveRequest request) {
        return ApiResponse.ok(receivingService.receive(id, request));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("@authz.has('purchasing:return')")
    @Operation(summary = "Return received goods to the supplier")
    public ApiResponse<ReturnResponse> returnToSupplier(@PathVariable Long id,
                                                        @Valid @RequestBody ReturnRequest request) {
        return ApiResponse.ok(returnService.returnToSupplier(id, request));
    }

    // ----------------------------------------------------------- attachments

    @GetMapping("/{id}/attachments")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "List attachments")
    public ApiResponse<List<AttachmentResponse>> attachments(@PathVariable Long id) {
        return ApiResponse.ok(attachmentService.list(id));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('purchasing:edit')")
    @Operation(summary = "Upload an attachment (invoice, receipt, quote, photo, any document)")
    public ApiResponse<AttachmentResponse> uploadAttachment(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(attachmentService.upload(id, file));
    }

    @GetMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "Download an attachment")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long id,
                                                     @PathVariable Long attachmentId) {
        return attachmentService.download(id, attachmentId)
                .map(file -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(
                                file.contentType() != null ? file.contentType()
                                        : "application/octet-stream"))
                        .header("Content-Disposition", ContentDisposition.attachment()
                                .filename(file.fileName(), StandardCharsets.UTF_8)
                                .build().toString())
                        .body(file.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('purchasing:edit')")
    @Operation(summary = "Delete an attachment")
    public void deleteAttachment(@PathVariable Long id, @PathVariable Long attachmentId) {
        attachmentService.delete(id, attachmentId);
    }
}
