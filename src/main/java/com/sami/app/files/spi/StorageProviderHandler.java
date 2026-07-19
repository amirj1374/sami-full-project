package com.sami.app.files.spi;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Pluggable physical storage. One bean per storage technology; the
 * {@code storage_providers} row supplies the configuration, so a tenant may run
 * several instances of the same handler with different buckets.
 *
 * <p>Named {@code StorageProviderHandler} rather than {@code StorageProvider} to
 * avoid a simple-name clash with the {@code storage_providers} entity, which
 * caused a {@code ConflictingBeanDefinitionException} elsewhere in this codebase.
 *
 * <p>Keys returned by {@link #store} are opaque and provider-internal. They are
 * never exposed through the API and never derived from client input.
 */
public interface StorageProviderHandler {

    /** Matches {@code storage_providers.handler_key}. */
    String key();

    /** Stores the content and returns the opaque storage key. */
    String store(String keyHint, FileContent content, Map<String, Object> config) throws IOException;

    Optional<FileContent> load(String storageKey, Map<String, Object> config) throws IOException;

    void delete(String storageKey, Map<String, Object> config) throws IOException;

    boolean exists(String storageKey, Map<String, Object> config);

    /**
     * A short-lived direct URL, for providers that support it. Empty means the
     * caller must stream through the application instead.
     */
    default Optional<String> signedUrl(String storageKey, Duration ttl, Map<String, Object> config) {
        return Optional.empty();
    }

    /** Whether the backing store is reachable — surfaced by the health endpoint. */
    default boolean available(Map<String, Object> config) {
        return true;
    }
}
