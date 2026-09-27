package com.lmf.finpro.domain.port.out;

/**
 * Armazenamento do conteúdo dos anexos. Hoje é disco local (volume Docker); a porta existe para
 * trocar por S3/MinIO no deploy sem mexer nas regras de negócio.
 */
public interface FileStoragePort {
    void store(String key, byte[] content);

    byte[] load(String key);

    /** Não falha se o arquivo não existir. */
    void delete(String key);
}
