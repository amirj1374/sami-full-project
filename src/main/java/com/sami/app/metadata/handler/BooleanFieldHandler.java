package com.sami.app.metadata.handler;

import com.sami.app.metadata.spi.FieldTypeHandler;
import com.sami.app.metadata.spi.FieldValue;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Yes/No. */
@Component
public class BooleanFieldHandler implements FieldTypeHandler {

    @Override public String key() { return "boolean"; }

    @Override public String label() { return "Yes / No"; }

    @Override
    public FieldValue coerce(Object raw, Map<String, Object> c) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return FieldValue.empty();
        }
        if (raw instanceof Boolean b) {
            return FieldValue.ofBoolean(b);
        }
        String v = String.valueOf(raw).trim().toLowerCase();
        if (v.equals("true") || v.equals("1") || v.equals("yes")) {
            return FieldValue.ofBoolean(true);
        }
        if (v.equals("false") || v.equals("0") || v.equals("no")) {
            return FieldValue.ofBoolean(false);
        }
        throw new IllegalArgumentException("must be true or false");
    }
}
