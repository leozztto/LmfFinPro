package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Meta de economia ("caixinha"). O valor guardado não fica aqui: é a soma dos {@link
 * GoalContribution}.
 *
 * @param deadline prazo opcional (ex.: reserva de emergência costuma não ter)
 * @param incomeRate fração das receitas sugerida para separar na meta (0.06 = 6%), opcional
 */
public record SavingsGoal(
        Long id,
        Long userId,
        String name,
        SavingsGoalType type,
        BigDecimal targetAmount,
        LocalDate deadline,
        BigDecimal incomeRate,
        LocalDateTime createdAt) {

    public static SavingsGoal create(
            Long userId,
            String name,
            SavingsGoalType type,
            BigDecimal targetAmount,
            LocalDate deadline,
            BigDecimal incomeRate) {
        return new SavingsGoal(
                null, userId, name, type, targetAmount, deadline, incomeRate, LocalDateTime.now());
    }

    public SavingsGoal withDetails(
            String newName,
            SavingsGoalType newType,
            BigDecimal newTargetAmount,
            LocalDate newDeadline,
            BigDecimal newIncomeRate) {
        return new SavingsGoal(
                id,
                userId,
                newName,
                newType,
                newTargetAmount,
                newDeadline,
                newIncomeRate,
                createdAt);
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }
}
