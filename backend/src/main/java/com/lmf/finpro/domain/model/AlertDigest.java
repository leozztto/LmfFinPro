package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Conteúdo do e-mail diário de alertas de um usuário; cada lista só tem o que ainda não foi
 * avisado.
 */
public record AlertDigest(
        List<BillDue> bills,
        List<OverdueBill> overdueBills,
        List<BudgetAlert> budgets,
        DasReminder das,
        List<RecurringBudgetExpiring> recurringBudgetsExpiring) {

    /** Resumo sem contas atrasadas. */
    public AlertDigest(
            List<BillDue> bills,
            List<BudgetAlert> budgets,
            DasReminder das,
            List<RecurringBudgetExpiring> recurringBudgetsExpiring) {
        this(bills, List.of(), budgets, das, recurringBudgetsExpiring);
    }

    public boolean isEmpty() {
        return bills.isEmpty()
                && overdueBills.isEmpty()
                && budgets.isEmpty()
                && das == null
                && recurringBudgetsExpiring.isEmpty();
    }

    public record BillDue(String description, BigDecimal amount, LocalDate dueDate) {}

    /** Despesa pendente cuja data de vencimento já passou. */
    public record OverdueBill(
            String description, BigDecimal amount, LocalDate dueDate, long daysOverdue) {}

    /**
     * @param threshold 80 ou 100 — o maior limiar atingido
     */
    public record BudgetAlert(
            String categoryName, BigDecimal limitValue, BigDecimal spent, int threshold) {}

    /**
     * @param estimatedValue valor da estimativa de imposto da competência, se cadastrada
     */
    public record DasReminder(YearMonth competence, LocalDate dueDate, BigDecimal estimatedValue) {}

    /** Orçamento recorrente ativo cujo {@code endMonth} é o mês atual ou o próximo. */
    public record RecurringBudgetExpiring(String categoryName, YearMonth endMonth) {}
}
