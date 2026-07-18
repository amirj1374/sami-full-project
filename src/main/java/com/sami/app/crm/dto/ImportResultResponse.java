package com.sami.app.crm.dto;

import java.util.List;

/**
 * Outcome of a customer import: per-row results — bad rows (validation
 * failures, duplicates) are reported and never abort the batch.
 */
public record ImportResultResponse(
        int created,
        List<SkippedRow> skipped
) {

    public record SkippedRow(int line, String reason) {
    }
}
