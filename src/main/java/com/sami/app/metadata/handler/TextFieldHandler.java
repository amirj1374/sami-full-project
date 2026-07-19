package com.sami.app.metadata.handler;

import com.sami.app.metadata.spi.FieldTypeHandler;
import com.sami.app.metadata.spi.FieldValue;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/** Text/textarea: length bounds and an optional regex pattern. */
@Component
public class TextFieldHandler implements FieldTypeHandler {

    @Override public String key() { return "text"; }

    @Override public String label() { return "Text"; }

    @Override
    public FieldValue coerce(Object raw, Map<String, Object> c) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return FieldValue.empty();
        }
        String value = String.valueOf(raw).trim();
        Integer min = Constraints.integer(c, "minLength");
        Integer max = Constraints.integer(c, "maxLength");
        if (min != null && value.length() < min) {
            throw new IllegalArgumentException("must be at least " + min + " characters");
        }
        if (max != null && value.length() > max) {
            throw new IllegalArgumentException("must be at most " + max + " characters");
        }
        String pattern = Constraints.text(c, "pattern");
        if (pattern != null && !Pattern.compile(pattern).matcher(value).matches()) {
            throw new IllegalArgumentException("does not match the required format");
        }
        return FieldValue.ofText(value);
    }
}
