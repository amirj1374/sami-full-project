package com.sami.app.files.spi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Factories for the two {@link FileContent} shapes this module needs. */
public final class FileContents {

    private FileContents() {
    }

    /** In-memory content. Use only for small payloads (derivatives, thumbnails). */
    public static FileContent ofBytes(byte[] bytes, String contentType, String filename) {
        return new FileContent() {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public long size() {
                return bytes.length;
            }

            @Override
            public String contentType() {
                return contentType;
            }

            @Override
            public String filename() {
                return filename;
            }
        };
    }

    /**
     * File-backed content. Re-readable, so processors and the storage provider
     * can each stream it without buffering the whole payload.
     */
    public static FileContent ofPath(Path path, String contentType, String filename) {
        return new FileContent() {
            @Override
            public InputStream openStream() throws IOException {
                return Files.newInputStream(path);
            }

            @Override
            public long size() {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return -1L;
                }
            }

            @Override
            public String contentType() {
                return contentType;
            }

            @Override
            public String filename() {
                return filename;
            }
        };
    }
}
