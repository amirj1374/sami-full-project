package com.sami.app.automation.engine;

import com.sami.app.automation.spi.AutomationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Evaluates a configurable, JSON-defined condition tree against an
 * {@link AutomationContext}. The rules are pure data; this component is the only
 * engine — so new business conditions are authored, never coded.
 *
 * <p>Grammar (recursive):
 * <pre>
 *   {}                                          → always true
 *   { "all": [ &lt;node&gt;, … ] }                     → AND
 *   { "any": [ &lt;node&gt;, … ] }                     → OR
 *   { "not": &lt;node&gt; }                            → NOT
 *   { "field": "amount", "op": "gte", "value": 1000 }   → leaf comparison
 * </pre>
 * Fields resolve from context roots (companyId, branchId, entityType, entityId,
 * triggerType, module, actorId) or a dotted path into the event {@code data}.
 * Operators: eq, neq, gt, gte, lt, lte, in, nin, contains, regex, exists, isnull.
 */
@Component
public class ConditionEvaluator {

    @SuppressWarnings("unchecked")
    public boolean evaluate(Map<String, Object> node, AutomationContext ctx) {
        if (node == null || node.isEmpty()) {
            return true;
        }
        if (node.get("all") instanceof List<?> list) {
            return list.stream().allMatch(n -> evaluate((Map<String, Object>) n, ctx));
        }
        if (node.get("any") instanceof List<?> list) {
            return list.stream().anyMatch(n -> evaluate((Map<String, Object>) n, ctx));
        }
        if (node.get("not") instanceof Map<?, ?> sub) {
            return !evaluate((Map<String, Object>) sub, ctx);
        }
        return evaluateLeaf(node, ctx);
    }

    private boolean evaluateLeaf(Map<String, Object> leaf, AutomationContext ctx) {
        String field = String.valueOf(leaf.get("field"));
        String op = String.valueOf(leaf.getOrDefault("op", "eq")).toLowerCase();
        Object expected = leaf.get("value");
        Object actual = resolveField(field, ctx);

        return switch (op) {
            case "exists", "notnull" -> actual != null;
            case "isnull", "notexists" -> actual == null;
            case "eq" -> equalsLoose(actual, expected);
            case "neq" -> !equalsLoose(actual, expected);
            case "gt" -> compare(actual, expected) > 0;
            case "gte" -> compare(actual, expected) >= 0;
            case "lt" -> compare(actual, expected) < 0;
            case "lte" -> compare(actual, expected) <= 0;
            case "in" -> inList(actual, expected);
            case "nin" -> !inList(actual, expected);
            case "contains" -> actual != null && expected != null
                    && String.valueOf(actual).contains(String.valueOf(expected));
            case "regex" -> actual != null && expected != null
                    && Pattern.compile(String.valueOf(expected)).matcher(String.valueOf(actual)).find();
            default -> false;
        };
    }

    private Object resolveField(String field, AutomationContext ctx) {
        if (field == null) {
            return null;
        }
        return switch (field) {
            case "companyId" -> ctx.companyId();
            case "branchId" -> ctx.branchId();
            case "entityType" -> ctx.entityType();
            case "entityId" -> ctx.entityId();
            case "triggerType" -> ctx.triggerType();
            case "module" -> ctx.module();
            case "actorId" -> ctx.actorId();
            default -> resolvePath(field, ctx.data());
        };
    }

    @SuppressWarnings("unchecked")
    private Object resolvePath(String path, Map<String, Object> data) {
        if (data == null) {
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
        BigDecimal na = toNumber(a);
        BigDecimal nb = toNumber(b);
        if (na != null && nb != null) {
            return na.compareTo(nb) == 0;
        }
        return String.valueOf(a).equalsIgnoreCase(String.valueOf(b));
    }

    /** Numeric comparison; non-numeric operands fall back to case-insensitive string order. */
    private int compare(Object a, Object b) {
        BigDecimal na = toNumber(a);
        BigDecimal nb = toNumber(b);
        if (na != null && nb != null) {
            return na.compareTo(nb);
        }
        if (a == null || b == null) {
            return a == b ? 0 : (a == null ? -1 : 1);
        }
        return String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
    }

    private boolean inList(Object actual, Object expected) {
        if (!(expected instanceof List<?> list) || actual == null) {
            return false;
        }
        return list.stream().anyMatch(v -> equalsLoose(actual, v));
    }

    private BigDecimal toNumber(Object o) {
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
