package com.lmf.finpro.application.importbatch;

import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * O resultado de uma importação em números: o primeiro retorno visível do extrato que a pessoa
 * trouxe. Os valores são em reais ({@code baseAmount}, quando a conta é em outra moeda).
 *
 * @param balance receitas menos despesas do período importado
 * @param topExpenseCategories as categorias onde mais saiu dinheiro, maiores primeiro
 * @param uncategorizedCount lançamentos que as regras não souberam categorizar
 */
public record ImportSummary(
        Long batchId,
        int transactionCount,
        LocalDate firstDate,
        LocalDate lastDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        List<CategoryTotal> topExpenseCategories,
        int uncategorizedCount) {

    /** Quantas categorias entram no ranking; o resto some do resumo (continua no extrato). */
    public static final int TOP_CATEGORIES = 5;

    private static final String UNCATEGORIZED_LABEL = "Sem categoria";

    /**
     * @param categoryId {@code null} no agrupamento "Sem categoria"
     * @param share fatia do total de despesas, em % com uma casa decimal
     */
    public record CategoryTotal(Long categoryId, String name, BigDecimal total, BigDecimal share) {}

    public static ImportSummary of(
            Long batchId, List<Transaction> transactions, List<Category> categories) {
        Map<Long, String> names = new HashMap<>();
        categories.forEach(category -> names.put(category.id(), category.name()));

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        int uncategorized = 0;
        LocalDate first = null;
        LocalDate last = null;
        Map<Long, BigDecimal> expenseByCategory = new HashMap<>();
        for (Transaction transaction : transactions) {
            BigDecimal value = valueOf(transaction);
            if (transaction.type() == CategoryType.INCOME) {
                income = income.add(value);
            } else {
                expense = expense.add(value);
                expenseByCategory.merge(transaction.categoryId(), value, BigDecimal::add);
            }
            if (transaction.categoryId() == null) {
                uncategorized++;
            }
            LocalDate date = transaction.transactionDate();
            first = first == null || date.isBefore(first) ? date : first;
            last = last == null || date.isAfter(last) ? date : last;
        }

        BigDecimal totalExpense = expense;
        List<CategoryTotal> top =
                expenseByCategory.entrySet().stream()
                        .map(
                                entry ->
                                        new CategoryTotal(
                                                entry.getKey(),
                                                entry.getKey() == null
                                                        ? UNCATEGORIZED_LABEL
                                                        : names.getOrDefault(
                                                                entry.getKey(),
                                                                UNCATEGORIZED_LABEL),
                                                entry.getValue(),
                                                share(entry.getValue(), totalExpense)))
                        .sorted(Comparator.comparing(CategoryTotal::total).reversed())
                        .limit(TOP_CATEGORIES)
                        .toList();

        return new ImportSummary(
                batchId,
                transactions.size(),
                first,
                last,
                income,
                expense,
                income.subtract(expense),
                top,
                uncategorized);
    }

    private static BigDecimal valueOf(Transaction transaction) {
        return transaction.baseAmount() != null ? transaction.baseAmount() : transaction.amount();
    }

    private static BigDecimal share(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }
}
