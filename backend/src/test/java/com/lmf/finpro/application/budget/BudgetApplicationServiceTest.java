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
import com.lmf.finpro.domain.model.Client;
import com.lmf.finpro.domain.model.ClientWorkType;
import com.lmf.finpro.domain.model.DocumentType;
import com.lmf.finpro.domain.port.out.BudgetRepositoryPort;
import com.lmf.finpro.domain.port.out.ClientRepositoryPort;
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
    @Mock private ClientRepositoryPort clientRepositoryPort;

    @InjectMocks private BudgetApplicationService service;

    @Test
    void createSavesBudgetBuiltFromInput() {
        when(budgetRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        YearMonth month = YearMonth.of(2026, 9);

        Budget created = service.create(10L, 5L, month, BigDecimal.valueOf(500), null);

        assertThat(created.householdId()).isEqualTo(10L);
        assertThat(created.categoryId()).isEqualTo(5L);
        assertThat(created.referenceMonth()).isEqualTo(month);
        assertThat(created.limitValue()).isEqualByComparingTo("500");
        assertThat(created.clientId()).isNull();
    }

    @Test
    void listReturnsAllBudgetsForUser() {
        Budget budget =
                new Budget(1L, 10L, 5L, YearMonth.of(2026, 9), BigDecimal.valueOf(500), null);
        when(budgetRepositoryPort.findAllByHouseholdId(10L)).thenReturn(List.of(budget));

        assertThat(service.list(10L)).containsExactly(budget);
    }

    @Test
    void calculateSpentQueriesTransactionsForTheBudgetsCategoryAndMonth() {
        Budget budget =
                new Budget(1L, 10L, 5L, YearMonth.of(2026, 9), BigDecimal.valueOf(500), null);
        when(transactionRepositoryPort.sumAmountByHouseholdIdAndCategoryIdAndTypeBetween(
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
        Budget budget =
                new Budget(1L, 10L, 5L, YearMonth.of(2026, 9), BigDecimal.valueOf(500), null);
        when(budgetRepositoryPort.findById(1L)).thenReturn(Optional.of(budget));

        service.delete(10L, 1L);

        verify(budgetRepositoryPort).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenBudgetBelongsToAnotherUser() {
        Budget budget =
                new Budget(1L, 10L, 5L, YearMonth.of(2026, 9), BigDecimal.valueOf(500), null);
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

    @Test
    void createLinksBudgetToOwnedClient() {
        when(clientRepositoryPort.findById(7L)).thenReturn(Optional.of(ownedClient(7L, 10L)));
        when(budgetRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Budget created =
                service.create(10L, 5L, YearMonth.of(2026, 9), BigDecimal.valueOf(500), 7L);

        assertThat(created.clientId()).isEqualTo(7L);
    }

    @Test
    void createThrowsWhenClientBelongsToAnotherUser() {
        when(clientRepositoryPort.findById(7L)).thenReturn(Optional.of(ownedClient(7L, 999L)));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        10L,
                                        5L,
                                        YearMonth.of(2026, 9),
                                        BigDecimal.valueOf(500),
                                        7L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(budgetRepositoryPort, never()).save(any());
    }

    @Test
    void createThrowsWhenClientDoesNotExist() {
        when(clientRepositoryPort.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.create(
                                        10L,
                                        5L,
                                        YearMonth.of(2026, 9),
                                        BigDecimal.valueOf(500),
                                        7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void calculateSpentRestrictsToTheClientWhenBudgetIsLinked() {
        Budget budget = new Budget(1L, 10L, 5L, YearMonth.of(2026, 9), BigDecimal.valueOf(500), 7L);
        when(transactionRepositoryPort.sumAmountByHouseholdIdAndCategoryIdAndClientIdAndTypeBetween(
                        10L,
                        5L,
                        7L,
                        CategoryType.EXPENSE,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 10, 1)))
                .thenReturn(BigDecimal.valueOf(120));

        assertThat(service.calculateSpent(budget)).isEqualByComparingTo("120");
    }

    private static Client ownedClient(Long id, Long householdId) {
        return new Client(
                id,
                householdId,
                "Acme",
                "a@acme.com",
                "11999998888",
                DocumentType.CNPJ,
                "11444777000161",
                ClientWorkType.PJ,
                null,
                null,
                true);
    }
}
