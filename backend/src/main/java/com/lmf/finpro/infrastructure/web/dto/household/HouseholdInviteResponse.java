package com.lmf.finpro.infrastructure.web.dto.household;

import java.time.LocalDateTime;

/** Nunca devolve o token nem o hash: o token só existe no link enviado por e-mail. */
public record HouseholdInviteResponse(
        Long id, String email, LocalDateTime expiresAt, LocalDateTime createdAt) {}
