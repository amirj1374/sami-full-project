package com.sami.app.dataquality.validator;

import com.sami.app.dataquality.spi.ValidationContext;
import com.sami.app.dataquality.spi.ValidationOutcome;
import com.sami.app.dataquality.spi.ValidationRule;
import org.springframework.stereotype.Component;

/** Enforces configurable {@code min}/{@code max} string length. */
@Component
public class LengthValidator implements ValidationRule {

    @Override public String type() { return "length"; }

    @Override public String label() { return "Length range"; }

    @Override
    public ValidationOutcome validate(ValidationContext context) {
        String value = context.stringValue();
        if (value == null || value.isBlank()) {
            return ValidationOutcome.skip();
        }
        Double min = context.configNumber("min");
        Double max = context.configNumber("max");
        if (min != null && value.length() < min) {
            return ValidationOutcome.fail(context.field() + " must be at least " + min.intValue() + " characters");
        }
        if (max != null && value.length() > max) {
            return ValidationOutcome.fail(context.field() + " must be at most " + max.intValue() + " characters");
        }
        return ValidationOutcome.ok();
    }
}
