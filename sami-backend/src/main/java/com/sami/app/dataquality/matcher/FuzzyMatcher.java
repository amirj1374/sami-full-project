package com.sami.app.dataquality.matcher;

import com.sami.app.dataquality.spi.DuplicateMatcher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Levenshtein-ratio similarity — catches typos and spacing differences in names
 * ("محمد رضایی" vs "محمدرضایی"). Unicode-safe, so it works for Persian text.
 */
@Component
public class FuzzyMatcher implements DuplicateMatcher {

    @Override public String strategy() { return "fuzzy"; }

    @Override public String label() { return "Fuzzy (edit distance)"; }

    @Override
    public double similarity(String left, String right, Map<String, Object> config) {
        if (left == null || right == null) {
            return 0;
        }
        String a = ExactMatcher.normalize(left);
        String b = ExactMatcher.normalize(right);
        if (a.equals(b)) {
            return 1.0;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int distance = levenshtein(a, b);
        return 1.0 - ((double) distance / Math.max(a.length(), b.length()));
    }

    private int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
