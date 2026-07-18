package com.sami.app.user.dto;

import com.sami.app.user.domain.ProfileFieldDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Payload to create or update a custom profile field definition. */
public record ProfileFieldDefinitionRequest(

        @NotBlank(message = "Field key is required")
        @Pattern(regexp = "^[a-z][a-zA-Z0-9_]{1,63}$",
                message = "Field key must start with a lowercase letter (letters, digits, underscores)")
        String fieldKey,

        @NotBlank(message = "Label is required")
        @Size(max = 100)
        String label,

        @NotNull(message = "Field type is required")
        ProfileFieldDefinition.FieldType fieldType,

        boolean required,

        Integer minLength,

        Integer maxLength,

        @Size(max = 255)
        String pattern,

        List<String> options,

        boolean active,

        int displayOrder
) {
}
