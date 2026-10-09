package com.lmf.finpro.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.application.onboarding.ActivationEmailApplicationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class ActivationSchedulerTest {

    private final ActivationEmailApplicationService service =
            mock(ActivationEmailApplicationService.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ActivationScheduler scheduler =
            new ActivationScheduler(service, new SchedulerMetrics(registry, Clock.systemUTC()));

    private double runs(String outcome) {
        return registry.get("finpro.scheduler.runs")
                .tag("scheduler", ActivationScheduler.LOCK_NAME)
                .tag("outcome", outcome)
                .counter()
                .count();
    }

    @Test
    void sendsTheDueEmailsAndCountsASuccessfulRun() {
        when(service.sendDue()).thenReturn(3);

        scheduler.sendActivationEmails();

        verify(service).sendDue();
        assertThat(runs("success")).isEqualTo(1);
        assertThat(runs("failure")).isZero();
    }

    @Test
    void aFailureIsCountedAndDoesNotKillTheSchedule() {
        when(service.sendDue()).thenThrow(new IllegalStateException("banco fora"));

        assertThatCode(scheduler::sendActivationEmails).doesNotThrowAnyException();

        assertThat(runs("failure")).isEqualTo(1);
    }
}
