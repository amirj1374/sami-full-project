package com.sami.app.automation.provider;

import com.sami.app.automation.spi.TriggerProvider;

/** Immutable {@link TriggerProvider} descriptor used to register built-in triggers. */
public record SimpleTrigger(String type, String label, String category) implements TriggerProvider {
}
