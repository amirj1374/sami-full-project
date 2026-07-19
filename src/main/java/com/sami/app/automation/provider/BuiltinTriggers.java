package com.sami.app.automation.provider;

import com.sami.app.automation.spi.TriggerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Built-in trigger descriptors for discovery/Ues. Trigger keys are open by
 * design — a rule may bind an exact key ({@code crm.customer.CREATED}), a
 * per-entity wildcard ({@code crm.customer.*}), or a future webhook/AI key that
 * needs no code change. These descriptors simply advertise the common ones.
 */
@Configuration
public class BuiltinTriggers {

    @Bean
    TriggerProvider manualTrigger() {
        return new SimpleTrigger("manual", "Manual execution", "MANUAL");
    }

    @Bean
    TriggerProvider scheduledTrigger() {
        return new SimpleTrigger("scheduled", "Scheduled time", "SCHEDULE");
    }

    @Bean
    TriggerProvider webhookTrigger() {
        return new SimpleTrigger("webhook", "Inbound webhook", "WEBHOOK");
    }

    @Bean
    TriggerProvider customerEventTrigger() {
        return new SimpleTrigger("crm.customer.*", "Customer event (any)", "EVENT");
    }

    @Bean
    TriggerProvider purchaseEventTrigger() {
        return new SimpleTrigger("purchasing.purchase.*", "Purchase event (any)", "EVENT");
    }

    @Bean
    TriggerProvider supplierEventTrigger() {
        return new SimpleTrigger("supplier.supplier.*", "Supplier event (any)", "EVENT");
    }

    @Bean
    TriggerProvider dashboardEventTrigger() {
        return new SimpleTrigger("dashboard.*", "Dashboard/KPI event (any)", "EVENT");
    }
}
