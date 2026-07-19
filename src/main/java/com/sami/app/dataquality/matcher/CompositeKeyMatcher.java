package com.sami.app.dataquality.matcher;

import com.sami.app.dataquality.spi.DuplicateMatcher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Composite-key matching: the caller has already joined the configured fields
 * into a single key (see {@code DuplicateDetectionService}), so a match means
 * every component field matched. Used for rules like "same national code AND
 * same birth date".
 */
@Component
public class CompositeKeyMatcher implements DuplicateMatcher {

    @Override public String strategy() { return "composite"; }

    @Override public String label() { return "Composite key"; }

    @Override
    public double similarity(String left, String right, Map<String, Object> config) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return 0;
        }
        return ExactMatcher.normalize(left).equals(ExactMatcher.normalize(right)) ? 1.0 : 0.0;
    }
}
