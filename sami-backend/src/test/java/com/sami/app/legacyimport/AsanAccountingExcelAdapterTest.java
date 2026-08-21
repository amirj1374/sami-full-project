package com.sami.app.legacyimport;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

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
}
