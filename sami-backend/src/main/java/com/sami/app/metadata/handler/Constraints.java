package com.sami.app.metadata.handler;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Null-safe readers for the constraint map passed to field-type handlers. */
final class Constraints {

    private Constraints() {
    }

    static String text(Map<String, Object> c, String key) {
        Object v = c == null ? null : c.get(key);
        return v == null || String.valueOf(v).isBlank() ? null : String.valueOf(v);
    }

    static Integer integer(Map<String, Object> c, String key) {
        Object v = c == null ? null : c.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return v == null ? null : Integer.valueOf(String.valueOf(v).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static BigDecimal decimal(Map<String, Object> c, String key) {
        Object v = c == null ? null : c.get(key);
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return v == null ? null : new BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    static List<Object> options(Map<String, Object> c) {
        Object v = c == null ? null : c.get("options");
        return v instanceof List<?> list ? (List<Object>) list : List.of();
    }
}
