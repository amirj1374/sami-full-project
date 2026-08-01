package com.sami.app.purchasing.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.purchasing.dto.PurLookupDtos.ApprovalRuleRequest;
import com.sami.app.purchasing.dto.PurLookupDtos.ApprovalRuleResponse;
import com.sami.app.purchasing.dto.PurLookupDtos.CancelReasonRequest;
import com.sami.app.purchasing.dto.PurLookupDtos.CancelReasonResponse;
import com.sami.app.purchasing.dto.PurLookupDtos.IdentifierTypeRequest;
import com.sami.app.purchasing.dto.PurLookupDtos.IdentifierTypeResponse;
import com.sami.app.purchasing.dto.PurLookupDtos.StatusResponse;
import com.sami.app.purchasing.dto.PurLookupDtos.TypeRequest;
import com.sami.app.purchasing.dto.PurLookupDtos.TypeResponse;
import com.sami.app.purchasing.dto.PurLookupDtos.WarehouseResponse;
import com.sami.app.purchasing.service.PurchasingConfigService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Tenant-aware purchasing lookups and approval configuration. */
@RestController
@RequestMapping("/api/v1/purchasing")
@RequiredArgsConstructor
@Tag(name = "Purchasing configuration", description = "Configurable purchasing lookups and rules")
public class PurchasingConfigController {

    private static final String MANAGE = "@authz.hasAny('purchasing:manage-config','purchasing:edit')";
    private final PurchasingConfigService service;

    @GetMapping("/statuses")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "List effective purchase statuses")
    public ApiResponse<List<StatusResponse>> statuses() {
        return ApiResponse.ok(service.statuses());
    }

    @GetMapping("/types")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "List effective purchase types")
    public ApiResponse<List<TypeResponse>> types() {
        return ApiResponse.ok(service.types());
    }

    @PostMapping("/types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public ApiResponse<TypeResponse> createType(@Valid @RequestBody TypeRequest request) {
        return ApiResponse.ok(service.createType(request));
    }

    @PutMapping("/types/{id}")
    @PreAuthorize(MANAGE)
    public ApiResponse<TypeResponse> updateType(@PathVariable Long id,
                                                @Valid @RequestBody TypeRequest request) {
        return ApiResponse.ok(service.updateType(id, request));
    }

    @DeleteMapping("/types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    public void deleteType(@PathVariable Long id) {
        service.deleteType(id);
    }

    @GetMapping("/cancel-reasons")
    @PreAuthorize("@authz.has('purchasing:view')")
    public ApiResponse<List<CancelReasonResponse>> cancelReasons() {
        return ApiResponse.ok(service.cancelReasons());
    }

    @PostMapping("/cancel-reasons")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public ApiResponse<CancelReasonResponse> createCancelReason(
            @Valid @RequestBody CancelReasonRequest request) {
        return ApiResponse.ok(service.createCancelReason(request));
    }

    @PutMapping("/cancel-reasons/{id}")
    @PreAuthorize(MANAGE)
    public ApiResponse<CancelReasonResponse> updateCancelReason(
            @PathVariable Long id, @Valid @RequestBody CancelReasonRequest request) {
        return ApiResponse.ok(service.updateCancelReason(id, request));
    }

    @DeleteMapping("/cancel-reasons/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    public void deleteCancelReason(@PathVariable Long id) {
        service.deleteCancelReason(id);
    }

    @GetMapping("/identifier-types")
    @PreAuthorize("@authz.has('purchasing:view')")
    public ApiResponse<List<IdentifierTypeResponse>> identifierTypes() {
        return ApiResponse.ok(service.identifierTypes());
    }

    @PostMapping("/identifier-types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public ApiResponse<IdentifierTypeResponse> createIdentifierType(
            @Valid @RequestBody IdentifierTypeRequest request) {
        return ApiResponse.ok(service.createIdentifierType(request));
    }

    @PutMapping("/identifier-types/{id}")
    @PreAuthorize(MANAGE)
    public ApiResponse<IdentifierTypeResponse> updateIdentifierType(
            @PathVariable Long id, @Valid @RequestBody IdentifierTypeRequest request) {
        return ApiResponse.ok(service.updateIdentifierType(id, request));
    }

    @DeleteMapping("/identifier-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    public void deleteIdentifierType(@PathVariable Long id) {
        service.deleteIdentifierType(id);
    }

    @GetMapping("/warehouses")
    @PreAuthorize("@authz.has('purchasing:view')")
    public ApiResponse<List<WarehouseResponse>> warehouses() {
        return ApiResponse.ok(service.warehouses());
    }

    @GetMapping("/approval-rules")
    @PreAuthorize("@authz.has('purchasing:view')")
    public ApiResponse<List<ApprovalRuleResponse>> approvalRules() {
        return ApiResponse.ok(service.approvalRules());
    }

    @PostMapping("/approval-rules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public ApiResponse<ApprovalRuleResponse> createApprovalRule(
            @Valid @RequestBody ApprovalRuleRequest request) {
        return ApiResponse.ok(service.createApprovalRule(request));
    }

    @PutMapping("/approval-rules/{id}")
    @PreAuthorize(MANAGE)
    public ApiResponse<ApprovalRuleResponse> updateApprovalRule(
            @PathVariable Long id, @Valid @RequestBody ApprovalRuleRequest request) {
        return ApiResponse.ok(service.updateApprovalRule(id, request));
    }

    @DeleteMapping("/approval-rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    public void deleteApprovalRule(@PathVariable Long id) {
        service.deleteApprovalRule(id);
    }
}
