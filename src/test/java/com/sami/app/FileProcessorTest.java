package com.sami.app;

import com.sami.app.files.provider.MetadataFileProcessor;
import com.sami.app.files.provider.ThumbnailFileProcessor;
import com.sami.app.files.spi.FileContent;
import com.sami.app.files.spi.FileContents;
import com.sami.app.files.spi.FileProcessor;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the JDK-only file processors. Pure (no Spring context, no
 * database, no filesystem): both processors take a {@link FileContent} and
 * return a result, so images are synthesised in memory.
 */
class FileProcessorTest {

    private final ThumbnailFileProcessor thumbnailProcessor = new ThumbnailFileProcessor();
    private final MetadataFileProcessor metadataProcessor = new MetadataFileProcessor();

    private static FileContent png(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(38, 55, 95));
        g.fillRect(0, 0, width, height);
        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return FileContents.ofBytes(out.toByteArray(), "image/png", "test.png");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- Thumbnails ---------------------------------------------------------

    @Test
    void thumbnailFitsTheBoundingBoxAndPreservesAspectRatio() {
        FileProcessor.Result result = thumbnailProcessor.process(
                png(800, 600), Map.of("width", 320, "height", 320));

        assertThat(result.derivatives()).hasSize(1);
        FileProcessor.Derivative thumb = result.derivatives().get(0);
        assertThat(thumb.kind()).isEqualTo("thumbnail");
        // 800x600 scaled to fit 320x320 is 320x240, not a squashed 320x320.
        assertThat(thumb.width()).isEqualTo(320);
        assertThat(thumb.height()).isEqualTo(240);
        assertThat(thumb.content().size()).isPositive();
    }

    @Test
    void thumbnailUsesTheConfiguredBoundsRatherThanADefault() {
        FileProcessor.Result result = thumbnailProcessor.process(
                png(1000, 500), Map.of("width", 100, "height", 100));

        FileProcessor.Derivative thumb = result.derivatives().get(0);
        assertThat(thumb.width()).isEqualTo(100);
        assertThat(thumb.height()).isEqualTo(50);
    }

    @Test
    void thumbnailNeverUpscalesASmallImage() {
        // A 100x80 avatar must stay 100x80 rather than becoming a 320px blur.
        FileProcessor.Result result = thumbnailProcessor.process(
                png(100, 80), Map.of("width", 320, "height", 320));

        FileProcessor.Derivative thumb = result.derivatives().get(0);
        assertThat(thumb.width()).isEqualTo(100);
        assertThat(thumb.height()).isEqualTo(80);
    }

    @Test
    void thumbnailFallsBackToDefaultBoundsWhenConfigIsEmpty() {
        FileProcessor.Result result = thumbnailProcessor.process(png(1600, 1600), Map.of());

        FileProcessor.Derivative thumb = result.derivatives().get(0);
        assertThat(thumb.width()).isEqualTo(320);
        assertThat(thumb.height()).isEqualTo(320);
    }

    @Test
    void thumbnailOnlySupportsImages() {
        assertThat(thumbnailProcessor.supports("image/png")).isTrue();
        assertThat(thumbnailProcessor.supports("image/jpeg")).isTrue();
        assertThat(thumbnailProcessor.supports("application/pdf")).isFalse();
        assertThat(thumbnailProcessor.supports(null)).isFalse();
    }

    @Test
    void thumbnailReturnsNothingForContentThatIsNotADecodableImage() {
        // Must not throw: a corrupt upload is an expected edge case, and a
        // failed derivative may never fail the upload itself.
        FileContent corrupt = FileContents.ofBytes(
                "not an image".getBytes(), "image/png", "corrupt.png");

        FileProcessor.Result result = thumbnailProcessor.process(corrupt, Map.of());

        assertThat(result.derivatives()).isEmpty();
        assertThat(result.verdict()).isNull();
    }

    // ---- Metadata -----------------------------------------------------------

    @Test
    void metadataExtractsImageDimensions() {
        FileProcessor.Result result = metadataProcessor.process(png(640, 480), Map.of());

        assertThat(result.metadata())
                .containsEntry("width", 640)
                .containsEntry("height", 480)
                .containsEntry("resolution", "640x480")
                .containsEntry("mimeType", "image/png");
        assertThat(result.metadata().get("sizeBytes")).isNotNull();
    }

    @Test
    void metadataAppliesToEveryContentTypeButOnlySizesNonImages() {
        assertThat(metadataProcessor.supports("application/pdf")).isTrue();

        FileContent pdf = FileContents.ofBytes(
                "%PDF-1.4 fake".getBytes(), "application/pdf", "contract.pdf");
        FileProcessor.Result result = metadataProcessor.process(pdf, Map.of());

        assertThat(result.metadata()).containsEntry("mimeType", "application/pdf");
        assertThat(result.metadata()).doesNotContainKey("width");
    }

    @Test
    void metadataSurvivesACorruptImageWithoutThrowing() {
        FileContent corrupt = FileContents.ofBytes(
                new byte[]{1, 2, 3}, "image/jpeg", "corrupt.jpg");

        FileProcessor.Result result = metadataProcessor.process(corrupt, Map.of());

        assertThat(result.metadata()).containsEntry("mimeType", "image/jpeg");
        assertThat(result.metadata()).doesNotContainKey("width");
    }
}
