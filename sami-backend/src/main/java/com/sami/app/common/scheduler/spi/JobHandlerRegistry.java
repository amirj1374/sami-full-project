package com.sami.app.common.scheduler.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves {@code scheduled_jobs.handler_key} to its bean. */
@Component
public class JobHandlerRegistry {

    private final TreeMap<String, JobHandler> byKey;

    public JobHandlerRegistry(List<JobHandler> handlers) {
        this.byKey = handlers.stream().collect(Collectors.toMap(
                JobHandler::key, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<JobHandler> find(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    public List<JobHandler> all() {
        return List.copyOf(byKey.values());
    }

    public List<String> keys() {
        return List.copyOf(byKey.keySet());
    }
}
