package com.lmf.finpro.application.household;

import java.time.LocalDateTime;

/**
 * Convite pendente endereçado ao e-mail de quem consulta, para aceitar ou recusar dentro do app.
 */
public record ReceivedInviteView(
        Long inviteId,
        Long householdId,
        String householdName,
        String inviterName,
        LocalDateTime expiresAt) {}
