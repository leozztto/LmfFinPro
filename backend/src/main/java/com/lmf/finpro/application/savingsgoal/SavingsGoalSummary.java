package com.lmf.finpro.application.savingsgoal;

import com.lmf.finpro.domain.model.SavingsGoal;
import java.math.BigDecimal;

/**
 * Meta com os valores calculados a partir dos aportes e das receitas do mês.
 *
 * @param monthlyNeeded quanto guardar por mês até o prazo (null sem prazo ou com o alvo atingido)
 * @param monthPaidIncome receitas já recebidas (pagas) no mês atual
 * @param suggestedContribution quanto separar agora pelo percentual da meta (null sem percentual)
 */
public record SavingsGoalSummary(
        SavingsGoal goal,
        BigDecimal savedAmount,
        BigDecimal remainingAmount,
        BigDecimal monthlyNeeded,
        BigDecimal monthPaidIncome,
        BigDecimal suggestedContribution) {}
