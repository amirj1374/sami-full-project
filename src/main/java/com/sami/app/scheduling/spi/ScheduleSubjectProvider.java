package com.sami.app.scheduling.spi;

import java.util.Optional;

/**
 * Resolves the business record an appointment is about.
 *
 * <p>This is the seam that keeps the scheduler free of direct dependencies on
 * business modules. An appointment type names a provider key
 * ({@code appointment_types.subject_provider_key}); when the repair module
 * ships it registers a bean with key {@code repair} and repair appointments
 * start resolving. Until then the appointment books normally and simply
 * carries no linked record — which is why REPAIR_INTAKE can be seeded today.
 *
 * <p>Implementations must not throw for an unknown id; return empty instead.
 */
public interface ScheduleSubjectProvider {

    /** Matches {@code appointment_types.subject_provider_key}. */
    String key();

    /** Entity type this provider resolves, e.g. {@code repair_order}. */
    String entityType();

    /** Human-readable summary shown on the appointment, if the record exists. */
    Optional<ScheduleSubject> resolve(Long entityId);

    /**
     * Whether the subject is currently in a state that permits scheduling.
     * A repair order already collected should not accept a new delivery slot.
     */
    default boolean isSchedulable(Long entityId) {
        return resolve(entityId).isPresent();
    }
}
