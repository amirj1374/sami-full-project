package com.sami.app.dashboard.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Indexes every {@link KpiCalculationProvider} bean by its method key. */
@Component
public class KpiCalculationRegistry {

    private final Map<String, KpiCalculationProvider> byMethod;

    public KpiCalculationRegistry(List<KpiCalculationProvider> providers) {
        this.byMethod = providers.stream().collect(Collectors.toMap(
                KpiCalculationProvider::method, Function.identity(), (a, b) -> a));
    }

    public Optional<KpiCalculationProvider> find(String method) {
        return Optional.ofNullable(byMethod.get(method));
    }
}
