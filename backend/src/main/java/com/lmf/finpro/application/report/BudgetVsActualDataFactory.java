package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.domain.model.BudgetVsActualReportData;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.User;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Monta os dados do relatório de orçamento vs. realizado. */
@Component
@RequiredArgsConstructor
class BudgetVsActualDataFactory {

    private final ReportLookups lookups;
    private final BudgetRepositoryPort budgetRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;

    /**
     * Compara, para cada orçamento cadastrado no mês, o limite definido com o total já gasto na
     * categoria (mesmo cálculo de {@code BudgetApplicationService.calculateSpent}, refeito aqui
     * para não acoplar um application service a outro), ordenado do maior percentual de uso para o
     * menor — assim os orçamentos estourados ou perto do limite aparecem primeiro.
     */
    BudgetVsActualReportData build(
            Long currentHouseholdId, Long currentUserId, YearMonth referenceMonth) {
        User issuer = lookups.findUserOrThrow(currentUserId);

        List<Budget> budgets =
                budgetRepositoryPort.findAllByHouseholdId(currentHouseholdId).stream()
                        .filter(budget -> budget.referenceMonth().equals(referenceMonth))
                        .toList();

        Map<Long, String> categoryNameById =
                categoryRepositoryPort.findAllVisibleToUser(currentHouseholdId).stream()
                        .collect(Collectors.toMap(Category::id, Category::name));

        Map<Long, String> clientNameById =
                clientRepositoryPort.findAllByHouseholdId(currentHouseholdId).stream()
                        .collect(Collectors.toMap(Client::id, Client::name));

        List<BudgetVsActualReportData.BudgetComparison> comparisons =
                budgets.stream()
                        .map(budget -> budgetComparison(budget, categoryNameById, clientNameById))
                        .sorted(Comparator.comparing(this::usageRatio).reversed())
                        .toList();

        BigDecimal totalLimit =
                comparisons.stream()
                        .map(BudgetVsActualReportData.BudgetComparison::limitValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent =
                comparisons.stream()
                        .map(BudgetVsActualReportData.BudgetComparison::spentValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BudgetVsActualReportData(
                issuer, referenceMonth, comparisons, totalLimit, totalSpent);
    }

    private BudgetVsActualReportData.BudgetComparison budgetComparison(
            Budget budget, Map<Long, String> categoryNameById, Map<Long, String> clientNameById) {
        LocalDate start = budget.referenceMonth().atDay(1);
        LocalDate end = budget.referenceMonth().plusMonths(1).atDay(1);
        BigDecimal spent =
                budget.clientId() == null
                        ? transactionRepositoryPort
                                .sumAmountByHouseholdIdAndCategoryIdAndTypeBetween(
                                        budget.householdId(),
                                        budget.categoryId(),
                                        CategoryType.EXPENSE,
                                        start,
                                        end)
                        : transactionRepositoryPort
                                .sumAmountByHouseholdIdAndCategoryIdAndClientIdAndTypeBetween(
                                        budget.householdId(),
                                        budget.categoryId(),
                                        budget.clientId(),
                                        CategoryType.EXPENSE,
                                        start,
                                        end);
        String categoryName =
                categoryNameById.getOrDefault(budget.categoryId(), "Categoria removida");
        if (budget.clientId() != null) {
            categoryName +=
                    " · " + clientNameById.getOrDefault(budget.clientId(), "Cliente removido");
        }
        BigDecimal difference = budget.limitValue().subtract(spent);
        return new BudgetVsActualReportData.BudgetComparison(
                categoryName,
                budget.limitValue(),
                spent,
                difference,
                spent.compareTo(budget.limitValue()) > 0);
    }

    private BigDecimal usageRatio(BudgetVsActualReportData.BudgetComparison comparison) {
        if (comparison.limitValue().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return comparison.spentValue().divide(comparison.limitValue(), 4, RoundingMode.HALF_UP);
    }
}
