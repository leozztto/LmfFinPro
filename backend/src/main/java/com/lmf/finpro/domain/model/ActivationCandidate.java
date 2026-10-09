package com.lmf.finpro.domain.model;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Um usuário recente que ainda pode receber e-mail de ativação.
 *
 * @param hasTransactions já tem algum lançamento em algum dos seus grupos
 * @param guideDismissed dispensou o guia de primeiros passos ("Não mostrar mais")
 * @param sentKinds e-mails de ativação que já saíram para ele
 */
public record ActivationCandidate(
        Long userId,
        String name,
        String email,
        LocalDateTime createdAt,
        boolean hasTransactions,
        boolean guideDismissed,
        Set<ActivationEmailKind> sentKinds) {}
