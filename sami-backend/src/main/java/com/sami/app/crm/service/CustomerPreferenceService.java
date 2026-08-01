package com.sami.app.crm.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.fields.DynamicFieldValidator;
import com.sami.app.crm.domain.Customer;
import com.sami.app.crm.domain.PreferenceDefinition;
import com.sami.app.crm.dto.PreferenceDtos.PreferenceDefinitionRequest;
import com.sami.app.crm.dto.PreferenceDtos.PreferenceDefinitionResponse;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.crm.repository.PreferenceDefinitionRepository;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Configurable customer preferences: definition CRUD plus validated updates of
 * a customer's preference values. Definitions are pure data — a preference
 * added by an administrator is immediately collectible and validated, with no
 * code or schema change (values live in {@code customers.preferences} JSONB).
 */
@Service
@RequiredArgsConstructor
public class CustomerPreferenceService {

    private final PreferenceDefinitionRepository definitionRepository;
    private final CustomerRepository customerRepository;
    private final DynamicFieldValidator validator;
    private final CustomerEventService events;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<PreferenceDefinitionResponse> listDefinitions(boolean activeOnly) {
        var definitions = visibleDefinitions().stream()
                .filter(definition -> !activeOnly || definition.isActive()).toList();
        return definitions.stream().map(PreferenceDefinitionResponse::from).toList();
    }

    @Transactional
    public PreferenceDefinitionResponse createDefinition(PreferenceDefinitionRequest request) {
        Long tenantId = tenantContext.requireTenantId();
        if (definitionRepository.existsByTenantIdAndPrefKeyIgnoreCase(tenantId, request.prefKey())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A preference with key '%s' already exists".formatted(request.prefKey()));
        }
        PreferenceDefinition def = PreferenceDefinition.builder()
                .tenantId(tenantId)
                .prefKey(request.prefKey())
                .label(request.label())
                .fieldType(request.fieldType())
                .required(request.required())
                .minLength(request.minLength())
                .maxLength(request.maxLength())
                .pattern(request.pattern())
                .options(request.options())
                .active(request.active())
                .displayOrder(request.displayOrder())
                .build();
        return PreferenceDefinitionResponse.from(definitionRepository.save(def));
    }

    /** Key and type stay immutable (stored values depend on them). */
    @Transactional
    public PreferenceDefinitionResponse updateDefinition(Long id, PreferenceDefinitionRequest request) {
        PreferenceDefinition def = tenantDefinitionOrOverride(id);
        def.setLabel(request.label());
        def.setRequired(request.required());
        def.setMinLength(request.minLength());
        def.setMaxLength(request.maxLength());
        def.setPattern(request.pattern());
        def.setOptions(request.options());
        def.setActive(request.active());
        def.setDisplayOrder(request.displayOrder());
        return PreferenceDefinitionResponse.from(def);
    }

    /** Stored values under the removed key remain in JSONB, inert but preserved. */
    @Transactional
    public void deleteDefinition(Long id) {
        PreferenceDefinition def = editableDefinition(id);
        definitionRepository.delete(def);
    }

    /** Validates and fully replaces a customer's preference values. */
    @Transactional
    public Map<String, Object> updatePreferences(Long customerId, Map<String, Object> submitted) {
        Customer customer = customerRepository.findByIdAndTenantId(
                        customerId, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", customerId));
        Map<String, Object> sanitized = validator.validate(submitted,
                visibleDefinitions().stream().filter(PreferenceDefinition::isActive)
                        .map(PreferenceDefinition::toSpec)
                        .toList());
        customer.setPreferences(sanitized);
        events.record(customerId, CustomerEventService.PREFERENCES_UPDATED,
                "Preferences updated", null);
        return sanitized;
    }

    private List<PreferenceDefinition> visibleDefinitions() {
        java.util.LinkedHashMap<String, PreferenceDefinition> resolved = new java.util.LinkedHashMap<>();
        definitionRepository.findVisible(tenantContext.requireTenantId()).forEach(definition -> {
            String key = definition.getPrefKey().toLowerCase(java.util.Locale.ROOT);
            if (definition.getTenantId() == null) resolved.putIfAbsent(key, definition); else resolved.put(key, definition);
        });
        return List.copyOf(resolved.values());
    }

    private PreferenceDefinition editableDefinition(Long id) {
        return definitionRepository.findByIdAndTenantId(id, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Preference definition", id));
    }

    private PreferenceDefinition tenantDefinitionOrOverride(Long id) {
        return definitionRepository.findByIdAndTenantId(id, tenantContext.requireTenantId())
                .orElseGet(() -> {
                    PreferenceDefinition shared = definitionRepository.findById(id)
                            .filter(row -> row.getTenantId() == null)
                            .orElseThrow(() -> ResourceNotFoundException.of("Preference definition", id));
                    return definitionRepository.save(PreferenceDefinition.builder()
                            .tenantId(tenantContext.requireTenantId())
                            .prefKey(shared.getPrefKey()).label(shared.getLabel())
                            .fieldType(shared.getFieldType()).required(shared.isRequired())
                            .minLength(shared.getMinLength()).maxLength(shared.getMaxLength())
                            .pattern(shared.getPattern()).options(shared.getOptions())
                            .active(shared.isActive()).displayOrder(shared.getDisplayOrder()).build());
                });
    }
}
