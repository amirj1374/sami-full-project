package com.sami.app.common.storage;

import java.util.Optional;

/**
 * Storage port for binary content (currently profile images).
 *
 * <p>Callers deal only in opaque keys — never paths or URLs — so the provider can
 * change (local disk today; MinIO or Amazon S3 tomorrow) by adding an
 * implementation and switching {@code app.storage.provider}, with zero changes to
 * calling code or stored data.
 */
public interface FileStorage {

    /**
     * Stores the content and returns the opaque key under which it can be
     * retrieved. {@code keyHint} namespaces the object (e.g. {@code avatars/42});
     * implementations must make the final key collision-free.
     */
    String store(String keyHint, byte[] content, String contentType);

    /** @return the stored file, or empty if the key is unknown. */
    Optional<StoredFile> load(String key);

    /** Removes the object if it exists (idempotent). */
    void delete(String key);

    /** A retrieved object: its bytes and the content type recorded at store time. */
    record StoredFile(byte[] content, String contentType) {
    }
}
