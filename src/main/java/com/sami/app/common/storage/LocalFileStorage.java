package com.sami.app.common.storage;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link FileStorage} backed by the local filesystem under
 * {@code app.storage.base-path}. Selected via {@code app.storage.provider=local}
 * (the default); a MinIO/S3 implementation registers under its own provider value.
 *
 * <p>The content type is encoded in the file extension so no metadata store is
 * needed. Keys are validated to stay inside the base directory.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private static final Map<String, String> TYPE_TO_EXT = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private static final Map<String, String> EXT_TO_TYPE = Map.of(
            "jpg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif");

    private final Path baseDir;

    public LocalFileStorage(StorageProperties properties) {
        this.baseDir = Path.of(properties.basePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage directory " + baseDir, e);
        }
    }

    @Override
    public String store(String keyHint, byte[] content, String contentType) {
        String ext = TYPE_TO_EXT.getOrDefault(contentType, "bin");
        String key = keyHint + "/" + UUID.randomUUID() + "." + ext;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            log.error("Failed to store file {}", key, e);
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Could not store file");
        }
        return key;
    }

    @Override
    public Optional<StoredFile> load(String key) {
        Path path = resolve(key);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            String ext = key.substring(key.lastIndexOf('.') + 1);
            String contentType = EXT_TO_TYPE.getOrDefault(ext, "application/octet-stream");
            return Optional.of(new StoredFile(Files.readAllBytes(path), contentType));
        } catch (IOException e) {
            log.error("Failed to read file {}", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            // Deletion is best-effort; an orphaned file is harmless.
            log.warn("Failed to delete file {}", key, e);
        }
    }

    /** Resolves a key inside the base directory, rejecting traversal attempts. */
    private Path resolve(String key) {
        Path resolved = baseDir.resolve(key).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Invalid storage key");
        }
        return resolved;
    }
}
