package com.sami.app.legacyimport;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AsanLegacyImportAdapter implements LegacyImportAdapter {
    public static final String PARSER_VERSION = "asan-1.0";
    private static final byte[] RAR4 = {0x52, 0x61, 0x72, 0x21, 0x1a, 0x07, 0x00};
    private static final byte[] RAR5 = {0x52, 0x61, 0x72, 0x21, 0x1a, 0x07, 0x01, 0x00};
    private static final Pattern REPORT_FIELD = Pattern.compile("(?i)(?:FieldName|DataField|DisplayText)\\s*[=:]\\s*[\\\"']?([A-Za-z_][A-Za-z0-9_]*)");
    private static final Set<String> CODE_FIELDS = Set.of("ID", "Radif", "ShahrCode", "asan_cod", "Serial", "Number", "cod_s");
    private final LegacyImportProperties properties;

    @Override public String sourceSystem() { return "ASAN"; }
    @Override public String parserVersion() { return PARSER_VERSION; }
    @Override public String mediaType() { return "application/vnd.rar"; }
    @Override public boolean supports(String filename, byte[] archive) {
        return archive != null && (startsWith(archive, RAR4) || startsWith(archive, RAR5));
    }

    @Override
    public Analysis analyze(byte[] archiveBytes, boolean includeRecords) {
        requireRarSignature(archiveBytes);
        Path root = null;
        List<SourceFile> files = new ArrayList<>();
        List<Dataset> datasets = new ArrayList<>();
        List<Message> messages = new ArrayList<>();
        try {
            root = Files.createTempDirectory("sami-asan-");
            Path rar = root.resolve("upload.rar");
            Files.write(rar, archiveBytes);
            if (startsWith(archiveBytes, RAR5)) return analyzeRar5(rar, root, includeRecords);
            try (Archive archive = new Archive(rar.toFile())) {
                int count = 0;
                long total = 0;
                for (FileHeader header : archive.getFileHeaders()) {
                    if (header.isDirectory()) continue;
                    String sourcePath = header.getFileName();
                    validateEntryPath(sourcePath);
                    if (++count > properties.maxFiles()) throw new IllegalArgumentException("Archive file count exceeds the configured limit");
                    long declared = header.getFullUnpackSize();
                    if (declared < 0 || declared > properties.maxSingleFileBytes()) throw new IllegalArgumentException("Archive entry exceeds the configured size limit: " + sourcePath);
                    total = Math.addExact(total, declared);
                    if (total > properties.maxExtractedBytes()) throw new IllegalArgumentException("Archive expanded size exceeds the configured limit");
                    Path extracted = root.resolve("entry-" + count + ".bin");
                    try (OutputStream out = Files.newOutputStream(extracted)) { archive.extractFile(header, out); }
                    long actual = Files.size(extracted);
                    if (actual > properties.maxSingleFileBytes()) throw new IllegalArgumentException("Extracted entry exceeds the configured limit: " + sourcePath);
                    byte[] prefix = readPrefix(extracted, 8192);
                    String safeName = Path.of(sourcePath.replace('\\', '/')).getFileName().toString();
                    String fileHash = sha256(Files.readAllBytes(extracted));
                    if (isJet(prefix)) {
                        files.add(new SourceFile(sourcePath, safeName, actual, fileHash, "MICROSOFT_JET", "SUPPORTED", Map.of("readOnly", true)));
                        readJet(extracted, sourcePath, includeRecords, datasets, messages);
                    } else if (isReport(prefix)) {
                        files.add(new SourceFile(sourcePath, safeName, actual, fileHash, "FASTREPORT_DEFINITION", "PARTIAL", Map.of("recordsAvailable", false)));
                        readReport(extracted, sourcePath, datasets, messages);
                    } else {
                        files.add(new SourceFile(sourcePath, safeName, actual, fileHash, "PROPRIETARY_BINARY", "UNSUPPORTED", Map.of("reason", "No verified record layout or character codec")));
                        messages.add(new Message(sourcePath, null, "WARNING", "UNSUPPORTED_LAYOUT", "File was inventoried but not decoded because its binary layout is not verified."));
                    }
                }
            }
            return new Analysis(List.copyOf(files), List.copyOf(datasets), List.copyOf(messages));
        } catch (IOException | RarException e) {
            throw new IllegalArgumentException("The archive could not be safely analyzed", e);
        } finally {
            deleteTree(root);
        }
    }

    private Analysis analyzeRar5(Path rar, Path root, boolean includeRecords) throws IOException {
        List<SourceFile> files = new ArrayList<>(); List<Dataset> datasets = new ArrayList<>(); List<Message> messages = new ArrayList<>();
        String executable = resolveUnrar();
        Process listing = new ProcessBuilder(executable, "lb", "-p-", rar.toString()).redirectErrorStream(true).start();
        String output = new String(readBounded(listing.getInputStream(), 4_000_000), StandardCharsets.UTF_8);
        int listExit = waitFor(listing);
        if (listExit != 0) throw new IllegalArgumentException("RAR5 listing failed; archive may be encrypted, corrupt, or UnRAR is unavailable");
        List<String> entries = output.lines().map(String::trim).filter(s -> !s.isBlank() && !s.endsWith("/") && !s.endsWith("\\")).toList();
        if (entries.size() > properties.maxFiles()) throw new IllegalArgumentException("Archive file count exceeds the configured limit");
        long total = 0; int count = 0;
        for (String sourcePath : entries) {
            validateEntryPath(sourcePath); count++;
            Process extraction = new ProcessBuilder(executable, "p", "-inul", "-p-", rar.toString(), sourcePath).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            byte[] content;
            try { content = readBounded(extraction.getInputStream(), properties.maxSingleFileBytes()); }
            catch (RuntimeException ex) { extraction.destroyForcibly(); throw ex; }
            if (waitFor(extraction) != 0) throw new IllegalArgumentException("RAR5 entry could not be extracted safely: " + sourcePath);
            total = Math.addExact(total, content.length); if (total > properties.maxExtractedBytes()) throw new IllegalArgumentException("Archive expanded size exceeds the configured limit");
            Path extracted = root.resolve("entry-r5-" + count + ".bin"); Files.write(extracted, content);
            byte[] prefix = content.length <= 8192 ? content : java.util.Arrays.copyOf(content,8192);
            String safeName = Path.of(sourcePath.replace('\\','/')).getFileName().toString(); String fileHash=sha256(content);
            if (isJet(prefix)) { files.add(new SourceFile(sourcePath,safeName,content.length,fileHash,"MICROSOFT_JET","SUPPORTED",Map.of("readOnly",true))); readJet(extracted,sourcePath,includeRecords,datasets,messages); }
            else if (isReport(prefix)) { files.add(new SourceFile(sourcePath,safeName,content.length,fileHash,"FASTREPORT_DEFINITION","PARTIAL",Map.of("recordsAvailable",false))); readReport(extracted,sourcePath,datasets,messages); }
            else { files.add(new SourceFile(sourcePath,safeName,content.length,fileHash,"PROPRIETARY_BINARY","UNSUPPORTED",Map.of("reason","No verified record layout or character codec"))); messages.add(new Message(sourcePath,null,"WARNING","UNSUPPORTED_LAYOUT","File was inventoried but not decoded because its binary layout is not verified.")); }
        }
        return new Analysis(List.copyOf(files),List.copyOf(datasets),List.copyOf(messages));
    }

    private String resolveUnrar() {
        String configured=properties.unrarExecutable();
        if (!"unrar".equalsIgnoreCase(configured)) return configured;
        Path windows=Path.of("C:/Program Files/WinRAR/UnRAR.exe"); return Files.isRegularFile(windows)?windows.toString():configured;
    }
    private static byte[] readBounded(InputStream in,long max) throws IOException { var out=new java.io.ByteArrayOutputStream(); byte[] buffer=new byte[8192]; long total=0; int n; while((n=in.read(buffer))>=0){ total+=n; if(total>max) throw new IllegalArgumentException("Extracted content exceeds the configured limit"); out.write(buffer,0,n); } return out.toByteArray(); }
    private static int waitFor(Process process) { try { return process.waitFor(); } catch(InterruptedException e){ Thread.currentThread().interrupt(); process.destroyForcibly(); throw new IllegalStateException("Archive process interrupted",e); } }

    static void requireRarSignature(byte[] bytes) {
        if (bytes == null || !(startsWith(bytes, RAR4) || startsWith(bytes, RAR5))) throw new IllegalArgumentException("Only valid RAR archives are accepted");
    }

    static void validateEntryPath(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Archive contains an unnamed entry");
        String normalized = value.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) throw new IllegalArgumentException("Archive contains an absolute path");
        for (String part : normalized.split("/")) if (part.equals("..")) throw new IllegalArgumentException("Archive contains a traversal path");
    }

    private void readJet(Path file, String sourcePath, boolean includeRecords, List<Dataset> datasets, List<Message> messages) throws IOException {
        try (Database db = new DatabaseBuilder(file.toFile()).setReadOnly(true).open()) {
            for (String tableName : db.getTableNames()) {
                Table table = db.getTable(tableName);
                List<Map<String,Object>> dictionary = table.getColumns().stream().map(c -> dictionary(c, tableName)).toList();
                List<Record> records = new ArrayList<>();
                long count = 0;
                for (Row row : table) {
                    count++;
                    if (!includeRecords) continue;
                    Map<String,Object> raw = new LinkedHashMap<>();
                    row.forEach((key, value) -> raw.put(key, jsonValue(value)));
                    String id = first(raw, "ID", "Radif", "ShahrCode", "Number", "Serial");
                    String code = first(raw, "asan_cod", "ID", "Radif", "ShahrCode", "Serial", "Number");
                    Map<String,Object> normalized = new LinkedHashMap<>();
                    raw.forEach((key, value) -> { if (value instanceof String s) normalized.put(key, LegacyNormalizer.text(s)); });
                    records.add(new Record(id, LegacyNormalizer.code(code), LegacyNormalizer.text(code), raw, normalized));
                }
                String key = sourcePath + "#" + tableName;
                datasets.add(new Dataset(sourcePath, key, tableName, semantic(tableName), "SUPPORTED", "UTF-16LE", count, dictionary, Map.of(), records));
            }
        } catch (RuntimeException ex) {
            messages.add(new Message(sourcePath, null, "ERROR", "JET_READ_FAILED", "Jet database metadata could not be read safely."));
            throw ex;
        }
    }

    private void readReport(Path file, String sourcePath, List<Dataset> datasets, List<Message> messages) throws IOException {
        String text = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
        Matcher matcher = REPORT_FIELD.matcher(text);
        Map<String,Map<String,Object>> fields = new LinkedHashMap<>();
        while (matcher.find()) fields.putIfAbsent(matcher.group(1), Map.of("sourceField", matcher.group(1), "meaning", "Report expression field", "sourceType", "UNKNOWN", "nullableEvidence", "UNKNOWN", "confidence", "MEDIUM", "stagingField", "raw_record." + matcher.group(1)));
        String key = sourcePath + "#report-fields";
        datasets.add(new Dataset(sourcePath, key, "report-fields", "REPORT_SCHEMA", "PARTIAL", "BINARY_TEXT", 0, new ArrayList<>(fields.values()), Map.of(), List.of()));
        messages.add(new Message(sourcePath, key, "INFO", "SCHEMA_ONLY", "Report field names were discovered; the report definition contains no source records."));
    }

    private Map<String,Object> dictionary(Column column, String table) {
        Map<String,Object> value = new LinkedHashMap<>();
        value.put("sourceField", column.getName()); value.put("meaning", meaning(table, column.getName()));
        value.put("sourceType", column.getType().name()); value.put("nullableEvidence", "UNKNOWN");
        value.put("sampleValue", "[redacted]"); value.put("confidence", "HIGH"); value.put("stagingField", "raw_record." + column.getName());
        return value;
    }

    private static String meaning(String table, String field) {
        String f = field.toLowerCase(Locale.ROOT);
        if (f.contains("national") || f.equals("cod_meli")) return "National identifier";
        if (f.contains("economic")) return "Economic identifier";
        if (f.contains("phone") || f.contains("tell") || f.equals("number")) return "Phone/contact number";
        if (f.contains("name") || f.equals("nam") || f.equals("foroshande") || f.equals("model")) return "Display name";
        if (CODE_FIELDS.stream().anyMatch(x -> x.equalsIgnoreCase(field))) return "Legacy identifier/code";
        return table + " source field";
    }

    private static String semantic(String table) {
        return switch (table.toLowerCase(Locale.ROOT)) {
            case "foroush_detail" -> "CUSTOMER"; case "serial_tbl" -> "WARRANTY_SERIAL";
            case "model_nam" -> "WARRANTY_MODEL"; case "foroshande_nam" -> "WARRANTY_SELLER";
            case "tarh_zemanat" -> "WARRANTY_PLAN"; case "my_zone" -> "REFERENCE_GEOGRAPHY";
            case "edarekol" -> "REFERENCE_TAX_OFFICE"; case "row_fac" -> "INVOICE_LINE_REFERENCE";
            case "det_row" -> "SERIAL_DETAIL"; case "sms_table" -> "MESSAGE"; default -> "UNKNOWN";
        };
    }

    private static Object jsonValue(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) return value;
        if (value instanceof Date d) return d.toInstant().toString();
        if (value instanceof TemporalAccessor) return value.toString();
        if (value instanceof byte[] b) return Map.of("binaryBytes", b.length, "sha256", sha256(b));
        return value.toString();
    }
    private static String first(Map<String,Object> row, String... fields) { for (String field : fields) { Object v = row.get(field); if (v != null) return String.valueOf(v); } return null; }
    private static boolean isJet(byte[] b) { return new String(b, StandardCharsets.ISO_8859_1).contains("Standard Jet DB"); }
    private static boolean isReport(byte[] b) { String s = new String(b, StandardCharsets.ISO_8859_1); return s.contains("TfrxReport") || s.contains("FastReport") || s.contains("Fast Reports"); }
    private static byte[] readPrefix(Path p, int max) throws IOException { byte[] all = Files.readAllBytes(p); return all.length <= max ? all : java.util.Arrays.copyOf(all, max); }
    private static boolean startsWith(byte[] value, byte[] prefix) { if (value.length < prefix.length) return false; for (int i=0;i<prefix.length;i++) if (value[i] != prefix[i]) return false; return true; }
    private static String sha256(byte[] bytes) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static void deleteTree(Path root) { if (root == null) return; try (var paths = Files.walk(root)) { paths.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) { } }); } catch (IOException ignored) { } }
}
