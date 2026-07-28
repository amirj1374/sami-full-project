package com.sami.app.automation.spi;

/**
 * Extension point for automation actions. A business capability is exposed to
 * the engine by publishing one bean per action type; the engine resolves an
 * {@code AutomationAction.actionType} to its provider through
 * {@link ActionProviderRegistry} and never needs to change when new actions are
 * added (Create Record, Send Notification, Generate Invoice, Execute API, …).
 */
public interface ActionProvider {

    /** Unique action-type key matching {@code automation_actions.action_type}. */
    String type();

    /** Human-readable label for configuration UIs. */
    String label();

    /** Runs the action. Implementations must be side-effect-safe on retry. */
    ActionResult execute(ActionContext context);

    /**
     * Optional cheap validation of a step's config before a rule is saved.
     *
     * @return null if valid, otherwise a human-readable error message
     */
    default String validate(java.util.Map<String, Object> config) {
        return null;
    }
}
