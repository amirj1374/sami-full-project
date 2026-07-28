package com.sami.app.common.fields;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Central validation of dynamic (runtime-defined) field values against their
 * {@link DynamicFieldSpec}s. Unknown keys are rejected; values are coerced to
 * their declared type. Every module with configurable fields funnels through
 * here so the rules exist exactly once.
 */
@Component
public class DynamicFieldValidator {

    /** @return the sanitized value map (only known, typed, valid values). */
    public Map<String, Object> validate(Map<String, Object> submitted, List<DynamicFieldSpec> specs) {
        Map<String, Object> values = submitted == null ? Map.of() : submitted;
        Map<String, DynamicFieldSpec> byKey = new HashMap<>();
        specs.forEach(s -> byKey.put(s.key(), s));

        for (String key : values.keySet()) {
            if (!byKey.containsKey(key)) {
                throw error("Unknown field '%s'".formatted(key));
            }
        }

        Map<String, Object> sanitized = new HashMap<>();
        for (DynamicFieldSpec spec : specs) {
            Object value = values.get(spec.key());
            if (value == null || (value instanceof String s && s.isBlank())) {
                if (spec.required()) {
                    throw error("Field '%s' is required".formatted(spec.label()));
                }
                continue;
            }
            sanitized.put(spec.key(), validateValue(spec, value));
        }
        return sanitized;
    }

    private Object validateValue(DynamicFieldSpec spec, Object value) {
        return switch (spec.type()) {
            case TEXT -> validateText(spec, asString(spec, value));
            case NUMBER -> {
                if (value instanceof Number n) {
                    yield n;
                }
                try {
                    yield Double.parseDouble(asString(spec, value));
                } catch (NumberFormatException e) {
                    throw error("Field '%s' must be a number".formatted(spec.label()));
                }
            }
            case DATE -> {
                try {
                    yield LocalDate.parse(asString(spec, value)).toString();
                } catch (DateTimeParseException e) {
                    throw error("Field '%s' must be an ISO date (yyyy-MM-dd)".formatted(spec.label()));
                }
            }
            case BOOLEAN -> {
                if (value instanceof Boolean b) {
                    yield b;
                }
                String s = asString(spec, value);
                if ("true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s)) {
                    yield Boolean.parseBoolean(s);
                }
                throw error("Field '%s' must be true or false".formatted(spec.label()));
            }
            case SELECT -> {
                String s = asString(spec, value);
                if (spec.options() == null || !spec.options().contains(s)) {
                    throw error("Field '%s' must be one of: %s".formatted(spec.label(), spec.options()));
                }
                yield s;
            }
        };
    }

    private String validateText(DynamicFieldSpec spec, String s) {
        if (spec.minLength() != null && s.length() < spec.minLength()) {
            throw error("Field '%s' must be at least %d characters"
                    .formatted(spec.label(), spec.minLength()));
        }
        if (spec.maxLength() != null && s.length() > spec.maxLength()) {
            throw error("Field '%s' must be at most %d characters"
                    .formatted(spec.label(), spec.maxLength()));
        }
        if (spec.pattern() != null && !spec.pattern().isBlank() && !Pattern.matches(spec.pattern(), s)) {
            throw error("Field '%s' has an invalid format".formatted(spec.label()));
        }
        return s;
    }

    private String asString(DynamicFieldSpec spec, Object value) {
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        throw error("Field '%s' has an unsupported value".formatted(spec.label()));
    }

    private ApiException error(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, message);
    }
}
