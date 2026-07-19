package com.sami.app.automation.provider;

import com.sami.app.automation.spi.ActionContext;
import com.sami.app.automation.spi.ActionProvider;
import com.sami.app.automation.spi.ActionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Baseline action: records a configurable message to the application log. Useful
 * for testing rules and as the canonical zero-coupling {@link ActionProvider}
 * example. Config: {@code {"message": "..."}}.
 */
@Slf4j
@Component
public class LogActionProvider implements ActionProvider {

    @Override
    public String type() {
        return "log";
    }

    @Override
    public String label() {
        return "Write to log";
    }

    @Override
    public ActionResult execute(ActionContext context) {
        String message = context.configString("message");
        if (message == null) {
            message = "Automation triggered by " + context.trigger().triggerType();
        }
        log.info("[automation] {}", message);
        return ActionResult.ok(message, Map.of());
    }
}
