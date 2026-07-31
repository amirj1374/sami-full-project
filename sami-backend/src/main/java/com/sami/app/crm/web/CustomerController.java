package com.sami.app.crm.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.crm.dto.CustomerDetailResponse;
import com.sami.app.crm.dto.CustomerEventResponse;
import com.sami.app.crm.dto.CustomerFilter;
import com.sami.app.crm.dto.CustomerRequest;
import com.sami.app.crm.dto.CustomerResponse;
import com.sami.app.crm.dto.DuplicateMatchResponse;
import com.sami.app.crm.dto.NoteDtos.NoteRequest;
import com.sami.app.crm.dto.NoteDtos.NoteResponse;
import com.sami.app.crm.service.CustomerEventService;
import com.sami.app.crm.service.CustomerMergeService;
import com.sami.app.crm.service.CustomerNoteService;
import com.sami.app.crm.service.CustomerService;
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

/**
 * Customer 360°: CRUD with duplicate prevention, lifecycle, merge, timeline,
 * notes, avatar and export. Every endpoint declares its permission code.
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer relationship management")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerMergeService mergeService;
    private final CustomerNoteService noteService;
    private final CustomerEventService eventService;

    // ----------------------------------------------------------------- reads

    @GetMapping
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "List customers (global search, combinable filters, pagination, sorting)")
    public ApiResponse<PageResponse<CustomerResponse>> list(
            CustomerFilter filter,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(customerService.list(filter, pageable)));
    }

    @GetMapping("/export")
    @PreAuthorize("@authz.has('customers:export')")
    @Operation(summary = "Export customers matching the current filters as CSV")
    public ResponseEntity<String> export(CustomerFilter filter) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=\"customers.csv\"")
                .body(customerService.exportCsv(filter));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "Get a customer's full profile (contacts, addresses, tags)")
    public ApiResponse<CustomerDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(customerService.getDetail(id));
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "Chronological 360° timeline (optionally filtered by event type)")
    public ApiResponse<PageResponse<CustomerEventResponse>> timeline(
            @PathVariable Long id,
            @RequestParam(required = false) String eventType,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(eventService.timeline(id, eventType, pageable)));
    }

    // ----------------------------------------------------------------- write

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('customers:create')")
    @Operation(summary = "Create a customer (409 with matches when duplicate rules trigger; "
            + "re-submit with ignoreDuplicates after confirmation)")
    public ApiResponse<CustomerDetailResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok(customerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('customers:edit')")
    @Operation(summary = "Update a customer (contacts/addresses are full-replacement lists)")
    public ApiResponse<CustomerDetailResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok(customerService.update(id, request));
    }

    // ------------------------------------------------------------ duplicates

    @PostMapping("/duplicates/check")
    @PreAuthorize("@authz.hasAny('customers:create','customers:edit')")
    @Operation(summary = "Check a candidate against the enabled duplicate rules")
    public ApiResponse<List<DuplicateMatchResponse>> checkDuplicates(
            @RequestBody CustomerRequest candidate,
            @RequestParam(required = false) Long excludeId) {
        return ApiResponse.ok(customerService.checkDuplicates(candidate, excludeId));
    }

    @PostMapping("/{sourceId}/merge-into/{targetId}")
    @PreAuthorize("@authz.has('customers:merge')")
    @Operation(summary = "Merge a duplicate into the surviving record "
            + "(history, notes, contacts and references all preserved)")
    public ApiResponse<CustomerDetailResponse> merge(@PathVariable Long sourceId,
                                                     @PathVariable Long targetId) {
        return ApiResponse.ok(mergeService.merge(sourceId, targetId));
    }

    // ------------------------------------------------------------- lifecycle

    @PostMapping("/{id}/archive")
    @PreAuthorize("@authz.has('customers:archive')")
    @Operation(summary = "Archive a customer")
    public ApiResponse<CustomerResponse> archive(@PathVariable Long id) {
        return ApiResponse.ok(customerService.archive(id));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("@authz.has('customers:restore')")
    @Operation(summary = "Restore an archived or soft-deleted customer")
    public ApiResponse<CustomerResponse> restore(@PathVariable Long id) {
        return ApiResponse.ok(customerService.restore(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.has('customers:delete')")
    @Operation(summary = "Soft-delete a customer (record and references preserved)")
    public ApiResponse<CustomerResponse> softDelete(@PathVariable Long id) {
        return ApiResponse.ok(customerService.softDelete(id));
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('customers:purge')")
    @Operation(summary = "Permanently remove a soft-deleted customer (refused for merge participants)")
    public void purge(@PathVariable Long id) {
        customerService.purge(id);
    }

    // ----------------------------------------------------------------- notes

    @GetMapping("/{id}/notes")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "Notes (public + the caller's private ones), newest first")
    public ApiResponse<PageResponse<NoteResponse>> notes(
            @PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(noteService.list(id, pageable)));
    }

    @PostMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('customers:edit')")
    @Operation(summary = "Add a note")
    public ApiResponse<NoteResponse> addNote(@PathVariable Long id,
                                             @Valid @RequestBody NoteRequest request) {
        return ApiResponse.ok(noteService.add(id, request));
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('customers:edit')")
    @Operation(summary = "Delete one's own note")
    public void deleteNote(@PathVariable Long id, @PathVariable Long noteId) {
        noteService.delete(id, noteId);
    }

    // ---------------------------------------------------------------- avatar

    @PutMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.has('customers:edit')")
    @Operation(summary = "Upload or replace the profile image")
    public ApiResponse<Void> uploadAvatar(@PathVariable Long id,
                                          @RequestParam("file") MultipartFile file) {
        customerService.uploadAvatar(id, file);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/avatar")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "Get the profile image")
    public ResponseEntity<byte[]> avatar(@PathVariable Long id) {
        return customerService.loadAvatar(id)
                .map(file -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(file.contentType()))
                        .body(file.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
