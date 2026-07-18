package com.sami.app.user.dto;

import com.sami.app.user.domain.ProfileFieldDefinition;

import java.util.List;

/** Public representation of a {@link ProfileFieldDefinition}. */
public record ProfileFieldDefinitionResponse(
        Long id,
        String fieldKey,
        String label,
        ProfileFieldDefinition.FieldType fieldType,
        boolean required,
        Integer minLength,
        Integer maxLength,
        String pattern,
        List<String> options,
        boolean active,
        int displayOrder
) {

    public static ProfileFieldDefinitionResponse from(ProfileFieldDefinition def) {
        return new ProfileFieldDefinitionResponse(
                def.getId(),
                def.getFieldKey(),
                def.getLabel(),
                def.getFieldType(),
                def.isRequired(),
                def.getMinLength(),
                def.getMaxLength(),
                def.getPattern(),
                def.getOptions(),
                def.isActive(),
                def.getDisplayOrder()
        );
    }
}
