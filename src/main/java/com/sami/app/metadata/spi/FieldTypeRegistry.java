package com.sami.app.metadata.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Indexes every {@link FieldTypeHandler} bean by its key. */
@Component
public class FieldTypeRegistry {

    private final TreeMap<String, FieldTypeHandler> byKey;

    public FieldTypeRegistry(List<FieldTypeHandler> handlers) {
        this.byKey = handlers.stream().collect(Collectors.toMap(
                FieldTypeHandler::key, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<FieldTypeHandler> find(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    public List<FieldTypeHandler> all() {
        return List.copyOf(byKey.values());
    }
}
