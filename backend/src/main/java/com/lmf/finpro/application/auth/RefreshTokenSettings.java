package com.lmf.finpro.application.auth;

/**
 * @param ttlDays validade de cada refresh token
 * @param reuseLeewaySeconds tolerância para a reapresentação de um token recém-rotacionado (duas
 *     abas renovando ao mesmo tempo); fora dela, o reuso derruba a sessão inteira
 */
public record RefreshTokenSettings(long ttlDays, long reuseLeewaySeconds) {}
