package com.sami.app;

import com.sami.app.licensing.service.LicenseReportService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsvEncodingTest {

    private final LicenseReportService reports = new LicenseReportService(null, null, null, null, null);

    @Test
    void csvStartsWithUtf8BomAndPreservesPersianAndMixedText() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("نام", "مشتری تستی");
        row.put("description", "خرید آزمایشی - Test 42");
        row.put("amount", 125000);

        String csv = reports.toCsv(List.of(row));
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        assertThat(bytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(new String(bytes, StandardCharsets.UTF_8))
                .contains("نام", "مشتری تستی", "خرید آزمایشی - Test 42", "125000");
    }

    @Test
    void emptyCsvStillContainsEncodingMarker() {
        assertThat(reports.toCsv(List.of())).isEqualTo("\uFEFF");
    }
}
