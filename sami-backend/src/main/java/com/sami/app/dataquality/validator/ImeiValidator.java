package com.sami.app.dataquality.validator;

import com.sami.app.dataquality.spi.ValidationContext;
import com.sami.app.dataquality.spi.ValidationOutcome;
import com.sami.app.dataquality.spi.ValidationRule;
import org.springframework.stereotype.Component;

/**
 * IMEI validation for handset stock: 15 digits terminated by a Luhn check digit.
 * Rejecting a malformed IMEI at entry is what keeps serialized inventory,
 * warranty and repair records referencing a real device.
 */
@Component
public class ImeiValidator implements ValidationRule {

    @Override public String type() { return "imei"; }

    @Override public String label() { return "IMEI (Luhn)"; }

    @Override
    public ValidationOutcome validate(ValidationContext context) {
        String raw = context.stringValue();
        if (raw == null || raw.isBlank()) {
            return ValidationOutcome.skip();
        }
        String imei = raw.replaceAll("[\\s-]", "");
        if (!imei.matches("\\d{15}")) {
            return ValidationOutcome.fail("IMEI must be exactly 15 digits");
        }
        return luhnValid(imei)
                ? ValidationOutcome.ok()
                : ValidationOutcome.fail("IMEI failed the Luhn check digit");
    }

    /** Standard Luhn: double every second digit from the right, sum, mod 10 == 0. */
    private boolean luhnValid(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}
