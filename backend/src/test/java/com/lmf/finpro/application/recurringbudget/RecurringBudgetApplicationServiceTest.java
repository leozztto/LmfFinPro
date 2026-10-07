package com.lmf.finpro.application.recurringbudget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecurringBudgetApplicationServiceTest {

    private static final YearMonth CURRENT_MONTH = YearMonth.of(2026, 9);

    @Mock private RecurringBudgetRepositoryPort recurringBudgetRepositoryPort;
    @Mock private BudgetRepositoryPort budgetRepositoryPort;
    @Mock private CategoryRepositoryPort categoryRepositoryPort;

    private RecurringBudgetApplicationService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock =
                Clock.fixed(
                        CURRENT_MONTH.atDay(15).atStartOfDay().toInstant(ZoneOffset.UTC),
                        ZoneId.of("UTC"));
        service =
                new RecurringBudgetApplicationService(
                        recurringBudgetRepositoryPort,
                        budgetRepositoryPort,
                        categoryRepositoryPort,
                        fixedClock);
    }

    private Category expenseCategory() {
        return new Category(5L, 10L, "Mercado", CategoryType.EXPENSE, null, null);
    }

    private RecurringBudget existing(YearMonth startMonth, int generatedMonths, boolean active) {
        return new RecurringBudget(
                1L,
                10L,
                5L,
                BigDecimal.valueOf(500),
                startMonth,
                null,
                generatedMonths,
                active,
                LocalDateTime.now());
    }

    @Test
    void createWithPastStartMonthGeneratesEveryDueMonthImmediately() {
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(expenseCategory()));
        when(recurringBudgetRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringBudget created =
                service.create(10L, 5L, BigDecimal.valueOf(500), YearMonth.of(2026, 7), null);

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepositoryPort, times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Budget::referenceMonth)
                .containsExactly(
                        YearMonth.of(2026, 7), YearMonth.of(2026, 8), YearMonth.of(2026, 9));
        assertThat(captor.getAllValues())
                .allSatisfy(
                        budget -> {
                            assertThat(budget.categoryId()).isEqualTo(5L);
                            assertThat(budget.limitValue()).isEqualByComparingTo("500");
                        });
        assertThat(created.generatedMonths()).isEqualTo(3);
        assertThat(created.nextGenerationMonth()).isEqualTo(YearMonth.of(2026, 10));
    }

    @Test
    void createWithFutureStartMonthDoesNotGenerateBudgets() {
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(expenseCategory()));
        when(recurringBudgetRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringBudget created =
                service.create(10L, 5L, BigDecimal.valueOf(500), YearMonth.of(2026, 10), null);

        verify(budgetRepositoryPort, never()).save(any());
        assertThat(created.generatedMonths()).isZero();
    }

    @Test
    void createSkipsMonthsThatAlreadyHaveABudget() {
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(expenseCategory()));
        when(recurringBudgetRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(budgetRepositoryPort.existsByHouseholdIdAndCategoryIdAndReferenceMonthAndClientId(
                        eq(10L), eq(5L), any(), isNull()))
                .thenReturn(false);
        when(budgetRepositoryPort.existsByHouseholdIdAndCategoryIdAndReferenceMonthAndClientId(
                        10L, 5L, YearMonth.of(2026, 8), null))
                .thenReturn(true);

        RecurringBudget created =
                service.create(10L, 5L, BigDecimal.valueOf(500), YearMonth.of(2026, 7), null);

        verify(budgetRepositoryPort, never()).save(argThatReferenceMonthIs(YearMonth.of(2026, 8)));
        verify(budgetRepositoryPort, times(1)).save(argThatReferenceMonthIs(YearMonth.of(2026, 7)));
        verify(budgetRepositoryPort, times(1)).save(argThatReferenceMonthIs(YearMonth.of(2026, 9)));
        assertThat(created.generatedMonths()).isEqualTo(3);
    }

    private Budget argThatReferenceMonthIs(YearMonth month) {
        return org.mockito.ArgumentMatchers.argThat(
                budget -> budget.referenceMonth().equals(month));
    }

    @Test
    void createThrowsWhenCategoryIsNotAnExpenseCategory() {
        when(categoryRepositoryPort.findById(5L))
                .thenReturn(
                        Optional.of(
                                new Category(5L, 10L, "Salário", CategoryType.INCOME, null, null)));

        assertThatThrownBy(() -> service.create(10L, 5L, BigDecimal.TEN, CURRENT_MONTH, null))
                .isInstanceOf(CategoryTypeMismatchException.class);
    }

    @Test
    void createThrowsWhenCategoryBelongsToAnotherUser() {
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(expenseCategory()));

        assertThatThrownBy(() -> service.create(999L, 5L, BigDecimal.TEN, CURRENT_MONTH, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(recurringBudgetRepositoryPort, never()).save(any());
    }

    @Test
    void createThrowsWhenEndMonthIsBeforeStartMonth() {
        assertThatThrownBy(
                        () ->
                                service.create(
                                        10L,
                                        5L,
                                        BigDecimal.TEN,
                                        CURRENT_MONTH,
                                        CURRENT_MONTH.minusMonths(1)))
                .isInstanceOf(InvalidRecurrencePeriodException.class);
    }

    @Test
    void createBatchCreatesOneRecurrencePerCategory() {
        Category otherExpenseCategory =
                new Category(6L, 10L, "Transporte", CategoryType.EXPENSE, null, null);
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(expenseCategory()));
        when(categoryRepositoryPort.findById(6L)).thenReturn(Optional.of(otherExpenseCategory));
        when(recurringBudgetRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<RecurringBudget> created =
                service.createBatch(
                        10L,
                        CURRENT_MONTH,
                        null,
                        List.of(
                                new CategoryLimit(5L, BigDecimal.valueOf(500)),
                                new CategoryLimit(6L, BigDecimal.valueOf(300))));

        assertThat(created).hasSize(2);
        assertThat(created).extracting(RecurringBudget::categoryId).containsExactly(5L, 6L);
        verify(budgetRepositoryPort, times(2)).save(any());
    }

    @Test
    void createBatchThrowsWhenSameCategoryAppearsTwice() {
        assertThatThrownBy(
                        () ->
                                service.createBatch(
                                        10L,
                                        CURRENT_MONTH,
                                        null,
                                        List.of(
                                                new CategoryLimit(5L, BigDecimal.valueOf(500)),
                                                new CategoryLimit(5L, BigDecimal.valueOf(300)))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(recurringBudgetRepositoryPort, never()).save(any());
    }

    @Test
    void createBatchThrowsWhenOneCategoryIsInvalidAndCreatesNothingForThatCall() {
        when(categoryRepositoryPort.findById(5L)).thenReturn(Optional.of(expenseCategory()));
        when(categoryRepositoryPort.findById(6L)).thenReturn(Optional.empty());
        when(recurringBudgetRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(
                        () ->
                                service.createBatch(
                                        10L,
                                        CURRENT_MONTH,
                                        null,
                                        List.of(
                                                new CategoryLimit(5L, BigDecimal.valueOf(500)),
                                                new CategoryLimit(6L, BigDecimal.valueOf(300)))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listReturnsRecurrencesOfUser() {
        RecurringBudget recurrence = existing(CURRENT_MONTH, 0, true);
        when(recurringBudgetRepositoryPort.findAllByHouseholdId(10L))
                .thenReturn(List.of(recurrence));

        assertThat(service.list(10L)).containsExactly(recurrence);
    }

    @Test
    void updateChangesDetailsWithoutGeneratingWhenNothingIsDue() {
        when(recurringBudgetRepositoryPort.findById(1L))
                .thenReturn(Optional.of(existing(YearMonth.of(2026, 6), 4, true)));
        when(recurringBudgetRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringBudget updated = service.update(10L, 1L, BigDecimal.valueOf(700), null, true);

        assertThat(updated.limitValue()).isEqualByComparingTo("700");
        verify(budgetRepositoryPort, never()).save(any());
    }

    @Test
    void updateThrowsWhenRecurrenceBelongsToAnotherUser() {
        when(recurringBudgetRepositoryPort.findById(1L))
                .thenReturn(Optional.of(existing(CURRENT_MONTH, 0, true)));

        assertThatThrownBy(() -> service.update(999L, 1L, BigDecimal.TEN, null, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesOwnedRecurrence() {
        when(recurringBudgetRepositoryPort.findById(1L))
                .thenReturn(Optional.of(existing(CURRENT_MONTH, 0, true)));

        service.delete(10L, 1L);

        verify(recurringBudgetRepositoryPort).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenRecurrenceDoesNotExist() {
        when(recurringBudgetRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(recurringBudgetRepositoryPort, never()).deleteById(any());
    }

    @Test
    void generateDueBudgetsOnlyLaunchesMissingOnes() {
        when(recurringBudgetRepositoryPort.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringBudget result =
                service.generateDueBudgets(existing(YearMonth.of(2026, 7), 2, true));

        verify(budgetRepositoryPort, times(1)).save(argThatReferenceMonthIs(YearMonth.of(2026, 9)));
        assertThat(result.generatedMonths()).isEqualTo(3);
    }

    @Test
    void findAllActiveDelegatesToRepository() {
        RecurringBudget recurrence = existing(CURRENT_MONTH, 0, true);
        when(recurringBudgetRepositoryPort.findAllActive()).thenReturn(List.of(recurrence));

        assertThat(service.findAllActive()).containsExactly(recurrence);
    }
}
