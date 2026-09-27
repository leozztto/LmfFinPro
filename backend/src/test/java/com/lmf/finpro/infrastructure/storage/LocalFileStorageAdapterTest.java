package com.lmf.finpro.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageAdapterTest {

    @TempDir Path baseDir;

    @Test
    void storesLoadsAndDeletesInsideTheBaseDirectory() {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(baseDir.toString());
        byte[] content = {1, 2, 3};

        storage.store("abc-123.pdf", content);

        assertThat(Files.exists(baseDir.resolve("abc-123.pdf"))).isTrue();
        assertThat(storage.load("abc-123.pdf")).containsExactly(1, 2, 3);

        storage.delete("abc-123.pdf");
        assertThat(Files.exists(baseDir.resolve("abc-123.pdf"))).isFalse();
    }

    @Test
    void deletingAMissingFileDoesNotFail() {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(baseDir.toString());

        assertThatCode(() -> storage.delete("nao-existe.pdf")).doesNotThrowAnyException();
    }

    @Test
    void rejectsKeysThatCouldEscapeTheBaseDirectory() {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(baseDir.toString());

        for (String key :
                new String[] {
                    "../segredo.txt", "..", "sub/arquivo.pdf", "a\\b.pdf", "", "/etc/passwd"
                }) {
            assertThatThrownBy(() -> storage.store(key, new byte[] {1}))
                    .as(key)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
