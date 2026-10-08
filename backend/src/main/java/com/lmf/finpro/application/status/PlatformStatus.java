package com.lmf.finpro.application.status;

import java.time.LocalDateTime;

/** Situação geral da plataforma num instante. Sem detalhe por componente, de propósito. */
public record PlatformStatus(State status, LocalDateTime checkedAt) {

    public enum State {
        OPERATIONAL,
        OUTAGE
    }
}
