package com.sami.app.dataquality.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Indexes every {@link DuplicateMatcher} plugin by its strategy key. */
@Component
public class DuplicateMatcherRegistry {

    private final TreeMap<String, DuplicateMatcher> byStrategy;

    public DuplicateMatcherRegistry(List<DuplicateMatcher> matchers) {
        this.byStrategy = matchers.stream().collect(Collectors.toMap(
                DuplicateMatcher::strategy, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<DuplicateMatcher> find(String strategy) {
        return Optional.ofNullable(byStrategy.get(strategy));
    }

    public List<DuplicateMatcher> all() {
        return List.copyOf(byStrategy.values());
    }
}
