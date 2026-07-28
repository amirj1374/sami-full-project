package com.sami.app.metadata.handler;

import com.sami.app.metadata.spi.FieldTypeHandler;
import com.sami.app.metadata.spi.FieldValue;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/** Date / date-time, stored as an instant so it sorts and filters natively. */
@Component
public class DateFieldHandler implements FieldTypeHandler {

    @Override public String key() { return "date"; }

    @Override public String label() { return "Date"; }

    @Override
    public FieldValue coerce(Object raw, Map<String, Object> c) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return FieldValue.empty();
        }
        String value = String.valueOf(raw).trim();
        try {
            return FieldValue.ofDate(Instant.parse(value));
        } catch (DateTimeParseException ignored) {
            // fall through to a plain ISO date
        }
        try {
            return FieldValue.ofDate(LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("must be a valid date");
        }
    }
}
