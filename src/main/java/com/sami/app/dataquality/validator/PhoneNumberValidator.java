package com.sami.app.dataquality.validator;

import com.sami.app.dataquality.spi.ValidationContext;
import com.sami.app.dataquality.spi.ValidationOutcome;
import com.sami.app.dataquality.spi.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Phone number validation. Defaults to Iranian mobile numbers (09xxxxxxxxx and
 * its +98/0098/98 prefixes); set {@code config.pattern} to override for another
 * region without touching code.
 */
@Component
public class PhoneNumberValidator implements ValidationRule {

    private static final Pattern IR_MOBILE = Pattern.compile("^(?:\\+98|0098|98|0)?9\\d{9}$");

    @Override public String type() { return "phone"; }

    @Override public String label() { return "Phone number"; }

    @Override
    public ValidationOutcome validate(ValidationContext context) {
        String raw = context.stringValue();
        if (raw == null || raw.isBlank()) {
            return ValidationOutcome.skip();
        }
        String normalized = raw.replaceAll("[\\s()-]", "");
        String override = context.configString("pattern");
        Pattern pattern = override == null ? IR_MOBILE : Pattern.compile(override);
        return pattern.matcher(normalized).matches()
                ? ValidationOutcome.ok()
                : ValidationOutcome.fail(context.field() + " is not a valid phone number");
    }
}
