package com.lmf.finpro.infrastructure.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lmf.finpro.domain.model.PersonalDataExport;
import com.lmf.finpro.domain.port.out.FileStoragePort;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;

class ZipPersonalDataArchiveWriterTest {

    /** Armazenamento em memória; chave ausente falha como o de disco falharia. */
    private static class InMemoryStorage implements FileStoragePort {
        final Map<String, byte[]> files = new HashMap<>();

        @Override
        public void store(String key, byte[] content) {
            files.put(key, content);
        }

        @Override
        public byte[] load(String key) {
            byte[] content = files.get(key);
            if (content == null) {
                throw new IllegalStateException("sem arquivo " + key);
            }
            return content;
        }

        @Override
        public void delete(String key) {
            files.remove(key);
        }
    }

    private final InMemoryStorage storage = new InMemoryStorage();
    private final ZipPersonalDataArchiveWriter writer =
            new ZipPersonalDataArchiveWriter(
                    new ObjectMapper()
                            .registerModule(new JavaTimeModule())
                            .disable(
                                    com.fasterxml.jackson.databind.SerializationFeature
                                            .WRITE_DATES_AS_TIMESTAMPS),
                    storage);

    private static Map<String, byte[]> unzip(byte[] zip) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                entries.put(entry.getName(), in.readAllBytes());
            }
        }
        return entries;
    }

    private byte[] write(PersonalDataExport export) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(export, out);
        return out.toByteArray();
    }

    @Test
    void writesTheJsonTheFilesAndTheReadme() throws IOException {
        storage.store("key-1", new byte[] {1, 2, 3});
        PersonalDataExport export =
                new PersonalDataExport(
                        Map.of("geradoEm", LocalDateTime.of(2026, 10, 7, 12, 0), "nome", "Ana"),
                        List.of(new PersonalDataExport.File("anexos/a.png", "key-1")),
                        "LEIA-ME base\n");

        Map<String, byte[]> entries = unzip(write(export));

        assertThat(entries.keySet())
                .containsExactlyInAnyOrder("dados.json", "anexos/a.png", "LEIA-ME.txt");
        assertThat(entries.get("anexos/a.png")).containsExactly(1, 2, 3);
        String json = new String(entries.get("dados.json"), StandardCharsets.UTF_8);
        assertThat(json).contains("\"nome\" : \"Ana\"").contains("2026-10-07T12:00:00");
        assertThat(new String(entries.get("LEIA-ME.txt"), StandardCharsets.UTF_8))
                .isEqualTo("LEIA-ME base\n");
    }

    @Test
    void aMissingFileDoesNotBreakTheExportAndIsListedInTheReadme() throws IOException {
        storage.store("key-ok", new byte[] {9});
        PersonalDataExport export =
                new PersonalDataExport(
                        Map.of("nome", "Ana"),
                        List.of(
                                new PersonalDataExport.File("anexos/sumiu.png", "key-lost"),
                                new PersonalDataExport.File("anexos/ok.png", "key-ok")),
                        "base\n");

        Map<String, byte[]> entries = unzip(write(export));

        assertThat(entries).containsKey("anexos/ok.png").doesNotContainKey("anexos/sumiu.png");
        assertThat(new String(entries.get("LEIA-ME.txt"), StandardCharsets.UTF_8))
                .contains("não puderam ser incluídos")
                .contains("anexos/sumiu.png");
    }
}
