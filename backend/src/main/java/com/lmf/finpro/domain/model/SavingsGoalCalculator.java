package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Contas das metas de economia — puras, sem acesso a repositório. */
public final class SavingsGoalCalculator {

    private SavingsGoalCalculator() {}

    public static BigDecimal savedAmount(List<GoalContribution> contributions) {
        return contributions.stream()
                .map(GoalContribution::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Quanto falta para o alvo (nunca negativo). */
    public static BigDecimal remaining(SavingsGoal goal, BigDecimal saved) {
        return goal.targetAmount().subtract(saved).max(BigDecimal.ZERO);
    }

    /**
     * Quanto guardar por mês para chegar ao alvo no prazo, contando o mês atual e o do prazo. Sem
     * prazo ou com o alvo atingido, não há valor mensal (null). Com o prazo vencido, é o total que
     * falta.
     */
    public static BigDecimal monthlyNeeded(SavingsGoal goal, BigDecimal saved, LocalDate today) {
        BigDecimal remaining = remaining(goal, saved);
        if (goal.deadline() == null || remaining.signum() == 0) {
            return null;
        }
        long months =
                ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(goal.deadline()))
                        + 1;
        if (months < 1) {
            return remaining;
        }
        return remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.UP);
    }

    /**
     * Sugestão de quanto separar agora: {@code incomeRate} das receitas já recebidas no mês, menos
     * o que já foi aportado na meta no mês, limitado ao que falta para o alvo. Zero quando não há o
     * que separar; sem percentual definido na meta, não há sugestão (null).
     */
    public static BigDecimal suggestedContribution(
            SavingsGoal goal,
            BigDecimal saved,
            BigDecimal monthPaidIncome,
            BigDecimal monthDeposits) {
        if (goal.incomeRate() == null || goal.incomeRate().signum() == 0) {
            return null;
        }
        BigDecimal target =
                monthPaidIncome.multiply(goal.incomeRate()).setScale(2, RoundingMode.HALF_UP);
        return target.subtract(monthDeposits).min(remaining(goal, saved)).max(BigDecimal.ZERO);
    }
}
