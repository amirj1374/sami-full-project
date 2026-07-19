package com.sami.app.automation.spi;

import java.util.Map;

/**
 * Outcome of a single action step. {@code output} is merged into the execution
 * result and made available to later steps.
 */
public record ActionResult(boolean success, String message, Map<String, Object> output) {

    public static ActionResult ok() {
        return new ActionResult(true, null, Map.of());
    }

    public static ActionResult ok(String message, Map<String, Object> output) {
        return new ActionResult(true, message, output == null ? Map.of() : output);
    }

    public static ActionResult fail(String message) {
        return new ActionResult(false, message, Map.of());
    }
}
