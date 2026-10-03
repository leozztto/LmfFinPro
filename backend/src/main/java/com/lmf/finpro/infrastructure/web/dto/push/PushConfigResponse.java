package com.lmf.finpro.infrastructure.web.dto.push;

/**
 * @param publicKey chave VAPID pública para {@code pushManager.subscribe}; nula quando o push está
 *     desligado no servidor
 */
public record PushConfigResponse(boolean enabled, String publicKey) {}
