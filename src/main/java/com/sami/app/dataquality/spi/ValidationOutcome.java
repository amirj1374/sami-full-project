package com.sami.app.dataquality.spi;

import java.util.Map;

/**
 * Result of one rule evaluation. {@code skipped} marks a rule that did not
 * apply (e.g. an optional field was absent) — skipped rules never count as
 * failures nor penalise the score.
 */
public record ValidationOutcome(boolean valid, boolean skipped, String message, Map<String, Object> detail) {

    public static ValidationOutcome ok() {
        return new ValidationOutcome(true, false, null, Map.of());
    }

    public static ValidationOutcome skip() {
        return new ValidationOutcome(true, true, null, Map.of());
    }

    public static ValidationOutcome fail(String message) {
        return new ValidationOutcome(false, false, message, Map.of());
    }

    public static ValidationOutcome fail(String message, Map<String, Object> detail) {
        return new ValidationOutcome(false, false, message, detail == null ? Map.of() : detail);
    }
}
