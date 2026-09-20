package com.lmf.finpro.infrastructure.security;

/** Principal populado pelo {@link JwtAuthenticationFilter} a partir do token validado. */
public record AuthenticatedUser(Long userId, String email) {}
