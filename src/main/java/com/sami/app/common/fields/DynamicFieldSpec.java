package com.sami.app.common.fields;

import java.util.List;

/**
 * Provider-agnostic description of one runtime-configurable field. Both user
 * profile custom fields and CRM preference fields validate through this shape,
 * keeping dynamic-field validation rules centralized in one component.
 */
public record DynamicFieldSpec(
        String key,
        String label,
        Type type,
        boolean required,
        Integer minLength,
        Integer maxLength,
        String pattern,
        List<String> options
) {
    public enum Type { TEXT, NUMBER, DATE, BOOLEAN, SELECT }
}
