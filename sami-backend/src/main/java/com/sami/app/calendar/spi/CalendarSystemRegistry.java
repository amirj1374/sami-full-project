package com.sami.app.calendar.spi;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves {@code calendar_systems.handler_key} to its bean. */
@Component
public class CalendarSystemRegistry {

    private final TreeMap<String, CalendarSystem> byKey;

    public CalendarSystemRegistry(List<CalendarSystem> systems) {
        this.byKey = systems.stream().collect(Collectors.toMap(
                CalendarSystem::key, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<CalendarSystem> find(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    /**
     * @throws ApiException when the configuration names a handler with no bean
     *                      — a date converted by the wrong chronology is worse
     *                      than a failed request
     */
    public CalendarSystem require(String key) {
        return find(key).orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                "No calendar system is registered for handler key '%s'".formatted(key)));
    }

    public List<String> keys() {
        return List.copyOf(byKey.keySet());
    }
}
