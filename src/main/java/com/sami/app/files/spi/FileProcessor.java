package com.sami.app.files.spi;

import java.util.List;
import java.util.Map;

/**
 * A step in the upload pipeline: metadata extraction, thumbnailing, OCR,
 * watermarking, virus scanning, conversion.
 *
 * <p>A processor never mutates the original — it returns {@link Result}s that the
 * service persists as metadata, derivatives or a scan verdict. That keeps the
 * stored bytes immutable and makes every processor safely re-runnable.
 */
public interface FileProcessor {

    /** Matches {@code file_processors.handler_key}. */
    String key();

    /** Whether this processor can act on the given content type. */
    boolean supports(String contentType);

    Result process(FileContent content, Map<String, Object> config);

    /**
     * @param metadata    merged into {@code files.metadata}
     * @param derivatives new renditions to store
     * @param verdict     non-null only for scanning processors
     */
    record Result(Map<String, Object> metadata,
                  List<Derivative> derivatives,
                  ScanVerdict verdict) {

        public static Result none() {
            return new Result(Map.of(), List.of(), null);
        }

        public static Result metadata(Map<String, Object> metadata) {
            return new Result(metadata, List.of(), null);
        }
    }

    /** A generated rendition. {@code text} is used by OCR; {@code content} by image kinds. */
    record Derivative(String kind,
                      FileContent content,
                      String text,
                      Integer width,
                      Integer height,
                      Integer pageCount,
                      Long durationMs) {
    }

    record ScanVerdict(String verdict, String threat, Map<String, Object> details) {
        public boolean infected() {
            return "infected".equals(verdict) || "suspicious".equals(verdict);
        }
    }
}
