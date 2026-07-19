package com.sami.app.automation.provider;

import com.sami.app.automation.spi.ActionContext;
import com.sami.app.automation.spi.ActionProvider;
import com.sami.app.automation.spi.ActionResult;
import com.sami.app.automation.spi.AutomationContext;
import com.sami.app.crm.service.CustomerEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Appends an entry to a customer's 360° timeline through the CRM's public
 * integration service. Demonstrates the intended direction of coupling: the
 * automation module consumes a business service; CRM knows nothing about
 * automation. The event is recorded with {@code sourceModule = "automation"} so
 * loop-guards can recognise automation-originated activity.
 *
 * <p>Config: {@code {"eventType","title","customerId?"}}. When {@code customerId}
 * is omitted it is taken from the trigger context (a CRM customer event).
 */
@Component
@RequiredArgsConstructor
public class CustomerTimelineActionProvider implements ActionProvider {

    private final CustomerEventService customerEventService;

    @Override
    public String type() {
        return "customer.timeline";
    }

    @Override
    public String label() {
        return "Add customer timeline entry";
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Long customerId = resolveCustomerId(context);
        if (customerId == null) {
            return ActionResult.fail("customer.timeline: no customerId in config or trigger context");
        }
        String eventType = context.config().getOrDefault("eventType", "AUTOMATION").toString();
        String title = context.configString("title");
        if (title == null || title.isBlank()) {
            title = "Automation: " + context.trigger().triggerType();
        }
        customerEventService.record(customerId, eventType, title, context.trigger().data(), "automation");
        return ActionResult.ok(title, Map.of("customerId", customerId));
    }

    private Long resolveCustomerId(ActionContext context) {
        Object configured = context.config().get("customerId");
        if (configured instanceof Number n) {
            return n.longValue();
        }
        if (configured instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        AutomationContext trigger = context.trigger();
        return "customer".equals(trigger.entityType()) ? trigger.entityId() : null;
    }

    @Override
    public String validate(Map<String, Object> config) {
        Object type = config.get("eventType");
        return (type != null && type.toString().isBlank()) ? "customer.timeline: 'eventType' must not be blank" : null;
    }
}
