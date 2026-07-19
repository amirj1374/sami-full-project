package com.sami.app.dataquality.validator;

import com.sami.app.dataquality.spi.ValidationContext;
import com.sami.app.dataquality.spi.ValidationOutcome;
import com.sami.app.dataquality.spi.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Matches the value against a configurable {@code pattern}. */
@Component
public class RegexValidator implements ValidationRule {

    @Override public String type() { return "regex"; }

    @Override public String label() { return "Format (regex)"; }

    @Override
    public ValidationOutcome validate(ValidationContext context) {
        String value = context.stringValue();
        String pattern = context.configString("pattern");
        if (value == null || value.isBlank() || pattern == null) {
            return ValidationOutcome.skip();
        }
        return Pattern.compile(pattern).matcher(value).matches()
                ? ValidationOutcome.ok()
                : ValidationOutcome.fail(context.field() + " does not match the required format");
    }

    @Override
    public String validateConfig(Map<String, Object> config) {
        Object pattern = config == null ? null : config.get("pattern");
        if (pattern == null || pattern.toString().isBlank()) {
            return "regex: 'pattern' is required";
        }
        try {
            Pattern.compile(pattern.toString());
            return null;
        } catch (PatternSyntaxException ex) {
            return "regex: invalid pattern — " + ex.getDescription();
        }
    }
}
