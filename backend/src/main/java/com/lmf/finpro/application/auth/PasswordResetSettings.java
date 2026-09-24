package com.lmf.finpro.application.auth;

/**
 * Parâmetros do fluxo de redefinição de senha. Montado na camada de infraestrutura a partir das
 * properties (ver PasswordResetProperties), para a aplicação não depender dela.
 *
 * @param resetPageUrl URL da tela do frontend que recebe o token (ex:
 *     http://localhost/redefinir-senha); o token é anexado como ?token=...
 * @param tokenTtlMinutes validade do link, em minutos
 */
public record PasswordResetSettings(String resetPageUrl, long tokenTtlMinutes) {}
