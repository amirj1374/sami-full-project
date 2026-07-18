package com.sami.app.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Storage configuration, bound from {@code app.storage.*}.
 *
 * @param provider            active provider; {@code local} today, {@code minio} /
 *                            {@code s3} once those implementations exist
 * @param basePath            root directory for the local provider
 * @param maxImageSizeBytes   upload size limit for profile images
 * @param allowedImageTypes   accepted content types for profile images
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String provider,
        String basePath,
        long maxImageSizeBytes,
        List<String> allowedImageTypes
) {
}
