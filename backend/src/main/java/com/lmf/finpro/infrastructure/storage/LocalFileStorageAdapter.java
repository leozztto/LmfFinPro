package com.lmf.finpro.infrastructure.storage;

import com.lmf.finpro.domain.port.out.FileStoragePort;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Guarda os anexos numa pasta do servidor (no Docker, um volume — ver docker-compose.yml). As
 * chaves são geradas pelo sistema, mas mesmo assim são validadas: só letras, números, hífen e
 * ponto, e o caminho final precisa ficar dentro da pasta base (nada de "../").
 */
@Slf4j
@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9.-]{0,99}");

    private final Path baseDir;

    public LocalFileStorageAdapter(
            @Value("${finpro.attachments.storage-dir:./data/attachments}") String storageDir) {
        this.baseDir = Path.of(storageDir).toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, byte[] content) {
        Path target = resolve(key);
        try {
            Files.createDirectories(baseDir);
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar o anexo " + key, e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o anexo " + key, e);
        }
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
