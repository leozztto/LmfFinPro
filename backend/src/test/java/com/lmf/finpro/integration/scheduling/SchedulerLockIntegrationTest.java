package com.lmf.finpro.integration.scheduling;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.infrastructure.scheduling.ExchangeRateScheduler;
import com.lmf.finpro.integration.support.AbstractIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

/** Com outra réplica já executando o job, esta não pode disparar o mesmo job em duplicidade. */
class SchedulerLockIntegrationTest extends AbstractIntegrationTest {

    @Autowired private ExchangeRateScheduler exchangeRateScheduler;
    @Autowired private LockProvider lockProvider;
    @MockBean private ExchangeRateApplicationService exchangeRateApplicationService;

    @Test
    void jobHeldByAnotherInstanceIsSkippedAndRunsAgainOnceReleased() {
        SimpleLock heldByOtherInstance =
                lockProvider
                        .lock(
                                new LockConfiguration(
                                        Instant.now(),
                                        "exchangeRates",
                                        Duration.ofMinutes(5),
                                        Duration.ZERO))
                        .orElseThrow();

        exchangeRateScheduler.refreshRecentRates();
        verify(exchangeRateApplicationService, never()).refreshRecent();

        heldByOtherInstance.unlock();

        exchangeRateScheduler.refreshRecentRates();
        verify(exchangeRateApplicationService).refreshRecent();
    }
}
