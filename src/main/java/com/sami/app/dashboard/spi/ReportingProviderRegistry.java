package com.sami.app.dashboard.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Discovers every {@link ReportingProvider} bean on the classpath and indexes it
 * by key. New business modules light up their data source simply by registering
 * a provider bean — nothing here changes.
 */
@Component
public class ReportingProviderRegistry {

    private final Map<String, ReportingProvider> byKey;

    public ReportingProviderRegistry(List<ReportingProvider> providers) {
        this.byKey = providers.stream().collect(java.util.stream.Collectors.toMap(
                ReportingProvider::key, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<ReportingProvider> find(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    public boolean isAvailable(String key) {
        return byKey.containsKey(key);
    }

    /** Provider keys currently registered (used to report data-source availability). */
    public Set<String> availableKeys() {
        return byKey.keySet();
    }
}
