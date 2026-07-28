package com.sami.app.files.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves {@code file_processors.handler_key} to its bean. */
@Component
public class FileProcessorRegistry {

    private final TreeMap<String, FileProcessor> byKey;

    public FileProcessorRegistry(List<FileProcessor> processors) {
        this.byKey = processors.stream().collect(Collectors.toMap(
                FileProcessor::key, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<FileProcessor> find(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    public List<FileProcessor> all() {
        return List.copyOf(byKey.values());
    }

    public List<String> keys() {
        return List.copyOf(byKey.keySet());
    }
}
