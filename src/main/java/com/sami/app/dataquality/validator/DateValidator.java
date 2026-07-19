package com.sami.app.dataquality.validator;

import com.sami.app.dataquality.spi.ValidationContext;
import com.sami.app.dataquality.spi.ValidationOutcome;
import com.sami.app.dataquality.spi.ValidationRule;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Parses an ISO date and optionally enforces {@code notFuture} / {@code notPast}.
 */
@Component
public class DateValidator implements ValidationRule {

    @Override public String type() { return "date"; }

    @Override public String label() { return "Date validation"; }

    @Override public String defaultDimension() { return "timeliness"; }

    @Override
    public ValidationOutcome validate(ValidationContext context) {
        String value = context.stringValue();
        if (value == null || value.isBlank()) {
            return ValidationOutcome.skip();
        }
        LocalDate date;
        try {
            date = LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
        } catch (DateTimeParseException ex) {
            return ValidationOutcome.fail(context.field() + " is not a valid date");
        }
        LocalDate today = LocalDate.now();
        if (Boolean.TRUE.equals(context.config().get("notFuture")) && date.isAfter(today)) {
            return ValidationOutcome.fail(context.field() + " may not be in the future");
        }
        if (Boolean.TRUE.equals(context.config().get("notPast")) && date.isBefore(today)) {
            return ValidationOutcome.fail(context.field() + " may not be in the past");
        }
        return ValidationOutcome.ok();
    }
}
