package com.sami.app.licensing.spi;

/**
 * Decides what remains permitted once a licence has lapsed. Selected by the
 * licence's configured {@code expiry_behaviors.code}, so new behaviours are new
 * beans — the gate never changes.
 *
 * <p>Handlers are pure decisions: they gate access only and must never mutate
 * business data, which is how expiry is guaranteed to be non-destructive.
 */
public interface ExpiryBehaviorHandler {

    /** Matches {@code expiry_behaviors.code}. */
    String code();

    /**
     * Whether a lapsed licence still permits the given feature.
     *
     * @param featureCode the feature being gated
     * @param coreFeature whether the feature is flagged as core
     * @param withinGrace whether the licence is still inside its grace window
     */
    boolean permits(String featureCode, boolean coreFeature, boolean withinGrace);

    /** Whether write operations remain permitted (false ⇒ read-only mode). */
    default boolean permitsWrites(boolean withinGrace) {
        return withinGrace;
    }
}
