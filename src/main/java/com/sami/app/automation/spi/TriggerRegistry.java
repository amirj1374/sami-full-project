package com.sami.app.automation.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Indexes every {@link TriggerProvider} bean by its {@link TriggerProvider#type()}. */
@Component
public class TriggerRegistry {

    private final TreeMap<String, TriggerProvider> byType;

    public TriggerRegistry(List<TriggerProvider> providers) {
        this.byType = providers.stream().collect(Collectors.toMap(
                TriggerProvider::type, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<TriggerProvider> find(String type) {
        return Optional.ofNullable(byType.get(type));
    }

    public List<TriggerProvider> all() {
        return List.copyOf(byType.values());
    }

    public boolean isKnown(String type) {
        return byType.containsKey(type);
    }
}
