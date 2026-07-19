package com.sami.app.dataquality.validator;

import com.sami.app.dataquality.service.DuplicateDetectionService;
import com.sami.app.dataquality.service.DuplicateResult;
import com.sami.app.dataquality.spi.ValidationContext;
import com.sami.app.dataquality.spi.ValidationOutcome;
import com.sami.app.dataquality.spi.ValidationRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Duplicate-detection rule. Delegates to {@link DuplicateDetectionService} so
 * the strategy (exact/fuzzy/phonetic/composite) stays configuration. This is the
 * rule behind "an IMEI may never be registered twice across the ERP".
 */
@Component
@RequiredArgsConstructor
public class DuplicateValidator implements ValidationRule {

    private final DuplicateDetectionService duplicateDetection;

    @Override public String type() { return "duplicate"; }

    @Override public String label() { return "Duplicate detection"; }

    @Override public String defaultDimension() { return "uniqueness"; }

    @Override
    public ValidationOutcome validate(ValidationContext context) {
        String value = context.stringValue();
        if (value == null || value.isBlank()) {
            return ValidationOutcome.skip();
        }
        DuplicateResult result = duplicateDetection.detect(context.moduleCode(), context.entityCode(),
                context.field(), context.data(), context.entityId(), context.config());
        if (!result.providerAvailable()) {
            return ValidationOutcome.skip();
        }
        if (!result.duplicateFound()) {
            return ValidationOutcome.ok();
        }
        return ValidationOutcome.fail(
                "Possible duplicate on " + context.field() + " (" + result.matches().size() + " match(es))",
                Map.of("strategy", result.strategy(),
                        "threshold", result.threshold(),
                        "matches", result.matches().stream()
                                .map(m -> Map.of("entityId", String.valueOf(m.entityId()),
                                        "value", m.value(), "score", m.score()))
                                .toList()));
    }
}
