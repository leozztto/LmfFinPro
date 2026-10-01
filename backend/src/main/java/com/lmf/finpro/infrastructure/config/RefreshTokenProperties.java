package com.lmf.finpro.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Refresh token da sessão, entregue em cookie httpOnly.
 *
 * @param ttlDays validade do refresh token (janela máxima de inatividade da sessão)
 * @param cookieSecure cookie só trafega por HTTPS; desligar apenas em HTTP fora de localhost
 * @param cookieSameSite Strict, Lax ou None — Strict basta enquanto frontend e API forem do mesmo
 *     site (nginx em /api, ou localhost:5173 chamando localhost:8080 em dev)
 * @param reuseLeewaySeconds tolerância para duas abas renovarem ao mesmo tempo com o mesmo cookie
 */
@ConfigurationProperties(prefix = "finpro.refresh-token")
public record RefreshTokenProperties(
        long ttlDays, boolean cookieSecure, String cookieSameSite, long reuseLeewaySeconds) {

    public static final String COOKIE_NAME = "finpro_refresh";
    public static final String COOKIE_PATH = "/api/auth";

    public RefreshTokenProperties {
        if (ttlDays <= 0) {
            throw new IllegalStateException("finpro.refresh-token.ttl-days deve ser positivo.");
        }
        if (cookieSameSite == null || cookieSameSite.isBlank()) {
            cookieSameSite = "Strict";
        }
    }
}
