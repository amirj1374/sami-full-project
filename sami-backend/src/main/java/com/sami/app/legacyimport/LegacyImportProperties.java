package com.sami.app.legacyimport;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.legacy-import")
public record LegacyImportProperties(long maxUploadBytes, int maxFiles, long maxExtractedBytes,
                                     long maxSingleFileBytes, int importChunkSize, String unrarExecutable) {
    public LegacyImportProperties {
        maxUploadBytes = maxUploadBytes <= 0 ? 104_857_600L : maxUploadBytes;
        maxFiles = maxFiles <= 0 ? 2_000 : maxFiles;
        maxExtractedBytes = maxExtractedBytes <= 0 ? 536_870_912L : maxExtractedBytes;
        maxSingleFileBytes = maxSingleFileBytes <= 0 ? 134_217_728L : maxSingleFileBytes;
        importChunkSize = importChunkSize <= 0 ? 500 : importChunkSize;
        unrarExecutable = unrarExecutable == null || unrarExecutable.isBlank() ? "unrar" : unrarExecutable;
    }
}
