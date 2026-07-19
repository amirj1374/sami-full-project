package com.sami.app.dataquality.spi;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Indexes {@link DuplicateCandidateProvider} beans by module + entity. */
@Component
public class DuplicateCandidateRegistry {

    private final Map<String, DuplicateCandidateProvider> byTarget = new HashMap<>();

    public DuplicateCandidateRegistry(List<DuplicateCandidateProvider> providers) {
        for (DuplicateCandidateProvider provider : providers) {
            byTarget.put(key(provider.moduleCode(), provider.entityCode()), provider);
        }
    }

    public Optional<DuplicateCandidateProvider> find(String moduleCode, String entityCode) {
        return Optional.ofNullable(byTarget.get(key(moduleCode, entityCode)));
    }

    public List<String> registeredTargets() {
        return List.copyOf(byTarget.keySet());
    }

    private String key(String moduleCode, String entityCode) {
        return moduleCode + ":" + entityCode;
    }
}
