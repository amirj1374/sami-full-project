package com.sami.app.automation.provider;

import com.sami.app.automation.spi.ActionContext;
import com.sami.app.automation.spi.ActionProvider;
import com.sami.app.automation.spi.ActionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Sends a notification. There is no general notification port in the ERP yet, so
 * this implementation is the future-ready seam (mirroring {@code LoggingMailSender}):
 * it validates and records the notification intent and returns its payload. A
 * real channel (email/SMS/push) drops in behind the same action type with no rule
 * or engine change. Config: {@code {"channel","recipient","subject","body"}}.
 */
@Slf4j
@Component
public class NotificationActionProvider implements ActionProvider {

    @Override
    public String type() {
        return "notify";
    }

    @Override
    public String label() {
        return "Send notification";
    }

    @Override
    public ActionResult execute(ActionContext context) {
        String channel = context.config().getOrDefault("channel", "log").toString();
        String recipient = context.configString("recipient");
        String subject = context.configString("subject");
        if (recipient == null || recipient.isBlank()) {
            return ActionResult.fail("notify: 'recipient' is required");
        }
        log.info("[automation] notify via {} → {} : {}", channel, recipient, subject);
        return ActionResult.ok("Notification queued",
                Map.of("channel", channel, "recipient", recipient));
    }

    @Override
    public String validate(Map<String, Object> config) {
        Object recipient = config.get("recipient");
        return (recipient == null || recipient.toString().isBlank())
                ? "notify: 'recipient' is required" : null;
    }
}
