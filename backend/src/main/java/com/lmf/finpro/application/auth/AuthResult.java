package com.lmf.finpro.application.auth;

/**
 * @param token access token (JWT curto, vai no corpo da resposta)
 * @param refreshToken valor para o cookie httpOnly; null quando o cookie existente deve ser mantido
 */
public record AuthResult(
        String token, String refreshToken, Long userId, String name, String email) {}
