package com.sami.app.legacyimport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyNormalizerTest {
    @Test void normalizesPersianLettersDigitsAndWhitespace() {
        assertThat(LegacyNormalizer.text("  كالا\u200cي ۱۲۳  ")).isEqualTo("کالا ی 123");
    }

    @Test void normalizesIranianPhonePrefixes() {
        assertThat(LegacyNormalizer.phone("+98 912-345-6789")).isEqualTo("09123456789");
        assertThat(LegacyNormalizer.phone("0098 912 345 6789")).isEqualTo("09123456789");
    }
}
