package com.lmf.finpro.infrastructure.scheduling;

import com.lmf.finpro.application.onboarding.ActivationEmailApplicationService;
import com.lmf.finpro.infrastructure.config.SchedulingConfig;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Procura, a cada 15 minutos, quem está devendo um e-mail de ativação. Roda com frequência para que
 * as boas-vindas cheguem logo depois do cadastro; quem decide o que sai e quando é a
 * ActivationEmailPolicy. Como o {@link AlertScheduler}, não roda na subida da aplicação.
 */
@Component
@RequiredArgsConstructor
public class ActivationScheduler {

    static final String LOCK_NAME = "activationEmails";

    private final ActivationEmailApplicationService activationEmailApplicationService;
    private final SchedulerMetrics metrics;

    @Scheduled(cron = "${finpro.activation.cron:0 */15 * * * *}", zone = SchedulingConfig.ZONE)
    @SchedulerLock(name = LOCK_NAME, lockAtLeastFor = SchedulingConfig.LOCK_AT_LEAST_FOR)
    public void sendActivationEmails() {
        metrics.run(LOCK_NAME, () -> metrics.produced(activationEmailApplicationService.sendDue()));
    }
}
