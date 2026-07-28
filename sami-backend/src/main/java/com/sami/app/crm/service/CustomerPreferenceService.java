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

    @Transactional(readOnly = true)
    public List<PreferenceDefinitionResponse> listDefinitions(boolean activeOnly) {
        var definitions = activeOnly
                ? definitionRepository.findByActiveTrueOrderByDisplayOrderAsc()
                : definitionRepository.findAllByOrderByDisplayOrderAsc();
        return definitions.stream().map(PreferenceDefinitionResponse::from).toList();
    }

    @Transactional
    public PreferenceDefinitionResponse createDefinition(PreferenceDefinitionRequest request) {
        if (definitionRepository.existsByPrefKeyIgnoreCase(request.prefKey())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A preference with key '%s' already exists".formatted(request.prefKey()));
        }
        PreferenceDefinition def = PreferenceDefinition.builder()
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
        PreferenceDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Preference definition", id));
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
        PreferenceDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Preference definition", id));
        definitionRepository.delete(def);
    }

    /** Validates and fully replaces a customer's preference values. */
    @Transactional
    public Map<String, Object> updatePreferences(Long customerId, Map<String, Object> submitted) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", customerId));
        Map<String, Object> sanitized = validator.validate(submitted,
                definitionRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                        .map(PreferenceDefinition::toSpec)
                        .toList());
        customer.setPreferences(sanitized);
        events.record(customerId, CustomerEventService.PREFERENCES_UPDATED,
                "Preferences updated", null);
        return sanitized;
    }
}
