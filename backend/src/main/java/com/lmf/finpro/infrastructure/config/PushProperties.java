package com.lmf.finpro.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Chaves VAPID do Web Push. Sem elas o push fica desligado (a tela de notificações esconde a
 * opção).
 *
 * @param vapidPublicKey chave pública (base64url, 65 bytes), enviada ao navegador na inscrição
 * @param vapidPrivateKey chave privada (base64url, 32 bytes); nunca sai do backend
 * @param subject contato do remetente exigido pelo VAPID ({@code mailto:} ou URL https)
 */
@ConfigurationProperties(prefix = "finpro.push")
public record PushProperties(String vapidPublicKey, String vapidPrivateKey, String subject) {

    public boolean configured() {
        return vapidPublicKey != null
                && !vapidPublicKey.isBlank()
                && vapidPrivateKey != null
                && !vapidPrivateKey.isBlank();
    }
}
