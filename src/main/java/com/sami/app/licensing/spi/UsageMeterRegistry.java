package com.sami.app.licensing.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Indexes every {@link UsageMeterProvider} bean by the limit type it measures. */
@Component
public class UsageMeterRegistry {

    private final TreeMap<String, UsageMeterProvider> byType;

    public UsageMeterRegistry(List<UsageMeterProvider> providers) {
        this.byType = providers.stream().collect(Collectors.toMap(
                UsageMeterProvider::limitType, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<UsageMeterProvider> find(String limitType) {
        return Optional.ofNullable(byType.get(limitType));
    }

    public List<String> meteredTypes() {
        return List.copyOf(byType.keySet());
    }
}
