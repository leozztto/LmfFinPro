package com.lmf.finpro.application.onboarding;

import com.lmf.finpro.domain.model.ActivationCandidate;
import com.lmf.finpro.domain.model.ActivationEmailKind;
import com.lmf.finpro.domain.port.out.ActivationCandidatePort;
import com.lmf.finpro.domain.port.out.ActivationMailerPort;
import com.lmf.finpro.infrastructure.logging.SafeErrors;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Envia os e-mails de ativação (boas-vindas e lembretes para quem ainda não trouxe lançamentos).
 * Cada tipo sai uma única vez por usuário: só é registrado depois do envio, então uma falha tenta
 * de novo na execução seguinte, enquanto a janela do e-mail estiver aberta.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivationEmailApplicationService {

    /** Só olha cadastros recentes: a última janela de e-mail fecha antes disso. */
    static final int LOOKBACK_DAYS = 14;

    private final ActivationCandidatePort activationCandidatePort;
    private final ActivationMailerPort activationMailerPort;
    private final Clock clock;

    /**
     * @return quantos e-mails foram enviados
     */
    public int sendDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        int sent = 0;
        for (ActivationCandidate candidate :
                activationCandidatePort.findCandidates(now.minusDays(LOOKBACK_DAYS))) {
            var due = ActivationEmailPolicy.nextDue(candidate, now);
            if (due.isEmpty()) {
                continue;
            }
            ActivationEmailKind kind = due.get();
            try {
                activationMailerPort.send(candidate.email(), candidate.name(), kind);
                activationCandidatePort.markSent(candidate.userId(), kind);
                sent++;
            } catch (RuntimeException ex) {
                log.warn(
                        "Falha ao enviar o e-mail de ativação {} ao usuário {}: {}",
                        kind,
                        candidate.userId(),
                        SafeErrors.describe(ex));
            }
        }
        return sent;
    }
}
