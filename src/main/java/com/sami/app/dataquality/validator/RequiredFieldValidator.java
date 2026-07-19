package com.sami.app.dataquality.validator;

import com.sami.app.dataquality.spi.ValidationContext;
import com.sami.app.dataquality.spi.ValidationOutcome;
import com.sami.app.dataquality.spi.ValidationRule;
import org.springframework.stereotype.Component;

/** Fails when the target field is null, blank or an empty collection. */
@Component
public class RequiredFieldValidator implements ValidationRule {

    @Override public String type() { return "required"; }

    @Override public String label() { return "Required field"; }

    @Override public String defaultDimension() { return "completeness"; }

    @Override
    public ValidationOutcome validate(ValidationContext context) {
        Object value = context.value();
        boolean missing = value == null
                || (value instanceof CharSequence cs && cs.toString().isBlank())
                || (value instanceof java.util.Collection<?> c && c.isEmpty());
        return missing
                ? ValidationOutcome.fail(context.field() + " is required")
                : ValidationOutcome.ok();
    }
}
