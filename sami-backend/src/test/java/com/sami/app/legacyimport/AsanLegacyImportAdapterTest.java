package com.sami.app.legacyimport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test void acceptsSafeNestedEntryPaths() {
        AsanLegacyImportAdapter.validateEntryPath("ASAN6/haji/sh_tm_n.dat");
    }

    @Test void parsesSuppliedArchiveWithoutGuessingUnsupportedLayouts() throws Exception {
        Path archive = suppliedArchive();
        var adapter = adapter(2_000);

        var result = adapter.analyze(Files.readAllBytes(archive), false);

        assertThat(result.files()).hasSize(190);
        assertThat(result.datasets()).hasSize(57);
        assertThat(result.datasets()).anySatisfy(dataset -> {
            if ("My_Zone".equals(dataset.sourceTable())) assertThat(dataset.sourceCount()).isEqualTo(7_401);
        });
        assertThat(result.files()).anyMatch(file -> "UNSUPPORTED".equals(file.supportStatus()));
    }

    @Test void enforcesArchiveFileCountBeforeImportingRecords() throws Exception {
        Path archive = suppliedArchive();
        assertThatThrownBy(() -> adapter(1).analyze(Files.readAllBytes(archive), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("file count");
    }

    private static Path suppliedArchive() {
        String configured = System.getProperty("legacy.asan.archive");
        Assumptions.assumeTrue(configured != null && Files.isRegularFile(Path.of(configured)),
                "Set -Dlegacy.asan.archive to run the supplied-archive parser gate");
        return Path.of(configured);
    }

    private static AsanLegacyImportAdapter adapter(int maxFiles) {
        String unrar = System.getProperty("legacy.unrar.executable", "unrar");
        return new AsanLegacyImportAdapter(new LegacyImportProperties(104_857_600L, maxFiles,
                536_870_912L, 134_217_728L, 500, unrar));
    }
}
