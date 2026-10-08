package com.lmf.finpro.infrastructure.web.dto.status;

import com.lmf.finpro.application.status.PlatformStatus;
import java.time.LocalDateTime;

/** Só o estado geral e a hora da verificação: nada que revele a infraestrutura. */
public record PlatformStatusResponse(String status, LocalDateTime checkedAt) {

    public static PlatformStatusResponse from(PlatformStatus platformStatus) {
        return new PlatformStatusResponse(
                platformStatus.status().name(), platformStatus.checkedAt());
    }
}
