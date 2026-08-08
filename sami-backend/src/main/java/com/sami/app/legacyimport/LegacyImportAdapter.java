package com.sami.app.legacyimport;

import java.util.List;
import java.util.Map;

public interface LegacyImportAdapter {
    String sourceSystem();
    Analysis analyze(byte[] archive, boolean includeRecords);

    record Analysis(List<SourceFile> files, List<Dataset> datasets, List<Message> messages) {}
    record SourceFile(String sourcePath, String safeName, long size, String sha256,
                      String format, String supportStatus, Map<String,Object> metadata) {}
    record Dataset(String sourcePath, String key, String sourceTable, String semanticType,
                   String supportStatus, String encoding, long sourceCount,
                   List<Map<String,Object>> dictionary, List<Record> records) {}
    record Record(String sourceId, String legacyCode, String normalizedKey,
                  Map<String,Object> raw, Map<String,Object> normalized) {}
    record Message(String sourcePath, String datasetKey, String severity, String code, String message) {}
}
