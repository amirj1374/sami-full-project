package com.sami.app.legacyimport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

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

    @Test void analyzesDeterministicRar5FixtureWithoutGuessingUnsupportedLayouts(@TempDir Path temp) throws Exception {
        var adapter = adapter(2_000, fakeUnrar(temp));

        var result = adapter.analyze(rar5Fixture(), false);

        assertThat(result.files()).hasSize(2);
        assertThat(result.datasets()).hasSize(1);
        assertThat(result.datasets()).anySatisfy(dataset -> {
            if ("report-fields".equals(dataset.sourceTable())) assertThat(dataset.dictionary()).isNotEmpty();
        });
        assertThat(result.files()).anyMatch(file -> "UNSUPPORTED".equals(file.supportStatus()));
    }

    @Test void enforcesArchiveFileCountBeforeImportingRecords(@TempDir Path temp) throws Exception {
        assertThatThrownBy(() -> adapter(1, fakeUnrar(temp)).analyze(rar5Fixture(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("file count");
    }

    private static byte[] rar5Fixture() {
        return new byte[]{0x52, 0x61, 0x72, 0x21, 0x1a, 0x07, 0x01, 0x00};
    }

    private static AsanLegacyImportAdapter adapter(int maxFiles, Path unrar) {
        return new AsanLegacyImportAdapter(new LegacyImportProperties(104_857_600L, maxFiles,
                536_870_912L, 134_217_728L, 500, unrar.toString()));
    }

    private static Path fakeUnrar(Path temp) throws Exception {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        Path executable = temp.resolve(windows ? "fake-unrar.cmd" : "fake-unrar");
        String script = windows
                ? "@echo off\r\nif \"%1\"==\"lb\" (\r\n  echo ASAN6/report.fr3\r\n  echo ASAN6/blob.bin\r\n  exit /b 0\r\n)\r\nif \"%1\"==\"p\" (\r\n  if \"%5\"==\"ASAN6/report.fr3\" (echo TfrxReport FieldName=\"CustomerCode\") else (echo BINARY)\r\n  exit /b 0\r\n)\r\nexit /b 1\r\n"
                : "#!/bin/sh\nif [ \"$1\" = \"lb\" ]; then printf '%s\\n' ASAN6/report.fr3 ASAN6/blob.bin; exit 0; fi\nif [ \"$1\" = \"p\" ]; then for entry do last=$entry; done; if [ \"$last\" = \"ASAN6/report.fr3\" ]; then printf '%s' 'TfrxReport FieldName=\"CustomerCode\"'; else printf '%s' BINARY; fi; exit 0; fi\nexit 1\n";
        Files.writeString(executable, script);
        if (!windows) Files.setPosixFilePermissions(executable, Set.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        return executable;
    }
}
