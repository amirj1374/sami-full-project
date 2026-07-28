package com.sami.app.metadata.handler;

import com.sami.app.metadata.spi.FieldTypeHandler;
import com.sami.app.metadata.spi.FieldValue;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.math.BigDecimal;

/** Integer/decimal: numeric coercion with min/max bounds. */
@Component
public class NumberFieldHandler implements FieldTypeHandler {

    @Override public String key() { return "number"; }

    @Override public String label() { return "Number"; }

    @Override
    public FieldValue coerce(Object raw, Map<String, Object> c) {
        if (raw == null || String.valueOf(raw).isBlank()) {
            return FieldValue.empty();
        }
        BigDecimal value;
        try {
            value = new BigDecimal(String.valueOf(raw).trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("must be a number");
        }
        BigDecimal min = Constraints.decimal(c, "minValue");
        BigDecimal max = Constraints.decimal(c, "maxValue");
        if (min != null && value.compareTo(min) < 0) {
            throw new IllegalArgumentException("must be at least " + min);
        }
        if (max != null && value.compareTo(max) > 0) {
            throw new IllegalArgumentException("must be at most " + max);
        }
        return FieldValue.ofNumber(value);
    }
}
