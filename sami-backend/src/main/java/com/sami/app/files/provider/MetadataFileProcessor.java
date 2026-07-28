package com.sami.app.files.provider;

import com.sami.app.files.spi.FileContent;
import com.sami.app.files.spi.FileProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Extracts intrinsic metadata. Uses only the JDK ({@code javax.imageio}), so it
 * adds no dependency and no licensing question.
 *
 * <p>Reads image dimensions without decoding the full raster — important for the
 * large repair-photo sets this ERP handles.
 */
@Slf4j
@Component
public class MetadataFileProcessor implements FileProcessor {

    @Override
    public String key() {
        return "metadata";
    }

    @Override
    public boolean supports(String contentType) {
        return true;
    }

    @Override
    public Result process(FileContent content, Map<String, Object> config) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sizeBytes", content.size());
        if (content.contentType() != null) {
            metadata.put("mimeType", content.contentType());
        }

        if (content.contentType() != null && content.contentType().startsWith("image/")) {
            readImageDimensions(content, metadata);
        }
        return Result.metadata(metadata);
    }

    private void readImageDimensions(FileContent content, Map<String, Object> metadata) {
        try (InputStream in = content.openStream();
             ImageInputStream imageStream = ImageIO.createImageInputStream(in)) {
            if (imageStream == null) {
                return;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
            if (!readers.hasNext()) {
                return;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageStream, true, true);
                metadata.put("width", reader.getWidth(0));
                metadata.put("height", reader.getHeight(0));
                metadata.put("resolution", reader.getWidth(0) + "x" + reader.getHeight(0));
                metadata.put("imageFormat", reader.getFormatName());
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            // Metadata is best-effort: a malformed image must not fail the upload.
            log.debug("Could not read image metadata for {}: {}", content.filename(), e.toString());
        }
    }
}
