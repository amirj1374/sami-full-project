package com.sami.app.dataquality.spi;

import java.util.Map;

/**
 * Extension point for validation logic. A quality rule row names a
 * {@link #type()}; the engine resolves it here and never needs to change when
 * new validation kinds (business rules, cross-module checks, AI similarity …)
 * are added — they are simply new beans.
 */
public interface ValidationRule {

    /** Unique key matching {@code quality_rules.validation_type}. */
    String type();

    /** Human-readable label for configuration UIs. */
    String label();

    /** The quality dimension this validator naturally serves (advisory). */
    default String defaultDimension() {
        return "validity";
    }

    ValidationOutcome validate(ValidationContext context);

    /**
     * Optional cheap check of a rule's config before it is saved.
     *
     * @return null when valid, otherwise a human-readable error
     */
    default String validateConfig(Map<String, Object> config) {
        return null;
    }
}
