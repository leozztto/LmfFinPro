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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetApplicationService {

    private final BudgetRepositoryPort budgetRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;

    public Budget create(
            Long currentHouseholdId,
            Long categoryId,
            YearMonth referenceMonth,
            BigDecimal limitValue,
            Long clientId) {
        log.debug(
                "Criando orçamento da categoria={} mês={} para o usuário={}",
                categoryId,
                referenceMonth,
                currentHouseholdId);
        if (clientId != null) {
            requireOwnedClient(currentHouseholdId, clientId);
        }
        Budget saved =
                budgetRepositoryPort.save(
                        Budget.create(
                                currentHouseholdId,
                                categoryId,
                                referenceMonth,
                                limitValue,
                                clientId));
        log.debug("Orçamento={} criado para o usuário={}", saved.id(), currentHouseholdId);
        return saved;
    }

    public List<Budget> list(Long currentHouseholdId) {
        log.debug("Listando orçamentos do usuário={}", currentHouseholdId);
        return budgetRepositoryPort.findAllByHouseholdId(currentHouseholdId);
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
                    .sumAmountByHouseholdIdAndCategoryIdAndClientIdAndTypeBetween(
                            budget.householdId(),
                            budget.categoryId(),
                            budget.clientId(),
                            CategoryType.EXPENSE,
                            start,
                            end);
        }
        return transactionRepositoryPort.sumAmountByHouseholdIdAndCategoryIdAndTypeBetween(
                budget.householdId(), budget.categoryId(), CategoryType.EXPENSE, start, end);
    }

    public void delete(Long currentHouseholdId, Long budgetId) {
        log.debug("Removendo orçamento={} do usuário={}", budgetId, currentHouseholdId);
        findOwnedOrThrow(currentHouseholdId, budgetId);
        budgetRepositoryPort.deleteById(budgetId);
    }

    /** Cliente de outro usuário é tratado como inexistente (404), não como 403. */
    private void requireOwnedClient(Long currentHouseholdId, Long clientId) {
        clientRepositoryPort
                .findById(clientId)
                .filter(client -> client.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () -> new ResourceNotFoundException("Cliente não encontrado: " + clientId));
    }

    /** Acesso a orçamento de outro usuário é tratado como inexistente (404), não como 403. */
    private Budget findOwnedOrThrow(Long currentHouseholdId, Long budgetId) {
        return budgetRepositoryPort
                .findById(budgetId)
                .filter(budget -> budget.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Orçamento não encontrado: " + budgetId));
    }
}
