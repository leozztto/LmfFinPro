package com.lmf.finpro.infrastructure.scheduling;

import com.lmf.finpro.application.recurringtransaction.RecurringTransactionApplicationService;
import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.infrastructure.config.SchedulingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lança as ocorrências recorrentes vencidas uma vez por dia, logo após a meia-noite, e também na
 * subida da aplicação — se o servidor ficou fora do ar (ou hibernando, em hospedagem gratuita), a
 * execução seguinte recupera todas as ocorrências que ficaram para trás.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransactionScheduler {

    private final RecurringTransactionApplicationService recurringTransactionApplicationService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        generateAllDueOccurrences();
    }

    @Scheduled(cron = "${finpro.recurring.cron:0 5 0 * * *}", zone = SchedulingConfig.ZONE)
    public void generateAllDueOccurrences() {
        for (RecurringTransaction recurrence :
                recurringTransactionApplicationService.findAllActive()) {
            // Cada recorrência na sua própria transação de banco: uma falha não impede as demais.
            try {
                recurringTransactionApplicationService.generateDueOccurrences(recurrence);
            } catch (RuntimeException ex) {
                log.error(
                        "Falha ao lançar ocorrências do lançamento recorrente {}",
                        recurrence.id(),
                        ex);
            }
        }
    }
}
