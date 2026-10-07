package com.lmf.finpro.infrastructure.web.dto.household;

import java.time.LocalDateTime;

/** Convite pendente recebido por quem consulta: o que a tela mostra para aceitar ou recusar. */
public record ReceivedInviteResponse(
        Long id,
        Long householdId,
        String householdName,
        String inviterName,
        LocalDateTime expiresAt) {}
