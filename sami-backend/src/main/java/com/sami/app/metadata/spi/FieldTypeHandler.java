package com.sami.app.metadata.spi;

import java.util.List;
import java.util.Map;

/**
 * Extension point for custom-field types. A handler coerces a submitted value
 * into the typed storage slot and enforces the type's own constraints. Adding a
 * field type (colour picker, geo point, signature, AI-classified text …) means
 * one bean plus a {@code meta_field_types} row — the engine never changes.
 */
public interface FieldTypeHandler {

    /** Matches {@code meta_field_types.handler_key}. */
    String key();

    String label();

    /**
     * Coerces and validates a raw submitted value.
     *
     * @param raw         the submitted value (may be null)
     * @param constraints field constraints: required, min/max, minLength/maxLength,
     *                    pattern, options
     * @return the normalized value
     * @throws IllegalArgumentException with a user-facing message when invalid
     */
    FieldValue coerce(Object raw, Map<String, Object> constraints);

    /** Options are only meaningful for choice types. */
    default boolean supportsOptions() {
        return false;
    }

    /** Optional check of a field definition before it is saved. */
    default String validateDefinition(Map<String, Object> constraints, List<Object> options) {
        return null;
    }
}
