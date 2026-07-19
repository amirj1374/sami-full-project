package com.sami.app.dataquality.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * The public result of validating one payload.
 *
 * @param blocking true when a failure came from a severity flagged
 *                 {@code blocks_save} — the caller should reject the write
 */
public record ValidationReport(
        String runNumber,
        String moduleCode,
        String entityCode,
        Long entityId,
        boolean passed,
        boolean blocking,
        int ruleCount,
        int passedCount,
        int failedCount,
        int skippedCount,
        BigDecimal score,
        String band,
        List<ValidationFinding> findings
) {

    /** One failed rule, as surfaced to the caller. */
    public record ValidationFinding(
            String ruleCode,
            String field,
            String severity,
            String dimension,
            String message
    ) {
    }
}
