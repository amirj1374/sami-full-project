package com.sami.app.crm.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.api.PageResponse;
import com.sami.app.crm.dto.BlacklistDtos.BlacklistEntryResponse;
import com.sami.app.crm.dto.BlacklistDtos.BlacklistRequest;
import com.sami.app.crm.dto.CrmStatsResponse;
import com.sami.app.crm.dto.CustomerResponse;
import com.sami.app.crm.dto.ImportResultResponse;
import com.sami.app.crm.dto.PreferenceDtos.PreferencesRequest;
import com.sami.app.crm.dto.RelationDtos.RelationRequest;
import com.sami.app.crm.dto.RelationDtos.RelationResponse;
import com.sami.app.crm.service.CrmStatsService;
import com.sami.app.crm.service.CustomerBlacklistService;
import com.sami.app.crm.service.CustomerImportService;
import com.sami.app.crm.service.CustomerPreferenceService;
import com.sami.app.crm.service.CustomerRelationService;
import com.sami.app.crm.service.SegmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.util.Map;

/**
 * Customer extensions: relationships, preferences, blacklist, CSV import,
 * segment evaluation and report stats.
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer extensions", description = "Relations, preferences, blacklist, import, stats")
public class CustomerExtensionsController {

    private final CustomerRelationService relationService;
    private final CustomerPreferenceService preferenceService;
    private final CustomerBlacklistService blacklistService;
    private final CustomerImportService importService;
    private final SegmentService segmentService;
    private final CrmStatsService statsService;

    // ------------------------------------------------------------- relations

    @GetMapping("/{id}/relations")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "Relationships (both directions)")
    public ApiResponse<List<RelationResponse>> relations(@PathVariable Long id) {
        return ApiResponse.ok(relationService.list(id));
    }

    @PostMapping("/{id}/relations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@authz.has('customers:edit')")
    @Operation(summary = "Add a relationship")
    public ApiResponse<RelationResponse> addRelation(@PathVariable Long id,
                                                     @Valid @RequestBody RelationRequest request) {
        return ApiResponse.ok(relationService.add(id, request));
    }

    @DeleteMapping("/{id}/relations/{relationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@authz.has('customers:edit')")
    @Operation(summary = "Remove a relationship")
    public void removeRelation(@PathVariable Long id, @PathVariable Long relationId) {
        relationService.remove(id, relationId);
    }

    // ----------------------------------------------------------- preferences

    @PutMapping("/{id}/preferences")
    @PreAuthorize("@authz.has('customers:edit')")
    @Operation(summary = "Replace a customer's preference values (validated against the definitions)")
    public ApiResponse<Map<String, Object>> updatePreferences(
            @PathVariable Long id, @RequestBody PreferencesRequest request) {
        return ApiResponse.ok(preferenceService.updatePreferences(id, request.preferences()));
    }

    // ------------------------------------------------------------- blacklist

    @PostMapping("/{id}/blacklist")
    @PreAuthorize("@authz.has('customers:edit')")
    @Operation(summary = "Blacklist a customer with a configurable reason "
            + "(publishes the BLACKLISTED business event)")
    public ApiResponse<CustomerResponse> blacklist(@PathVariable Long id,
                                                   @Valid @RequestBody BlacklistRequest request) {
        return ApiResponse.ok(blacklistService.blacklist(id, request));
    }

    @PostMapping("/{id}/unblacklist")
    @PreAuthorize("@authz.has('customers:edit')")
    @Operation(summary = "Lift the blacklist and return the customer to the default status")
    public ApiResponse<CustomerResponse> unblacklist(@PathVariable Long id) {
        return ApiResponse.ok(blacklistService.lift(id));
    }

    @GetMapping("/{id}/blacklist-history")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "All blacklist episodes of a customer")
    public ApiResponse<List<BlacklistEntryResponse>> blacklistHistory(@PathVariable Long id) {
        return ApiResponse.ok(blacklistService.history(id));
    }

    // ---------------------------------------------------------------- import

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.has('customers:import')")
    @Operation(summary = "Import customers from CSV (header-driven; duplicates and bad rows "
            + "are reported per line, never aborting the batch)")
    public ApiResponse<ImportResultResponse> importCsv(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(importService.importCsv(file));
    }

    // ------------------------------------------------------ segments & stats

    @GetMapping("/segments/{segmentId}/run")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "Evaluate a stored segment against live data")
    public ApiResponse<PageResponse<CustomerResponse>> runSegment(
            @PathVariable Long segmentId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(segmentService.run(segmentId, pageable)));
    }

    @GetMapping("/stats")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "Aggregate counts: total, new (30 days), by status/type/source")
    public ApiResponse<CrmStatsResponse> stats() {
        return ApiResponse.ok(statsService.stats());
    }
}
