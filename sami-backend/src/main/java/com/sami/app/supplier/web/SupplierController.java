package com.sami.app.supplier.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.supplier.dto.SupplierDtos.DocumentResponse;
import com.sami.app.supplier.dto.SupplierDtos.LogResponse;
import com.sami.app.supplier.dto.SupplierDtos.RateRequest;
import com.sami.app.supplier.dto.SupplierDtos.RatingResponse;
import com.sami.app.supplier.dto.SupplierDtos.SupplierDetailResponse;
import com.sami.app.supplier.dto.SupplierDtos.SupplierFilter;
import com.sami.app.supplier.dto.SupplierDtos.SupplierRequest;
import com.sami.app.supplier.dto.SupplierDtos.SupplierRowResponse;
import com.sami.app.supplier.service.SupLogService;
import com.sami.app.supplier.service.SupplierDocumentService;
import com.sami.app.supplier.service.SupplierRatingService;
import com.sami.app.supplier.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import java.util.List;

/** Supplier registry: profiles, lifecycle, evaluation, documents, history. */
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Supplier registry, contacts, banking and evaluation")
public class SupplierController {

    private final SupplierService supplierService;
    private final SupplierRatingService ratingService;
    private final SupplierDocumentService documentService;
    private final SupLogService logService;

    // ----------------------------------------------------------------- reads

    @GetMapping
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "List suppliers (global search + combinable filters incl. rating)")
    public ApiResponse<PageResponse<SupplierRowResponse>> list(
            SupplierFilter filter,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(supplierService.list(filter, pageable)));
    }

    @GetMapping("/export")
    @PreAuthorize("@authz.has('suppliers:export')")
    @Operation(summary = "Export suppliers matching the current filters as CSV")
    public ResponseEntity<String> export(SupplierFilter filter) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=\"suppliers.csv\"")
                .body(supplierService.exportCsv(filter));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "Full supplier profile (channels, addresses, contacts, banks)")
    public ApiResponse<SupplierDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.getDetail(id));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "Chronological supplier history")
    public ApiResponse<PageResponse<LogResponse>> logs(
            @PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(logService.history(id, pageable)));
    }

    // ----------------------------------------------------------------- write

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('suppliers:create')")
    @Operation(summary = "Create a supplier (409 on configured duplicate identifiers; "
            + "re-submit with ignoreDuplicates after confirmation)")
    public ApiResponse<SupplierDetailResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ApiResponse.ok(supplierService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('suppliers:edit')")
    @Operation(summary = "Update a supplier (child lists are full replacements)")
    public ApiResponse<SupplierDetailResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody SupplierRequest request) {
        return ApiResponse.ok(supplierService.update(id, request));
    }

    // ------------------------------------------------------------- lifecycle

    @PostMapping("/{id}/archive")
    @PreAuthorize("@authz.has('suppliers:archive')")
    @Operation(summary = "Archive a supplier")
    public ApiResponse<SupplierRowResponse> archive(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.archive(id));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("@authz.has('suppliers:restore')")
    @Operation(summary = "Restore an archived or soft-deleted supplier")
    public ApiResponse<SupplierRowResponse> restore(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.restore(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.has('suppliers:delete')")
    @Operation(summary = "Soft-delete a supplier (record and references preserved)")
    public ApiResponse<SupplierRowResponse> softDelete(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.softDelete(id));
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('suppliers:purge')")
    @Operation(summary = "Permanently remove a soft-deleted supplier (refused while referenced)")
    public void purge(@PathVariable Long id) {
        supplierService.purge(id);
    }

    // ---------------------------------------------------------------- rating

    @GetMapping("/{id}/ratings")
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "Current per-criterion scores")
    public ApiResponse<List<RatingResponse>> ratings(@PathVariable Long id) {
        return ApiResponse.ok(ratingService.ratings(id));
    }

    @PutMapping("/{id}/ratings")
    @PreAuthorize("@authz.has('suppliers:rate')")
    @Operation(summary = "Rate a supplier (scores 0-5; overall = configurable weighted average)")
    public ApiResponse<List<RatingResponse>> rate(@PathVariable Long id,
                                                  @Valid @RequestBody RateRequest request) {
        return ApiResponse.ok(ratingService.rate(id, request));
    }

    // ------------------------------------------------------------- documents

    @GetMapping("/{id}/documents")
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "List documents")
    public ApiResponse<List<DocumentResponse>> documents(@PathVariable Long id) {
        return ApiResponse.ok(documentService.list(id));
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('suppliers:edit')")
    @Operation(summary = "Upload a document (any file type, tagged with a configurable kind)")
    public ApiResponse<DocumentResponse> uploadDocument(
            @PathVariable Long id,
            @RequestParam(required = false) Long docTypeId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(documentService.upload(id, docTypeId, file));
    }

    @GetMapping("/{id}/documents/{documentId}")
    @PreAuthorize("@authz.has('suppliers:view')")
    @Operation(summary = "Download a document")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id,
                                                   @PathVariable Long documentId) {
        return documentService.download(id, documentId)
                .map(file -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(
                                file.contentType() != null ? file.contentType()
                                        : "application/octet-stream"))
                        .header("Content-Disposition",
                                "attachment; filename=\"" + file.fileName() + "\"")
                        .body(file.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('suppliers:edit')")
    @Operation(summary = "Delete a document")
    public void deleteDocument(@PathVariable Long id, @PathVariable Long documentId) {
        documentService.delete(id, documentId);
    }
}
