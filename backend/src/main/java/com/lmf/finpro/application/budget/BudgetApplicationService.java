package com.lmf.finpro.application.budget;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BudgetApplicationService {

    private final BudgetRepositoryPort budgetRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;

    public Budget create(
            Long currentUserId, Long categoryId, YearMonth referenceMonth, BigDecimal limitValue) {
        return budgetRepositoryPort.save(
                Budget.create(currentUserId, categoryId, referenceMonth, limitValue));
    }

    public List<Budget> list(Long currentUserId) {
        return budgetRepositoryPort.findAllByUserId(currentUserId);
    }

    /**
     * Total já gasto (despesas, sem transferências) na categoria/mês do orçamento — não é
     * persistido: é recalculado a cada leitura a partir das transações já lançadas, para nunca
     * ficar dessincronizado (mesma convenção de {@code
     * AccountApplicationService.calculateCurrentBalance}).
     */
    public BigDecimal calculateSpent(Budget budget) {
        return transactionRepositoryPort.sumAmountByUserIdAndCategoryIdAndTypeBetween(
                budget.userId(),
                budget.categoryId(),
                CategoryType.EXPENSE,
                budget.referenceMonth().atDay(1),
                budget.referenceMonth().plusMonths(1).atDay(1));
    }

    public void delete(Long currentUserId, Long budgetId) {
        findOwnedOrThrow(currentUserId, budgetId);
        budgetRepositoryPort.deleteById(budgetId);
    }

    /** Acesso a orçamento de outro usuário é tratado como inexistente (404), não como 403. */
    private Budget findOwnedOrThrow(Long currentUserId, Long budgetId) {
        return budgetRepositoryPort
                .findById(budgetId)
                .filter(budget -> budget.belongsTo(currentUserId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Orçamento não encontrado: " + budgetId));
    }
}
