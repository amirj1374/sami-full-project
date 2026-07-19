package com.sami.app.licensing.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Indexes every {@link ExpiryBehaviorHandler} bean by its code. */
@Component
public class ExpiryBehaviorRegistry {

    private final TreeMap<String, ExpiryBehaviorHandler> byCode;

    public ExpiryBehaviorRegistry(List<ExpiryBehaviorHandler> handlers) {
        this.byCode = handlers.stream().collect(Collectors.toMap(
                ExpiryBehaviorHandler::code, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<ExpiryBehaviorHandler> find(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    public List<String> codes() {
        return List.copyOf(byCode.keySet());
    }
}
