package com.sami.app.legacyimport;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class AsanAccountingExcelAdapterTest {
    private final AsanAccountingExcelAdapter adapter = new AsanAccountingExcelAdapter(
            new LegacyImportProperties(20_000_000, 100, 50_000_000, 10_000_000, 50, "unrar"));

    @Test
    void detectsDailyJournalAndSkipsRepeatedHeaders() throws Exception {
        var analysis = adapter.analyze("daily.xlsx", workbook("دفتر روزنامه", List.of(
                new String[]{"دفتر روزنامه", "", "", "", "", ""},
                new String[]{"بدهکار", "بستانکار", "شرح", "کد", "سند", "تاریخ"},
                new String[]{"", "125,000", "خرید", "992", "1", "1403/12/30"},
                new String[]{"125,000", "", "کالا", "984", "1", "1403/12/30"},
                new String[]{"بدهکار", "بستانکار", "شرح", "کد", "سند", "تاریخ"}
        )), true);

        assertThat(analysis.datasets()).singleElement().satisfies(dataset -> {
            assertThat(dataset.semanticType()).isEqualTo("ACCOUNTING_JOURNAL");
            assertThat(dataset.sourceCount()).isEqualTo(2);
            assertThat(dataset.records()).allSatisfy(record -> assertThat(record.raw()).containsKeys("sourceSheet", "sourceRowNumber"));
        });
    }

    @Test
    void detectsTrialBalanceAndPreservesMoneyWithoutFloatingPoint() throws Exception {
        var analysis = adapter.analyze("trial.xlsx", workbook("تراز", List.of(
                new String[]{"مانده پایان دوره بدهکار", "مانده پایان دوره بستانکار", "گردش طی دوره بدهکار", "گردش طی دوره بستانکار", "شرح حساب", "کد"},
                new String[]{"1,234,567", "", "1,234,567", "", "صندوق", "986"}
        )), true);

        assertThat(analysis.datasets()).singleElement().satisfies(dataset -> {
            assertThat(dataset.semanticType()).isEqualTo("TRIAL_BALANCE");
            assertThat(dataset.records().getFirst().normalized()).containsEntry("closingDebit", new java.math.BigDecimal("1234567"));
        });
    }

    @Test
    void detectsChequeLayoutAndNormalizesStatusEvidence() throws Exception {
        var analysis = adapter.analyze("cheques.xlsx", workbook("چک ها", List.of(
                new String[]{"شماره", "نام بانک", "مبلغ", "سررسید", "وضعیت چک", "نام صاحب چک", "شماره صیادی"},
                new String[]{"123", "ملت", "500,000", "1404/01/02", "وصول شده", "مشتری آزمایشی", "9988"}
        )), true);

        assertThat(analysis.datasets()).singleElement().satisfies(dataset -> {
            assertThat(dataset.semanticType()).isEqualTo("CHEQUE_RECEIVABLE");
            assertThat(dataset.records().getFirst().normalized()).containsEntry("chequeNumber", "123").containsEntry("amount", new java.math.BigDecimal("500000"));
        });
    }

    @Test
    void preservesUnparseableMoneyCellsAsRawEvidenceWithoutFailingTheBatch() throws Exception {
        var analysis = adapter.analyze("journal.xlsx", workbook("دفتر روزنامه", List.of(
                new String[]{"دفتر روزنامه", "", "", "", "", ""},
                new String[]{"بدهکار", "بستانکار", "شرح", "کد", "سند", "تاریخ"},
                new String[]{"-", "(125,000)", "اصلاح", "984", "2", "1404/01/01"}
        )), true);

        var record = analysis.datasets().getFirst().records().getFirst();
        assertThat(record.raw()).containsEntry("بدهکار", "-");
        assertThat(record.normalized()).containsEntry("debit", null).containsEntry("credit", new java.math.BigDecimal("-125000"));
    }

    @Test
    void streamsRecordsWithTheAnalyzedDatasetKeyWithoutRetainingThemInAnalysis() throws Exception {
        byte[] source = workbook("دفتر روزنامه", List.of(
                new String[]{"دفتر روزنامه", "", "", "", "", ""},
                new String[]{"بدهکار", "بستانکار", "شرح", "کد", "سند", "تاریخ"},
                new String[]{"100", "", "ثبت اول", "10", "1", "1404/01/01"},
                new String[]{"", "100", "ثبت دوم", "20", "1", "1404/01/01"}
        ));

        var analysis = adapter.analyze("daily.xlsx", source, false);
        List<String> datasetKeys = new ArrayList<>();
        List<LegacyImportAdapter.Record> streamed = new ArrayList<>();
        adapter.streamRecords("daily.xlsx", source, (datasetKey, record) -> {
            datasetKeys.add(datasetKey);
            streamed.add(record);
        });

        assertThat(analysis.datasets()).singleElement().satisfies(dataset -> {
            assertThat(dataset.key()).isEqualTo("daily.xlsx#دفتر روزنامه");
            assertThat(dataset.sourceCount()).isEqualTo(2);
            assertThat(dataset.records()).isEmpty();
        });
        assertThat(datasetKeys).containsOnly("daily.xlsx#دفتر روزنامه");
        assertThat(streamed).hasSize(2);
        assertThat(streamed.getFirst().sourceId()).isEqualTo("دفتر روزنامه:3");
        assertThat(streamed.getFirst().normalized()).containsEntry("debit", new java.math.BigDecimal("100"));
        assertThat(streamed.getLast().normalized()).containsEntry("credit", new java.math.BigDecimal("100"));
    }

    @Test
    void streamsAWorkbookLargerThanTheFailedDailyJournalWithoutRetainingRows() throws Exception {
        int rows = 34_000;
        byte[] source = largeJournal(rows);

        var analysis = adapter.analyze("large-daily.xlsx", source, false);
        AtomicInteger streamed = new AtomicInteger();
        adapter.streamRecords("large-daily.xlsx", source, (datasetKey, record) -> streamed.incrementAndGet());

        assertThat(analysis.datasets()).singleElement().satisfies(dataset -> {
            assertThat(dataset.semanticType()).isEqualTo("ACCOUNTING_JOURNAL");
            assertThat(dataset.sourceCount()).isEqualTo(rows);
            assertThat(dataset.records()).isEmpty();
        });
        assertThat(streamed).hasValue(rows);
    }

    @Test
    void rejectsAnXlsxEntryThatExpandsPastTheConfiguredLimit() throws Exception {
        var bounded = new AsanAccountingExcelAdapter(
                new LegacyImportProperties(20_000_000, 100, 50_000_000, 1_024, 50, "unrar"));
        byte[] source;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("xl/styles.xml"));
            zip.write("A".repeat(2_048).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.finish();
            source = output.toByteArray();
        }

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> bounded.analyze("oversized-entry.xlsx", source, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entry exceeds");
    }

    @Test
    void rejectsUnsafeWorkbookEntryPathsBeforePoiParsing() throws Exception {
        byte[] source;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("../xl/styles.xml"));
            zip.write("<styleSheet/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.finish();
            source = output.toByteArray();
        }

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.analyze("unsafe.xlsx", source, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsafe entry path");
    }

    private static byte[] workbook(String sheetName, List<String[]> rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(sheetName);
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                var row = sheet.createRow(rowIndex);
                String[] values = rows.get(rowIndex);
                for (int column = 0; column < values.length; column++) row.createCell(column).setCellValue(values[column]);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] largeJournal(int dataRows) throws Exception {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.setCompressTempFiles(true);
            workbook.setZip64Mode(Zip64Mode.Never);
            var sheet = workbook.createSheet("دفتر روزنامه");
            sheet.createRow(0).createCell(0).setCellValue("دفتر روزنامه");
            String[] headings = {"بدهکار", "بستانکار", "شرح", "کد", "سند", "تاریخ"};
            var header = sheet.createRow(1);
            for (int column = 0; column < headings.length; column++) header.createCell(column).setCellValue(headings[column]);
            for (int index = 0; index < dataRows; index++) {
                var row = sheet.createRow(index + 2);
                row.createCell(index % 2).setCellValue("100");
                row.createCell(2).setCellValue("ثبت آزمایشی");
                row.createCell(3).setCellValue(String.valueOf(index % 100));
                row.createCell(4).setCellValue(String.valueOf(index / 2 + 1));
                row.createCell(5).setCellValue("1404/01/01");
            }
            workbook.write(output);
            workbook.dispose();
            return output.toByteArray();
        }
    }
}
