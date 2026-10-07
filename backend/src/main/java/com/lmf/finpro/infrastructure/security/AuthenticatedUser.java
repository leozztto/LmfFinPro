package com.lmf.finpro.infrastructure.security;

import com.lmf.finpro.domain.model.HouseholdRole;

/**
 * Principal populado pelo {@link JwtAuthenticationFilter}. {@code householdId} é o grupo dono dos
 * dados que o usuário enxerga; é resolvido no banco a cada requisição, não vem do token.
 */
public record AuthenticatedUser(
        Long userId, String email, Long householdId, HouseholdRole householdRole) {}
