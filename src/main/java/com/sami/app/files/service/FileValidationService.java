package com.sami.app.files.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.files.FilesProperties;
import com.sami.app.files.domain.FileCategory;
import com.sami.app.files.domain.StorageProviderConfig;
import com.sami.app.files.spi.StorageProviderHandler;
import com.sami.app.files.spi.StorageProviderRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Every gate an upload must pass, driven entirely by {@code file_categories}
 * configuration rather than hardcoded rules — which is what
 * {@code ImageUploads.validated} could not express.
 */
@Service
@RequiredArgsConstructor
public class FileValidationService {

    private final FilesProperties properties;
    private final StorageProviderRegistry providerRegistry;

    public void validateUpload(FileCategory category, String filename, String contentType, long sizeBytes) {
        if (filename == null || filename.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A filename is required");
        }
        if (sizeBytes <= 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "The uploaded file is empty");
        }
        if (sizeBytes > properties.maxUploadBytes()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "File exceeds the maximum upload size of %d MB"
                            .formatted(properties.maxUploadBytes() / (1024 * 1024)));
        }
        if (category.getMaxBytes() != null && sizeBytes > category.getMaxBytes()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "File exceeds the %s category limit of %d MB"
                            .formatted(category.getName(), category.getMaxBytes() / (1024 * 1024)));
        }

        // An empty allow-list means "any", so a category can stay permissive.
        if (!category.getAllowedMimeTypes().isEmpty()
                && (contentType == null || !category.getAllowedMimeTypes().contains(contentType))) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Content type %s is not allowed for category %s. Allowed: %s"
                            .formatted(contentType, category.getName(),
                                    String.join(", ", category.getAllowedMimeTypes())));
        }

        String extension = extensionOf(filename);
        if (!category.getAllowedExtensions().isEmpty()
                && (extension == null || !category.getAllowedExtensions().contains(extension))) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Extension .%s is not allowed for category %s. Allowed: %s"
                            .formatted(extension, category.getName(),
                                    String.join(", ", category.getAllowedExtensions())));
        }
    }

    /**
     * A provider must be enabled, have a registered handler bean and report
     * itself reachable. Checked before bytes are accepted, so an unavailable
     * provider fails the request instead of stranding a half-written upload.
     */
    public StorageProviderHandler requireAvailable(StorageProviderConfig provider) {
        if (!provider.isEnabled()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Storage provider %s is disabled".formatted(provider.getCode()));
        }
        StorageProviderHandler handler = providerRegistry.find(provider.getHandlerKey())
                .orElseThrow(() -> new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                        "No storage handler is registered for '%s'. Provider %s cannot be used."
                                .formatted(provider.getHandlerKey(), provider.getCode())));
        if (!handler.available(provider.getConfig())) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                    "Storage provider %s is currently unavailable".formatted(provider.getCode()));
        }
        return handler;
    }

    public void verifyChecksum(String expected, String actual) {
        if (expected != null && !expected.isBlank() && !expected.equalsIgnoreCase(actual)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Checksum mismatch: the upload was corrupted in transit");
        }
    }

    public static String extensionOf(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
