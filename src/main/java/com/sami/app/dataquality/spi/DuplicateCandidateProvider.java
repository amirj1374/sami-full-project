package com.sami.app.dataquality.spi;

import java.util.List;
import java.util.Map;

/**
 * Supplies the existing records a new payload should be compared against. The
 * quality module must never query business tables directly, so the owning
 * module publishes one bean per entity it wants duplicate-checked. Without a
 * provider a duplicate rule reports "no candidate provider" instead of failing.
 */
public interface DuplicateCandidateProvider {

    /** Business module slug, e.g. {@code crm}. */
    String moduleCode();

    /** Entity slug, e.g. {@code customer}. */
    String entityCode();

    /**
     * Candidate records to compare against, each as a field map.
     *
     * @param field    the field being matched on
     * @param value    the incoming value (implementations may pre-filter on it)
     * @param excludeId the record being validated, excluded from its own check
     */
    List<Map<String, Object>> candidates(String field, String value, Long excludeId);
}
