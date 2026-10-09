package com.lmf.finpro.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
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

    private static final String KEY_A = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String KEY_B =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @Test
    void encryptedStorageKeepsTheContentUnreadableOnDiskButRoundTrips() throws Exception {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(baseDir.toString(), KEY_A);
        byte[] content = "%PDF-1.4 comprovante com dados pessoais".getBytes();

        storage.store("doc.pdf", content);

        byte[] onDisk = Files.readAllBytes(baseDir.resolve("doc.pdf"));
        assertThat(onDisk).isNotEqualTo(content);
        assertThat(new String(onDisk, java.nio.charset.StandardCharsets.ISO_8859_1))
                .doesNotContain("comprovante");
        assertThat(storage.load("doc.pdf")).isEqualTo(content);
    }

    @Test
    void sameContentGetsADifferentCiphertextEachTime() throws Exception {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(baseDir.toString(), KEY_A);
        storage.store("a.pdf", new byte[] {1, 2, 3});
        storage.store("b.pdf", new byte[] {1, 2, 3});

        assertThat(Files.readAllBytes(baseDir.resolve("a.pdf")))
                .isNotEqualTo(Files.readAllBytes(baseDir.resolve("b.pdf")));
    }

    @Test
    void wrongKeyOrTamperedOrSwappedFilesFailToLoad() throws Exception {
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(baseDir.toString(), KEY_A);
        storage.store("a.pdf", new byte[] {1, 2, 3});
        storage.store("b.pdf", new byte[] {9, 9, 9});

        assertThatThrownBy(
                        () -> new LocalFileStorageAdapter(baseDir.toString(), KEY_B).load("a.pdf"))
                .isInstanceOf(IllegalStateException.class);

        // Arquivo trocado de lugar: o nome entra na autenticação (AAD).
        Files.copy(
                baseDir.resolve("b.pdf"),
                baseDir.resolve("a.pdf"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertThatThrownBy(() -> storage.load("a.pdf")).isInstanceOf(IllegalStateException.class);

        // Byte alterado.
        byte[] tampered = Files.readAllBytes(baseDir.resolve("b.pdf"));
        tampered[tampered.length - 1] ^= 1;
        Files.write(baseDir.resolve("b.pdf"), tampered);
        assertThatThrownBy(() -> storage.load("b.pdf")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void legacyPlainFilesStillLoadAndAreEncryptedByTheMigration() throws Exception {
        byte[] content = "%PDF legado".getBytes();
        Files.write(baseDir.resolve("antigo.pdf"), content);
        LocalFileStorageAdapter storage = new LocalFileStorageAdapter(baseDir.toString(), KEY_A);

        assertThat(storage.load("antigo.pdf")).isEqualTo(content);
        assertThat(storage.encryptLegacyFiles()).isEqualTo(1);
        assertThat(Files.readAllBytes(baseDir.resolve("antigo.pdf"))).isNotEqualTo(content);
        assertThat(storage.load("antigo.pdf")).isEqualTo(content);
        assertThat(storage.encryptLegacyFiles()).isZero();
    }

    @Test
    void encryptedFileWithoutAKeyIsRefusedInsteadOfServedAsGarbage() {
        new LocalFileStorageAdapter(baseDir.toString(), KEY_A).store("x.pdf", new byte[] {1});

        assertThatThrownBy(() -> new LocalFileStorageAdapter(baseDir.toString()).load("x.pdf"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsAKeyThatIsNotAes256() {
        assertThatThrownBy(
                        () ->
                                new LocalFileStorageAdapter(
                                        baseDir.toString(),
                                        Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class);
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
