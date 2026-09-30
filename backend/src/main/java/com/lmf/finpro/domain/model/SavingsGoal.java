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
 * @param autoContribute quando ligado, {@code SavingsGoalContributionScheduler} aplica a sugestão
 *     automaticamente (mesmo cálculo do "separar com 1 clique") uma vez por dia, sem o usuário
 *     precisar clicar — exige {@code incomeRate} definido, senão não há o que aplicar
 */
public record SavingsGoal(
        Long id,
        Long userId,
        String name,
        SavingsGoalType type,
        BigDecimal targetAmount,
        LocalDate deadline,
        BigDecimal incomeRate,
        LocalDateTime createdAt,
        boolean autoContribute) {

    /** Compatibilidade com código anterior ao aporte automático: nasce desligado. */
    public SavingsGoal(
            Long id,
            Long userId,
            String name,
            SavingsGoalType type,
            BigDecimal targetAmount,
            LocalDate deadline,
            BigDecimal incomeRate,
            LocalDateTime createdAt) {
        this(id, userId, name, type, targetAmount, deadline, incomeRate, createdAt, false);
    }

    public static SavingsGoal create(
            Long userId,
            String name,
            SavingsGoalType type,
            BigDecimal targetAmount,
            LocalDate deadline,
            BigDecimal incomeRate,
            boolean autoContribute) {
        return new SavingsGoal(
                null,
                userId,
                name,
                type,
                targetAmount,
                deadline,
                incomeRate,
                LocalDateTime.now(),
                autoContribute);
    }

    public SavingsGoal withDetails(
            String newName,
            SavingsGoalType newType,
            BigDecimal newTargetAmount,
            LocalDate newDeadline,
            BigDecimal newIncomeRate,
            boolean newAutoContribute) {
        return new SavingsGoal(
                id,
                userId,
                newName,
                newType,
                newTargetAmount,
                newDeadline,
                newIncomeRate,
                createdAt,
                newAutoContribute);
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }
}
