package com.sami.app.metadata.spi;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * A normalized custom-field value, already coerced into the storage kind its
 * type declares. Exactly one slot is populated (JSON values use {@code json}).
 */
public record FieldValue(String text, BigDecimal number, Boolean bool, Instant date, Map<String, Object> json) {

    public static FieldValue ofText(String v) {
        return new FieldValue(v, null, null, null, null);
    }

    public static FieldValue ofNumber(BigDecimal v) {
        return new FieldValue(null, v, null, null, null);
    }

    public static FieldValue ofBoolean(Boolean v) {
        return new FieldValue(null, null, v, null, null);
    }

    public static FieldValue ofDate(Instant v) {
        return new FieldValue(null, null, null, v, null);
    }

    public static FieldValue ofJson(Map<String, Object> v) {
        return new FieldValue(null, null, null, null, v);
    }

    public static FieldValue empty() {
        return new FieldValue(null, null, null, null, null);
    }

    public boolean isEmpty() {
        return text == null && number == null && bool == null && date == null && json == null;
    }
}
