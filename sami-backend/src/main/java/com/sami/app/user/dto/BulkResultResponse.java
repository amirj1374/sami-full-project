package com.sami.app.user.dto;

import java.util.List;

/**
 * Outcome of a bulk operation. Bulk ops are best-effort per item: valid targets
 * are processed, invalid ones (unknown id, self, already in state, guard
 * violation) are reported in {@code skipped} with a reason — one bad row never
 * aborts the batch.
 */
public record BulkResultResponse(
        int processed,
        List<SkippedItem> skipped
) {

    public record SkippedItem(Long id, String reason) {
    }
}
