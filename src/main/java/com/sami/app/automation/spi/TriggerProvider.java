package com.sami.app.automation.spi;

/**
 * Extension point describing a kind of automation trigger. A provider declares a
 * canonical {@link #type()} key (or a wildcard prefix ending in {@code .*}) that
 * a rule's {@code triggerType} can bind to; the engine matches dispatched
 * contexts against rules by this key. New trigger sources — webhooks, external
 * systems, AI signals — register as beans without touching the engine.
 *
 * <p>Providers are metadata/descriptors: the actual firing is done by event
 * bridges, the scheduler, or the manual-run API, all of which build an
 * {@link AutomationContext} whose {@code triggerType} the engine resolves here.
 */
public interface TriggerProvider {

    /** Canonical trigger key, e.g. {@code crm.customer.CREATED}, {@code manual}. */
    String type();

    /** Human-readable label for configuration UIs. */
    String label();

    /** Coarse grouping: {@code EVENT}, {@code SCHEDULE}, {@code MANUAL}, {@code WEBHOOK}. */
    String category();
}
