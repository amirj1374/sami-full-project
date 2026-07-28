package com.sami.app.automation.spi;

import java.util.Map;

/**
 * Everything an {@link ActionProvider} needs to run one step: the step's own
 * JSON {@code config}, the firing {@link AutomationContext}, and the accumulated
 * {@code state} produced by earlier steps in the same execution.
 */
public record ActionContext(
        Map<String, Object> config,
        AutomationContext trigger,
        Map<String, Object> state
) {

    /** Convenience: a config value as String, or null. */
    public String configString(String key) {
        Object v = config.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
