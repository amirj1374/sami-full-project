package com.sami.app.metadata.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.metadata.domain.MetaField;
import com.sami.app.metadata.domain.MetaFieldValue;
import com.sami.app.metadata.event.MetadataDomainEvent;
import com.sami.app.metadata.repository.MetaFieldRepository;
import com.sami.app.metadata.repository.MetaFieldValueRepository;
import com.sami.app.metadata.spi.FieldTypeHandler;
import com.sami.app.metadata.spi.FieldTypeRegistry;
import com.sami.app.metadata.spi.FieldValue;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes custom-field values for a business record. Values are coerced
 * by the field type's handler and stored in typed columns, which is what keeps
 * custom fields searchable, sortable and reportable.
 *
 * <p>The owning module still owns its own record; this service only stores the
 * extension values keyed by (module, entity, recordId).
 */
@Service
@RequiredArgsConstructor
public class CustomFieldValueService {

    private final MetaFieldRepository fieldRepository;
    private final MetaFieldValueRepository valueRepository;
    private final FieldTypeRegistry handlerRegistry;
    private final ApplicationEventPublisher eventPublisher;

    /** Current values for a record, keyed by field code. */
    @Transactional(readOnly = true)
    public Map<String, Object> getValues(String moduleCode, String entityCode, Long recordId) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (MetaFieldValue value : valueRepository
                .findByModuleCodeAndEntityCodeAndRecordId(moduleCode, entityCode, recordId)) {
            out.put(value.getField().getCode(), extract(value));
        }
        return out;
    }

    /**
     * Validates and stores values. Unknown field codes are rejected, required
     * fields enforced, and each value coerced by its type handler.
     */
    @Transactional
    public Map<String, Object> setValues(String moduleCode, String entityCode, Long recordId,
                                         Map<String, Object> submitted) {
        List<MetaField> fields = fieldRepository.findActiveFor(moduleCode, entityCode);
        Map<String, MetaField> byCode = new HashMap<>();
        fields.forEach(f -> byCode.put(f.getCode(), f));

        Map<String, Object> input = submitted == null ? Map.of() : submitted;
        List<String> unknown = input.keySet().stream().filter(k -> !byCode.containsKey(k)).toList();
        if (!unknown.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown custom fields: " + unknown);
        }

        List<String> errors = new ArrayList<>();
        Map<MetaField, FieldValue> coerced = new LinkedHashMap<>();
        for (MetaField field : fields) {
            Object raw = input.containsKey(field.getCode()) ? input.get(field.getCode()) : null;
            if (raw == null && field.getDefaultValue() != null && !input.containsKey(field.getCode())) {
                raw = field.getDefaultValue();
            }
            FieldTypeHandler handler = handlerRegistry.find(field.getFieldType().getHandlerKey()).orElse(null);
            if (handler == null) {
                errors.add(field.getCode() + ": no handler for type " + field.getFieldType().getCode());
                continue;
            }
            FieldValue value;
            try {
                value = handler.coerce(raw, constraintsOf(field));
            } catch (IllegalArgumentException ex) {
                errors.add(field.getLabel() + " " + ex.getMessage());
                continue;
            }
            if (field.isRequired() && value.isEmpty()) {
                errors.add(field.getLabel() + " is required");
                continue;
            }
            if (input.containsKey(field.getCode()) || !value.isEmpty()) {
                coerced.put(field, value);
            }
        }
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, String.join("; ", errors));
        }

        for (Map.Entry<MetaField, FieldValue> entry : coerced.entrySet()) {
            persist(moduleCode, entityCode, recordId, entry.getKey(), entry.getValue());
        }

        eventPublisher.publishEvent(new MetadataDomainEvent(
                "meta-values-" + moduleCode + "-" + entityCode + "-" + recordId,
                MetadataDomainEvent.VALUES_UPDATED, moduleCode, entityCode, recordId,
                Map.of("fields", coerced.keySet().stream().map(MetaField::getCode).toList()),
                Instant.now()));
        return getValues(moduleCode, entityCode, recordId);
    }

    /** Search record ids by a custom text field (powers filtering on extensions). */
    @Transactional(readOnly = true)
    public List<Long> searchByText(String moduleCode, String entityCode, String fieldCode, String value) {
        return fieldRepository.findByTarget(moduleCode, entityCode, fieldCode)
                .map(f -> valueRepository.searchByText(f.getId(), value))
                .orElse(List.of());
    }

    /** Filter record ids by a numeric custom field range. */
    @Transactional(readOnly = true)
    public List<Long> searchByNumber(String moduleCode, String entityCode, String fieldCode,
                                     BigDecimal min, BigDecimal max) {
        return fieldRepository.findByTarget(moduleCode, entityCode, fieldCode)
                .map(f -> valueRepository.searchByNumberRange(f.getId(), min, max))
                .orElse(List.of());
    }

    private void persist(String moduleCode, String entityCode, Long recordId,
                         MetaField field, FieldValue value) {
        MetaFieldValue row = valueRepository.findByFieldIdAndRecordId(field.getId(), recordId)
                .orElseGet(() -> MetaFieldValue.builder()
                        .field(field)
                        .moduleCode(moduleCode)
                        .entityCode(entityCode)
                        .recordId(recordId)
                        .build());
        row.setValueText(value.text());
        row.setValueNumber(value.number());
        row.setValueBoolean(value.bool());
        row.setValueDate(value.date());
        row.setValueJson(value.json());
        valueRepository.save(row);
    }

    private Object extract(MetaFieldValue value) {
        if (value.getValueText() != null) {
            return value.getValueText();
        }
        if (value.getValueNumber() != null) {
            return value.getValueNumber();
        }
        if (value.getValueBoolean() != null) {
            return value.getValueBoolean();
        }
        if (value.getValueDate() != null) {
            return value.getValueDate();
        }
        return value.getValueJson();
    }

    private Map<String, Object> constraintsOf(MetaField field) {
        Map<String, Object> c = new HashMap<>();
        c.put("minLength", field.getMinLength());
        c.put("maxLength", field.getMaxLength());
        c.put("minValue", field.getMinValue());
        c.put("maxValue", field.getMaxValue());
        c.put("pattern", field.getPattern());
        c.put("options", field.getOptions());
        return c;
    }
}
