package com.lmf.finpro.application.budget;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BudgetApplicationService {

    private final BudgetRepositoryPort budgetRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;

    public Budget create(
            Long currentUserId,
            Long categoryId,
            YearMonth referenceMonth,
            BigDecimal limitValue,
            Long clientId) {
        if (clientId != null) {
            requireOwnedClient(currentUserId, clientId);
        }
        return budgetRepositoryPort.save(
                Budget.create(currentUserId, categoryId, referenceMonth, limitValue, clientId));
    }

    public List<Budget> list(Long currentUserId) {
        return budgetRepositoryPort.findAllByUserId(currentUserId);
    }

    /**
     * Total já gasto (despesas, sem transferências) na categoria/mês do orçamento — e, se ele for
     * vinculado a um cliente/projeto, só nas despesas lançadas para esse cliente — não é
     * persistido: é recalculado a cada leitura a partir das transações já lançadas, para nunca
     * ficar dessincronizado (mesma convenção de {@code
     * AccountApplicationService.calculateCurrentBalance}).
     */
    public BigDecimal calculateSpent(Budget budget) {
        LocalDate start = budget.referenceMonth().atDay(1);
        LocalDate end = budget.referenceMonth().plusMonths(1).atDay(1);
        if (budget.clientId() != null) {
            return transactionRepositoryPort
                    .sumAmountByUserIdAndCategoryIdAndClientIdAndTypeBetween(
                            budget.userId(),
                            budget.categoryId(),
                            budget.clientId(),
                            CategoryType.EXPENSE,
                            start,
                            end);
        }
        return transactionRepositoryPort.sumAmountByUserIdAndCategoryIdAndTypeBetween(
                budget.userId(), budget.categoryId(), CategoryType.EXPENSE, start, end);
    }

    public void delete(Long currentUserId, Long budgetId) {
        findOwnedOrThrow(currentUserId, budgetId);
        budgetRepositoryPort.deleteById(budgetId);
    }

    /** Cliente de outro usuário é tratado como inexistente (404), não como 403. */
    private void requireOwnedClient(Long currentUserId, Long clientId) {
        clientRepositoryPort
                .findById(clientId)
                .filter(client -> client.belongsTo(currentUserId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Cliente não encontrado: " + clientId));
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
