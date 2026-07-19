package com.sami.app.dataquality.validator;

import com.sami.app.dataquality.spi.ValidationContext;
import com.sami.app.dataquality.spi.ValidationOutcome;
import com.sami.app.dataquality.spi.ValidationRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Enforces a configurable numeric {@code min}/{@code max}. */
@Component
public class RangeValidator implements ValidationRule {

    @Override public String type() { return "range"; }

    @Override public String label() { return "Numeric range"; }

    @Override
    public ValidationOutcome validate(ValidationContext context) {
        Object raw = context.value();
        if (raw == null || String.valueOf(raw).isBlank()) {
            return ValidationOutcome.skip();
        }
        BigDecimal value;
        try {
            value = new BigDecimal(String.valueOf(raw).trim());
        } catch (NumberFormatException ex) {
            return ValidationOutcome.fail(context.field() + " must be numeric");
        }
        Double min = context.configNumber("min");
        Double max = context.configNumber("max");
        if (min != null && value.compareTo(BigDecimal.valueOf(min)) < 0) {
            return ValidationOutcome.fail(context.field() + " must be at least " + min);
        }
        if (max != null && value.compareTo(BigDecimal.valueOf(max)) > 0) {
            return ValidationOutcome.fail(context.field() + " must be at most " + max);
        }
        return ValidationOutcome.ok();
    }
}
