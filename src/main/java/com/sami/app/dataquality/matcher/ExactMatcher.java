package com.sami.app.dataquality.matcher;

import com.sami.app.dataquality.spi.DuplicateMatcher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Case-insensitive exact match after trimming and collapsing whitespace. This is
 * the strategy behind "an IMEI may never be registered twice".
 */
@Component
public class ExactMatcher implements DuplicateMatcher {

    @Override public String strategy() { return "exact"; }

    @Override public String label() { return "Exact match"; }

    @Override
    public double similarity(String left, String right, Map<String, Object> config) {
        if (left == null || right == null) {
            return 0;
        }
        return normalize(left).equals(normalize(right)) ? 1.0 : 0.0;
    }

    static String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
