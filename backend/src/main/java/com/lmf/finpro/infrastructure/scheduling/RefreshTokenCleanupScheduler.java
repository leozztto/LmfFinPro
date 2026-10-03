package com.lmf.finpro.infrastructure.scheduling;

import com.lmf.finpro.application.auth.RefreshTokenApplicationService;
import com.lmf.finpro.infrastructure.config.SchedulingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Apaga refresh tokens expirados para a tabela não crescer indefinidamente. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    static final String LOCK_NAME = "refreshTokenCleanup";

    private final RefreshTokenApplicationService refreshTokenApplicationService;
    private final SchedulerMetrics metrics;

    @Scheduled(cron = "${finpro.refresh-token.cleanup-cron:0 15 3 * * *}")
    @SchedulerLock(name = LOCK_NAME, lockAtLeastFor = SchedulingConfig.LOCK_AT_LEAST_FOR)
    public void purgeExpired() {
        metrics.run(
                LOCK_NAME,
                () -> {
                    int removed = refreshTokenApplicationService.purgeExpired();
                    if (removed > 0) {
                        log.info("Refresh tokens expirados removidos: {}", removed);
                    }
                });
    }
}
