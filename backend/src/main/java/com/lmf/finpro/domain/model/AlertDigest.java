package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Conteúdo do e-mail diário de alertas de um usuário; cada lista só tem o que ainda não foi
 * avisado.
 */
public record AlertDigest(List<BillDue> bills, List<BudgetAlert> budgets, DasReminder das) {

    public boolean isEmpty() {
        return bills.isEmpty() && budgets.isEmpty() && das == null;
    }

    public record BillDue(String description, BigDecimal amount, LocalDate dueDate) {}

    /**
     * @param threshold 80 ou 100 — o maior limiar atingido
     */
    public record BudgetAlert(
            String categoryName, BigDecimal limitValue, BigDecimal spent, int threshold) {}

    /**
     * @param estimatedValue valor da estimativa de imposto da competência, se cadastrada
     */
    public record DasReminder(YearMonth competence, LocalDate dueDate, BigDecimal estimatedValue) {}
}
