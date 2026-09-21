package com.lmf.finpro.application.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetApplicationServiceTest {

    @Mock private BudgetRepositoryPort budgetRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;

    @InjectMocks private BudgetApplicationService service;

    @Test
    void createSavesBudgetBuiltFromInput() {
        when(budgetRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        YearMonth month = YearMonth.of(2026, 9);

        Budget created = service.create(10L, 5L, month, BigDecimal.valueOf(500));

        assertThat(created.userId()).isEqualTo(10L);
        assertThat(created.categoryId()).isEqualTo(5L);
        assertThat(created.referenceMonth()).isEqualTo(month);
        assertThat(created.limitValue()).isEqualByComparingTo("500");
    }

    @Test
    void listReturnsAllBudgetsForUser() {
        Budget budget = new Budget(1L, 10L, 5L, YearMonth.of(2026, 9), BigDecimal.valueOf(500));
        when(budgetRepositoryPort.findAllByUserId(10L)).thenReturn(List.of(budget));

        assertThat(service.list(10L)).containsExactly(budget);
    }

    @Test
    void calculateSpentQueriesTransactionsForTheBudgetsCategoryAndMonth() {
        Budget budget = new Budget(1L, 10L, 5L, YearMonth.of(2026, 9), BigDecimal.valueOf(500));
        when(transactionRepositoryPort.sumAmountByUserIdAndCategoryIdAndTypeBetween(
                        10L,
                        5L,
                        CategoryType.EXPENSE,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 10, 1)))
                .thenReturn(BigDecimal.valueOf(350));

        BigDecimal spent = service.calculateSpent(budget);

        assertThat(spent).isEqualByComparingTo("350");
    }

    @Test
    void deleteRemovesBudgetWhenOwned() {
        Budget budget = new Budget(1L, 10L, 5L, YearMonth.of(2026, 9), BigDecimal.valueOf(500));
        when(budgetRepositoryPort.findById(1L)).thenReturn(Optional.of(budget));

        service.delete(10L, 1L);

        verify(budgetRepositoryPort).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenBudgetBelongsToAnotherUser() {
        Budget budget = new Budget(1L, 10L, 5L, YearMonth.of(2026, 9), BigDecimal.valueOf(500));
        when(budgetRepositoryPort.findById(1L)).thenReturn(Optional.of(budget));

        assertThatThrownBy(() -> service.delete(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(budgetRepositoryPort, never()).deleteById(any());
    }

    @Test
    void deleteThrowsWhenBudgetDoesNotExist() {
        when(budgetRepositoryPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
