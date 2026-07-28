package com.sami.app.purchasing.web;

import com.sami.app.common.api.ApiResponse;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.purchasing.domain.PurApprovalRule;
import com.sami.app.purchasing.domain.PurType;
import com.sami.app.purchasing.dto.PurLookupDtos.ApprovalRuleRequest;
import com.sami.app.purchasing.dto.PurLookupDtos.ApprovalRuleResponse;
import com.sami.app.purchasing.dto.PurLookupDtos.CancelReasonResponse;
import com.sami.app.purchasing.dto.PurLookupDtos.IdentifierTypeResponse;
import com.sami.app.purchasing.dto.PurLookupDtos.StatusResponse;
import com.sami.app.purchasing.dto.PurLookupDtos.TypeRequest;
import com.sami.app.purchasing.dto.PurLookupDtos.TypeResponse;
import com.sami.app.purchasing.dto.PurLookupDtos.WarehouseResponse;
import com.sami.app.purchasing.repository.PurApprovalRuleRepository;
import com.sami.app.purchasing.repository.PurCancelReasonRepository;
import com.sami.app.purchasing.repository.PurIdentifierTypeRepository;
import com.sami.app.purchasing.repository.PurStatusRepository;
import com.sami.app.purchasing.repository.PurTypeRepository;
import com.sami.app.purchasing.repository.PurWarehouseRepository;
import com.sami.app.purchasing.repository.PurchaseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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

/**
 * Purchasing configuration: statuses (read-only structure, cosmetic edits via
 * DB seeds), types, cancel reasons, identifier types, warehouses and approval
 * rules. Writes require {@code purchasing:manage-config} (or purchasing:edit).
 */
@RestController
@RequestMapping("/api/v1/purchasing")
@RequiredArgsConstructor
@Tag(name = "Purchasing configuration", description = "Configurable purchasing lookups and rules")
public class PurchasingConfigController {

    private static final String MANAGE = "@authz.hasAny('purchasing:manage-config','purchasing:edit')";

    private final PurStatusRepository statusRepository;
    private final PurTypeRepository typeRepository;
    private final PurCancelReasonRepository cancelReasonRepository;
    private final PurIdentifierTypeRepository identifierTypeRepository;
    private final PurWarehouseRepository warehouseRepository;
    private final PurApprovalRuleRepository approvalRuleRepository;
    private final PurchaseRepository purchaseRepository;

    @GetMapping("/statuses")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "List purchase statuses")
    public ApiResponse<List<StatusResponse>> statuses() {
        return ApiResponse.ok(statusRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(StatusResponse::from).toList());
    }

    @GetMapping("/types")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "List purchase types")
    public ApiResponse<List<TypeResponse>> types() {
        return ApiResponse.ok(typeRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(TypeResponse::from).toList());
    }

    @PostMapping("/types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a purchase type (with its numbering prefix)")
    @Transactional
    public ApiResponse<TypeResponse> createType(@Valid @RequestBody TypeRequest request) {
        if (typeRepository.existsByCodeIgnoreCase(request.code())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A purchase type with code '%s' already exists".formatted(request.code()));
        }
        return ApiResponse.ok(TypeResponse.from(typeRepository.save(PurType.builder()
                .code(request.code()).name(request.name()).description(request.description())
                .numberPrefix(request.numberPrefix()).active(request.active())
                .displayOrder(request.displayOrder()).build())));
    }

    @PutMapping("/types/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update a purchase type")
    @Transactional
    public ApiResponse<TypeResponse> updateType(@PathVariable Long id,
                                                @Valid @RequestBody TypeRequest request) {
        PurType type = typeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Purchase type", id));
        if (type.isSystem() && !type.getCode().equals(request.code())) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System type codes are immutable");
        }
        if (typeRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A purchase type with code '%s' already exists".formatted(request.code()));
        }
        type.setCode(request.code());
        type.setName(request.name());
        type.setDescription(request.description());
        type.setNumberPrefix(request.numberPrefix());
        type.setActive(request.active());
        type.setDisplayOrder(request.displayOrder());
        return ApiResponse.ok(TypeResponse.from(type));
    }

    @DeleteMapping("/types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete a purchase type (blocked for system/in-use entries)")
    @Transactional
    public void deleteType(@PathVariable Long id) {
        PurType type = typeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Purchase type", id));
        if (type.isSystem()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System types cannot be deleted");
        }
        if (purchaseRepository.countByTypeId(id) > 0) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Cannot delete a type that is used by purchases");
        }
        typeRepository.delete(type);
    }

    @GetMapping("/cancel-reasons")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "List cancellation reasons")
    public ApiResponse<List<CancelReasonResponse>> cancelReasons() {
        return ApiResponse.ok(cancelReasonRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(CancelReasonResponse::from).toList());
    }

    @GetMapping("/identifier-types")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "List serialized-unit identifier types")
    public ApiResponse<List<IdentifierTypeResponse>> identifierTypes() {
        return ApiResponse.ok(identifierTypeRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(IdentifierTypeResponse::from).toList());
    }

    @GetMapping("/warehouses")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "List warehouses")
    public ApiResponse<List<WarehouseResponse>> warehouses() {
        return ApiResponse.ok(warehouseRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(WarehouseResponse::from).toList());
    }

    // -------------------------------------------------------- approval rules

    @GetMapping("/approval-rules")
    @PreAuthorize("@authz.has('purchasing:view')")
    @Operation(summary = "List approval rules")
    public ApiResponse<List<ApprovalRuleResponse>> approvalRules() {
        return ApiResponse.ok(approvalRuleRepository.findAll().stream()
                .map(ApprovalRuleResponse::from).toList());
    }

    @PostMapping("/approval-rules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create an amount-based approval rule")
    @Transactional
    public ApiResponse<ApprovalRuleResponse> createApprovalRule(
            @Valid @RequestBody ApprovalRuleRequest request) {
        return ApiResponse.ok(ApprovalRuleResponse.from(approvalRuleRepository.save(
                PurApprovalRule.builder()
                        .name(request.name())
                        .minAmount(request.minAmount())
                        .active(request.active())
                        .build())));
    }

    @PutMapping("/approval-rules/{id}")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Update an approval rule")
    @Transactional
    public ApiResponse<ApprovalRuleResponse> updateApprovalRule(
            @PathVariable Long id, @Valid @RequestBody ApprovalRuleRequest request) {
        PurApprovalRule rule = approvalRuleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Approval rule", id));
        rule.setName(request.name());
        rule.setMinAmount(request.minAmount());
        rule.setActive(request.active());
        return ApiResponse.ok(ApprovalRuleResponse.from(rule));
    }

    @DeleteMapping("/approval-rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Delete an approval rule")
    @Transactional
    public void deleteApprovalRule(@PathVariable Long id) {
        approvalRuleRepository.delete(approvalRuleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Approval rule", id)));
    }
}
