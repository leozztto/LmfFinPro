package com.lmf.finpro.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Cifra, uma vez, os anexos gravados antes da criptografia em repouso. Ligue com {@code
 * FINPRO_ATTACHMENTS_ENCRYPT_EXISTING=true} no primeiro start com a chave definida e desligue
 * depois (é idempotente, mas varre a pasta inteira). Faça um backup antes
 * (docs/operacao/backup-restauracao.md).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "finpro.attachments.encrypt-existing", havingValue = "true")
class AttachmentEncryptionMigrationRunner implements ApplicationRunner {

    private final LocalFileStorageAdapter storage;

    @Override
    public void run(ApplicationArguments args) {
        int converted = storage.encryptLegacyFiles();
        log.info("Anexos antigos cifrados convertidos={}", converted);
    }
}
