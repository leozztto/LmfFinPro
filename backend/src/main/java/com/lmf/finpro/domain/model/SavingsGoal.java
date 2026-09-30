package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Meta de economia ("caixinha"). O valor guardado não fica aqui: é a soma dos {@link
 * GoalContribution}, cada um uma transferência real entre {@code fundingAccountId} e {@code
 * accountId}.
 *
 * @param accountId conta "reserva" onde o dinheiro guardado fica de fato — várias metas podem
 *     compartilhar a mesma conta
 * @param fundingAccountId conta de onde o aporte sai (e para onde o resgate volta); também a origem
 *     automática do "separar com 1 clique" e do aporte automático diário
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
        boolean autoContribute,
        Long accountId,
        Long fundingAccountId) {

    public static SavingsGoal create(
            Long userId,
            String name,
            SavingsGoalType type,
            BigDecimal targetAmount,
            LocalDate deadline,
            BigDecimal incomeRate,
            boolean autoContribute,
            Long accountId,
            Long fundingAccountId) {
        return new SavingsGoal(
                null,
                userId,
                name,
                type,
                targetAmount,
                deadline,
                incomeRate,
                LocalDateTime.now(),
                autoContribute,
                accountId,
                fundingAccountId);
    }

    /**
     * Categoria, tipo de meta à parte: nome, valor-alvo, prazo, percentual e aporte automático
     * podem mudar livremente. {@code accountId} e {@code fundingAccountId} não entram aqui — são
     * fixos desde a criação, porque já significam para onde os aportes já lançados foram.
     */
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
                newAutoContribute,
                accountId,
                fundingAccountId);
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }
}
