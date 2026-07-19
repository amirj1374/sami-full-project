package com.sami.app;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.files.FilesProperties;
import com.sami.app.files.domain.FileCategory;
import com.sami.app.files.domain.StorageProviderConfig;
import com.sami.app.files.service.FileValidationService;
import com.sami.app.files.spi.StorageProviderRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FileValidationService}. Pure: the service reads category
 * configuration and properties only, so both are built directly.
 *
 * <p>These cover the rules that {@code ImageUploads} could not express — per
 * category size, MIME and extension allow-lists — plus the storage-availability
 * gate that stops an upload before bytes are accepted.
 */
class FileValidationTest {

    private final FilesProperties properties = new FilesProperties(
            "./data/files", "./data/staging", 10_485_760L,
            Duration.ofHours(6), Duration.ofMinutes(5), false);

    /** No handler beans registered — models a provider whose implementation is absent. */
    private final StorageProviderRegistry emptyRegistry = new StorageProviderRegistry(List.of());

    private final FileValidationService validation =
            new FileValidationService(properties, emptyRegistry);

    private static FileCategory category(Long maxBytes, List<String> mimes, List<String> exts) {
        return FileCategory.builder()
                .code("image").name("Image")
                .maxBytes(maxBytes)
                .allowedMimeTypes(mimes)
                .allowedExtensions(exts)
                .versioningEnabled(true).dedupeEnabled(true)
                .processors(List.of())
                .build();
    }

    @Test
    void acceptsAFileMatchingItsCategory() {
        FileCategory image = category(1_000_000L, List.of("image/png"), List.of("png"));

        assertThatCode(() -> validation.validateUpload(image, "device.png", "image/png", 5_000))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAFileOverTheCategoryLimit() {
        FileCategory image = category(1_000L, List.of(), List.of());

        assertThatThrownBy(() -> validation.validateUpload(image, "big.png", "image/png", 2_000))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("category limit");
    }

    @Test
    void rejectsAFileOverTheGlobalUploadLimitEvenWhenTheCategoryAllowsIt() {
        // Category is unbounded; the module-level ceiling must still apply.
        FileCategory unbounded = category(null, List.of(), List.of());

        assertThatThrownBy(() ->
                validation.validateUpload(unbounded, "huge.bin", "application/octet-stream",
                        properties.maxUploadBytes() + 1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("maximum upload size");
    }

    @Test
    void rejectsADisallowedMimeType() {
        FileCategory image = category(null, List.of("image/png", "image/jpeg"), List.of());

        assertThatThrownBy(() ->
                validation.validateUpload(image, "script.sh", "application/x-sh", 100))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not allowed for category");
    }

    @Test
    void rejectsADisallowedExtension() {
        FileCategory image = category(null, List.of(), List.of("png", "jpg"));

        assertThatThrownBy(() ->
                validation.validateUpload(image, "payload.exe", "image/png", 100))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining(".exe is not allowed");
    }

    @Test
    void anEmptyAllowListMeansAnyTypeIsAccepted() {
        FileCategory permissive = category(null, List.of(), List.of());

        assertThatCode(() ->
                validation.validateUpload(permissive, "anything.xyz", "application/octet-stream", 10))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAnEmptyOrUnnamedUpload() {
        FileCategory any = category(null, List.of(), List.of());

        assertThatThrownBy(() -> validation.validateUpload(any, "empty.png", "image/png", 0))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("empty");

        assertThatThrownBy(() -> validation.validateUpload(any, "  ", "image/png", 10))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("filename is required");
    }

    @Test
    void checksumMismatchIsRejectedAndMatchIsCaseInsensitive() {
        assertThatThrownBy(() -> validation.verifyChecksum("aaaa", "bbbb"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("corrupted in transit");

        assertThatCode(() -> validation.verifyChecksum("ABCD", "abcd")).doesNotThrowAnyException();
        // No declared checksum means the client opted out of integrity checking.
        assertThatCode(() -> validation.verifyChecksum(null, "abcd")).doesNotThrowAnyException();
    }

    @Test
    void refusesAProviderWhoseHandlerBeanIsMissing() {
        // The catalogue seeds object/cloud/archive providers with no implementation.
        // Enabling one must fail loudly rather than silently dropping the upload.
        StorageProviderConfig object = StorageProviderConfig.builder()
                .code("object").name("Object Storage").handlerKey("object")
                .config(Map.of()).enabled(true).build();

        assertThatThrownBy(() -> validation.requireAvailable(object))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No storage handler is registered");
    }

    @Test
    void refusesADisabledProvider() {
        StorageProviderConfig disabled = StorageProviderConfig.builder()
                .code("cold").name("Cold Storage").handlerKey("cold")
                .config(Map.of()).enabled(false).build();

        ApiException error = org.junit.jupiter.api.Assertions.assertThrows(
                ApiException.class, () -> validation.requireAvailable(disabled));

        assertThat(error.getErrorCode()).isEqualTo(ErrorCode.OPERATION_NOT_ALLOWED);
        assertThat(error.getMessage()).contains("is disabled");
    }

    @Test
    void extractsExtensionsCaseInsensitivelyAndToleratesNone() {
        assertThat(FileValidationService.extensionOf("Photo.JPG")).isEqualTo("jpg");
        assertThat(FileValidationService.extensionOf("archive.tar.gz")).isEqualTo("gz");
        assertThat(FileValidationService.extensionOf("noextension")).isNull();
        assertThat(FileValidationService.extensionOf("trailing.")).isNull();
        assertThat(FileValidationService.extensionOf(null)).isNull();
    }
}
