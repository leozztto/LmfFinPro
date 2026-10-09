package com.lmf.finpro.infrastructure.storage;

import com.lmf.finpro.domain.port.out.FileStoragePort;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Guarda os anexos numa pasta do servidor (no Docker, um volume — ver docker-compose.yml). As
 * chaves são geradas pelo sistema, mas mesmo assim são validadas: só letras, números, hífen e
 * ponto, e o caminho final precisa ficar dentro da pasta base (nada de "../").
 *
 * <p>Com {@code finpro.attachments.encryption-key} definida, o conteúdo é cifrado em repouso
 * (AES-256-GCM, ver {@link AttachmentCipher}). Sem a chave, grava em claro (só desenvolvimento).
 * Arquivos antigos, sem criptografia, continuam legíveis e podem ser cifrados de uma vez por {@link
 * #encryptLegacyFiles()}.
 */
@Slf4j
@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9.-]{0,99}");

    private final Path baseDir;
    private final AttachmentCipher cipher;

    public LocalFileStorageAdapter(String storageDir) {
        this(storageDir, "");
    }

    @Autowired
    public LocalFileStorageAdapter(
            @Value("${finpro.attachments.storage-dir:./data/attachments}") String storageDir,
            @Value("${finpro.attachments.encryption-key:}") String encryptionKey) {
        this.baseDir = Path.of(storageDir).toAbsolutePath().normalize();
        this.cipher =
                encryptionKey == null || encryptionKey.isBlank()
                        ? null
                        : new AttachmentCipher(encryptionKey);
        if (cipher == null) {
            log.warn(
                    "Criptografia de anexos DESLIGADA (FINPRO_ATTACHMENTS_ENCRYPTION_KEY vazia):"
                            + " os arquivos ficam em claro no disco. Use só em desenvolvimento.");
        }
    }

    @Override
    public void store(String key, byte[] content) {
        Path target = resolve(key);
        try {
            Files.createDirectories(baseDir);
            Files.write(target, cipher == null ? content : cipher.encrypt(key, content));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar o anexo " + key, e);
        }
    }

    @Override
    public byte[] load(String key) {
        byte[] stored;
        try {
            stored = Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o anexo " + key, e);
        }
        if (!AttachmentCipher.isEncrypted(stored)) {
            return stored;
        }
        if (cipher == null) {
            throw new IllegalStateException(
                    "O anexo "
                            + key
                            + " está cifrado, mas FINPRO_ATTACHMENTS_ENCRYPTION_KEY não"
                            + " está definida.");
        }
        return cipher.decrypt(key, stored);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            // O registro já foi removido; um arquivo que sobrar no disco não afeta o usuário.
            log.warn("Não foi possível apagar o anexo {} do disco", key, e);
        }
    }

    /**
     * Cifra os arquivos gravados antes de a criptografia existir. Idempotente: pula os que já estão
     * cifrados. Devolve quantos converteu. Não faz nada sem chave.
     */
    public int encryptLegacyFiles() {
        if (cipher == null || !Files.isDirectory(baseDir)) {
            return 0;
        }
        List<Path> files;
        try (Stream<Path> stream = Files.list(baseDir)) {
            files = stream.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao listar os anexos", e);
        }
        int converted = 0;
        for (Path file : files) {
            String key = file.getFileName().toString();
            if (!SAFE_KEY.matcher(key).matches()) {
                continue;
            }
            try {
                byte[] stored = Files.readAllBytes(file);
                if (AttachmentCipher.isEncrypted(stored)) {
                    continue;
                }
                // Grava num temporário e troca, para uma queda no meio não deixar o anexo truncado.
                Path tmp = file.resolveSibling(key + ".tmp");
                Files.write(tmp, cipher.encrypt(key, stored));
                Files.move(
                        tmp,
                        file,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                converted++;
            } catch (IOException e) {
                throw new UncheckedIOException("Falha ao cifrar o anexo " + key, e);
            }
        }
        return converted;
    }

    private Path resolve(String key) {
        if (key == null || !SAFE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Chave de anexo inválida");
        }
        Path target = baseDir.resolve(key).normalize();
        if (!target.startsWith(baseDir)) {
            throw new IllegalArgumentException("Chave de anexo inválida");
        }
        return target;
    }
}
