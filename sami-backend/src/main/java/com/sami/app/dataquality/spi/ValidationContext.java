package com.sami.app.dataquality.spi;

import java.util.Map;

/**
 * Everything a {@link ValidationRule} needs to judge one payload: the entity
 * being validated, the field the rule targets, the submitted {@code data} and
 * the rule's own JSON {@code config}.
 */
public record ValidationContext(
        String moduleCode,
        String entityCode,
        Long entityId,
        String field,
        Map<String, Object> data,
        Map<String, Object> config
) {

    /** The value under the rule's target field (dot paths supported). */
    public Object value() {
        return valueOf(field);
    }

    public Object valueOf(String path) {
        if (path == null || data == null) {
            return null;
        }
        Object current = data;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    public String stringValue() {
        Object v = value();
        return v == null ? null : String.valueOf(v);
    }

    public String configString(String key) {
        Object v = config == null ? null : config.get(key);
        return v == null ? null : String.valueOf(v);
    }

    public Double configNumber(String key) {
        Object v = config == null ? null : config.get(key);
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
