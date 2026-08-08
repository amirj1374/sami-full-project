package com.sami.app.legacyimport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsanLegacyImportAdapterTest {
    @Test void rejectsNonRarContent() {
        assertThatThrownBy(() -> AsanLegacyImportAdapter.requireRarSignature(new byte[]{1,2,3}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejectsTraversalAndAbsoluteEntryPaths() {
        assertThatThrownBy(() -> AsanLegacyImportAdapter.validateEntryPath("../secret.env")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AsanLegacyImportAdapter.validateEntryPath("C:\\secret.env")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AsanLegacyImportAdapter.validateEntryPath("/root/key.pem")).isInstanceOf(IllegalArgumentException.class);
    }
}
