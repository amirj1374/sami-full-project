package com.sami.app.purchasing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.inventory.repository.InventoryWarehouseRepository;
import com.sami.app.purchasing.domain.PurApprovalRule;
import com.sami.app.purchasing.domain.PurCancelReason;
import com.sami.app.purchasing.domain.PurIdentifierType;
import com.sami.app.purchasing.domain.PurStatus;
import com.sami.app.purchasing.domain.PurType;
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
import com.sami.app.purchasing.repository.PurApprovalRuleRepository;
import com.sami.app.purchasing.repository.PurCancelReasonRepository;
import com.sami.app.purchasing.repository.PurIdentifierTypeRepository;
import com.sami.app.purchasing.repository.PurStatusRepository;
import com.sami.app.purchasing.repository.PurTypeRepository;
import com.sami.app.purchasing.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Tenant-aware purchasing lookups and approval policy. */
@Service
@RequiredArgsConstructor
public class PurchasingConfigService {

    private final TenantContext tenantContext;
    private final PurStatusRepository statusRepository;
    private final PurTypeRepository typeRepository;
    private final PurCancelReasonRepository cancelReasonRepository;
    private final PurIdentifierTypeRepository identifierTypeRepository;
    private final PurApprovalRuleRepository approvalRuleRepository;
    private final InventoryWarehouseRepository warehouseRepository;
    private final PurchaseRepository purchaseRepository;

