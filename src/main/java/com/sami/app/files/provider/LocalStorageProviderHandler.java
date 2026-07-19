package com.sami.app.files.provider;

import com.sami.app.files.FilesProperties;
import com.sami.app.files.spi.FileContent;
import com.sami.app.files.spi.FileContents;
import com.sami.app.files.spi.StorageProviderHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Local-filesystem storage.
 *
 * <p>Fixes three defects of the {@code LocalFileStorage} it supersedes:
 * content type is no longer encoded in the file extension (it is a column), any
 * content type is supported rather than four image types, and store/load are
 * streamed instead of materialising the whole file in heap.
 *
 * <p>Keys are date-sharded ({@code yyyy/MM/dd/uuid}) so no directory accumulates
 * an unbounded number of entries.
 */
@Slf4j
@Component
public class LocalStorageProviderHandler implements StorageProviderHandler {

    private static final DateTimeFormatter SHARD = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Path baseDir;

    public LocalStorageProviderHandler(FilesProperties properties) {
        this.baseDir = Path.of(properties.basePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create file storage directory " + baseDir, e);
        }
    }

    @Override
    public String key() {
        return "local";
    }

    @Override
    public String store(String keyHint, FileContent content, Map<String, Object> config) throws IOException {
        String safeHint = keyHint == null || keyHint.isBlank() ? "misc" : sanitize(keyHint);
        String storageKey = safeHint + "/" + LocalDate.now().format(SHARD) + "/" + UUID.randomUUID();
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());
        try (InputStream in = content.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return storageKey;
    }

    @Override
    public Optional<FileContent> load(String storageKey, Map<String, Object> config) {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.of(FileContents.ofPath(path, null, path.getFileName().toString()));
    }

    @Override
    public void delete(String storageKey, Map<String, Object> config) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    @Override
    public boolean exists(String storageKey, Map<String, Object> config) {
        return Files.isRegularFile(resolve(storageKey));
    }

    @Override
    public boolean available(Map<String, Object> config) {
        return Files.isDirectory(baseDir) && Files.isWritable(baseDir);
    }

    /** Rejects traversal: the resolved path must stay inside the base directory. */
    private Path resolve(String storageKey) {
        Path path = baseDir.resolve(storageKey).normalize();
        if (!path.startsWith(baseDir)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }

    private String sanitize(String hint) {
        return hint.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
