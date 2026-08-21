package com.sami.app.legacyimport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
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
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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

    @Override
    public Analysis analyze(String filename, byte[] bytes, boolean includeRecords) {
        if (!supports(filename, bytes)) throw new IllegalArgumentException("Only XLSX Asan accounting reports are accepted");
        if (bytes.length > properties.maxSingleFileBytes()) throw new IllegalArgumentException("Workbook exceeds the configured per-file limit");
        List<Dataset> datasets = new ArrayList<>();
        List<Message> messages = new ArrayList<>();
        Map<String, Object> fileMetadata = new LinkedHashMap<>();
        fileMetadata.put("readOnly", true); fileMetadata.put("reportSource", "ASAN_EXCEL_REPORT"); fileMetadata.put("macrosExecuted", false);
        SanitizedWorkbook sanitized = sanitizeInvalidStyleColors(bytes);
        if (sanitized.changed()) {
            fileMetadata.put("styleSanitizedInMemory", true);
            messages.add(new Message(filename, null, "WARNING", "INVALID_STYLE_COLOR_SANITIZED",
                    "Malformed workbook color attributes were normalized in memory only; source data and cells were not changed."));
        }
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(sanitized.bytes()))) {
            if (workbook.getNumberOfSheets() > 100) throw new IllegalArgumentException("Workbook contains too many worksheets");
            for (Sheet sheet : workbook) {
                ParsedSheet parsed = parseSheet(filename, sheet, includeRecords, new DataFormatter(Locale.ROOT));
                datasets.add(parsed.dataset()); messages.addAll(parsed.messages());
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Workbook contents could not be parsed safely", ex);
        }
        if (datasets.isEmpty()) throw new IllegalArgumentException("Workbook has no readable worksheets");
        return new Analysis(List.of(new SourceFile(filename, filename, bytes.length, sha256(bytes), "XLSX", "SUPPORTED", fileMetadata)), datasets, messages);
    }

    /**
     * Some Asan exports contain non-OOXML RGB attributes in xl/styles.xml.
     * Styles are presentation-only, so repair is strictly in memory: valid
     * six-digit colors get the opaque alpha channel POI requires and malformed
     * color attributes are omitted.  No workbook cell, value, formula, or
     * source archive byte is written back to disk.
     */
    private static SanitizedWorkbook sanitizeInvalidStyleColors(byte[] bytes) {
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(bytes)); ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            boolean changed = false;
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] content = input.readAllBytes();
                if ("xl/styles.xml".equals(entry.getName())) {
                    String styles = new String(content, StandardCharsets.UTF_8);
                    Matcher matcher = RGB_ATTRIBUTE.matcher(styles);
                    StringBuffer repaired = new StringBuffer();
                    boolean styleChanged = false;
                    while (matcher.find()) {
                        String rgb = matcher.group(1);
                        String replacement;
                        if (rgb.matches("[0-9A-Fa-f]{8}")) replacement = matcher.group();
                        else if (rgb.matches("[0-9A-Fa-f]{6}")) replacement = " rgb=\"FF" + rgb + "\"";
                        else replacement = "";
                        styleChanged |= !replacement.equals(matcher.group());
                        matcher.appendReplacement(repaired, Matcher.quoteReplacement(replacement));
                    }
                    matcher.appendTail(repaired);
                    if (styleChanged) { content = repaired.toString().getBytes(StandardCharsets.UTF_8); changed = true; }
                }
                ZipEntry copy = new ZipEntry(entry.getName());
                zip.putNextEntry(copy); zip.write(content); zip.closeEntry();
            }
            zip.finish();
            return new SanitizedWorkbook(changed ? output.toByteArray() : bytes, changed);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Workbook ZIP structure could not be read safely", exception);
        }
    }

    private ParsedSheet parseSheet(String filename, Sheet sheet, boolean includeRecords, DataFormatter formatter) {
        int headerIndex = findHeader(sheet, formatter);
        if (headerIndex < 0) {
            Dataset dataset = new Dataset(filename, filename + "#" + sheet.getSheetName(), sheet.getSheetName(), "UNKNOWN", "PARTIAL", "UTF-8", 0,
                    List.of(), Map.of("detectionConfidence", "LOW", "reason", "No verified header signature"), List.of());
            return new ParsedSheet(dataset, List.of(new Message(filename, dataset.key(), "WARNING", "HEADER_NOT_DETECTED", "No supported report header was found in worksheet '" + sheet.getSheetName() + "'.")));
        }
        List<String> headers = headers(sheet.getRow(headerIndex), formatter);
        Detection detection = detect(sheet, headerIndex, headers, formatter);
        List<Record> records = new ArrayList<>();
        int skipped = headerIndex + 1, sourceRows = 0;
        for (int i = headerIndex + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            List<String> rowValues = values(row, formatter);
            RowKind kind = classify(rowValues, headers);
            if (kind != RowKind.DATA) { skipped++; continue; }
            sourceRows++;
            if (includeRecords) records.add(record(sheet, row, headers, detection.type(), rowValues));
        }
        Map<String,Object> metadata = new LinkedHashMap<>();
        metadata.put("reportType", detection.type()); metadata.put("detectionConfidence", detection.confidence());
        metadata.put("headerRow", headerIndex + 1); metadata.put("skippedRows", skipped); metadata.put("rowClassification", "DATA/HEADER/SUBTOTAL/TOTAL/FOOTER/UNKNOWN");
        metadata.put("requiresTypeConfirmation", "LOW".equals(detection.confidence()));
        Dataset dataset = new Dataset(filename, filename + "#" + sheet.getSheetName(), sheet.getSheetName(), detection.type(),
                "UNKNOWN".equals(detection.type()) ? "PARTIAL" : "SUPPORTED", "UTF-8", sourceRows, dictionary(headers), metadata, records);
        List<Message> messages = new ArrayList<>();
        if ("LOW".equals(detection.confidence())) messages.add(new Message(filename, dataset.key(), "WARNING", "AMBIGUOUS_REPORT_TYPE", "The worksheet is an account ledger with no safe structural distinction between general, subsidiary, and detailed levels. Confirm its type before acceptance."));
        return new ParsedSheet(dataset, messages);
    }

    private Detection detect(Sheet sheet, int headerIndex, List<String> headers, DataFormatter formatter) {
        Set<String> normalized = headers.stream().map(LegacyNormalizer::text).collect(java.util.stream.Collectors.toSet());
        if (normalized.containsAll(CHEQUE_HEADERS)) return new Detection(headers.contains("نام صاحب چک") ? "CHEQUE_RECEIVABLE" : "CHEQUE_PAYABLE", "HIGH");
        if (normalized.contains("مانده پایان دوره بدهکار") && normalized.contains("مانده پایان دوره بستانکار")) return new Detection("TRIAL_BALANCE", "HIGH");
        if (normalized.contains("نام کالا") && normalized.contains("کد کالا") && normalized.contains("شماره فاکتور")) return new Detection("PRODUCT_MOVEMENT", "HIGH");
        if (normalized.containsAll(LEDGER_HEADERS)) {
            boolean daily = false;
            for (int row = 0; row <= Math.min(headerIndex, 12); row++) {
                for (String value : values(sheet.getRow(row), formatter)) if (LegacyNormalizer.text(value).contains("دفتر روزنامه")) daily = true;
            }
            return daily ? new Detection("ACCOUNTING_JOURNAL", "HIGH") : new Detection("ACCOUNT_LEDGER", "LOW");
        }
        return new Detection("UNKNOWN", "LOW");
    }

    private int findHeader(Sheet sheet, DataFormatter formatter) {
        for (int i = sheet.getFirstRowNum(); i <= Math.min(sheet.getLastRowNum(), 40); i++) {
            List<String> values = values(sheet.getRow(i), formatter);
            long score = values.stream().map(LegacyNormalizer::text).filter(v -> LEDGER_HEADERS.contains(v) || CHEQUE_HEADERS.contains(v)
                    || Set.of("نام کالا", "کد کالا", "شماره فاکتور", "مانده پایان دوره بدهکار", "مانده پایان دوره بستانکار").contains(v)).count();
            if (score >= 3) return i;
        }
        return -1;
    }

    private RowKind classify(List<String> values, List<String> headers) {
        if (values.isEmpty() || values.stream().allMatch(String::isBlank)) return RowKind.FOOTER;
        long headerMatches = values.stream().map(LegacyNormalizer::text).filter(headers::contains).count();
        if (headerMatches >= Math.max(2, headers.size() / 3)) return RowKind.HEADER;
        String joined = LegacyNormalizer.text(String.join(" ", values));
        if (joined.matches(".*(جمع|مجموع|جمع کل|صفحه).*") && numericCount(values) < 2) return RowKind.SUBTOTAL;
        return numericCount(values) > 0 || values.stream().filter(v -> !v.isBlank()).count() >= 3 ? RowKind.DATA : RowKind.UNKNOWN;
    }

    private Record record(Sheet sheet, Row row, List<String> headers, String type, List<String> rowValues) {
        Map<String,Object> raw = new LinkedHashMap<>(); Map<String,Object> normalized = new LinkedHashMap<>();
        raw.put("sourceSheet", sheet.getSheetName()); raw.put("sourceRowNumber", row.getRowNum() + 1);
        for (int i=0;i<headers.size();i++) {
            String value = i < rowValues.size() ? rowValues.get(i) : ""; if (value.isBlank()) continue;
            String header = headers.get(i); raw.put(header, value);
            String target = target(header); if (target != null) normalized.put(target, normalize(target, value));
        }
        normalized.put("reportType", type); normalized.put("sourceSheet", sheet.getSheetName()); normalized.put("sourceRowNumber", row.getRowNum() + 1);
        String code = first(normalized, "documentNumber", "accountCode", "productCode", "chequeNumber", "invoiceNumber");
        return new Record(sheet.getSheetName() + ":" + (row.getRowNum()+1), code, LegacyNormalizer.code(code), raw, normalized);
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
    private static List<String> headers(Row row, DataFormatter formatter) { List<String> result=new ArrayList<>(); Map<String,Integer> seen=new LinkedHashMap<>(); for (String value:values(row, formatter)) { String base=LegacyNormalizer.text(value); int n=seen.merge(base,1,Integer::sum); result.add(n==1?base:base+"#"+n); } return result; }
    private static List<String> values(Row row, DataFormatter formatter) { if(row==null)return List.of(); List<String> values=new ArrayList<>(); for(int i=0;i<row.getLastCellNum();i++) values.add(cell(row.getCell(i), formatter)); return values; }
    private static String cell(Cell cell, DataFormatter formatter) { return cell==null?"":formatter.formatCellValue(cell).trim(); }
    private static long numericCount(List<String> values) { return values.stream().filter(v -> { try { money(v); return !v.isBlank(); } catch (RuntimeException ignored) { return false; }}).count(); }
    private static String sha256(byte[] bytes) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch(Exception e){ throw new IllegalStateException(e); } }
    private record Detection(String type, String confidence) {}
    private record SanitizedWorkbook(byte[] bytes, boolean changed) {}
    private record ParsedSheet(Dataset dataset, List<Message> messages) {}
    private enum RowKind { DATA, HEADER, SUBHEADER, SUBTOTAL, TOTAL, FOOTER, UNKNOWN }
}
