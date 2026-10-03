package com.lmf.finpro.domain.model;

/**
 * Inscrição de Web Push de um navegador do usuário.
 *
 * @param endpoint URL do serviço de push do navegador; identifica a inscrição
 * @param p256dh chave pública do navegador (base64url), usada para cifrar a mensagem
 * @param auth segredo de autenticação da inscrição (base64url)
 */
public record PushSubscription(Long userId, String endpoint, String p256dh, String auth) {}
