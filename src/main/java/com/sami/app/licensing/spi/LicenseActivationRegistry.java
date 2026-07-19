package com.sami.app.licensing.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Indexes every {@link LicenseActivationProvider} bean by its mode. */
@Component
public class LicenseActivationRegistry {

    private final TreeMap<String, LicenseActivationProvider> byMode;

    public LicenseActivationRegistry(List<LicenseActivationProvider> providers) {
        this.byMode = providers.stream().collect(Collectors.toMap(
                LicenseActivationProvider::mode, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<LicenseActivationProvider> find(String mode) {
        return Optional.ofNullable(byMode.get(mode));
    }

    public List<String> modes() {
        return List.copyOf(byMode.keySet());
    }
}
