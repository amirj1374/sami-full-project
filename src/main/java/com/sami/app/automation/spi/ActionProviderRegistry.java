package com.sami.app.automation.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Indexes every {@link ActionProvider} bean by its {@link ActionProvider#type()}. */
@Component
public class ActionProviderRegistry {

    private final TreeMap<String, ActionProvider> byType;

    public ActionProviderRegistry(List<ActionProvider> providers) {
        this.byType = providers.stream().collect(Collectors.toMap(
                ActionProvider::type, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<ActionProvider> find(String type) {
        return Optional.ofNullable(byType.get(type));
    }

    public List<ActionProvider> all() {
        return List.copyOf(byType.values());
    }
}
