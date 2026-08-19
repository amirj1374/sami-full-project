package com.sami.app.legacyimport;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsanExcelPackageAdapterTest {
    private final AsanExcelPackageAdapter adapter = new AsanExcelPackageAdapter(
            new LegacyImportProperties(20_000_000, 100, 50_000_000, 10_000_000, 50, "unrar"));

    @Test
    void parsesManifestMappedWorkbookAndPreservesLegacyCodeInStaging() throws Exception {
        byte[] packageBytes = packageWithManifest("مخاطبین(1).xlsx", "parties / customers / suppliers", contacts());

        var analysis = adapter.analyze(packageBytes, true);

        assertThat(analysis.files()).hasSize(2);
        assertThat(analysis.datasets()).singleElement().satisfies(dataset -> {
            assertThat(dataset.semanticType()).isEqualTo("PARTY");
            assertThat(dataset.sourceCount()).isEqualTo(1);
            assertThat(dataset.metadata()).containsEntry("stagingOnly", true)
                    .containsEntry("canonicalWrites", 0)
                    .containsEntry("uniqueLegacyCodes", 1);
            assertThat(dataset.records()).singleElement().satisfies(record -> {
                assertThat(record.sourceId()).isEqualTo("1001");
                assertThat(record.legacyCode()).isEqualTo("1001");
                assertThat(record.raw()).containsEntry("Name", "مشتری آزمایشی");
            });
        });
    }

    @Test
    void analysisModeCountsRowsWithoutCopyingSensitiveRecords() throws Exception {
        var analysis = adapter.analyze(
                packageWithManifest("مخاطبین(1).xlsx", "parties / customers / suppliers", contacts()), false);

        assertThat(analysis.datasets()).singleElement().satisfies(dataset -> {
            assertThat(dataset.sourceCount()).isEqualTo(1);
            assertThat(dataset.records()).isEmpty();
            assertThat(dataset.dictionary()).allSatisfy(field -> assertThat(field).containsEntry("sampleValue", "[redacted]"));
        });
    }

    @Test
    void rejectsTraversalBeforeWorkbookParsing() throws Exception {
        byte[] bytes;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("../secret.xlsx"));
            zip.write(new byte[]{1, 2, 3});
            zip.closeEntry();
            zip.finish();
            bytes = output.toByteArray();
        }

        assertThatThrownBy(() -> adapter.analyze(bytes, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traversal");
    }

    @Test
    void rejectsPackageWithoutManifest() throws Exception {
        byte[] bytes;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("source/data.xlsx"));
            zip.write(contacts());
            zip.closeEntry();
            zip.finish();
            bytes = output.toByteArray();
        }

        assertThatThrownBy(() -> adapter.analyze(bytes, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manifest");
    }

    private static byte[] packageWithManifest(String sourceName, String target, byte[] source) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("SAMI_ERP_Migration_Manifest.xlsx"));
            zip.write(manifest(sourceName, target));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("source/" + sourceName));
            zip.write(source);
            zip.closeEntry();
            zip.finish();
            return output.toByteArray();
        }
    }

    private static byte[] manifest(String sourceName, String target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Migration Manifest");
            var header = sheet.createRow(0);
            String[] headings = {"ردیف", "فایل منبع", "ماژول مقصد در SAMI ERP", "نوع استفاده", "وضعیت", "توضیحات"};
            for (int index = 0; index < headings.length; index++) header.createCell(index).setCellValue(headings[index]);
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(1);
            row.createCell(1).setCellValue(sourceName);
            row.createCell(2).setCellValue(target);
            row.createCell(3).setCellValue("Import / Validation");
            row.createCell(4).setCellValue("Ready for mapping");
            row.createCell(5).setCellValue("fixture");
            var rules = workbook.createSheet("Migration Rules");
            rules.createRow(0).createCell(0).setCellValue("Rule");
            rules.getRow(0).createCell(1).setCellValue("Requirement");
            rules.createRow(1).createCell(0).setCellValue("No destructive import");
            rules.getRow(1).createCell(1).setCellValue("Staging only");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] contacts() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet1");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("Asan_Code");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Bed");
            header.createCell(3).setCellValue("Bes");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue(1001);
            row.createCell(1).setCellValue("مشتری آزمایشی");
            row.createCell(2).setCellValue(500_000);
            row.createCell(3).setCellValue(250_000);
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
