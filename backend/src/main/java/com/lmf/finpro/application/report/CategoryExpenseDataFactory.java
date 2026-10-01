package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryExpenseReportData;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Monta os dados do relatório de despesas por categoria. */
@Component
@RequiredArgsConstructor
class CategoryExpenseDataFactory {

    private final ReportLookups lookups;
    private final AccountRepositoryPort accountRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;

    /**
     * Soma as despesas do usuário (em todas as contas) por categoria dentro do mês, ordenadas da
     * maior para a menor — só entram categorias com pelo menos uma despesa no período. Transações
     * sem categoria entram como "Sem categoria"; uma categoria excluída depois de usada entra como
     * "Categoria removida", já que o histórico da transação não pode sumir.
     */
    CategoryExpenseReportData build(Long currentUserId, YearMonth referenceMonth) {
        User issuer = lookups.findUserOrThrow(currentUserId);

        LocalDate start = referenceMonth.atDay(1);
        LocalDate end = referenceMonth.plusMonths(1).atDay(1);

        List<Long> accountIds =
                accountRepositoryPort.findAllByUserId(currentUserId).stream()
                        .map(Account::id)
                        .toList();
        List<Transaction> expensesInPeriod =
                transactionRepositoryPort.findAllByAccountIds(accountIds).stream()
                        .filter(transaction -> transaction.transferId() == null)
                        .filter(transaction -> transaction.type() == CategoryType.EXPENSE)
                        .filter(
                                transaction ->
                                        !transaction.transactionDate().isBefore(start)
                                                && transaction.transactionDate().isBefore(end))
                        .toList();

        Map<Long, String> categoryNameById =
                categoryRepositoryPort.findAllVisibleToUser(currentUserId).stream()
                        .collect(Collectors.toMap(Category::id, Category::name));

        Map<String, BigDecimal> totalsByCategoryName = new LinkedHashMap<>();
        for (Transaction transaction : expensesInPeriod) {
            String categoryName =
                    transaction.categoryId() == null
                            ? "Sem categoria"
                            : categoryNameById.getOrDefault(
                                    transaction.categoryId(), "Categoria removida");
            totalsByCategoryName.merge(categoryName, transaction.baseAmount(), BigDecimal::add);
        }

        List<CategoryExpenseReportData.CategoryExpense> categoryExpenses =
                totalsByCategoryName.entrySet().stream()
                        .map(
                                entry ->
                                        new CategoryExpenseReportData.CategoryExpense(
                                                entry.getKey(), entry.getValue()))
                        .sorted(
                                Comparator.comparing(
                                                CategoryExpenseReportData.CategoryExpense::total)
                                        .reversed())
                        .toList();

        BigDecimal totalExpense =
                categoryExpenses.stream()
                        .map(CategoryExpenseReportData.CategoryExpense::total)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CategoryExpenseReportData(
                issuer, referenceMonth, categoryExpenses, totalExpense);
    }
}
