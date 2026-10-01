package com.lmf.finpro.infrastructure.scheduling;

import com.lmf.finpro.infrastructure.config.SchedulingConfig;
import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Roda um job na subida da aplicação sob a mesma trava do seu {@code @SchedulerLock}: chamadas
 * diretas ao método não passam pelo proxy do ShedLock, e sem isso duas réplicas subindo juntas
 * executariam o job em duplicidade.
 */
@Component
public class StartupLockRunner {

    private final LockingTaskExecutor lockingTaskExecutor;
    private final Duration lockAtMostFor;
    private final Duration lockAtLeastFor;

    public StartupLockRunner(
            LockingTaskExecutor lockingTaskExecutor,
            @Value(SchedulingConfig.LOCK_AT_MOST_FOR) Duration lockAtMostFor,
            @Value(SchedulingConfig.LOCK_AT_LEAST_FOR) Duration lockAtLeastFor) {
        this.lockingTaskExecutor = lockingTaskExecutor;
        this.lockAtMostFor = lockAtMostFor;
        this.lockAtLeastFor = lockAtLeastFor;
    }

    public void run(String lockName, Runnable job) {
        lockingTaskExecutor.executeWithLock(
                job, new LockConfiguration(Instant.now(), lockName, lockAtMostFor, lockAtLeastFor));
    }
}
