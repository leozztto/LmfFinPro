package com.lmf.finpro.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param frontendUrl URL base do frontend, usada no link do e-mail de alertas
 * @param mailFrom remetente do e-mail de alertas
 */
@ConfigurationProperties(prefix = "finpro.alerts")
public record AlertProperties(String frontendUrl, String mailFrom) {}
