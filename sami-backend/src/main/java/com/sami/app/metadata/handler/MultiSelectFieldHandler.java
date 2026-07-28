package com.sami.app.metadata.handler;

import com.sami.app.metadata.spi.FieldTypeHandler;
import com.sami.app.metadata.spi.FieldValue;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/** Multi select: stored as JSON so the whole selection round-trips. */
@Component
public class MultiSelectFieldHandler implements FieldTypeHandler {

    @Override public String key() { return "multiselect"; }

    @Override public String label() { return "Multi select"; }

    @Override public boolean supportsOptions() { return true; }

    @Override
    public FieldValue coerce(Object raw, Map<String, Object> c) {
        if (raw == null) {
            return FieldValue.empty();
        }
        List<Object> selected = new ArrayList<>();
        if (raw instanceof List<?> list) {
            selected.addAll(list);
        } else if (!String.valueOf(raw).isBlank()) {
            for (String part : String.valueOf(raw).split(",")) {
                selected.add(part.trim());
            }
        }
        if (selected.isEmpty()) {
            return FieldValue.empty();
        }
        List<Object> options = Constraints.options(c);
        if (!options.isEmpty()) {
            for (Object value : selected) {
                if (options.stream().noneMatch(o -> String.valueOf(o).equals(String.valueOf(value)))) {
                    throw new IllegalArgumentException("'" + value + "' is not a configured option");
                }
            }
        }
        return FieldValue.ofJson(java.util.Map.of("values", selected));
    }

    @Override
    public String validateDefinition(java.util.Map<String, Object> constraints, List<Object> options) {
        return (options == null || options.isEmpty()) ? "multiselect fields require at least one option" : null;
    }
}
