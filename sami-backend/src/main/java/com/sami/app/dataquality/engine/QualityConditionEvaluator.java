package com.sami.app.dataquality.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Evaluates a rule's JSON condition tree against the submitted payload, so a
 * rule can be made conditional without code ("only validate the tax number when
 * the customer type is business"). Grammar mirrors the automation engine's:
 * {@code {}} / {@code {all:[…]}} / {@code {any:[…]}} / {@code {not:{…}}} /
 * {@code {field,op,value}}. This bounded context owns its own evaluator so the
 * two infrastructure modules stay independent.
 */
@Component
public class QualityConditionEvaluator {

    @SuppressWarnings("unchecked")
    public boolean evaluate(Map<String, Object> node, Map<String, Object> data) {
        if (node == null || node.isEmpty()) {
            return true;
        }
        if (node.get("all") instanceof List<?> list) {
            return list.stream().allMatch(n -> evaluate((Map<String, Object>) n, data));
        }
        if (node.get("any") instanceof List<?> list) {
            return list.stream().anyMatch(n -> evaluate((Map<String, Object>) n, data));
        }
        if (node.get("not") instanceof Map<?, ?> sub) {
            return !evaluate((Map<String, Object>) sub, data);
        }
        return leaf(node, data);
    }

    private boolean leaf(Map<String, Object> leaf, Map<String, Object> data) {
        Object actual = resolve(String.valueOf(leaf.get("field")), data);
        Object expected = leaf.get("value");
        String op = String.valueOf(leaf.getOrDefault("op", "eq")).toLowerCase();
        return switch (op) {
            case "exists", "notnull" -> actual != null;
            case "isnull", "notexists" -> actual == null;
            case "eq" -> equalsLoose(actual, expected);
            case "neq" -> !equalsLoose(actual, expected);
            case "gt" -> compare(actual, expected) > 0;
            case "gte" -> compare(actual, expected) >= 0;
            case "lt" -> compare(actual, expected) < 0;
            case "lte" -> compare(actual, expected) <= 0;
            case "in" -> expected instanceof List<?> l && actual != null
                    && l.stream().anyMatch(v -> equalsLoose(actual, v));
            case "contains" -> actual != null && expected != null
                    && String.valueOf(actual).contains(String.valueOf(expected));
            case "regex" -> actual != null && expected != null
                    && Pattern.compile(String.valueOf(expected)).matcher(String.valueOf(actual)).find();
            default -> false;
        };
    }

    @SuppressWarnings("unchecked")
    private Object resolve(String path, Map<String, Object> data) {
        if (path == null || data == null) {
            return null;
        }
        Object current = data;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(segment);
        }
        return current;
    }

    private boolean equalsLoose(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        BigDecimal na = number(a);
        BigDecimal nb = number(b);
        if (na != null && nb != null) {
            return na.compareTo(nb) == 0;
        }
        return String.valueOf(a).equalsIgnoreCase(String.valueOf(b));
    }

    private int compare(Object a, Object b) {
        BigDecimal na = number(a);
        BigDecimal nb = number(b);
        if (na != null && nb != null) {
            return na.compareTo(nb);
        }
        if (a == null || b == null) {
            return a == b ? 0 : (a == null ? -1 : 1);
        }
        return String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
    }

    private BigDecimal number(Object o) {
        if (o instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        if (o instanceof String s) {
            try {
                return new BigDecimal(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
