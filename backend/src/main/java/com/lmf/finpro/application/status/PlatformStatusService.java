package com.lmf.finpro.application.status;

import com.lmf.finpro.application.status.PlatformStatus.State;
import com.lmf.finpro.domain.port.out.PlatformHealthPort;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Monta a situação geral da plataforma para a página pública de suporte. Não se chama {@code
 * *ApplicationService} de propósito: é consultada em intervalo regular por visitantes anônimos, e o
 * aspecto de log de fluxo registraria cada consulta.
 */
@Service
@RequiredArgsConstructor
public class PlatformStatusService {

    private final PlatformHealthPort platformHealthPort;
    private final Clock clock;

    public PlatformStatus check() {
        // Se esta chamada chegou aqui, a API está de pé; o que pode faltar é o banco.
        State state = platformHealthPort.isDatabaseAvailable() ? State.OPERATIONAL : State.OUTAGE;
        return new PlatformStatus(state, LocalDateTime.now(clock));
    }
}
