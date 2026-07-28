package com.sami.app.metadata.dto;

import com.sami.app.metadata.domain.MetaEntity;
import com.sami.app.metadata.domain.MetaField;
import com.sami.app.metadata.domain.MetaFieldType;
import com.sami.app.metadata.domain.MetaForm;
import com.sami.app.metadata.domain.MetaFormLayout;
import com.sami.app.metadata.domain.MetaFormVersion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Request/response records for the metadata module. */
public final class MetadataDtos {

    private MetadataDtos() {
    }

    // ---- Requests -----------------------------------------------------------

    public record FieldRequest(
            @NotBlank String moduleCode,
            @NotBlank String entityCode,
            @NotBlank @Pattern(regexp = "^[a-z][a-zA-Z0-9_]{1,63}$",
                    message = "code must start with a lowercase letter") String code,
            @NotBlank String label,
            String helpText,
            @NotBlank String fieldType,
            boolean required,
            String defaultValue,
            BigDecimal minValue,
            BigDecimal maxValue,
            Integer minLength,
            Integer maxLength,
            String pattern,
            List<Object> options,
            Boolean searchable,
            Boolean sortable,
            Boolean reportable,
            Boolean localized,
            String viewPermission,
            String editPermission,
            String qualityRuleCode,
            Integer displayOrder
    ) {
    }

    public record ValuesRequest(
            @NotBlank String moduleCode,
            @NotBlank String entityCode,
            Long recordId,
            Map<String, Object> values
    ) {
    }

    public record FormRequest(@NotBlank String moduleCode, @NotBlank String entityCode,
                              @NotBlank String code, @NotBlank String name, String description) {
    }

    public record DraftRequest(Map<String, Object> schema, String changeNote) {
    }

    public record LayoutRequest(@NotBlank String targetType, String targetValue,
                                Map<String, Object> layout, Integer priority) {
    }

    public record BindRequest(@NotBlank String moduleCode, @NotBlank String entityCode,
                              Long recordId, Long formVersionId) {
    }

    // ---- Responses ----------------------------------------------------------

    public record EntityResponse(Long id, String moduleCode, String entityCode, String label,
                                 String description, boolean active) {
        public static EntityResponse from(MetaEntity e) {
            return new EntityResponse(e.getId(), e.getModuleCode(), e.getEntityCode(), e.getLabel(),
                    e.getDescription(), e.isActive());
        }
    }

    public record FieldTypeResponse(Long id, String code, String name, String handlerKey,
                                    String storageKind, boolean supportsOptions) {
        public static FieldTypeResponse from(MetaFieldType t) {
            return new FieldTypeResponse(t.getId(), t.getCode(), t.getName(), t.getHandlerKey(),
                    t.getStorageKind(), t.isSupportsOptions());
        }
    }

    public record FieldResponse(Long id, String moduleCode, String entityCode, String code, String label,
                                String helpText, String fieldType, String storageKind, boolean required,
                                String defaultValue, List<Object> options, boolean searchable,
                                boolean sortable, boolean reportable, boolean localized,
                                String viewPermission, String editPermission, String qualityRuleCode,
                                int displayOrder, boolean active, Instant createdAt, long version) {
        public static FieldResponse from(MetaField f) {
            return new FieldResponse(f.getId(), f.getEntity().getModuleCode(), f.getEntity().getEntityCode(),
                    f.getCode(), f.getLabel(), f.getHelpText(), f.getFieldType().getCode(),
                    f.getFieldType().getStorageKind(), f.isRequired(), f.getDefaultValue(), f.getOptions(),
                    f.isSearchable(), f.isSortable(), f.isReportable(), f.isLocalized(),
                    f.getViewPermission(), f.getEditPermission(), f.getQualityRuleCode(),
                    f.getDisplayOrder(), f.isActive(), f.getCreatedAt(), f.getVersion());
        }
    }

    public record FormResponse(Long id, String code, String name, String description,
                               String moduleCode, String entityCode, boolean active) {
        public static FormResponse from(MetaForm f) {
            return new FormResponse(f.getId(), f.getCode(), f.getName(), f.getDescription(),
                    f.getEntity().getModuleCode(), f.getEntity().getEntityCode(), f.isActive());
        }
    }

    public record FormVersionResponse(Long id, Long formId, String formCode, int versionNo, String status,
                                      Map<String, Object> schema, String changeNote, Instant publishedAt) {
        public static FormVersionResponse from(MetaFormVersion v) {
            return new FormVersionResponse(v.getId(), v.getForm().getId(), v.getForm().getCode(),
                    v.getVersionNo(), v.getStatus(), v.getSchemaJson(), v.getChangeNote(), v.getPublishedAt());
        }
    }

    public record LayoutResponse(Long id, Long formVersionId, String targetType, String targetValue,
                                 Map<String, Object> layout, int priority) {
        public static LayoutResponse from(MetaFormLayout l) {
            return new LayoutResponse(l.getId(), l.getFormVersionId(), l.getTargetType(),
                    l.getTargetValue(), l.getLayoutJson(), l.getPriority());
        }
    }
}
