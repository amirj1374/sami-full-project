package com.sami.app.crm.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CRUD for the CRM's configurable lookup data: types, statuses, sources, tags
 * and duplicate rules. Uniform guards: system rows keep their code and cannot
 * be deleted; rows still referenced by customers cannot be deleted.
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

    // ----------------------------------------------------------------- types

    @Transactional(readOnly = true)
    public List<CustomerTypeResponse> listTypes() {
        return typeRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(CustomerTypeResponse::from).toList();
    }

    @Transactional
    public CustomerTypeResponse createType(TypeRequest r) {
        if (typeRepository.existsByCodeIgnoreCase(r.code())) {
            throw conflict("type", r.code());
        }
        return CustomerTypeResponse.from(typeRepository.save(CustomerType.builder()
                .code(r.code()).name(r.name()).description(r.description())
                .active(r.active()).displayOrder(r.displayOrder()).build()));
    }

    @Transactional
    public CustomerTypeResponse updateType(Long id, TypeRequest r) {
        CustomerType type = typeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer type", id));
        guardCode(type.isSystem(), type.getCode(), r.code());
        if (typeRepository.existsByCodeIgnoreCaseAndIdNot(r.code(), id)) {
            throw conflict("type", r.code());
        }
        type.setCode(r.code());
        type.setName(r.name());
        type.setDescription(r.description());
        type.setActive(r.active());
        type.setDisplayOrder(r.displayOrder());
        return CustomerTypeResponse.from(type);
    }

    @Transactional
    public void deleteType(Long id) {
        CustomerType type = typeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer type", id));
        guardSystemDelete(type.isSystem());
        guardInUse(customerRepository.countByTypeId(id));
        typeRepository.delete(type);
    }

    // -------------------------------------------------------------- statuses

    @Transactional(readOnly = true)
    public List<CustomerStatusResponse> listStatuses() {
        return statusRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(CustomerStatusResponse::from).toList();
    }

    @Transactional
    public CustomerStatusResponse createStatus(StatusRequest r) {
        if (statusRepository.existsByCodeIgnoreCase(r.code())) {
            throw conflict("status", r.code());
        }
        return CustomerStatusResponse.from(statusRepository.save(CustomerStatus.builder()
                .code(r.code()).name(r.name()).description(r.description())
                .isBlocking(r.isBlocking()).hiddenByDefault(r.hiddenByDefault())
                .displayOrder(r.displayOrder()).build()));
    }

    @Transactional
    public CustomerStatusResponse updateStatus(Long id, StatusRequest r) {
        CustomerStatus status = statusRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer status", id));
        guardCode(status.isSystem(), status.getCode(), r.code());
        if (statusRepository.existsByCodeIgnoreCaseAndIdNot(r.code(), id)) {
            throw conflict("status", r.code());
        }
        status.setCode(r.code());
        status.setName(r.name());
        status.setDescription(r.description());
        status.setBlocking(r.isBlocking());
        status.setHiddenByDefault(r.hiddenByDefault());
        status.setDisplayOrder(r.displayOrder());
        return CustomerStatusResponse.from(status);
    }

    @Transactional
    public void deleteStatus(Long id) {
        CustomerStatus status = statusRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer status", id));
        guardSystemDelete(status.isSystem());
        guardInUse(customerRepository.countByStatusId(id));
        statusRepository.delete(status);
    }

    // --------------------------------------------------------------- sources

    @Transactional(readOnly = true)
    public List<CustomerSourceResponse> listSources() {
        return sourceRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(CustomerSourceResponse::from).toList();
    }

    @Transactional
    public CustomerSourceResponse createSource(SourceRequest r) {
        if (sourceRepository.existsByCodeIgnoreCase(r.code())) {
            throw conflict("source", r.code());
        }
        return CustomerSourceResponse.from(sourceRepository.save(CustomerSource.builder()
                .code(r.code()).name(r.name()).active(r.active())
                .displayOrder(r.displayOrder()).build()));
    }

    @Transactional
    public CustomerSourceResponse updateSource(Long id, SourceRequest r) {
        CustomerSource source = sourceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer source", id));
        guardCode(source.isSystem(), source.getCode(), r.code());
        if (sourceRepository.existsByCodeIgnoreCaseAndIdNot(r.code(), id)) {
            throw conflict("source", r.code());
        }
        source.setCode(r.code());
        source.setName(r.name());
        source.setActive(r.active());
        source.setDisplayOrder(r.displayOrder());
        return CustomerSourceResponse.from(source);
    }

    @Transactional
    public void deleteSource(Long id) {
        CustomerSource source = sourceRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer source", id));
        guardSystemDelete(source.isSystem());
        guardInUse(customerRepository.countBySourceId(id));
        sourceRepository.delete(source);
    }

    // ------------------------------------------------------------------ tags

    @Transactional(readOnly = true)
    public List<CustomerTagResponse> listTags() {
        return tagRepository.findAllByOrderByNameAsc().stream()
                .map(CustomerTagResponse::from).toList();
    }

    @Transactional
    public CustomerTagResponse createTag(TagRequest r) {
        if (tagRepository.existsByNameIgnoreCase(r.name())) {
            throw conflict("tag", r.name());
        }
        return CustomerTagResponse.from(tagRepository.save(CustomerTag.builder()
                .name(r.name()).color(r.color()).description(r.description())
                .active(r.active()).build()));
    }

    @Transactional
    public CustomerTagResponse updateTag(Long id, TagRequest r) {
        CustomerTag tag = tagRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tag", id));
        if (tagRepository.existsByNameIgnoreCaseAndIdNot(r.name(), id)) {
            throw conflict("tag", r.name());
        }
        tag.setName(r.name());
        tag.setColor(r.color());
        tag.setDescription(r.description());
        tag.setActive(r.active());
        return CustomerTagResponse.from(tag);
    }

    /** Tags cascade off customers via the link table; deletion is always safe. */
    @Transactional
    public void deleteTag(Long id) {
        CustomerTag tag = tagRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tag", id));
        tagRepository.delete(tag);
    }

    // ------------------------------------------------------- duplicate rules

    @Transactional(readOnly = true)
    public Map<String, Boolean> listDuplicateRules() {
        return ruleRepository.findAll().stream().collect(Collectors.toMap(
                rule -> rule.getIdentifier().name(), DuplicateRule::isEnabled));
    }

    @Transactional
    public Map<String, Boolean> updateDuplicateRules(DuplicateRulesRequest request) {
        Map<DuplicateRule.Identifier, Boolean> toggles = request.rules().stream()
                .collect(Collectors.toMap(
                        DuplicateRulesRequest.RuleToggle::identifier,
                        DuplicateRulesRequest.RuleToggle::enabled,
                        (a, b) -> b));
        ruleRepository.findAll().forEach(rule -> {
            Boolean enabled = toggles.get(rule.getIdentifier());
            if (enabled != null) {
                rule.setEnabled(enabled);
            }
        });
        return listDuplicateRules();
    }

    // -------------------------------------------------------------- internals

    private ApiException conflict(String kind, String code) {
        return new ApiException(ErrorCode.RESOURCE_CONFLICT,
                "A customer %s named '%s' already exists".formatted(kind, code));
    }

    private void guardCode(boolean isSystem, String currentCode, String newCode) {
        if (isSystem && !currentCode.equals(newCode)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "System entries keep their code");
        }
    }

    private void guardSystemDelete(boolean isSystem) {
        if (isSystem) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "System entries cannot be deleted");
        }
    }

    private void guardInUse(long count) {
        if (count > 0) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Cannot delete an entry that is still assigned to customers");
        }
    }
}
