package com.sami.app.metadata.handler;

import com.sami.app.metadata.spi.FieldTypeHandler;
import com.sami.app.metadata.spi.FieldValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Format-constrained text types plus reference and raw JSON. Registered as beans
 * so the set can grow without touching the engine.
 */
@Configuration
public class PatternFieldHandlers {

    private static FieldTypeHandler pattern(String key, String label, Pattern regex, String message) {
        return new FieldTypeHandler() {
            @Override public String key() { return key; }

            @Override public String label() { return label; }

            @Override
            public FieldValue coerce(Object raw, Map<String, Object> constraints) {
                if (raw == null || String.valueOf(raw).isBlank()) {
                    return FieldValue.empty();
                }
                String value = String.valueOf(raw).trim();
                if (!regex.matcher(value).matches()) {
                    throw new IllegalArgumentException(message);
                }
                return FieldValue.ofText(value);
            }
        };
    }

    @Bean
    FieldTypeHandler emailFieldHandler() {
        return pattern("email", "Email",
                Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"),
                "must be a valid email address");
    }

    @Bean
    FieldTypeHandler phoneFieldHandler() {
        return pattern("phone", "Phone",
                Pattern.compile("^(?:\\+98|0098|98|0)?9\\d{9}$"),
                "must be a valid mobile number");
    }

    @Bean
    FieldTypeHandler urlFieldHandler() {
        return pattern("url", "URL",
                Pattern.compile("^https?://[^\\s]+$"),
                "must be a valid URL");
    }

    /** Reference to another record: stored numerically so joins/reports work. */
    @Bean
    FieldTypeHandler referenceFieldHandler() {
        return new FieldTypeHandler() {
            @Override public String key() { return "reference"; }

            @Override public String label() { return "Reference"; }

            @Override
            public FieldValue coerce(Object raw, Map<String, Object> constraints) {
                if (raw == null || String.valueOf(raw).isBlank()) {
                    return FieldValue.empty();
                }
                try {
                    return FieldValue.ofNumber(new BigDecimal(String.valueOf(raw).trim()));
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("must reference a record id");
                }
            }
        };
    }

    @Bean
    @SuppressWarnings("unchecked")
    FieldTypeHandler jsonFieldHandler() {
        return new FieldTypeHandler() {
            @Override public String key() { return "json"; }

            @Override public String label() { return "Structured (JSON)"; }

            @Override
            public FieldValue coerce(Object raw, Map<String, Object> constraints) {
                if (raw == null) {
                    return FieldValue.empty();
                }
                if (raw instanceof Map<?, ?> map) {
                    return FieldValue.ofJson((Map<String, Object>) map);
                }
                throw new IllegalArgumentException("must be a JSON object");
            }
        };
    }
}
