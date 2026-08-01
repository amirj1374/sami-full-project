package com.sami.app.crm.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.tenancy.TenantContext;
import com.sami.app.crm.domain.CustomerSource;
import com.sami.app.crm.domain.CustomerStatus;
import com.sami.app.crm.domain.CustomerTag;
import com.sami.app.crm.domain.CustomerType;
import com.sami.app.crm.domain.DuplicateRule;
import com.sami.app.crm.dto.CustomerSourceResponse;
import com.sami.app.crm.dto.CustomerStatusResponse;
import com.sami.app.crm.dto.CustomerTagResponse;
import com.sami.app.crm.dto.CustomerTypeResponse;
import com.sami.app.crm.dto.LookupRequests.DuplicateRulesRequest;
import com.sami.app.crm.dto.LookupRequests.SourceRequest;
import com.sami.app.crm.dto.LookupRequests.StatusRequest;
import com.sami.app.crm.dto.LookupRequests.TagRequest;
import com.sami.app.crm.dto.LookupRequests.TypeRequest;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.repository.CustomerSourceRepository;
import com.sami.app.crm.repository.CustomerStatusRepository;
import com.sami.app.crm.repository.CustomerTagRepository;
import com.sami.app.crm.repository.CustomerTypeRepository;
import com.sami.app.crm.repository.DuplicateRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tenant-aware CRM lookup configuration. Platform rows remain immutable
 * defaults; editing one creates a tenant override with the same business key.
 */
@Service
@RequiredArgsConstructor
public class CrmConfigService {

    private final CustomerTypeRepository typeRepository;
    private final CustomerStatusRepository statusRepository;
    private final CustomerSourceRepository sourceRepository;
    private final CustomerTagRepository tagRepository;
    private final DuplicateRuleRepository ruleRepository;
    private final CustomerRepository customerRepository;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<CustomerTypeResponse> listTypes() {
        return visibleTypes().stream().map(CustomerTypeResponse::from).toList();
    }

