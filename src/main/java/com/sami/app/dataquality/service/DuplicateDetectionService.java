package com.sami.app.dataquality.service;

import com.sami.app.dataquality.spi.DuplicateCandidateProvider;
import com.sami.app.dataquality.spi.DuplicateCandidateRegistry;
import com.sami.app.dataquality.spi.DuplicateMatcher;
import com.sami.app.dataquality.spi.DuplicateMatcherRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Public duplicate-detection service. Strategy and threshold are configuration;
 * candidate records come from the owning module's
 * {@link DuplicateCandidateProvider}, so this service never queries business
 * tables and works for any entity that opts in.
 */
// Explicit bean name: the CRM module already contributes a
// `duplicateDetectionService` bean, and two beans may not share a name.
@Service("qualityDuplicateDetectionService")
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private final DuplicateMatcherRegistry matcherRegistry;
    private final DuplicateCandidateRegistry candidateRegistry;

    public DuplicateResult detect(String moduleCode, String entityCode, String field,
                                  Map<String, Object> data, Long excludeId, Map<String, Object> config) {
        String strategy = config != null && config.get("strategy") != null
                ? String.valueOf(config.get("strategy")) : "exact";
        DuplicateMatcher matcher = matcherRegistry.find(strategy).orElse(null);
        if (matcher == null) {
            return DuplicateResult.unavailable(strategy);
        }
        DuplicateCandidateProvider provider = candidateRegistry.find(moduleCode, entityCode).orElse(null);
        if (provider == null) {
            return DuplicateResult.unavailable(strategy);
        }

        String value = compositeValue(field, data, config);
        if (value == null || value.isBlank()) {
            return new DuplicateResult(false, strategy, threshold(config, strategy), true, List.of());
        }
        double threshold = threshold(config, strategy);

        List<DuplicateMatch> matches = new ArrayList<>();
        for (Map<String, Object> candidate : provider.candidates(field, value, excludeId)) {
            String candidateValue = compositeValue(field, candidate, config);
            if (candidateValue == null || candidateValue.isBlank()) {
                continue;
            }
            double score = matcher.similarity(value, candidateValue, config);
            if (score >= threshold) {
                Object id = candidate.get("id");
                matches.add(new DuplicateMatch(id instanceof Number n ? n.longValue() : null,
                        candidateValue, score));
            }
        }
        matches.sort((a, b) -> Double.compare(b.score(), a.score()));
        return new DuplicateResult(!matches.isEmpty(), strategy, threshold, true, matches);
    }

    /** For the composite strategy, join the configured fields into one key. */
    @SuppressWarnings("unchecked")
    private String compositeValue(String field, Map<String, Object> source, Map<String, Object> config) {
        if (source == null) {
            return null;
        }
        Object fields = config == null ? null : config.get("fields");
        if (fields instanceof List<?> list && !list.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (Object f : (List<Object>) list) {
                Object v = source.get(String.valueOf(f));
                parts.add(v == null ? "" : String.valueOf(v));
            }
            return String.join("|", parts);
        }
        Object value = source.get(field);
        return value == null ? null : String.valueOf(value);
    }

    private double threshold(Map<String, Object> config, String strategy) {
        Object configured = config == null ? null : config.get("threshold");
        if (configured instanceof Number n) {
            return n.doubleValue();
        }
        if (configured instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                // fall through to the strategy default
            }
        }
        return "fuzzy".equals(strategy) ? 0.85 : 1.0;
    }
}
