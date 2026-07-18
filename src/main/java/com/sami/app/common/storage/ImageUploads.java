package com.sami.app.common.storage;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Shared validation for image uploads against the configured storage limits. */
public final class ImageUploads {

    private ImageUploads() {
    }

    /** Validates presence, size and content type; returns the bytes. */
    public static byte[] validated(MultipartFile file, StorageProperties properties) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "An image file is required");
        }
        if (file.getSize() > properties.maxImageSizeBytes()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Image must be at most %d KB".formatted(properties.maxImageSizeBytes() / 1024));
        }
        String contentType = file.getContentType();
        if (contentType == null || !properties.allowedImageTypes().contains(contentType)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Allowed image types: " + String.join(", ", properties.allowedImageTypes()));
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Could not read the uploaded file");
        }
    }
}
