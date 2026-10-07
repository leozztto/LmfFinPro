package com.lmf.finpro.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param frontendUrl URL base do frontend, usada para montar o link enviado por e-mail
 * @param inviteTtlDays validade do convite, em dias
 * @param mailFrom remetente do e-mail de convite
 */
@ConfigurationProperties(prefix = "finpro.household")
public record HouseholdProperties(String frontendUrl, long inviteTtlDays, String mailFrom) {}
