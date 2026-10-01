package com.lmf.finpro.infrastructure.scheduling;

import com.lmf.finpro.application.auth.RefreshTokenApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Apaga refresh tokens expirados para a tabela não crescer indefinidamente. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenApplicationService refreshTokenApplicationService;

    @Scheduled(cron = "${finpro.refresh-token.cleanup-cron:0 15 3 * * *}")
    public void purgeExpired() {
        int removed = refreshTokenApplicationService.purgeExpired();
        if (removed > 0) {
            log.info("Refresh tokens expirados removidos: {}", removed);
        }
    }
}