    @Transactional
    public CustomerTypeResponse createType(TypeRequest request) {
        Long tenantId = tenantId();
        if (typeRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, request.code())) {
            throw conflict("type", request.code());
        }
        return CustomerTypeResponse.from(typeRepository.save(CustomerType.builder()
                .tenantId(tenantId).code(request.code()).name(request.name())
                .description(request.description()).active(request.active())
                .displayOrder(request.displayOrder()).build()));
    }

    @Transactional
    public CustomerTypeResponse updateType(Long id, TypeRequest request) {
        CustomerType type = tenantTypeOrOverride(id);
        guardCode(type.isSystem(), type.getCode(), request.code());
        if (typeRepository.existsByTenantIdAndCodeIgnoreCaseAndIdNot(
                tenantId(), request.code(), type.getId())) {
            throw conflict("type", request.code());
        }
        type.setCode(request.code());
        type.setName(request.name());
        type.setDescription(request.description());
        type.setActive(request.active());
        type.setDisplayOrder(request.displayOrder());
        return CustomerTypeResponse.from(type);
    }

    @Transactional
    public void deleteType(Long id) {
        CustomerType type = typeRepository.findByIdAndTenantId(id, tenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer type", id));
        guardSystemDelete(type.isSystem());
        guardInUse(customerRepository.countByTenantIdAndTypeId(tenantId(), id));
        typeRepository.delete(type);
    }

    @Transactional(readOnly = true)
    public List<CustomerStatusResponse> listStatuses() {
        return visibleStatuses().stream().map(CustomerStatusResponse::from).toList();
    }

    @Transactional
    public CustomerStatusResponse createStatus(StatusRequest request) {
        Long tenantId = tenantId();
        if (statusRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, request.code())) {
            throw conflict("status", request.code());
        }
        return CustomerStatusResponse.from(statusRepository.save(CustomerStatus.builder()
                .tenantId(tenantId).code(request.code()).name(request.name())
                .description(request.description()).isBlocking(request.isBlocking())
                .hiddenByDefault(request.hiddenByDefault()).displayOrder(request.displayOrder())
                .build()));
    }

    @Transactional
    public CustomerStatusResponse updateStatus(Long id, StatusRequest request) {
        CustomerStatus status = tenantStatusOrOverride(id);
        guardCode(status.isSystem(), status.getCode(), request.code());
        if (statusRepository.existsByTenantIdAndCodeIgnoreCaseAndIdNot(
                tenantId(), request.code(), status.getId())) {
            throw conflict("status", request.code());
        }
        status.setCode(request.code());
        status.setName(request.name());
        status.setDescription(request.description());
        status.setBlocking(request.isBlocking());
        status.setHiddenByDefault(request.hiddenByDefault());
        status.setDisplayOrder(request.displayOrder());
        return CustomerStatusResponse.from(status);
    }

    @Transactional
    public void deleteStatus(Long id) {
        CustomerStatus status = statusRepository.findByIdAndTenantId(id, tenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer status", id));
        guardSystemDelete(status.isSystem());
        guardInUse(customerRepository.countByTenantIdAndStatusId(tenantId(), id));
        statusRepository.delete(status);
    }

    @Transactional(readOnly = true)
    public List<CustomerSourceResponse> listSources() {
        return visibleSources().stream().map(CustomerSourceResponse::from).toList();
    }

    @Transactional
    public CustomerSourceResponse createSource(SourceRequest request) {
        Long tenantId = tenantId();
        if (sourceRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, request.code())) {
            throw conflict("source", request.code());
        }
        return CustomerSourceResponse.from(sourceRepository.save(CustomerSource.builder()
                .tenantId(tenantId).code(request.code()).name(request.name())
                .active(request.active()).displayOrder(request.displayOrder()).build()));
    }

    @Transactional
    public CustomerSourceResponse updateSource(Long id, SourceRequest request) {
        CustomerSource source = tenantSourceOrOverride(id);
        guardCode(source.isSystem(), source.getCode(), request.code());
        if (sourceRepository.existsByTenantIdAndCodeIgnoreCaseAndIdNot(
                tenantId(), request.code(), source.getId())) {
            throw conflict("source", request.code());
        }
        source.setCode(request.code());
        source.setName(request.name());
        source.setActive(request.active());
        source.setDisplayOrder(request.displayOrder());
        return CustomerSourceResponse.from(source);
    }

    @Transactional
    public void deleteSource(Long id) {
        CustomerSource source = sourceRepository.findByIdAndTenantId(id, tenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer source", id));
        guardSystemDelete(source.isSystem());
        guardInUse(customerRepository.countByTenantIdAndSourceId(tenantId(), id));
        sourceRepository.delete(source);
    }

    @Transactional(readOnly = true)
    public List<CustomerTagResponse> listTags() {
        return visibleTags().stream().map(CustomerTagResponse::from).toList();
    }

    @Transactional
    public CustomerTagResponse createTag(TagRequest request) {
        Long tenantId = tenantId();
        if (tagRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.name())) {
            throw conflict("tag", request.name());
        }
        return CustomerTagResponse.from(tagRepository.save(CustomerTag.builder()
                .tenantId(tenantId).name(request.name()).color(request.color())
                .description(request.description()).active(request.active()).build()));
    }

    @Transactional
    public CustomerTagResponse updateTag(Long id, TagRequest request) {
        CustomerTag tag = tenantTagOrOverride(id);
        if (tagRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(
                tenantId(), request.name(), tag.getId())) {
            throw conflict("tag", request.name());
        }
        tag.setName(request.name());
        tag.setColor(request.color());
        tag.setDescription(request.description());
        tag.setActive(request.active());
        return CustomerTagResponse.from(tag);
    }

    @Transactional
    public void deleteTag(Long id) {
        CustomerTag tag = tagRepository.findByIdAndTenantId(id, tenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Tag", id));
        tagRepository.delete(tag);
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> listDuplicateRules() {
        LinkedHashMap<String, Boolean> result = new LinkedHashMap<>();
        visibleRules().forEach(rule -> result.put(rule.getIdentifier().name(), rule.isEnabled()));
        return result;
    }

    @Transactional
    public Map<String, Boolean> updateDuplicateRules(DuplicateRulesRequest request) {
        Map<DuplicateRule.Identifier, Boolean> toggles = new LinkedHashMap<>();
        request.rules().forEach(rule -> toggles.put(rule.identifier(), rule.enabled()));
        for (DuplicateRule visible : visibleRules()) {
            Boolean enabled = toggles.get(visible.getIdentifier());
            if (enabled == null || visible.isEnabled() == enabled) continue;
            DuplicateRule editable = visible.getTenantId() == null
                    ? ruleRepository.save(DuplicateRule.builder().tenantId(tenantId())
                            .identifier(visible.getIdentifier()).enabled(enabled).build())
                    : visible;
            editable.setEnabled(enabled);
        }
        return listDuplicateRules();
    }

    private CustomerType tenantTypeOrOverride(Long id) {
        return typeRepository.findByIdAndTenantId(id, tenantId()).orElseGet(() -> {
            CustomerType shared = sharedType(id);
            return typeRepository.save(CustomerType.builder().tenantId(tenantId())
                    .code(shared.getCode()).name(shared.getName()).description(shared.getDescription())
                    .isDefault(shared.isDefault()).isSystem(shared.isSystem())
                    .active(shared.isActive()).displayOrder(shared.getDisplayOrder()).build());
        });
    }

    private CustomerStatus tenantStatusOrOverride(Long id) {
        return statusRepository.findByIdAndTenantId(id, tenantId()).orElseGet(() -> {
            CustomerStatus shared = sharedStatus(id);
            return statusRepository.save(CustomerStatus.builder().tenantId(tenantId())
                    .code(shared.getCode()).name(shared.getName()).description(shared.getDescription())
                    .isBlocking(shared.isBlocking()).hiddenByDefault(shared.isHiddenByDefault())
                    .isDefault(shared.isDefault()).isArchivedState(shared.isArchivedState())
                    .isDeletedState(shared.isDeletedState()).isBlacklistState(shared.isBlacklistState())
                    .isSystem(shared.isSystem()).displayOrder(shared.getDisplayOrder()).build());
        });
    }

    private CustomerSource tenantSourceOrOverride(Long id) {
        return sourceRepository.findByIdAndTenantId(id, tenantId()).orElseGet(() -> {
            CustomerSource shared = sharedSource(id);
            return sourceRepository.save(CustomerSource.builder().tenantId(tenantId())
                    .code(shared.getCode()).name(shared.getName()).active(shared.isActive())
                    .isSystem(shared.isSystem()).displayOrder(shared.getDisplayOrder()).build());
        });
    }

    private CustomerTag tenantTagOrOverride(Long id) {
        return tagRepository.findByIdAndTenantId(id, tenantId()).orElseGet(() -> {
            CustomerTag shared = sharedTag(id);
            return tagRepository.save(CustomerTag.builder().tenantId(tenantId())
                    .name(shared.getName()).color(shared.getColor()).description(shared.getDescription())
                    .active(shared.isActive()).build());
        });
    }

    private CustomerType sharedType(Long id) {
        return typeRepository.findById(id).filter(row -> row.getTenantId() == null)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer type", id));
    }

    private CustomerStatus sharedStatus(Long id) {
        return statusRepository.findById(id).filter(row -> row.getTenantId() == null)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer status", id));
    }

    private CustomerSource sharedSource(Long id) {
        return sourceRepository.findById(id).filter(row -> row.getTenantId() == null)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer source", id));
    }

    private CustomerTag sharedTag(Long id) {
        return tagRepository.findById(id).filter(row -> row.getTenantId() == null)
                .orElseThrow(() -> ResourceNotFoundException.of("Tag", id));
    }

    private List<CustomerType> visibleTypes() {
        return resolve(typeRepository.findVisible(tenantId()), CustomerType::getCode, CustomerType::getTenantId);
    }

    private List<CustomerStatus> visibleStatuses() {
        return resolve(statusRepository.findVisible(tenantId()), CustomerStatus::getCode, CustomerStatus::getTenantId);
    }

    private List<CustomerSource> visibleSources() {
        return resolve(sourceRepository.findVisible(tenantId()), CustomerSource::getCode, CustomerSource::getTenantId);
    }

    private List<CustomerTag> visibleTags() {
        return resolve(tagRepository.findVisible(tenantId()), CustomerTag::getName, CustomerTag::getTenantId);
    }

    private List<DuplicateRule> visibleRules() {
        return resolve(ruleRepository.findVisible(tenantId()), row -> row.getIdentifier().name(), DuplicateRule::getTenantId);
    }

    private <T> List<T> resolve(List<T> rows, java.util.function.Function<T, String> key,
                                java.util.function.Function<T, Long> tenant) {
        LinkedHashMap<String, T> resolved = new LinkedHashMap<>();
        rows.forEach(row -> {
            String normalized = key.apply(row).toLowerCase(Locale.ROOT);
            if (tenant.apply(row) == null) resolved.putIfAbsent(normalized, row);
            else resolved.put(normalized, row);
        });
        return List.copyOf(resolved.values());
    }

    private Long tenantId() {
        return tenantContext.requireTenantId();
    }

    private ApiException conflict(String kind, String value) {
        return new ApiException(ErrorCode.RESOURCE_CONFLICT,
                "A customer %s named '%s' already exists".formatted(kind, value));
    }

    private void guardCode(boolean system, String current, String requested) {
        if (system && !current.equals(requested)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System entries keep their code");
        }
    }

    private void guardSystemDelete(boolean system) {
        if (system) throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED, "System entries cannot be deleted");
    }

    private void guardInUse(long count) {
        if (count > 0) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Cannot delete an entry that is still assigned to customers");
        }
    }
}
