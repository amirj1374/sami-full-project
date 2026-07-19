package com.sami.app.dataquality.service;

import java.util.List;

/**
 * Outcome of a duplicate check.
 *
 * @param providerAvailable false when no module registered a candidate provider
 *                          for the target entity — reported rather than failing
 */
public record DuplicateResult(
        boolean duplicateFound,
        String strategy,
        double threshold,
        boolean providerAvailable,
        List<DuplicateMatch> matches
) {
    public static DuplicateResult unavailable(String strategy) {
        return new DuplicateResult(false, strategy, 0, false, List.of());
    }
}
