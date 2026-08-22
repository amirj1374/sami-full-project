package com.sami.app.siminvestment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimRondiAnalyzerTest {
    @Test void normalizesSupportedIranian0912Forms() {
        assertThat(SimRondiAnalyzer.normalize("0912 332 3333")).isEqualTo("09123323333");
        assertThat(SimRondiAnalyzer.normalize("9123323333")).isEqualTo("09123323333");
        assertThat(SimRondiAnalyzer.normalize("989123323333")).isEqualTo("09123323333");
        assertThatThrownBy(() -> SimRondiAnalyzer.normalize("09135555555")).isInstanceOf(RuntimeException.class);
    }

    @Test void detectsRepeatedAndStructuredPatternsDeterministically() {
        var result = SimRondiAnalyzer.analyze("09123323333");
        assertThat(result.patterns()).contains("FOUR_OR_MORE_REPEAT", "SPECIAL_ENDING");
        assertThat(result.score()).isBetween(0, 100);
        assertThat(result.numberClass()).isIn("ROUND", "SPECIAL", "VIP");
    }

    @Test void ordinaryNumberDoesNotReceiveAVipClassification() {
        assertThat(SimRondiAnalyzer.analyze("09126545008").numberClass()).isNotEqualTo("VIP");
    }
}