    @Transactional(readOnly = true)
    public List<StatusResponse> statuses() {
        return visible(statusRepository.findByTenantIdIsNullOrTenantIdOrderByDisplayOrderAsc(
                tenantId()), PurStatus::getCode, PurStatus::getTenantId).stream()
                .map(StatusResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TypeResponse> types() {
        return visibleTypes().stream().map(TypeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CancelReasonResponse> cancelReasons() {
        return visibleReasons().stream().map(CancelReasonResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<IdentifierTypeResponse> identifierTypes() {
        return visibleIdentifierTypes().stream().map(IdentifierTypeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> warehouses() {
        return warehouseRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId()).stream()
                .map(WarehouseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ApprovalRuleResponse> approvalRules() {
        return effectiveApprovalRules().stream().map(ApprovalRuleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PurType requireType(Long id) {
        return visibleTypes().stream().filter(value -> value.getId().equals(id)).findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Purchase type", id));
    }

    @Transactional(readOnly = true)
    public PurType requireTypeByCode(String code) {
        return visibleTypes().stream().filter(value -> value.isActive())
                .filter(value -> value.getCode().equalsIgnoreCase(code)).findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED,
                        "Unknown or inactive purchase type '" + code + "'"));
    }

    @Transactional(readOnly = true)
    public PurCancelReason requireCancelReason(Long id) {
        return visibleReasons().stream().filter(value -> value.getId().equals(id)).findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Cancellation reason", id));
    }

    @Transactional(readOnly = true)
    public PurIdentifierType requireIdentifierType(Long id) {
        return visibleIdentifierTypes().stream().filter(value -> value.getId().equals(id))
                .filter(PurIdentifierType::isActive).findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Identifier type", id));
    }

    @Transactional(readOnly = true)
    public PurStatus requireDraftStatus() {
        return structural(PurStatus::isDraftState, "draft");
    }

    @Transactional(readOnly = true)
    public PurStatus requirePendingStatus() {
        return structural(PurStatus::isPendingState, "pending");
    }

    @Transactional(readOnly = true)
    public PurStatus requireApprovedStatus() {
        return structural(PurStatus::isApprovedState, "approved");
    }

    @Transactional(readOnly = true)
    public PurStatus requirePartialStatus() {
        return structural(PurStatus::isPartialState, "partially received");
    }

    @Transactional(readOnly = true)
    public PurStatus requireCompletedStatus() {
        return structural(PurStatus::isCompletedState, "completed");
    }

    @Transactional(readOnly = true)
    public PurStatus requireCancelledStatus() {
        return structural(PurStatus::isCancelledState, "cancelled");
    }

    @Transactional(readOnly = true)
    public PurStatus requireRejectedStatus() {
        return structural(PurStatus::isRejectedState, "rejected");
    }

    @Transactional(readOnly = true)
    public boolean requiresApproval(java.math.BigDecimal total) {
        return effectiveApprovalRules().stream().filter(PurApprovalRule::isActive)
                .anyMatch(rule -> total.compareTo(rule.getMinAmount()) >= 0);
    }

    @Transactional
    public TypeResponse createType(TypeRequest request) {
        assertUniqueTypeCode(request.code(), null);
        return TypeResponse.from(typeRepository.save(PurType.builder()
                .tenantId(tenantId()).code(normalize(request.code())).name(request.name())
                .description(request.description()).numberPrefix(request.numberPrefix())
                .active(request.active()).displayOrder(request.displayOrder()).build()));
    }

    @Transactional
    public TypeResponse updateType(Long id, TypeRequest request) {
        PurType source = requireType(id);
        PurType target = ownedOrTypeOverride(source, request.code());
        assertUniqueTypeCode(request.code(), target.getId());
        target.setCode(normalize(request.code()));
        target.setName(request.name());
        target.setDescription(request.description());
        target.setNumberPrefix(request.numberPrefix());
        target.setActive(request.active());
        target.setDisplayOrder(request.displayOrder());
        return TypeResponse.from(typeRepository.save(target));
    }

    @Transactional
    public void deleteType(Long id) {
        PurType type = requireOwned(requireType(id), PurType::getTenantId, "Purchase type");
        if (purchaseRepository.countByTenantIdAndTypeId(tenantId(), id) > 0) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Cannot delete a type that is used by purchases");
        }
        typeRepository.delete(type);
    }

    @Transactional
    public CancelReasonResponse createCancelReason(CancelReasonRequest request) {
        assertUniqueReasonCode(request.code(), null);
        return CancelReasonResponse.from(cancelReasonRepository.save(PurCancelReason.builder()
                .tenantId(tenantId()).code(normalize(request.code())).name(request.name())
                .active(request.active()).displayOrder(request.displayOrder()).build()));
    }

    @Transactional
    public CancelReasonResponse updateCancelReason(Long id, CancelReasonRequest request) {
        PurCancelReason source = requireCancelReason(id);
        PurCancelReason target = ownedOrReasonOverride(source, request.code());
        assertUniqueReasonCode(request.code(), target.getId());
        target.setCode(normalize(request.code()));
        target.setName(request.name());
        target.setActive(request.active());
        target.setDisplayOrder(request.displayOrder());
        return CancelReasonResponse.from(cancelReasonRepository.save(target));
    }

    @Transactional
    public void deleteCancelReason(Long id) {
        cancelReasonRepository.delete(requireOwned(requireCancelReason(id),
                PurCancelReason::getTenantId, "Cancellation reason"));
    }

    @Transactional
    public IdentifierTypeResponse createIdentifierType(IdentifierTypeRequest request) {
        assertUniqueIdentifierCode(request.code(), null);
        return IdentifierTypeResponse.from(identifierTypeRepository.save(PurIdentifierType.builder()
                .tenantId(tenantId()).code(normalize(request.code())).name(request.name())
                .satisfiesSerial(request.satisfiesSerial()).satisfiesImei(request.satisfiesImei())
                .active(request.active()).displayOrder(request.displayOrder()).build()));
    }

    @Transactional
    public IdentifierTypeResponse updateIdentifierType(Long id, IdentifierTypeRequest request) {
        PurIdentifierType source = requireIdentifierTypeIncludingInactive(id);
        PurIdentifierType target = ownedOrIdentifierOverride(source, request.code());
        assertUniqueIdentifierCode(request.code(), target.getId());
        target.setCode(normalize(request.code()));
        target.setName(request.name());
        target.setSatisfiesSerial(request.satisfiesSerial());
        target.setSatisfiesImei(request.satisfiesImei());
        target.setActive(request.active());
        target.setDisplayOrder(request.displayOrder());
        return IdentifierTypeResponse.from(identifierTypeRepository.save(target));
    }

    @Transactional
    public void deleteIdentifierType(Long id) {
        identifierTypeRepository.delete(requireOwned(requireIdentifierTypeIncludingInactive(id),
                PurIdentifierType::getTenantId, "Identifier type"));
    }

    @Transactional
    public ApprovalRuleResponse createApprovalRule(ApprovalRuleRequest request) {
        return ApprovalRuleResponse.from(approvalRuleRepository.save(PurApprovalRule.builder()
                .tenantId(tenantId()).name(request.name()).minAmount(request.minAmount())
                .active(request.active()).build()));
    }

    @Transactional
    public ApprovalRuleResponse updateApprovalRule(Long id, ApprovalRuleRequest request) {
        PurApprovalRule rule = approvalRuleRepository.findById(id)
                .filter(value -> tenantId().equals(value.getTenantId()))
                .orElseThrow(() -> ResourceNotFoundException.of("Approval rule", id));
        rule.setName(request.name());
        rule.setMinAmount(request.minAmount());
        rule.setActive(request.active());
        return ApprovalRuleResponse.from(rule);
    }

    @Transactional
    public void deleteApprovalRule(Long id) {
        PurApprovalRule rule = approvalRuleRepository.findById(id)
                .filter(value -> tenantId().equals(value.getTenantId()))
                .orElseThrow(() -> ResourceNotFoundException.of("Approval rule", id));
        approvalRuleRepository.delete(rule);
    }

    private List<PurType> visibleTypes() {
        return visible(typeRepository.findByTenantIdIsNullOrTenantIdOrderByDisplayOrderAsc(
                tenantId()), PurType::getCode, PurType::getTenantId);
    }

    private List<PurCancelReason> visibleReasons() {
        return visible(cancelReasonRepository.findByTenantIdIsNullOrTenantIdOrderByDisplayOrderAsc(
                tenantId()), PurCancelReason::getCode, PurCancelReason::getTenantId);
    }

    private List<PurIdentifierType> visibleIdentifierTypes() {
        return visible(identifierTypeRepository
                .findByTenantIdIsNullOrTenantIdOrderByDisplayOrderAsc(tenantId()),
                PurIdentifierType::getCode, PurIdentifierType::getTenantId);
    }

    private PurIdentifierType requireIdentifierTypeIncludingInactive(Long id) {
        return visibleIdentifierTypes().stream().filter(value -> value.getId().equals(id)).findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Identifier type", id));
    }

    private List<PurApprovalRule> effectiveApprovalRules() {
        List<PurApprovalRule> visible = approvalRuleRepository
                .findByTenantIdIsNullOrTenantId(tenantId());
        List<PurApprovalRule> tenantRules = visible.stream()
                .filter(value -> tenantId().equals(value.getTenantId())).toList();
        return tenantRules.isEmpty()
                ? visible.stream().filter(value -> value.getTenantId() == null).toList()
                : tenantRules;
    }

    private PurStatus structural(java.util.function.Predicate<PurStatus> role, String label) {
        return visible(statusRepository.findByTenantIdIsNullOrTenantIdOrderByDisplayOrderAsc(
                tenantId()), PurStatus::getCode, PurStatus::getTenantId).stream()
                .filter(role).findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "No " + label + " purchase status is configured"));
    }

    private PurType ownedOrTypeOverride(PurType source, String requestedCode) {
        if (tenantId().equals(source.getTenantId())) {
            return source;
        }
        if (!source.getCode().equalsIgnoreCase(requestedCode)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Platform type codes are immutable");
        }
        return PurType.builder().tenantId(tenantId()).code(source.getCode())
                .name(source.getName()).description(source.getDescription())
                .numberPrefix(source.getNumberPrefix()).isDefault(source.isDefault())
                .active(source.isActive()).displayOrder(source.getDisplayOrder()).build();
    }

    private PurCancelReason ownedOrReasonOverride(PurCancelReason source, String requestedCode) {
        if (tenantId().equals(source.getTenantId())) {
            return source;
        }
        if (!source.getCode().equalsIgnoreCase(requestedCode)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Platform cancellation reason codes are immutable");
        }
        return PurCancelReason.builder().tenantId(tenantId()).code(source.getCode())
                .name(source.getName()).active(source.isActive())
                .displayOrder(source.getDisplayOrder()).build();
    }

    private PurIdentifierType ownedOrIdentifierOverride(PurIdentifierType source,
                                                         String requestedCode) {
        if (tenantId().equals(source.getTenantId())) {
            return source;
        }
        if (!source.getCode().equalsIgnoreCase(requestedCode)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Platform identifier type codes are immutable");
        }
        return PurIdentifierType.builder().tenantId(tenantId()).code(source.getCode())
                .name(source.getName()).satisfiesSerial(source.isSatisfiesSerial())
                .satisfiesImei(source.isSatisfiesImei()).active(source.isActive())
                .displayOrder(source.getDisplayOrder()).build();
    }

    private void assertUniqueTypeCode(String code, Long excludingId) {
        boolean exists = excludingId == null
                ? typeRepository.existsByTenantIdAndCodeIgnoreCase(tenantId(), code)
                : typeRepository.existsByTenantIdAndCodeIgnoreCaseAndIdNot(
                        tenantId(), code, excludingId);
        assertUnique(exists, "purchase type", code);
    }

    private void assertUniqueReasonCode(String code, Long excludingId) {
        boolean exists = excludingId == null
                ? cancelReasonRepository.existsByTenantIdAndCodeIgnoreCase(tenantId(), code)
                : cancelReasonRepository.existsByTenantIdAndCodeIgnoreCaseAndIdNot(
                        tenantId(), code, excludingId);
        assertUnique(exists, "cancellation reason", code);
    }

    private void assertUniqueIdentifierCode(String code, Long excludingId) {
        boolean exists = excludingId == null
                ? identifierTypeRepository.existsByTenantIdAndCodeIgnoreCase(tenantId(), code)
                : identifierTypeRepository.existsByTenantIdAndCodeIgnoreCaseAndIdNot(
                        tenantId(), code, excludingId);
        assertUnique(exists, "identifier type", code);
    }

    private void assertUnique(boolean exists, String label, String code) {
        if (exists) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A %s with code '%s' already exists".formatted(label, code));
        }
    }

    private <T> T requireOwned(T value, Function<T, Long> tenant, String label) {
        if (!tenantId().equals(tenant.apply(value))) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    label + " is a platform default; create or edit a tenant override instead");
        }
        return value;
    }

    private <T> List<T> visible(List<T> values, Function<T, String> code,
                                Function<T, Long> tenant) {
        LinkedHashMap<String, T> byCode = new LinkedHashMap<>();
        values.stream().filter(value -> tenant.apply(value) == null)
                .forEach(value -> byCode.put(normalize(code.apply(value)), value));
        values.stream().filter(value -> tenantId().equals(tenant.apply(value)))
                .forEach(value -> byCode.put(normalize(code.apply(value)), value));
        return byCode.values().stream()
                .sorted(Comparator.comparingInt(this::displayOrder)).toList();
    }

    private int displayOrder(Object value) {
        if (value instanceof PurStatus status) return status.getDisplayOrder();
        if (value instanceof PurType type) return type.getDisplayOrder();
        if (value instanceof PurCancelReason reason) return reason.getDisplayOrder();
        return ((PurIdentifierType) value).getDisplayOrder();
    }

    private String normalize(String code) {
        return code.toLowerCase(Locale.ROOT);
    }

    private Long tenantId() {
        return tenantContext.requireTenantId();
    }
}
