package com.sami.app.legacyimport;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Stages a single, untrusted Asan accounting export. It is deliberately
 * read-only: rows retain source sheet/row provenance and cannot create SAMI
 * accounting or operational records.
 */
@Component
public class AsanAccountingExcelAdapter implements LegacyImportAdapter {
    static final String PARSER_VERSION = "asan-accounting-excel-1.0";
    private static final Set<String> LEDGER_HEADERS = Set.of("بدهکار", "بستانکار", "سند", "کد", "تاریخ", "شرح");
    private static final Set<String> CHEQUE_HEADERS = Set.of("شماره", "مبلغ", "وضعیت چک", "سررسید");
    private static final Set<String> OTHER_DETECTION_HEADERS = Set.of(
            "نام کالا", "کد کالا", "شماره فاکتور", "مانده پایان دوره بدهکار", "مانده پایان دوره بستانکار");
    private static final Pattern RGB_ATTRIBUTE = Pattern.compile("\\s+rgb=\\\"([^\\\"]*)\\\"");
    private final LegacyImportProperties properties;

    public AsanAccountingExcelAdapter(LegacyImportProperties properties) { this.properties = properties; }
    @Override public String sourceSystem() { return "ASAN"; }
    @Override public String parserVersion() { return PARSER_VERSION; }
    @Override public String mediaType() { return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; }
    @Override public boolean supports(String filename, byte[] bytes) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".xlsx") && bytes != null && bytes.length >= 4
                && bytes[0] == 0x50 && bytes[1] == 0x4b && bytes[2] == 0x03 && bytes[3] == 0x04;
    }
    @Override public Analysis analyze(byte[] bytes, boolean includeRecords) { return analyze("asan-report.xlsx", bytes, includeRecords); }
    @Override public boolean supportsStreamingRecords() { return true; }

    @Override
    public Analysis analyze(String filename, byte[] bytes, boolean includeRecords) {
        if (!supports(filename, bytes)) throw new IllegalArgumentException("Only XLSX Asan accounting reports are accepted");
        if (bytes.length > properties.maxSingleFileBytes()) throw new IllegalArgumentException("Workbook exceeds the configured per-file limit");
        List<Dataset> datasets = new ArrayList<>(); List<Message> messages = new ArrayList<>();
        Map<String, Object> fileMetadata = new LinkedHashMap<>();
        fileMetadata.put("readOnly", true); fileMetadata.put("reportSource", "ASAN_EXCEL_REPORT"); fileMetadata.put("macrosExecuted", false);
        SanitizedWorkbook sanitized = sanitizeInvalidStyleColors(bytes);
        if (sanitized.changed()) {
            fileMetadata.put("styleSanitizedInMemory", true);
            messages.add(new Message(filename, null, "WARNING", "INVALID_STYLE_COLOR_SANITIZED",
                    "Malformed workbook color attributes were normalized in memory only; source data and cells were not changed."));
        }
        try {
            stream(filename, sanitized.bytes(), null, datasets, messages, includeRecords);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Workbook contents could not be parsed safely", ex);
        }
        if (datasets.isEmpty()) throw new IllegalArgumentException("Workbook has no readable worksheets");
        return new Analysis(List.of(new SourceFile(filename, filename, bytes.length, sha256(bytes), "XLSX", "SUPPORTED", fileMetadata)), datasets, messages);
    }

    @Override
    public void streamRecords(String filename, byte[] bytes, BiConsumer<String, Record> consumer) {
        if (!supports(filename, bytes)) throw new IllegalArgumentException("Only XLSX Asan accounting reports are accepted");
        if (bytes.length > properties.maxSingleFileBytes()) throw new IllegalArgumentException("Workbook exceeds the configured per-file limit");
        if (consumer == null) throw new IllegalArgumentException("A streaming record consumer is required");
        SanitizedWorkbook sanitized = sanitizeInvalidStyleColors(bytes);
        try {
            stream(filename, sanitized.bytes(), consumer, new ArrayList<>(), new ArrayList<>(), false);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Workbook contents could not be streamed safely", ex);
        }
    }

    private void stream(String filename, byte[] bytes, BiConsumer<String, Record> consumer, List<Dataset> datasets, List<Message> messages, boolean retainRecords) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(new ByteArrayInputStream(bytes))) {
            XSSFReader reader = new XSSFReader(pkg);
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            int count = 0;
            while (sheets.hasNext()) {
                if (++count > 100) throw new IllegalArgumentException("Workbook contains too many worksheets");
                try (var input = sheets.next()) {
                    StreamingSheet handler = new StreamingSheet(filename, sheets.getSheetName(), consumer, retainRecords);
                    XMLReader xml = SAXHelper.newXMLReader();
                    xml.setContentHandler(new XSSFSheetXMLHandler(
                            reader.getStylesTable(), null, strings, handler, new DataFormatter(Locale.ROOT), false));
                    xml.parse(new InputSource(input));
                    datasets.add(handler.dataset());
                    messages.addAll(handler.messages);
                }
            }
        }
    }

    private final class StreamingSheet implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final String filename;
        private final String sheetName;
        private final BiConsumer<String, Record> consumer;
        private final boolean retain;
        private final List<Message> messages = new ArrayList<>();
        private final List<String> title = new ArrayList<>();
        private final List<Record> retained = new ArrayList<>();
        private int rowNumber;
        private int headerRow = -1;
        private int skipped;
        private int sourceRows;
        private List<String> headers;
        private List<String> row;
        private Detection detection;

        private StreamingSheet(String filename, String sheetName, BiConsumer<String, Record> consumer, boolean retain) {
            this.filename = filename;
            this.sheetName = sheetName;
            this.consumer = consumer;
            this.retain = retain;
        }

        @Override
        public void startRow(int rowNum) {
            rowNumber = rowNum;
            row = new ArrayList<>();
        }

        @Override
        public void endRow(int rowNum) {
            if (headers == null) {
                if (rowNum <= 40) {
                    title.addAll(row);
                    if (headerScore(row) >= 3) {
                        headers = headers(row);
                        headerRow = rowNum;
                        skipped = rowNum + 1;
                        detection = detect(headers, title);
                    }
                }
                return;
            }
            RowKind kind = classify(row, headers);
            if (kind != RowKind.DATA) {
                skipped++;
                return;
            }
            sourceRows++;
            Record record = record(sheetName, rowNumber, headers, detection.type(), row);
            if (retain) retained.add(record);
            if (consumer != null) consumer.accept(key(), record);
        }

        @Override
        public void cell(String ref, String value, XSSFComment comment) {
            int column = column(ref);
            while (row.size() <= column) row.add("");
            row.set(column, value == null ? "" : value.trim());
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {}

        private Dataset dataset() {
            if (headers == null) {
                Dataset dataset = new Dataset(filename, key(), sheetName, "UNKNOWN", "PARTIAL", "UTF-8", 0,
                        List.of(), Map.of("detectionConfidence", "LOW", "reason", "No verified header signature"), List.of());
                messages.add(new Message(filename, key(), "WARNING", "HEADER_NOT_DETECTED", "No supported report header was found."));
                return dataset;
            }
            Map<String,Object> metadata = new LinkedHashMap<>();
            metadata.put("reportType", detection.type());
            metadata.put("detectionConfidence", detection.confidence());
            metadata.put("headerRow", headerRow + 1);
            metadata.put("skippedRows", skipped);
            metadata.put("rowClassification", "DATA/HEADER/SUBTOTAL/TOTAL/FOOTER/UNKNOWN");
            metadata.put("requiresTypeConfirmation", "LOW".equals(detection.confidence()));
            if ("LOW".equals(detection.confidence())) {
                messages.add(new Message(filename, key(), "WARNING", "AMBIGUOUS_REPORT_TYPE",
                        "Confirm the account ledger level before acceptance."));
            }
            return new Dataset(filename, key(), sheetName, detection.type(),
                    "UNKNOWN".equals(detection.type()) ? "PARTIAL" : "SUPPORTED", "UTF-8", sourceRows,
                    dictionary(headers), metadata, retained);
        }

        private String key() {
            return filename + "#" + sheetName;
        }
    }

    private static Detection detect(List<String> headers, List<String> title) {
        Set<String> normalized = headers.stream().map(LegacyNormalizer::text).collect(java.util.stream.Collectors.toSet());
        if (normalized.containsAll(CHEQUE_HEADERS)) {
            return new Detection(normalized.contains("نام صاحب چک") ? "CHEQUE_RECEIVABLE" : "CHEQUE_PAYABLE", "HIGH");
        }
        if (normalized.contains("مانده پایان دوره بدهکار") && normalized.contains("مانده پایان دوره بستانکار")) {
            return new Detection("TRIAL_BALANCE", "HIGH");
        }
        if (normalized.contains("نام کالا") && normalized.contains("کد کالا") && normalized.contains("شماره فاکتور")) {
            return new Detection("PRODUCT_MOVEMENT", "HIGH");
        }
        if (normalized.containsAll(LEDGER_HEADERS)) {
            boolean daily = title.stream().map(LegacyNormalizer::text).anyMatch(value -> value.contains("دفتر روزنامه"));
            return new Detection(daily ? "ACCOUNTING_JOURNAL" : "ACCOUNT_LEDGER", daily ? "HIGH" : "LOW");
        }
        return new Detection("UNKNOWN", "LOW");
    }

    private static int headerScore(List<String> values) {
        return (int) values.stream().map(LegacyNormalizer::text)
                .filter(value -> LEDGER_HEADERS.contains(value) || CHEQUE_HEADERS.contains(value) || OTHER_DETECTION_HEADERS.contains(value))
                .count();
    }

    private static int column(String ref) {
        int end = 0;
        while (end < ref.length() && Character.isLetter(ref.charAt(end))) end++;
        int value = 0;
        for (int i = 0; i < end; i++) value = value * 26 + (Character.toUpperCase(ref.charAt(i)) - 'A' + 1);
        return Math.max(0, value - 1);
    }

    /**
     * Some Asan exports contain non-OOXML RGB attributes in xl/styles.xml.
     * Styles are presentation-only, so repair is strictly in memory: valid
     * six-digit colors get the opaque alpha channel POI requires and malformed
     * color attributes are omitted.  No workbook cell, value, formula, or
     * source archive byte is written back to disk.
     */
    private SanitizedWorkbook sanitizeInvalidStyleColors(byte[] bytes) {
        byte[] originalStyles = null;
        byte[] repairedStyles = null;
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            long[] expandedTotal = {0};
            Set<String> entryNames = new HashSet<>();
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                String safeName = safeEntryName(entry.getName());
                if (!entryNames.add(safeName.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Workbook contains duplicate entry names");
                }
                if ("xl/styles.xml".equals(entry.getName())) {
                    originalStyles = readBounded(input, properties.maxSingleFileBytes(), properties.maxExtractedBytes(), expandedTotal);
                    repairedStyles = repairStyles(originalStyles);
                } else {
                    copyBounded(input, OutputStream.nullOutputStream(), properties.maxExtractedBytes(), properties.maxExtractedBytes(), expandedTotal);
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Workbook ZIP structure could not be read safely", exception);
        }
        if (repairedStyles == null) return new SanitizedWorkbook(bytes, false);
        if (java.util.Arrays.equals(originalStyles, repairedStyles)) return new SanitizedWorkbook(bytes, false);

        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output)) {
            long[] expandedTotal = {0};
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                zip.putNextEntry(new ZipEntry(entry.getName()));
                if ("xl/styles.xml".equals(entry.getName())) {
                    copyBounded(input, OutputStream.nullOutputStream(), properties.maxSingleFileBytes(), properties.maxExtractedBytes(), expandedTotal);
                    zip.write(repairedStyles);
                } else {
                    copyBounded(input, zip, properties.maxExtractedBytes(), properties.maxExtractedBytes(), expandedTotal);
                }
                zip.closeEntry();
            }
            zip.finish();
            return new SanitizedWorkbook(output.toByteArray(), true);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Workbook ZIP structure could not be read safely", exception);
        }
    }

    private static byte[] repairStyles(byte[] content) {
        String styles = new String(content, StandardCharsets.UTF_8);
        Matcher matcher = RGB_ATTRIBUTE.matcher(styles);
        StringBuffer repaired = new StringBuffer();
        while (matcher.find()) {
            String rgb = matcher.group(1);
            String replacement;
            if (rgb.matches("[0-9A-Fa-f]{8}")) replacement = matcher.group();
            else if (rgb.matches("[0-9A-Fa-f]{6}")) replacement = " rgb=\"FF" + rgb + "\"";
            else replacement = "";
            matcher.appendReplacement(repaired, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(repaired);
        return repaired.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String safeEntryName(String name) {
        String normalized = name == null ? "" : name.replace('\\', '/');
        if (normalized.isBlank() || normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")
                || java.util.Arrays.stream(normalized.split("/")).anyMatch(".."::equals)) {
            throw new IllegalArgumentException("Workbook contains an unsafe entry path");
        }
        return normalized;
    }

    private static byte[] readBounded(ZipInputStream input, long entryLimit, long totalLimit, long[] expandedTotal) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copyBounded(input, output, entryLimit, totalLimit, expandedTotal);
        return output.toByteArray();
    }

    private static void copyBounded(ZipInputStream input, OutputStream output, long entryLimit, long totalLimit, long[] expandedTotal) throws IOException {
        byte[] buffer = new byte[8192];
        long entryTotal = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            entryTotal += read;
            expandedTotal[0] += read;
            if (entryTotal > entryLimit) throw new IllegalArgumentException("Workbook entry exceeds the configured limit");
            if (expandedTotal[0] > totalLimit) throw new IllegalArgumentException("Workbook expanded content exceeds the configured limit");
            output.write(buffer, 0, read);
        }
    }

    private RowKind classify(List<String> values, List<String> headers) {
        if (values.isEmpty() || values.stream().allMatch(String::isBlank)) return RowKind.FOOTER;
        long headerMatches = values.stream().map(LegacyNormalizer::text).filter(headers::contains).count();
        if (headerMatches >= Math.max(2, headers.size() / 3)) return RowKind.HEADER;
        String joined = LegacyNormalizer.text(String.join(" ", values));
        if (joined.matches(".*(جمع|مجموع|جمع کل|صفحه).*") && numericCount(values) < 2) return RowKind.SUBTOTAL;
        return numericCount(values) > 0 || values.stream().filter(v -> !v.isBlank()).count() >= 3 ? RowKind.DATA : RowKind.UNKNOWN;
    }

    private Record record(String sheetName, int rowNumber, List<String> headers, String type, List<String> rowValues) {
        Map<String,Object> raw = new LinkedHashMap<>(); Map<String,Object> normalized = new LinkedHashMap<>();
        raw.put("sourceSheet", sheetName); raw.put("sourceRowNumber", rowNumber + 1);
        for (int i=0;i<headers.size();i++) { String value=i<rowValues.size()?rowValues.get(i):""; if(value.isBlank())continue; String header=headers.get(i); raw.put(header,value); String target=target(header); if(target!=null)normalized.put(target,normalize(target,value)); }
        normalized.put("reportType",type); normalized.put("sourceSheet",sheetName); normalized.put("sourceRowNumber",rowNumber+1);
        String code=first(normalized,"documentNumber","accountCode","productCode","chequeNumber","invoiceNumber"); return new Record(sheetName+":"+(rowNumber+1),code,LegacyNormalizer.code(code),raw,normalized);
    }

    private static Object normalize(String target, String value) {
        if (Set.of("debit","credit","amount","quantity","unitAmount","totalAmount","openingDebit","openingCredit","periodDebit","periodCredit","closingDebit","closingCredit").contains(target)) return money(value);
        if (target.endsWith("Date") || "date".equals(target)) return date(value);
        return Set.of("phone","accountCode","productCode","chequeNumber","sayadIdentifier","documentNumber","invoiceNumber").contains(target) ? LegacyNormalizer.code(value) : LegacyNormalizer.text(value);
    }
    private static String target(String header) {
        return switch (LegacyNormalizer.text(header)) {
            case "بدهکار" -> "debit"; case "بستانکار" -> "credit"; case "مبلغ" -> "amount"; case "تعداد" -> "quantity";
            case "مبلغ فی" -> "unitAmount"; case "مبلغ کل" -> "totalAmount"; case "تاریخ" -> "date"; case "سند" -> "documentNumber";
            case "شماره فاکتور" -> "invoiceNumber"; case "کد کالا" -> "productCode"; case "نام کالا" -> "productName";
            case "کد", "کد حسابداری" -> "accountCode"; case "شرح حساب", "نام حساب", "نام حساب کل" -> "accountName";
            case "شرح" -> "description"; case "شماره" -> "chequeNumber"; case "شماره صیادی" -> "sayadIdentifier";
            case "سررسید" -> "dueDate"; case "تاریخ دریافت" -> "receivedDate"; case "تاریخ واگذاری" -> "assignedDate";
            case "تاریخ  پاس شدن" -> "clearedDate"; case "وضعیت چک" -> "chequeStatus"; case "نام بانک" -> "bankName";
            case "شعبه" -> "bankBranch"; case "نام صاحب چک" -> "ownerName"; case "نام گیرنده" -> "recipientName";
            case "کد صاحب چک" -> "ownerLegacyCode"; case "کد گیرنده" -> "recipientLegacyCode";
            case "مانده اول دوره بدهکار" -> "openingDebit"; case "مانده اول دوره بستانکار" -> "openingCredit";
            case "گردش طی دوره بدهکار" -> "periodDebit"; case "گردش طی دوره بستانکار" -> "periodCredit";
            case "مانده پایان دوره بدهکار" -> "closingDebit"; case "مانده پایان دوره بستانکار" -> "closingCredit";
            default -> null;
        };
    }
    private static BigDecimal money(String value) {
        String s = LegacyNormalizer.text(value).replace(",", "").replace("٬", "").replace("ریال", "").replace("﷼", "").replace(" ", "");
        if (s.isBlank() || s.matches("[-–—]+")) return null;
        if (s.matches("\\(\\d+(?:\\.\\d+)?\\)")) s = "-" + s.substring(1, s.length() - 1);
        if (!s.matches("[+-]?\\d+(?:\\.\\d+)?")) return null;
        return new BigDecimal(s);
    }
    private static String date(String value) { String s=LegacyNormalizer.text(value).replace('.', '/'); return s.matches("1[34]\\d{2}/\\d{2}/\\d{2}") ? s : LegacyNormalizer.text(value); }
    private static String first(Map<String,Object> values, String... keys) { for (String key:keys) if (values.get(key)!=null && !String.valueOf(values.get(key)).isBlank()) return String.valueOf(values.get(key)); return null; }
    private static List<Map<String,Object>> dictionary(List<String> headers) { return headers.stream().map(h -> Map.<String,Object>of("sourceField",h,"sourceType","TEXT","meaning", target(h)==null?"UNKNOWN":target(h),"confidence",target(h)==null?"LOW":"HIGH")).toList(); }
    private static List<String> headers(List<String> values) {
        List<String> result = new ArrayList<>();
        Map<String,Integer> seen = new LinkedHashMap<>();
        for (String value : values) {
            String base = LegacyNormalizer.text(value);
            int occurrence = seen.merge(base, 1, Integer::sum);
            result.add(occurrence == 1 ? base : base + "#" + occurrence);
        }
        return result;
    }
    private static long numericCount(List<String> values) { return values.stream().filter(value -> money(value) != null).count(); }
    private static String sha256(byte[] bytes) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch(Exception e){ throw new IllegalStateException(e); } }
    private record Detection(String type, String confidence) {}
    private record SanitizedWorkbook(byte[] bytes, boolean changed) {}
    private enum RowKind { DATA, HEADER, SUBHEADER, SUBTOTAL, TOTAL, FOOTER, UNKNOWN }
}
