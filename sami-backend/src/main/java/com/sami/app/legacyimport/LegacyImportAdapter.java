package com.sami.app.legacyimport;

import java.util.List;
import java.util.Map;

public interface LegacyImportAdapter {
    String sourceSystem();
    String parserVersion();
    String mediaType();
    boolean supports(String filename, byte[] archive);
    Analysis analyze(byte[] archive, boolean includeRecords);
    default Analysis analyze(String filename, byte[] archive, boolean includeRecords) {
        return analyze(archive, includeRecords);
    }

    record Analysis(List<SourceFile> files, List<Dataset> datasets, List<Message> messages) {}
    record SourceFile(String sourcePath, String safeName, long size, String sha256,
                      String format, String supportStatus, Map<String,Object> metadata) {}
    record Dataset(String sourcePath, String key, String sourceTable, String semanticType,
                   String supportStatus, String encoding, long sourceCount,
                   List<Map<String,Object>> dictionary, Map<String,Object> metadata,
                   List<Record> records) {}
    record Record(String sourceId, String legacyCode, String normalizedKey,
                  Map<String,Object> raw, Map<String,Object> normalized) {}
    record Message(String sourcePath, String datasetKey, String severity, String code, String message) {}
}
