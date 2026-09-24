package com.lmf.finpro.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param frontendUrl URL base do frontend, usada para montar o link enviado por e-mail
 * @param tokenTtlMinutes validade do link de redefinição, em minutos
 * @param mailFrom remetente do e-mail de redefinição
 */
@ConfigurationProperties(prefix = "finpro.password-reset")
public record PasswordResetProperties(String frontendUrl, long tokenTtlMinutes, String mailFrom) {}
