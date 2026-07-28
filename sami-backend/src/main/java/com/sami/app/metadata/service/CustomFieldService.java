package com.sami.app.metadata.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.metadata.domain.MetaEntity;
import com.sami.app.metadata.domain.MetaField;
import com.sami.app.metadata.domain.MetaFieldType;
import com.sami.app.metadata.event.MetadataDomainEvent;
import com.sami.app.metadata.repository.MetaEntityRepository;
import com.sami.app.metadata.repository.MetaFieldRepository;
import com.sami.app.metadata.repository.MetaFieldTypeRepository;
import com.sami.app.metadata.spi.FieldTypeHandler;
import com.sami.app.metadata.spi.FieldTypeRegistry;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom field definitions. A field can only be created against a registered
 * extensible entity and a known field type, and the type's handler validates the
 * definition — so a broken field can never reach runtime.
 */
@Service
@RequiredArgsConstructor
public class CustomFieldService {

    private final MetaFieldRepository fieldRepository;
    private final MetaEntityRepository entityRepository;
    private final MetaFieldTypeRepository fieldTypeRepository;
    private final FieldTypeRegistry handlerRegistry;
    private final MetaAuditService audit;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<MetaField> list() {
        return fieldRepository.findAllBy();
    }

    @Transactional(readOnly = true)
    public List<MetaField> forEntity(String moduleCode, String entityCode) {
        return fieldRepository.findActiveFor(moduleCode, entityCode);
    }

    @Transactional(readOnly = true)
    public MetaField get(Long id) {
        return fieldRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Custom field not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<MetaEntity> entities() {
        return entityRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<MetaFieldType> fieldTypes() {
        return fieldTypeRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional
    public MetaField create(String moduleCode, String entityCode, String code, String label, String helpText,
                            String fieldTypeCode, boolean required, String defaultValue,
                            BigDecimal minValue, BigDecimal maxValue, Integer minLength, Integer maxLength,
                            String pattern, List<Object> options, Boolean searchable, Boolean sortable,
                            Boolean reportable, Boolean localized, String viewPermission,
                            String editPermission, String qualityRuleCode, Integer displayOrder) {
        MetaEntity entity = entityRepository.findByModuleCodeAndEntityCode(moduleCode, entityCode)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST,
                        "Entity is not registered as extensible: " + moduleCode + "." + entityCode));
        if (fieldRepository.existsByEntityIdAndCode(entity.getId(), code)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Field code already exists: " + code);
        }
        MetaFieldType type = fieldTypeRepository.findByCode(fieldTypeCode)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "Unknown field type: " + fieldTypeCode));
        FieldTypeHandler handler = handlerRegistry.find(type.getHandlerKey())
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST,
                        "No handler registered for field type " + fieldTypeCode));

        List<Object> safeOptions = options == null ? new ArrayList<>() : new ArrayList<>(options);
        String definitionError = handler.validateDefinition(
                Map.of("minLength", String.valueOf(minLength), "maxLength", String.valueOf(maxLength)),
                safeOptions);
        if (definitionError != null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, definitionError);
        }

        MetaField field = MetaField.builder()
                .entity(entity)
                .code(code)
                .label(label)
                .helpText(helpText)
                .fieldType(type)
                .required(required)
                .defaultValue(defaultValue)
                .minValue(minValue)
                .maxValue(maxValue)
                .minLength(minLength)
                .maxLength(maxLength)
                .pattern(pattern)
                .options(safeOptions)
                .searchable(searchable == null || searchable)
                .sortable(Boolean.TRUE.equals(sortable))
                .reportable(reportable == null || reportable)
                .localized(Boolean.TRUE.equals(localized))
                .viewPermission(viewPermission)
                .editPermission(editPermission)
                .qualityRuleCode(qualityRuleCode)
                .displayOrder(displayOrder == null ? 100 : displayOrder)
                .createdBy(CurrentActor.id())
                .createdByEmail(CurrentActor.email())
                .build();
        MetaField saved = fieldRepository.save(field);

        audit.record("FIELD", saved.getId(), "CREATED", null,
                Map.of("code", code, "type", fieldTypeCode, "entity", moduleCode + "." + entityCode));
        eventPublisher.publishEvent(new MetadataDomainEvent(
                "meta-field-" + saved.getId(), MetadataDomainEvent.FIELD_CREATED,
                moduleCode, entityCode, saved.getId(), Map.of("code", code), Instant.now()));
        return saved;
    }

    @Transactional
    public MetaField setActive(Long id, boolean active) {
        MetaField field = get(id);
        field.setActive(active);
        MetaField saved = fieldRepository.save(field);
        audit.record("FIELD", id, active ? "ACTIVATED" : "DISABLED", null, Map.of("active", active));
        return saved;
    }

    /** Deleting a definition cascades its stored values (FK ON DELETE CASCADE). */
    @Transactional
    public void delete(Long id) {
        MetaField field = get(id);
        audit.record("FIELD", id, "DELETED", Map.of("code", field.getCode()), null);
        eventPublisher.publishEvent(new MetadataDomainEvent(
                "meta-field-" + id, MetadataDomainEvent.FIELD_DELETED,
                field.getEntity().getModuleCode(), field.getEntity().getEntityCode(), id,
                Map.of("code", field.getCode()), Instant.now()));
        fieldRepository.delete(field);
    }
}
