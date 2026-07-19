package com.sami.app.files.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves {@code storage_providers.handler_key} to its bean. */
@Component
public class StorageProviderRegistry {

    private final TreeMap<String, StorageProviderHandler> byKey;

    public StorageProviderRegistry(List<StorageProviderHandler> handlers) {
        this.byKey = handlers.stream().collect(Collectors.toMap(
                StorageProviderHandler::key, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<StorageProviderHandler> find(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    public List<StorageProviderHandler> all() {
        return List.copyOf(byKey.values());
    }

    public List<String> keys() {
        return List.copyOf(byKey.keySet());
    }
}
