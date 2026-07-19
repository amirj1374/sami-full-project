package com.sami.app.dataquality.validator;

import com.sami.app.dataquality.spi.ValidationContext;
import com.sami.app.dataquality.spi.ValidationOutcome;
import com.sami.app.dataquality.spi.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Email address format. */
@Component
public class EmailValidator implements ValidationRule {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Override public String type() { return "email"; }

    @Override public String label() { return "Email address"; }

    @Override
    public ValidationOutcome validate(ValidationContext context) {
        String value = context.stringValue();
        if (value == null || value.isBlank()) {
            return ValidationOutcome.skip();
        }
        return EMAIL.matcher(value.trim()).matches()
                ? ValidationOutcome.ok()
                : ValidationOutcome.fail(context.field() + " is not a valid email address");
    }
}
