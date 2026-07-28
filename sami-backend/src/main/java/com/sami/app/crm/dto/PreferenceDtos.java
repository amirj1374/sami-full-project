package com.sami.app.crm.dto;

import com.sami.app.common.fields.DynamicFieldSpec;
import com.sami.app.crm.domain.PreferenceDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** Request/response payloads for configurable customer preferences. */
public final class PreferenceDtos {

    private PreferenceDtos() {
    }

    public record PreferenceDefinitionResponse(
            Long id, String prefKey, String label, DynamicFieldSpec.Type fieldType,
            boolean required, Integer minLength, Integer maxLength, String pattern,
            List<String> options, boolean active, int displayOrder
    ) {
        public static PreferenceDefinitionResponse from(PreferenceDefinition d) {
            return new PreferenceDefinitionResponse(d.getId(), d.getPrefKey(), d.getLabel(),
                    d.getFieldType(), d.isRequired(), d.getMinLength(), d.getMaxLength(),
                    d.getPattern(), d.getOptions(), d.isActive(), d.getDisplayOrder());
        }
    }

    public record PreferenceDefinitionRequest(
            @NotBlank
            @Pattern(regexp = "^[a-z][a-zA-Z0-9_]{1,63}$",
                    message = "Key must start with a lowercase letter (letters, digits, underscores)")
            String prefKey,

            @NotBlank @Size(max = 100) String label,

            @NotNull DynamicFieldSpec.Type fieldType,

            boolean required,
            Integer minLength,
            Integer maxLength,
            @Size(max = 255) String pattern,
            List<String> options,
            boolean active,
            int displayOrder
    ) {
    }

    /** Full replacement of a customer's preference values. */
    public record PreferencesRequest(Map<String, Object> preferences) {
    }
}
