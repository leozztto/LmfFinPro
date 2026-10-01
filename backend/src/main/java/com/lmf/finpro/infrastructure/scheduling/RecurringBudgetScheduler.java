package com.lmf.finpro.infrastructure.scheduling;

import com.lmf.finpro.application.recurringbudget.RecurringBudgetApplicationService;
import com.lmf.finpro.domain.model.RecurringBudget;
import com.lmf.finpro.infrastructure.config.SchedulingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Lança os orçamentos recorrentes vencidos uma vez por dia, logo após a meia-noite, e também na
 * subida da aplicação — se o servidor ficou fora do ar (ou hibernando, em hospedagem gratuita), a
 * execução seguinte recupera todos os meses que ficaram para trás. Como cada mês só é lançado uma
 * vez (controlado por {@code generatedMonths}), rodar diariamente é só uma forma barata de garantir
 * que o orçamento do mês apareça no primeiro dia útil possível, sem depender de um cron exato de
 * "todo dia 1".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringBudgetScheduler {

    static final String LOCK_NAME = "recurringBudgets";

    private final RecurringBudgetApplicationService recurringBudgetApplicationService;
    private final StartupLockRunner startupLockRunner;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        startupLockRunner.run(LOCK_NAME, this::generateAllDueBudgets);
    }

    @Scheduled(cron = "${finpro.recurring-budget.cron:0 10 0 * * *}", zone = SchedulingConfig.ZONE)
    @SchedulerLock(name = LOCK_NAME, lockAtLeastFor = SchedulingConfig.LOCK_AT_LEAST_FOR)
    public void generateAllDueBudgets() {
        for (RecurringBudget recurrence : recurringBudgetApplicationService.findAllActive()) {
            // Cada recorrência na sua própria transação de banco: uma falha não impede as demais.
            try {
                recurringBudgetApplicationService.generateDueBudgets(recurrence);
            } catch (RuntimeException ex) {
                log.error("Falha ao lançar orçamento recorrente {}", recurrence.id(), ex);
            }
        }
    }
}
