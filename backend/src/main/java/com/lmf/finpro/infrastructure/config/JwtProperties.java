package com.lmf.finpro.infrastructure.config;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sem valor padrão para o segredo: se o deploy esquecer JWT_SECRET (ou usar o placeholder público
 * do .env.example), a aplicação falha na inicialização em vez de subir aceitando tokens forjáveis.
 */
@ConfigurationProperties(prefix = "finpro.jwt")
public record JwtProperties(String secret, long expirationMs) {

    static final int MIN_SECRET_BYTES = 32;
    static final String PLACEHOLDER_SECRET = "change-me-to-a-long-random-secret";

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET não configurado. Defina um segredo aleatório de pelo menos "
                            + MIN_SECRET_BYTES
                            + " bytes (ex.: openssl rand -base64 48).");
        }
        if (PLACEHOLDER_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET está com o valor de exemplo público. Defina um segredo aleatório próprio.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET deve ter pelo menos " + MIN_SECRET_BYTES + " bytes.");
        }
    }
}
