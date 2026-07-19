package com.sami.app.dataquality.spi;

import java.util.Map;

/**
 * Extension point for duplicate-matching strategies (exact, fuzzy, phonetic,
 * composite key, and later AI similarity). Returns a similarity score so the
 * engine can apply a configurable threshold uniformly.
 */
public interface DuplicateMatcher {

    /** Unique strategy key referenced by a rule's {@code config.strategy}. */
    String strategy();

    String label();

    /**
     * Similarity between two candidate values in the range 0.0–1.0.
     *
     * @param config the rule config (fields, threshold, options)
     */
    double similarity(String left, String right, Map<String, Object> config);
}
