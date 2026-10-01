package com.lmf.finpro.infrastructure.scheduling;

import com.lmf.finpro.application.savingsgoal.SavingsGoalApplicationService;
import com.lmf.finpro.domain.model.SavingsGoal;
import com.lmf.finpro.infrastructure.config.SchedulingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Aplica o "separar com 1 clique" automaticamente, uma vez por dia, para quem ligou o aporte
 * automático na meta — mesma ideia do {@link RecurringBudgetScheduler}, mas aqui não há um "mês
 * vencido" para lançar: a sugestão é recalculada do zero a cada execução (percentual da meta sobre
 * a receita já paga no mês, menos o que já foi aportado), então rodar todo dia só vai capturando a
 * fração de cada receita nova que chega, sem nunca duplicar aporte. Roda também na subida da
 * aplicação pelo mesmo motivo do outro scheduler: recuperar dias em que o servidor ficou fora do
 * ar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SavingsGoalContributionScheduler {

    static final String LOCK_NAME = "savingsGoalContributions";

    private final SavingsGoalApplicationService savingsGoalApplicationService;
    private final StartupLockRunner startupLockRunner;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        startupLockRunner.run(LOCK_NAME, this::applyAllAutomaticContributions);
    }

    @Scheduled(cron = "${finpro.savings-goal.cron:0 20 0 * * *}", zone = SchedulingConfig.ZONE)
    @SchedulerLock(name = LOCK_NAME, lockAtLeastFor = SchedulingConfig.LOCK_AT_LEAST_FOR)
    public void applyAllAutomaticContributions() {
        for (SavingsGoal goal : savingsGoalApplicationService.findAllAutoContribute()) {
            // Cada meta isolada: uma falha (ex.: percentual removido entre uma execução e outra)
            // não impede o aporte automático das demais.
            try {
                savingsGoalApplicationService.applyAutomaticContributionIfDue(goal);
            } catch (RuntimeException ex) {
                log.error("Falha ao aplicar aporte automático da meta {}", goal.id(), ex);
            }
        }
    }
}
