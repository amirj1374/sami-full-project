package com.sami.app.comm.spi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves {@code comm_providers.handler_key} to its bean. */
@Slf4j
@Component
public class CommProviderRegistry {

    private final TreeMap<String, CommProviderHandler> byKey;

    public CommProviderRegistry(List<CommProviderHandler> handlers) {
        this.byKey = handlers.stream().collect(Collectors.toMap(
                CommProviderHandler::key, Function.identity(), (a, b) -> a, TreeMap::new));
        log.info("Communication provider handlers registered: {}", byKey.keySet());
    }

    public Optional<CommProviderHandler> find(String key) {
        return key == null ? Optional.empty() : Optional.ofNullable(byKey.get(key));
    }

    public List<String> keys() {
        return List.copyOf(byKey.keySet());
    }
}
