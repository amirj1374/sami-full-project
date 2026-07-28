package com.sami.app.files;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Module-level configuration. Storage-provider specifics live in
 * {@code storage_providers.config}, not here — this is only what the module
 * itself needs before it can read the database.
 */
@ConfigurationProperties(prefix = "app.files")
public record FilesProperties(
        String basePath,
        String stagingPath,
        long maxUploadBytes,
        Duration uploadSessionTtl,
        Duration signedUrlTtl,
        boolean quotasEnforced
) {

    public FilesProperties {
        basePath = basePath == null ? "./data/files" : basePath;
        stagingPath = stagingPath == null ? "./data/files-staging" : stagingPath;
        maxUploadBytes = maxUploadBytes <= 0 ? 524_288_000L : maxUploadBytes;
        uploadSessionTtl = uploadSessionTtl == null ? Duration.ofHours(6) : uploadSessionTtl;
        signedUrlTtl = signedUrlTtl == null ? Duration.ofMinutes(5) : signedUrlTtl;
    }
}
