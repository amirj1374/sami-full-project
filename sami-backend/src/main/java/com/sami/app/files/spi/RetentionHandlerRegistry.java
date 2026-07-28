package com.sami.app.files.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves {@code retention_policies.action_on_expiry} to its handler bean. */
@Component
public class RetentionHandlerRegistry {

    private final TreeMap<String, RetentionHandler> byAction;

    public RetentionHandlerRegistry(List<RetentionHandler> handlers) {
        this.byAction = handlers.stream().collect(Collectors.toMap(
                RetentionHandler::action, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<RetentionHandler> find(String action) {
        return Optional.ofNullable(byAction.get(action));
    }

    public List<String> actions() {
        return List.copyOf(byAction.keySet());
    }
}
