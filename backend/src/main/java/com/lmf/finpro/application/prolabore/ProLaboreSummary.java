package com.lmf.finpro.application.prolabore;

import com.lmf.finpro.domain.model.ProLaboreCalculator;
import com.lmf.finpro.domain.model.ProLaboreSettings;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * "Quanto posso me pagar este mês", com os números de entrada e o resultado do cálculo, para a tela
 * explicar cada parcela.
 *
 * @param monthExpenses despesas PJ do mês (pagas no mês e pendentes até o fim dele)
 * @param pendingExpenses despesas PJ pendentes até o fim do mês (inclusive atrasadas)
 * @param taxRate alíquota usada: a manual, a da caixinha do imposto ou a de referência do regime
 * @param taxReserveSaved valor guardado nas caixinhas do imposto
 * @param withdrawnThisMonth pró-labore já retirado no mês (transferências de conta PJ para PF)
 * @param businessExpenses as transações que compõem {@code monthBusinessExpenses}, da mais recente
 *     para a mais antiga
 * @param suggestedFromAccountId conta PJ com mais saldo, para pré-preencher a transferência
 * @param suggestedToAccountId primeira conta PF, para pré-preencher a transferência
 */
public record ProLaboreSummary(
        boolean hasBusinessAccounts,
        boolean hasPersonalAccounts,
        ProLaboreSettings settings,
        ProLaboreCalculator.Result result,
        BigDecimal businessBalance,
        BigDecimal monthBusinessIncome,
        BigDecimal monthBusinessExpenses,
        BigDecimal pendingBusinessExpenses,
        BigDecimal taxRate,
        BigDecimal taxReserveSaved,
        BigDecimal averageMonthlyBusinessExpense,
        BigDecimal withdrawnThisMonth,
        List<Withdrawal> withdrawals,
        List<BusinessExpense> businessExpenses,
        Long suggestedFromAccountId,
        Long suggestedToAccountId) {

    public record Withdrawal(
            Long transferId,
            LocalDate date,
            BigDecimal amount,
            String fromAccountName,
            String toAccountName) {}

    /**
     * Despesa de conta PJ que entrou no custo do mês.
     *
     * @param overdue pendente com data anterior a hoje
     * @param categoryName {@code null} quando a transação não tem categoria
     */
    public record BusinessExpense(
            Long transactionId,
            LocalDate date,
            String description,
            BigDecimal amount,
            boolean paid,
            boolean overdue,
            String accountName,
            String categoryName) {}
}
