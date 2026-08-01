package com.sami.app.crm.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.crm.dto.BlacklistDtos.BlacklistReasonResponse;
import com.sami.app.crm.dto.PreferenceDtos.PreferenceDefinitionRequest;
import com.sami.app.crm.dto.PreferenceDtos.PreferenceDefinitionResponse;
import com.sami.app.crm.dto.RelationDtos.RelationTypeResponse;
import com.sami.app.crm.dto.SegmentDtos.SegmentRequest;
import com.sami.app.crm.dto.SegmentDtos.SegmentResponse;
import com.sami.app.crm.service.CustomerBlacklistService;
import com.sami.app.crm.service.CustomerRelationService;
import com.sami.app.crm.service.CustomerPreferenceService;
import com.sami.app.crm.service.SegmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

import java.util.List;

/**
 * Configuration for the CRM extensions: relation types, preference
 * definitions, blacklist reasons and segments. Writes require the dedicated
 * {@code customers:manage-config} permission (or {@code customers:edit}).
 */
@RestController
@RequestMapping("/api/v1/crm")
@RequiredArgsConstructor
@Tag(name = "CRM extension configuration",
        description = "Relation types, preference definitions, blacklist reasons, segments")
public class CrmExtensionConfigController {

    private static final String MANAGE = "@authz.hasAny('customers:manage-config','customers:edit')";

    private final CustomerRelationService relationService;
    private final CustomerBlacklistService blacklistService;
    private final CustomerPreferenceService preferenceService;
    private final SegmentService segmentService;

    @GetMapping("/relation-types")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "List relation types")
    public ApiResponse<List<RelationTypeResponse>> relationTypes() {
        return ApiResponse.ok(relationService.listTypes());
    }

    @GetMapping("/blacklist-reasons")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "List blacklist reasons")
    public ApiResponse<List<BlacklistReasonResponse>> blacklistReasons() {
        return ApiResponse.ok(blacklistService.reasons());
    }

    // ------------------------------------------------ preference definitions

    @GetMapping("/preference-definitions")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "List preference definitions (activeOnly=true for form rendering)")
    public ApiResponse<List<PreferenceDefinitionResponse>> preferenceDefinitions(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ApiResponse.ok(preferenceService.listDefinitions(activeOnly));
    }

    @PostMapping("/preference-definitions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a preference definition")
    public ApiResponse<PreferenceDefinitionResponse> createPreferenceDefinition(
            @Valid @RequestBody PreferenceDefinitionRequest request) {
        return ApiResponse.ok(preferenceService.createDefinition(request));
    }

    @PutMapping("/preference-definitions/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a preference definition (key and type immutable)")
    public ApiResponse<PreferenceDefinitionResponse> updatePreferenceDefinition(
            @PathVariable Long id, @Valid @RequestBody PreferenceDefinitionRequest request) {
        return ApiResponse.ok(preferenceService.updateDefinition(id, request));
    }

    @DeleteMapping("/preference-definitions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a preference definition (stored values remain)")
    public void deletePreferenceDefinition(@PathVariable Long id) {
        preferenceService.deleteDefinition(id);
    }

    // -------------------------------------------------------------- segments

    @GetMapping("/segments")
    @PreAuthorize("@authz.has('customers:view')")
    @Operation(summary = "List stored segments")
    public ApiResponse<List<SegmentResponse>> segments() {
        return ApiResponse.ok(segmentService.list());
    }

    @PostMapping("/segments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a segment (stored customer filter)")
    public ApiResponse<SegmentResponse> createSegment(@Valid @RequestBody SegmentRequest request) {
        return ApiResponse.ok(segmentService.create(request));
    }

    @PutMapping("/segments/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a segment")
    public ApiResponse<SegmentResponse> updateSegment(@PathVariable Long id,
                                                      @Valid @RequestBody SegmentRequest request) {
        return ApiResponse.ok(segmentService.update(id, request));
    }

    @DeleteMapping("/segments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a segment")
    public void deleteSegment(@PathVariable Long id) {
        segmentService.delete(id);
    }
}
