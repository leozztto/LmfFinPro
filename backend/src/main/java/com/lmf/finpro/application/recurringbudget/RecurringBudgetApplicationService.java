package com.lmf.finpro.application.recurringbudget;

import com.lmf.finpro.domain.exception.CategoryTypeMismatchException;
import com.lmf.finpro.domain.exception.InvalidRecurrencePeriodException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.RecurringBudget;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.CategoryRepositoryPort;
import com.lmf.finpro.domain.port.out.RecurringBudgetRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecurringBudgetApplicationService {

    private final RecurringBudgetRepositoryPort recurringBudgetRepositoryPort;
    private final BudgetRepositoryPort budgetRepositoryPort;
    private final CategoryRepositoryPort categoryRepositoryPort;
    private final Clock clock;

    /**
     * Cria a recorrência e já lança os orçamentos vencidos até o mês atual — uma recorrência com
     * mês inicial no passado gera na hora os orçamentos que faltam.
     */
    @Transactional
    public RecurringBudget create(
            Long currentUserId,
            Long categoryId,
            BigDecimal limitValue,
            YearMonth startMonth,
            YearMonth endMonth) {
        requireValidPeriod(startMonth, endMonth);
        return createOne(currentUserId, categoryId, limitValue, startMonth, endMonth);
    }

    /**
     * Cria uma recorrência por categoria, todas com o mesmo período — um "pacote" mensal em vez de
     * cadastrar uma de cada vez. Roda numa única transação: se uma categoria for inválida ou
     * repetida no lote, nenhuma é criada.
     */
    @Transactional
    public List<RecurringBudget> createBatch(
            Long currentUserId,
            YearMonth startMonth,
            YearMonth endMonth,
            List<CategoryLimit> items) {
        requireValidPeriod(startMonth, endMonth);
        requireNoDuplicateCategories(items);
        return items.stream()
                .map(
                        item ->
                                createOne(
                                        currentUserId,
                                        item.categoryId(),
                                        item.limitValue(),
                                        startMonth,
                                        endMonth))
                .toList();
    }

    private RecurringBudget createOne(
            Long currentUserId,
            Long categoryId,
            BigDecimal limitValue,
            YearMonth startMonth,
            YearMonth endMonth) {
        requireMatchingCategory(currentUserId, categoryId);
        RecurringBudget saved =
                recurringBudgetRepositoryPort.save(
                        RecurringBudget.create(
                                currentUserId, categoryId, limitValue, startMonth, endMonth));
        return generateDueBudgets(saved);
    }

    public List<RecurringBudget> list(Long currentUserId) {
        return recurringBudgetRepositoryPort.findAllByUserId(currentUserId);
    }

    @Transactional
    public RecurringBudget update(
            Long currentUserId,
            Long recurringBudgetId,
            BigDecimal limitValue,
            YearMonth endMonth,
            boolean active) {
        RecurringBudget existing = findOwnedOrThrow(currentUserId, recurringBudgetId);
        requireValidPeriod(existing.startMonth(), endMonth);
        RecurringBudget updated =
                recurringBudgetRepositoryPort.save(
                        existing.withDetails(limitValue, endMonth, active, YearMonth.now(clock)));
        return generateDueBudgets(updated);
    }

    /** Exclui só o modelo: os orçamentos já lançados continuam, sem nenhum vínculo com ele. */
    public void delete(Long currentUserId, Long recurringBudgetId) {
        findOwnedOrThrow(currentUserId, recurringBudgetId);
        recurringBudgetRepositoryPort.deleteById(recurringBudgetId);
    }

    public List<RecurringBudget> findAllActive() {
        return recurringBudgetRepositoryPort.findAllActive();
    }

    /**
     * Lança um orçamento para cada mês vencido até hoje e avança o contador — na mesma transação de
     * banco, para uma falha no meio não deixar mês lançado sem contar. Se já existir um orçamento
     * (manual ou de um ciclo anterior) para a categoria/mês, o mês é pulado sem duplicar, mas ainda
     * conta como gerado, para não tentar de novo no próximo ciclo.
     */
    @Transactional
    public RecurringBudget generateDueBudgets(RecurringBudget recurrence) {
        List<YearMonth> dueMonths = recurrence.dueMonths(YearMonth.now(clock));
        if (dueMonths.isEmpty()) {
            return recurrence;
        }
        for (YearMonth month : dueMonths) {
            boolean alreadyExists =
                    budgetRepositoryPort.existsByUserIdAndCategoryIdAndReferenceMonth(
                            recurrence.userId(), recurrence.categoryId(), month);
            if (!alreadyExists) {
                budgetRepositoryPort.save(
                        Budget.create(
                                recurrence.userId(),
                                recurrence.categoryId(),
                                month,
                                recurrence.limitValue()));
            }
        }
        return recurringBudgetRepositoryPort.save(
                recurrence.withGeneratedMonths(recurrence.generatedMonths() + dueMonths.size()));
    }

    private void requireValidPeriod(YearMonth startMonth, YearMonth endMonth) {
        if (endMonth != null && endMonth.isBefore(startMonth)) {
            throw new InvalidRecurrencePeriodException(
                    "O mês final da recorrência não pode ser anterior ao mês inicial.");
        }
    }

    private void requireNoDuplicateCategories(List<CategoryLimit> items) {
        long distinctCategories = items.stream().map(CategoryLimit::categoryId).distinct().count();
        if (distinctCategories != items.size()) {
            throw new IllegalArgumentException("Cada categoria só pode aparecer uma vez no lote.");
        }
    }

    /** Acesso a recorrência de outro usuário é tratado como inexistente (404), não como 403. */
    private RecurringBudget findOwnedOrThrow(Long currentUserId, Long recurringBudgetId) {
        return recurringBudgetRepositoryPort
                .findById(recurringBudgetId)
                .filter(recurrence -> recurrence.belongsTo(currentUserId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Orçamento recorrente não encontrado: "
                                                + recurringBudgetId));
    }

    private void requireMatchingCategory(Long currentUserId, Long categoryId) {
        Category category =
                categoryRepositoryPort
                        .findById(categoryId)
                        .filter(candidate -> candidate.isVisibleTo(currentUserId))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Categoria não encontrada: " + categoryId));
        if (category.type() != CategoryType.EXPENSE) {
            throw new CategoryTypeMismatchException(
                    "A categoria \"" + category.name() + "\" não é uma categoria de despesa.");
        }
    }
}
