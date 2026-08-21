package com.sami.app.legacyimport;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Read-only adapter for the manifest-driven Hesabdari Asan Excel migration package.
 * Source workbooks are inventoried and staged; this adapter never writes canonical
 * customer, product, inventory or accounting tables.
 */
@Component
public class AsanExcelPackageAdapter implements LegacyImportAdapter {
    public static final String PARSER_VERSION = "asan-excel-1.0";
    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4b, 0x03, 0x04};
    private static final String MANIFEST = "SAMI_ERP_Migration_Manifest.xlsx";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("xlsx", "xls", "txt");
    private static final List<String> HEADER_TERMS = List.of(
            "کد", "نام", "تاریخ", "شرح", "بدهکار", "بستانکار", "مانده", "موجودی",
            "شماره", "ردیف", "موبایل", "address", "name", "recid", "asan_code");

    private final LegacyImportProperties properties;

    public AsanExcelPackageAdapter(LegacyImportProperties properties) {
        this.properties = properties;
    }

    @Override public String sourceSystem() { return "ASAN"; }
    @Override public String parserVersion() { return PARSER_VERSION; }
    @Override public String mediaType() { return "application/zip"; }
    @Override public boolean supports(String filename, byte[] archive) {
        return (filename == null || filename.toLowerCase(Locale.ROOT).endsWith(".zip"))
                && archive != null && startsWith(archive, ZIP_SIGNATURE);
    }

    @Override
    public Analysis analyze(byte[] archive, boolean includeRecords) {
        if (!supports(null, archive)) throw new IllegalArgumentException("Only a valid ZIP package is accepted");
        try {
            Map<String, EntryData> entries = readEntries(archive);
            EntryData manifestFile = entries.values().stream()
                    .filter(entry -> MANIFEST.equalsIgnoreCase(entry.safeName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Migration manifest is required"));
            ManifestData manifest = readManifest(manifestFile.content());
            List<SourceFile> files = new ArrayList<>();
            List<Dataset> datasets = new ArrayList<>();
            List<Message> messages = new ArrayList<>();

            Set<String> availableSources = new HashSet<>();
            for (EntryData entry : entries.values()) {
                String extension = extension(entry.safeName());
                boolean supported = ALLOWED_EXTENSIONS.contains(extension);
                ManifestEntry mapping = manifest.files().get(normalizeName(entry.safeName()));
                if (entry.sourcePath().replace('\\', '/').startsWith("source/")) {
                    availableSources.add(normalizeName(entry.safeName()));
                }
                Map<String,Object> metadata = new LinkedHashMap<>();
                metadata.put("readOnly", true);
                metadata.put("macrosExecuted", false);
                metadata.put("manifestRole", MANIFEST.equalsIgnoreCase(entry.safeName()));
                if (mapping != null) {
                    metadata.put("target", mapping.target());
                    metadata.put("usage", mapping.usage());
                    metadata.put("manifestStatus", mapping.status());
                }
                files.add(new SourceFile(entry.sourcePath(), entry.safeName(), entry.content().length,
                        sha256(entry.content()), format(extension), supported ? "SUPPORTED" : "UNSUPPORTED", metadata));

                if (!entry.sourcePath().replace('\\', '/').startsWith("source/")) continue;
                if (!Set.of("xlsx", "xls").contains(extension)) {
                    messages.add(new Message(entry.sourcePath(), null, "WARNING", "UNSUPPORTED_SOURCE_FILE",
                            "Only XLSX and XLS source workbooks are parsed into staging records."));
                    continue;
                }
                if (mapping == null) {
                    messages.add(new Message(entry.sourcePath(), null, "ERROR", "MANIFEST_MAPPING_MISSING",
                            "The source workbook has no matching row in the migration manifest."));
                }
                try {
                    readWorkbook(entry, mapping, includeRecords, datasets, messages);
                } catch (EncryptedDocumentException ex) {
                    messages.add(new Message(entry.sourcePath(), null, "ERROR", "ENCRYPTED_WORKBOOK",
                            "Encrypted workbooks are not imported."));
                } catch (RuntimeException | IOException ex) {
                    messages.add(new Message(entry.sourcePath(), null, "ERROR", "WORKBOOK_READ_FAILED",
                            "Workbook contents could not be parsed safely: " + safeMessage(ex)));
                }
            }
            for (ManifestEntry expected : manifest.files().values()) {
                if (!availableSources.contains(normalizeName(expected.sourceFile()))) {
                    messages.add(new Message(expected.sourceFile(), null, "ERROR", "MANIFEST_SOURCE_MISSING",
                            "A workbook declared in the manifest is absent from source/."));
                }
            }
            messages.add(new Message(manifestFile.sourcePath(), null, "INFO", "MIGRATION_RULES_LOADED",
                    "Loaded " + manifest.rules().size() + " manifest migration rules; canonical writes remain disabled."));
            files.sort(Comparator.comparing(SourceFile::sourcePath));
            datasets.sort(Comparator.comparing(Dataset::key));
            return new Analysis(List.copyOf(files), List.copyOf(datasets), List.copyOf(messages));
        } catch (IOException ex) {
            throw new IllegalArgumentException("ZIP package could not be safely analyzed", ex);
        }
    }

    private Map<String, EntryData> readEntries(byte[] archive) throws IOException {
        Map<String, EntryData> result = new LinkedHashMap<>();
        Set<String> normalizedPaths = new HashSet<>();
        long total = 0;
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String sourcePath = entry.getName();
                AsanLegacyImportAdapter.validateEntryPath(sourcePath);
                if (sourcePath.length() > 500) throw new IllegalArgumentException("Archive entry path exceeds the supported length");
                if (++count > properties.maxFiles()) throw new IllegalArgumentException("Archive file count exceeds the configured limit");
                String normalized = sourcePath.replace('\\', '/').toLowerCase(Locale.ROOT);
                if (!normalizedPaths.add(normalized)) throw new IllegalArgumentException("Archive contains a duplicate entry path");
                byte[] content = readBounded(zip, properties.maxSingleFileBytes());
                total = Math.addExact(total, content.length);
                if (total > properties.maxExtractedBytes()) throw new IllegalArgumentException("Archive expanded size exceeds the configured limit");
                String safeName = sourcePath.replace('\\', '/');
                safeName = safeName.substring(safeName.lastIndexOf('/') + 1);
                if (safeName.length() > 255) throw new IllegalArgumentException("Archive entry filename exceeds the supported length");
                result.put(sourcePath, new EntryData(sourcePath, safeName, content));
                zip.closeEntry();
            }
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Migration package is empty");
        return result;
    }

    private ManifestData readManifest(byte[] bytes) throws IOException {
        Map<String,ManifestEntry> files = new LinkedHashMap<>();
        Map<String,String> rules = new LinkedHashMap<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet manifest = workbook.getSheet("Migration Manifest");
            if (manifest == null) throw new IllegalArgumentException("Manifest sheet 'Migration Manifest' is required");
            for (int index = manifest.getFirstRowNum() + 1; index <= manifest.getLastRowNum(); index++) {
                Row row = manifest.getRow(index);
                if (row == null) continue;
                String sourceFile = text(row.getCell(1));
                if (sourceFile.isBlank()) continue;
                ManifestEntry value = new ManifestEntry(sourceFile, text(row.getCell(2)), text(row.getCell(3)),
                        text(row.getCell(4)), text(row.getCell(5)));
                if (files.putIfAbsent(normalizeName(sourceFile), value) != null) {
                    throw new IllegalArgumentException("Manifest contains a duplicate source filename: " + sourceFile);
                }
            }
            Sheet rulesSheet = workbook.getSheet("Migration Rules");
            if (rulesSheet != null) {
                for (int index = rulesSheet.getFirstRowNum() + 1; index <= rulesSheet.getLastRowNum(); index++) {
                    Row row = rulesSheet.getRow(index);
                    if (row == null) continue;
                    String rule = text(row.getCell(0));
                    if (!rule.isBlank()) rules.put(rule, text(row.getCell(1)));
                }
            }
        }
        if (files.isEmpty()) throw new IllegalArgumentException("Migration manifest contains no source mappings");
        return new ManifestData(Map.copyOf(files), Map.copyOf(rules));
    }

    private void readWorkbook(EntryData entry, ManifestEntry mapping, boolean includeRecords,
                              List<Dataset> datasets, List<Message> messages) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(entry.content()))) {
            for (Sheet sheet : workbook) {
                int headerIndex = findHeaderRow(sheet);
                if (headerIndex < 0) {
                    messages.add(new Message(entry.sourcePath(), null, "WARNING", "EMPTY_WORKSHEET",
                            "Worksheet '" + sheet.getSheetName() + "' contains no readable rows."));
                    continue;
                }
                List<String> headers = headers(sheet.getRow(headerIndex));
                String semantic = semantic(mapping == null ? "" : mapping.target());
                String support = "UNKNOWN".equals(semantic) || mapping == null ? "PARTIAL" : "SUPPORTED";
                List<Record> records = new ArrayList<>();
                Set<String> uniqueCodes = new LinkedHashSet<>();
                long duplicateCodes = 0;
                long blankCodes = 0;
                long sourceCount = 0;
                Map<String,BigDecimal> totals = new LinkedHashMap<>();
                Map<String,Set<String>> types = new LinkedHashMap<>();
                for (String header : headers) types.put(header, new LinkedHashSet<>());

                for (Row row : sheet) {
                    int rowIndex = row.getRowNum();
                    if (rowIndex <= headerIndex || empty(row, headers.size())) continue;
                    sourceCount++;
                    Map<String,Object> raw = new LinkedHashMap<>();
                    Map<String,Object> normalized = new LinkedHashMap<>();
                    for (int column = 0; column < headers.size(); column++) {
                        Object value = value(row.getCell(column));
                        String header = headers.get(column);
                        raw.put(header, value);
                        if (value != null) types.get(header).add(value.getClass().getSimpleName());
                        if (value instanceof String text) normalized.put(header, LegacyNormalizer.text(text));
                        else if (value != null) normalized.put(header, value);
                        if (controlColumn(header) && value != null) {
                            BigDecimal number = decimal(value);
                            if (number != null) totals.merge(header, number, BigDecimal::add);
                        }
                    }
                    String sourceId = first(raw, sourceIdCandidates(semantic));
                    if (sourceId == null) sourceId = String.valueOf(rowIndex + 1);
                    String legacyCode = first(raw, legacyCodeCandidates(semantic));
                    legacyCode = LegacyNormalizer.code(legacyCode);
                    if (legacyCode == null || legacyCode.isBlank()) blankCodes++;
                    else if (!uniqueCodes.add(legacyCode)) duplicateCodes++;
                    if (includeRecords) {
                        records.add(new Record(sourceId, legacyCode, LegacyNormalizer.text(legacyCode), raw, normalized));
                    }
                }
                List<Map<String,Object>> dictionary = new ArrayList<>();
                for (String header : headers) {
                    Map<String,Object> field = new LinkedHashMap<>();
                    field.put("sourceField", header);
                    field.put("meaning", mapping == null ? "Unmapped source field" : mapping.target() + " source field");
                    field.put("sourceType", types.get(header).isEmpty() ? "UNKNOWN" : String.join("/", types.get(header)));
                    field.put("nullableEvidence", "UNKNOWN");
                    field.put("sampleValue", "[redacted]");
                    field.put("confidence", mapping == null ? "LOW" : "HIGH");
                    field.put("stagingField", "raw_record." + header);
                    dictionary.add(field);
                }
                Map<String,Object> metadata = new LinkedHashMap<>();
                metadata.put("stagingOnly", true);
                metadata.put("canonicalWrites", 0);
                metadata.put("sourceFile", entry.safeName());
                metadata.put("headerRow", headerIndex + 1);
                metadata.put("target", mapping == null ? "UNMAPPED" : mapping.target());
                metadata.put("usage", mapping == null ? "UNMAPPED" : mapping.usage());
                metadata.put("manifestStatus", mapping == null ? "MISSING" : mapping.status());
                metadata.put("description", mapping == null ? "" : mapping.description());
                metadata.put("uniqueLegacyCodes", uniqueCodes.size());
                metadata.put("duplicateLegacyCodes", duplicateCodes);
                metadata.put("blankLegacyCodes", blankCodes);
                Map<String,String> renderedTotals = new LinkedHashMap<>();
                totals.forEach((key, value) -> renderedTotals.put(key, value.stripTrailingZeros().toPlainString()));
                metadata.put("columnTotals", renderedTotals);
                String key = entry.sourcePath() + "#" + sheet.getSheetName();
                datasets.add(new Dataset(entry.sourcePath(), key, sheet.getSheetName(), semantic, support,
                        "EXCEL_CELL_TYPES", sourceCount, dictionary, metadata, records));
                if (requiresUniqueLegacyCode(semantic) && duplicateCodes > 0) {
                    messages.add(new Message(entry.sourcePath(), key, "WARNING", "DUPLICATE_LEGACY_CODE",
                            duplicateCodes + " duplicate legacy codes require mapping review."));
                }
                if (requiresUniqueLegacyCode(semantic) && blankCodes > 0) {
                    messages.add(new Message(entry.sourcePath(), key, "WARNING", "BLANK_LEGACY_CODE",
                            blankCodes + " rows have no approved legacy-code field and remain row-addressable only."));
                }
                if ("UNKNOWN".equals(semantic)) {
                    messages.add(new Message(entry.sourcePath(), key, "WARNING", "UNMAPPED_TARGET",
                            "Manifest target is not mapped to an approved SAMI staging semantic type."));
                }
            }
        }
    }

    private static int findHeaderRow(Sheet sheet) {
        int bestIndex = -1;
        int bestScore = -1;
        int end = Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + 19);
        for (int index = sheet.getFirstRowNum(); index <= end; index++) {
            Row row = sheet.getRow(index);
            if (row == null) continue;
            int filled = 0;
            int labels = 0;
            for (Cell cell : row) {
                String value = text(cell).toLowerCase(Locale.ROOT);
                if (value.isBlank()) continue;
                filled++;
                if (HEADER_TERMS.stream().anyMatch(value::contains)) labels++;
            }
            int score = filled + labels * 100;
            if (score > bestScore) { bestScore = score; bestIndex = index; }
        }
        return bestIndex;
    }

    private static List<String> headers(Row row) {
        int columns = Math.max(row.getLastCellNum(), 0);
        List<String> headers = new ArrayList<>(columns);
        Map<String,Integer> seen = new HashMap<>();
        for (int index = 0; index < columns; index++) {
            String header = LegacyNormalizer.text(text(row.getCell(index)));
            if (header == null || header.isBlank()) header = "COLUMN_" + (index + 1);
            int occurrence = seen.merge(header, 1, Integer::sum);
            headers.add(occurrence == 1 ? header : header + "_" + occurrence);
        }
        return headers;
    }

    private static boolean empty(Row row, int columns) {
        for (int index = 0; index < columns; index++) if (value(row.getCell(index)) != null) return false;
        return true;
    }

    private static Object value(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> blankToNull(cell.getStringCellValue());
            case BOOLEAN -> cell.getBooleanCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toInstant().toString()
                    : BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
            case ERROR -> "#ERROR:" + cell.getErrorCellValue();
            default -> null;
        };
    }

    private static String text(Cell cell) {
        Object value = value(cell);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String first(Map<String,Object> raw, String... candidates) {
        for (String candidate : candidates) {
            for (Map.Entry<String,Object> field : raw.entrySet()) {
                if (sameField(field.getKey(), candidate) && field.getValue() != null) return scalar(field.getValue());
            }
        }
        return null;
    }

    private static String[] sourceIdCandidates(String semantic) {
        return switch (semantic) {
            case "PARTY" -> new String[]{"Asan_Code", "کد", "RecId"};
            case "PRODUCT_MOVEMENT" -> new String[]{"RecId", "شماره سند", "ردیف ف"};
            case "INVENTORY_BALANCE" -> new String[]{"کد کالا", "Cod_Kala_128c"};
            case "TRIAL_BALANCE" -> new String[]{"RecId", "id", "کد حسابداری", "کد"};
            case "ACCOUNT_BALANCE" -> new String[]{"RecId", "کد"};
            case "ACCOUNTING_JOURNAL" -> new String[]{"F_5", "شماره سند", "ردیف"};
            case "DAILY_OPERATION" -> new String[]{"ش.سند", "ش .سند", "شماره سند", "ردیف"};
            case "BANK_TRANSACTION" -> new String[]{"ش.سند", "ش .سند", "شماره سند", "ردیف"};
            case "CASH_TRANSACTION" -> new String[]{"ردیف", "شماره سند"};
            default -> new String[]{"RecId", "id", "کد", "ردیف", "شماره سند"};
        };
    }

    private static String[] legacyCodeCandidates(String semantic) {
        return switch (semantic) {
            case "PARTY" -> new String[]{"Asan_Code", "کد", "RecId"};
            case "PRODUCT_MOVEMENT", "INVENTORY_BALANCE" -> new String[]{"کد کالا", "کد کالا1", "Cod_Kala_128c", "RecId"};
            case "TRIAL_BALANCE" -> new String[]{"کد حسابداری", "کد", "RecId", "id"};
            case "ACCOUNT_BALANCE" -> new String[]{"کد", "RecId", "کد حسابداری/گروه حساب"};
            case "ACCOUNTING_JOURNAL" -> new String[]{"شماره سند", "F_5", "ردیف"};
            case "DAILY_OPERATION", "BANK_TRANSACTION" -> new String[]{"ش.سند", "ش .سند", "شماره سند", "ردیف"};
            case "CASH_TRANSACTION" -> new String[]{"ردیف", "شماره سند"};
            default -> new String[]{"RecId", "id", "کد", "ردیف", "شماره سند"};
        };
    }

    private static String semantic(String target) {
        String normalized = target == null ? "" : target.toLowerCase(Locale.ROOT);
        if (normalized.contains("daily_operations")) return "DAILY_OPERATION";
        if (normalized.contains("accounting_documents") || normalized.contains("accounting_entries")) return "ACCOUNTING_JOURNAL";
        if (normalized.contains("account_balances") && normalized.contains("validation")) return "TRIAL_BALANCE";
        if (normalized.contains("parties") || normalized.contains("customers") || normalized.contains("suppliers")) return "PARTY";
        if (normalized.contains("opening_balances") || normalized.contains("party_balances")) return "ACCOUNT_BALANCE";
        if (normalized.contains("products") || normalized.contains("inventory_movements")) return "PRODUCT_MOVEMENT";
        if (normalized.contains("inventory_balances")) return "INVENTORY_BALANCE";
        if (normalized.contains("bank_transactions")) return "BANK_TRANSACTION";
        if (normalized.contains("cash_transactions")) return "CASH_TRANSACTION";
        return "UNKNOWN";
    }

    private static boolean controlColumn(String header) {
        String value = LegacyNormalizer.text(header);
        if (value == null) return false;
        return List.of("بدهکار", "بستانکار", "مانده", "موجودی", "ورود", "خروج", "مبلغ کل")
                .stream().anyMatch(value::contains);
    }

    private static boolean requiresUniqueLegacyCode(String semantic) {
        return Set.of("PARTY", "ACCOUNT_BALANCE", "TRIAL_BALANCE", "INVENTORY_BALANCE").contains(semantic);
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (!(value instanceof String text)) return null;
        String normalized = LegacyNormalizer.code(text);
        if (normalized == null) return null;
        normalized = normalized.replace(",", "").replace("[", "-").replace("]", "").trim();
        if (!normalized.matches("[-+]?\\d+(?:\\.\\d+)?")) return null;
        try { return new BigDecimal(normalized); } catch (NumberFormatException ignored) { return null; }
    }

    private static boolean sameField(String left, String right) {
        String a = LegacyNormalizer.text(left);
        String b = LegacyNormalizer.text(right);
        return a != null && b != null && a.replace(" ", "").equalsIgnoreCase(b.replace(" ", ""));
    }

    private static String scalar(Object value) {
        return value instanceof BigDecimal decimal ? decimal.stripTrailingZeros().toPlainString() : String.valueOf(value).trim();
    }

    private static String normalizeName(String value) {
        String normalized = LegacyNormalizer.text(value);
        return normalized == null ? "" : normalized.toLowerCase(Locale.ROOT);
    }

    private static String format(String extension) {
        return switch (extension) {
            case "xlsx" -> "OFFICE_OPEN_XML_WORKBOOK";
            case "xls" -> "MICROSOFT_EXCEL_BINARY";
            case "txt" -> "UTF8_TEXT";
            default -> "UNSUPPORTED";
        };
    }

    private static String extension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] readBounded(ZipInputStream input, long max) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            if (total > max) throw new IllegalArgumentException("Archive entry exceeds the configured size limit");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) if (value[index] != prefix[index]) return false;
        return true;
    }

    private static String blankToNull(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) return ex.getClass().getSimpleName();
        return message.length() > 240 ? message.substring(0, 240) : message;
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    private record EntryData(String sourcePath, String safeName, byte[] content) {}
    private record ManifestEntry(String sourceFile, String target, String usage, String status, String description) {}
    private record ManifestData(Map<String,ManifestEntry> files, Map<String,String> rules) {}
}
