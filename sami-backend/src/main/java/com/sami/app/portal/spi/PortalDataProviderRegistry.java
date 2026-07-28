package com.sami.app.portal.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves {@code portal_widgets.provider_key} to its bean. */
@Component
public class PortalDataProviderRegistry {

    private final TreeMap<String, PortalDataProvider> byKey;

    public PortalDataProviderRegistry(List<PortalDataProvider> providers) {
        this.byKey = providers.stream().collect(Collectors.toMap(
                PortalDataProvider::key, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<PortalDataProvider> find(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    public List<String> keys() {
        return List.copyOf(byKey.keySet());
    }

    public List<PortalDataProvider> all() {
        return List.copyOf(byKey.values());
    }
}
