package com.lmf.finpro.application.household;

/**
 * Parâmetros dos convites. Montado na infraestrutura a partir das properties (ver
 * HouseholdProperties), para a aplicação não depender dela.
 *
 * @param invitePageUrl URL da tela do frontend que recebe o convite; o token vai como ?token=...
 * @param ttlDays validade do convite, em dias
 */
public record HouseholdInviteSettings(String invitePageUrl, long ttlDays) {}
