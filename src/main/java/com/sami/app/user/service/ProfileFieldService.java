package com.sami.app.user.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.user.domain.ProfileFieldDefinition;
import com.sami.app.user.dto.ProfileFieldDefinitionRequest;
import com.sami.app.user.dto.ProfileFieldDefinitionResponse;
import com.sami.app.user.repository.ProfileFieldDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Custom profile fields: definition CRUD plus generic validation of submitted
 * values against the active definitions.
 *
 * <p>This is what makes profiles extendable without schema changes: an admin
 * defines a field (key, type, rules) at runtime and it is immediately validated
 * and stored for every user.
 */
@Service
@RequiredArgsConstructor
public class ProfileFieldService {

    private final ProfileFieldDefinitionRepository definitionRepository;

    @Transactional(readOnly = true)
    public List<ProfileFieldDefinitionResponse> listAll() {
        return definitionRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(ProfileFieldDefinitionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProfileFieldDefinitionResponse> listActive() {
        return definitionRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(ProfileFieldDefinitionResponse::from)
                .toList();
    }

    @Transactional
    public ProfileFieldDefinitionResponse create(ProfileFieldDefinitionRequest request) {
        if (definitionRepository.existsByFieldKeyIgnoreCase(request.fieldKey())) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT,
                    "A field with key '%s' already exists".formatted(request.fieldKey()));
        }
        requireValidPattern(request.pattern());

        ProfileFieldDefinition def = ProfileFieldDefinition.builder()
                .fieldKey(request.fieldKey())
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
        return ProfileFieldDefinitionResponse.from(definitionRepository.save(def));
    }

    /** Field key and type are immutable once created (stored data depends on them). */
    @Transactional
    public ProfileFieldDefinitionResponse update(Long id, ProfileFieldDefinitionRequest request) {
        ProfileFieldDefinition def = findOrThrow(id);
        requireValidPattern(request.pattern());

        def.setLabel(request.label());
        def.setRequired(request.required());
        def.setMinLength(request.minLength());
        def.setMaxLength(request.maxLength());
        def.setPattern(request.pattern());
        def.setOptions(request.options());
        def.setActive(request.active());
        def.setDisplayOrder(request.displayOrder());
        return ProfileFieldDefinitionResponse.from(def);
    }

    @Transactional
    public void delete(Long id) {
        definitionRepository.delete(findOrThrow(id));
        // Stored values under the removed key remain in profiles' JSONB; they are
        // inert (no definition -> not rendered, not validated) and preserved for
        // history, mirroring the module's never-lose-data rule.
    }

    /**
     * Validates submitted custom-field values against the ACTIVE definitions and
     * returns the sanitized map (unknown keys are rejected, so stale clients
     * cannot smuggle arbitrary data into profiles).
     *
     * @throws ApiException VALIDATION_FAILED with a specific message on the first violation
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateCustomFields(Map<String, Object> submitted) {
        Map<String, Object> values = submitted == null ? Map.of() : submitted;
        List<ProfileFieldDefinition> definitions = definitionRepository.findByActiveTrueOrderByDisplayOrderAsc();
        Map<String, ProfileFieldDefinition> byKey = new HashMap<>();
        definitions.forEach(d -> byKey.put(d.getFieldKey(), d));

        for (String key : values.keySet()) {
            if (!byKey.containsKey(key)) {
                throw validationError("Unknown profile field '%s'".formatted(key));
            }
        }

        Map<String, Object> sanitized = new HashMap<>();
        for (ProfileFieldDefinition def : definitions) {
            Object value = values.get(def.getFieldKey());
            if (value == null || (value instanceof String s && s.isBlank())) {
                if (def.isRequired()) {
                    throw validationError("Field '%s' is required".formatted(def.getLabel()));
                }
                continue;
            }
            sanitized.put(def.getFieldKey(), validateValue(def, value));
        }
        return sanitized;
    }

    private Object validateValue(ProfileFieldDefinition def, Object value) {
        return switch (def.getFieldType()) {
            case TEXT -> validateText(def, asString(def, value));
            case NUMBER -> {
                if (value instanceof Number n) {
                    yield n;
                }
                try {
                    yield Double.parseDouble(asString(def, value));
                } catch (NumberFormatException e) {
                    throw validationError("Field '%s' must be a number".formatted(def.getLabel()));
                }
            }
            case DATE -> {
                try {
                    yield LocalDate.parse(asString(def, value)).toString();
                } catch (DateTimeParseException e) {
                    throw validationError("Field '%s' must be an ISO date (yyyy-MM-dd)".formatted(def.getLabel()));
                }
            }
            case BOOLEAN -> {
                if (value instanceof Boolean b) {
                    yield b;
                }
                String s = asString(def, value);
                if ("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
                    yield Boolean.parseBoolean(s);
                }
                throw validationError("Field '%s' must be true or false".formatted(def.getLabel()));
            }
            case SELECT -> {
                String s = asString(def, value);
                if (def.getOptions() == null || !def.getOptions().contains(s)) {
                    throw validationError("Field '%s' must be one of: %s"
                            .formatted(def.getLabel(), def.getOptions()));
                }
                yield s;
            }
        };
    }

    private String validateText(ProfileFieldDefinition def, String s) {
        if (def.getMinLength() != null && s.length() < def.getMinLength()) {
            throw validationError("Field '%s' must be at least %d characters"
                    .formatted(def.getLabel(), def.getMinLength()));
        }
        if (def.getMaxLength() != null && s.length() > def.getMaxLength()) {
            throw validationError("Field '%s' must be at most %d characters"
                    .formatted(def.getLabel(), def.getMaxLength()));
        }
        if (def.getPattern() != null && !def.getPattern().isBlank()
                && !Pattern.matches(def.getPattern(), s)) {
            throw validationError("Field '%s' has an invalid format".formatted(def.getLabel()));
        }
        return s;
    }

    private String asString(ProfileFieldDefinition def, Object value) {
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        throw validationError("Field '%s' has an unsupported value".formatted(def.getLabel()));
    }

    private void requireValidPattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return;
        }
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw validationError("Invalid regular expression: " + e.getDescription());
        }
    }

    private ApiException validationError(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, message);
    }

    private ProfileFieldDefinition findOrThrow(Long id) {
        return definitionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Profile field", id));
    }
}
