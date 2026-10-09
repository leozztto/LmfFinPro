package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.ActivationCandidate;
import com.lmf.finpro.domain.model.ActivationEmailKind;
import java.time.LocalDateTime;
import java.util.List;

public interface ActivationCandidatePort {

    /** Usuários cadastrados desde {@code createdSince} que aceitam e-mails de ativação. */
    List<ActivationCandidate> findCandidates(LocalDateTime createdSince);

    void markSent(Long userId, ActivationEmailKind kind);
}
