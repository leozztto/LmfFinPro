package com.lmf.finpro.application.recurringbudget;

import com.lmf.finpro.application.FlowLog;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
            Long currentHouseholdId,
            Long categoryId,
            BigDecimal limitValue,
            YearMonth startMonth,
            YearMonth endMonth) {
        log.debug(
                "Criando orçamento recorrente da categoria={} para o usuário={}",
                categoryId,
                currentHouseholdId);
        requireValidPeriod(startMonth, endMonth);
        return createOne(currentHouseholdId, categoryId, limitValue, startMonth, endMonth);
    }

    /**
     * Cria uma recorrência por categoria, todas com o mesmo período — um "pacote" mensal em vez de
     * cadastrar uma de cada vez. Roda numa única transação: se uma categoria for inválida ou
     * repetida no lote, nenhuma é criada.
     */
    @Transactional
    public List<RecurringBudget> createBatch(
            Long currentHouseholdId,
            YearMonth startMonth,
            YearMonth endMonth,
            List<CategoryLimit> items) {
        log.debug(
                "Criando lote de {} orçamento(s) recorrente(s) para o usuário={}",
                items.size(),
                currentHouseholdId);
        requireValidPeriod(startMonth, endMonth);
        requireNoDuplicateCategories(items);
        return items.stream()
                .map(
                        item ->
                                createOne(
                                        currentHouseholdId,
                                        item.categoryId(),
                                        item.limitValue(),
                                        startMonth,
                                        endMonth))
                .toList();
    }

    private RecurringBudget createOne(
            Long currentHouseholdId,
            Long categoryId,
            BigDecimal limitValue,
            YearMonth startMonth,
            YearMonth endMonth) {
        requireMatchingCategory(currentHouseholdId, categoryId);
        RecurringBudget saved =
                recurringBudgetRepositoryPort.save(
                        RecurringBudget.create(
                                currentHouseholdId, categoryId, limitValue, startMonth, endMonth));
        return generateDueBudgets(saved);
    }

    public List<RecurringBudget> list(Long currentHouseholdId) {
        log.debug("Listando orçamentos recorrentes do usuário={}", currentHouseholdId);
        return recurringBudgetRepositoryPort.findAllByHouseholdId(currentHouseholdId);
    }

    @Transactional
    public RecurringBudget update(
            Long currentHouseholdId,
            Long recurringBudgetId,
            BigDecimal limitValue,
            YearMonth endMonth,
            boolean active) {
        log.debug(
                "Atualizando orçamento recorrente={} do usuário={}",
                recurringBudgetId,
                currentHouseholdId);
        RecurringBudget existing = findOwnedOrThrow(currentHouseholdId, recurringBudgetId);
        requireValidPeriod(existing.startMonth(), endMonth);
        RecurringBudget updated =
                recurringBudgetRepositoryPort.save(
                        existing.withDetails(limitValue, endMonth, active, YearMonth.now(clock)));
        return generateDueBudgets(updated);
    }

    /** Exclui só o modelo: os orçamentos já lançados continuam, sem nenhum vínculo com ele. */
    public void delete(Long currentHouseholdId, Long recurringBudgetId) {
        log.debug(
                "Removendo orçamento recorrente={} do usuário={}",
                recurringBudgetId,
                currentHouseholdId);
        findOwnedOrThrow(currentHouseholdId, recurringBudgetId);
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
        FlowLog.detail("recurringBudgetId", recurrence.id());
        FlowLog.detail("dueMonths", dueMonths.size());
        if (dueMonths.isEmpty()) {
            return recurrence;
        }
        int skipped = 0;
        for (YearMonth month : dueMonths) {
            boolean alreadyExists =
                    budgetRepositoryPort
                            .existsByHouseholdIdAndCategoryIdAndReferenceMonthAndClientId(
                                    recurrence.householdId(), recurrence.categoryId(), month, null);
            if (alreadyExists) {
                skipped++;
            } else {
                budgetRepositoryPort.save(
                        Budget.create(
                                recurrence.householdId(),
                                recurrence.categoryId(),
                                month,
                                recurrence.limitValue(),
                                null));
            }
        }
        FlowLog.detail("skippedExisting", skipped);
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
    private RecurringBudget findOwnedOrThrow(Long currentHouseholdId, Long recurringBudgetId) {
        return recurringBudgetRepositoryPort
                .findById(recurringBudgetId)
                .filter(recurrence -> recurrence.belongsTo(currentHouseholdId))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Orçamento recorrente não encontrado: "
                                                + recurringBudgetId));
    }

    private void requireMatchingCategory(Long currentHouseholdId, Long categoryId) {
        Category category =
                categoryRepositoryPort
                        .findById(categoryId)
                        .filter(candidate -> candidate.isVisibleTo(currentHouseholdId))
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
