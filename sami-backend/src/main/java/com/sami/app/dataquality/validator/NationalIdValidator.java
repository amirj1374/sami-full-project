package com.sami.app.dataquality.validator;

import com.sami.app.dataquality.spi.ValidationContext;
import com.sami.app.dataquality.spi.ValidationOutcome;
import com.sami.app.dataquality.spi.ValidationRule;
import org.springframework.stereotype.Component;

/**
 * Iranian national ID (کد ملی): ten digits with a modulus-11 check digit.
 * Repeated-digit sequences (e.g. 1111111111) are structurally valid under the
 * checksum but are never issued, so they are rejected too.
 */
@Component
public class NationalIdValidator implements ValidationRule {

    @Override public String type() { return "national-id"; }

    @Override public String label() { return "Iranian national ID"; }

    @Override
    public ValidationOutcome validate(ValidationContext context) {
        String raw = context.stringValue();
        if (raw == null || raw.isBlank()) {
            return ValidationOutcome.skip();
        }
        String id = raw.replaceAll("[\\s-]", "");
        if (!id.matches("\\d{10}")) {
            return ValidationOutcome.fail("National ID must be exactly 10 digits");
        }
        if (id.chars().distinct().count() == 1) {
            return ValidationOutcome.fail("National ID with identical digits is not issued");
        }
        int checkDigit = id.charAt(9) - '0';
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (id.charAt(i) - '0') * (10 - i);
        }
        int remainder = sum % 11;
        boolean valid = (remainder < 2) ? (checkDigit == remainder) : (checkDigit == 11 - remainder);
        return valid
                ? ValidationOutcome.ok()
                : ValidationOutcome.fail("National ID check digit is invalid");
    }
}
