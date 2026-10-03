package com.lmf.finpro.infrastructure.scheduling;

import com.lmf.finpro.application.alert.AlertApplicationService;
import com.lmf.finpro.infrastructure.config.SchedulingConfig;
import com.lmf.finpro.infrastructure.logging.SafeErrors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Envia o resumo diário de alertas pela manhã. Diferente do {@link RecurringTransactionScheduler},
 * não roda na subida da aplicação: um deploy fora de hora não deve disparar e-mails, e o que não
 * foi avisado hoje entra no resumo de amanhã (enquanto ainda estiver na janela).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    static final String LOCK_NAME = "dailyAlerts";

    private final AlertApplicationService alertApplicationService;
    private final SchedulerMetrics metrics;

    @Scheduled(cron = "${finpro.alerts.cron:0 0 8 * * *}", zone = SchedulingConfig.ZONE)
    @SchedulerLock(name = LOCK_NAME, lockAtLeastFor = SchedulingConfig.LOCK_AT_LEAST_FOR)
    public void sendDailyAlerts() {
        metrics.run(
                LOCK_NAME,
                () -> {
                    for (Long userId : alertApplicationService.findAllRecipientIds()) {
                        // Cada usuário é carregado e processado na sua própria transação de banco:
                        // um cadastro com problema (ou uma falha de envio) não impede os demais.
                        try {
                            alertApplicationService.sendAlertsTo(userId);
                        } catch (RuntimeException ex) {
                            metrics.itemFailed(LOCK_NAME);
                            log.error(
                                    "Falha ao enviar os alertas do usuário {}: {}",
                                    userId,
                                    SafeErrors.describe(ex));
                        }
                    }
                });
    }
}
