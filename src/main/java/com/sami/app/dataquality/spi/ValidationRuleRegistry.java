package com.sami.app.dataquality.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Indexes every {@link ValidationRule} plugin by its type key. */
@Component
public class ValidationRuleRegistry {

    private final TreeMap<String, ValidationRule> byType;

    public ValidationRuleRegistry(List<ValidationRule> rules) {
        this.byType = rules.stream().collect(Collectors.toMap(
                ValidationRule::type, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<ValidationRule> find(String type) {
        return Optional.ofNullable(byType.get(type));
    }

    public List<ValidationRule> all() {
        return List.copyOf(byType.values());
    }
}
