package com.sami.app.files.provider;

import com.sami.app.files.spi.FileContent;
import com.sami.app.files.spi.FileContents;
import com.sami.app.files.spi.FileProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Generates a bounded-box thumbnail for raster images, preserving aspect ratio.
 * JDK-only ({@code javax.imageio} + Java2D) — no third-party imaging dependency.
 *
 * <p>Non-image content is reported as skipped rather than failed, so the same
 * processor can be configured on a mixed category without noise.
 */
@Slf4j
@Component
public class ThumbnailFileProcessor implements FileProcessor {

    private static final int DEFAULT_MAX = 320;

    @Override
    public String key() {
        return "thumbnail";
    }

    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    @Override
    public Result process(FileContent content, Map<String, Object> config) {
        int maxWidth = intConfig(config, "width", DEFAULT_MAX);
        int maxHeight = intConfig(config, "height", DEFAULT_MAX);

        try (InputStream in = content.openStream()) {
            BufferedImage source = ImageIO.read(in);
            if (source == null) {
                return Result.none();
            }

            double scale = Math.min(
                    (double) maxWidth / source.getWidth(),
                    (double) maxHeight / source.getHeight());
            // Never upscale: a 100px avatar should not become a 320px blur.
            if (scale >= 1.0) {
                scale = 1.0;
            }
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

            BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = target.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(source, 0, 0, width, height, null);
            } finally {
                g.dispose();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(target, "jpg", out);
            byte[] bytes = out.toByteArray();

            return new Result(Map.of(), List.of(new Derivative(
                    "thumbnail",
                    FileContents.ofBytes(bytes, "image/jpeg", "thumbnail.jpg"),
                    null, width, height, null, null)), null);
        } catch (Exception e) {
            log.debug("Thumbnail generation failed for {}: {}", content.filename(), e.toString());
            return Result.none();
        }
    }

    private int intConfig(Map<String, Object> config, String key, int fallback) {
        Object value = config == null ? null : config.get(key);
        return value instanceof Number n ? n.intValue() : fallback;
    }
}
