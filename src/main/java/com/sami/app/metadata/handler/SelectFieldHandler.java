package com.sami.app.metadata.handler;

import com.sami.app.metadata.spi.FieldTypeHandler;
import com.sami.app.metadata.spi.FieldValue;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

/** Single select: the value must be one of the configured options. */
@Component
public class SelectFieldHandler implements FieldTypeHandler {

    @Override public String key() { return "select"; }

    @Override public String label() { return "Single select"; }

    @Override public boolean supportsOptions() { return true; }

    @Override
    public FieldValue coerce(Object raw, Map<String, Object> c) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return FieldValue.empty();
        }
        String value = String.valueOf(raw).trim();
        List<Object> options = Constraints.options(c);
        if (!options.isEmpty() && options.stream().noneMatch(o -> String.valueOf(o).equals(value))) {
            throw new IllegalArgumentException("must be one of the configured options");
        }
        return FieldValue.ofText(value);
    }

    @Override
    public String validateDefinition(Map<String, Object> constraints, List<Object> options) {
        return (options == null || options.isEmpty()) ? "select fields require at least one option" : null;
    }
}
