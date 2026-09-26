package com.lmf.finpro.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.domain.model.AccountType;
import com.lmf.finpro.domain.model.BreakdownPoint;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.DashboardOverview;
import com.lmf.finpro.domain.model.RecurrenceFrequency;
import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.domain.model.TransactionStatus;
import com.lmf.finpro.domain.port.out.AccountRepositoryPort;
import com.lmf.finpro.domain.port.out.RecurringTransactionRepositoryPort;
import com.lmf.finpro.domain.port.out.TransactionRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardApplicationServiceTest {

    @Mock private AccountRepositoryPort accountRepositoryPort;
    @Mock private TransactionRepositoryPort transactionRepositoryPort;
    @Mock private RecurringTransactionRepositoryPort recurringTransactionRepositoryPort;

    @InjectMocks private DashboardApplicationService service;

    private static Account account(BigDecimal initialBalance) {
        return new Account(
                1L, 10L, "Conta", AccountType.CHECKING, initialBalance, LocalDateTime.now());
    }

    private static Transaction transaction(
            BigDecimal amount, LocalDate date, CategoryType type, Long transferId) {
        Transaction base = Transaction.create(1L, null, null, "desc", amount, date, type);
        return new Transaction(
                base.id(),
                base.accountId(),
                base.categoryId(),
                base.clientId(),
                base.description(),
                base.amount(),
                base.transactionDate(),
                base.type(),
                base.origin(),
                base.createdAt(),
                transferId,
                base.importBatchId());
    }

    @Test
    void getOverviewExcludesTransfersFromCurrentBalance() {
        YearMonth currentMonth = YearMonth.now();
        when(accountRepositoryPort.findAllByUserId(10L))
                .thenReturn(List.of(account(BigDecimal.valueOf(1000))));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                transaction(
                                        BigDecimal.valueOf(500),
                                        currentMonth.atDay(1),
                                        CategoryType.INCOME,
                                        null),
                                // transferência: não deve entrar no saldo nem na receita do mês
                                transaction(
                                        BigDecimal.valueOf(9999),
                                        currentMonth.atDay(1),
                                        CategoryType.INCOME,
                                        77L)));

        DashboardOverview overview = service.getOverview(10L);

        assertThat(overview.currentBalance()).isEqualByComparingTo("1500");
        assertThat(overview.currentMonthIncome()).isEqualByComparingTo("500");
    }

    @Test
    void getOverviewLeavesPendingOutOfCurrentBalanceButInProjectedBalance() {
        YearMonth currentMonth = YearMonth.now();
        when(accountRepositoryPort.findAllByUserId(10L))
                .thenReturn(List.of(account(BigDecimal.valueOf(1000))));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                Transaction.create(
                                        1L,
                                        null,
                                        null,
                                        "pago",
                                        BigDecimal.valueOf(200),
                                        currentMonth.atDay(1),
                                        CategoryType.INCOME,
                                        TransactionStatus.PAID),
                                Transaction.create(
                                        1L,
                                        null,
                                        null,
                                        "a receber",
                                        BigDecimal.valueOf(500),
                                        currentMonth.atDay(1),
                                        CategoryType.INCOME,
                                        TransactionStatus.PENDING),
                                Transaction.create(
                                        1L,
                                        null,
                                        null,
                                        "a pagar",
                                        BigDecimal.valueOf(150),
                                        currentMonth.atDay(1),
                                        CategoryType.EXPENSE,
                                        TransactionStatus.PENDING)));

        DashboardOverview overview = service.getOverview(10L);

        assertThat(overview.currentBalance()).isEqualByComparingTo("1200");
        assertThat(overview.pendingIncome()).isEqualByComparingTo("500");
        assertThat(overview.pendingExpense()).isEqualByComparingTo("150");
        assertThat(overview.projectedBalance()).isEqualByComparingTo("1550");
        // receita do mês segue por competência: paga + pendente
        assertThat(overview.currentMonthIncome()).isEqualByComparingTo("700");
    }

    @Test
    void getCategoryBreakdownDelegatesToAggregatorWithOwnedTransactionsOnly() {
        YearMonth month = YearMonth.now();
        when(accountRepositoryPort.findAllByUserId(10L))
                .thenReturn(List.of(account(BigDecimal.ZERO)));
        Transaction ownTransaction =
                Transaction.create(
                        1L,
                        5L,
                        null,
                        "desc",
                        BigDecimal.valueOf(100),
                        month.atDay(1),
                        CategoryType.EXPENSE);
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(List.of(ownTransaction));

        List<BreakdownPoint> breakdown =
                service.getCategoryBreakdown(10L, CategoryType.EXPENSE, month);

        assertThat(breakdown).hasSize(1);
        assertThat(breakdown.get(0).entityId()).isEqualTo(5L);
        assertThat(breakdown.get(0).value()).isEqualByComparingTo("100");
    }

    @Test
    void getMonthlyFlowReturnsRequestedNumberOfMonths() {
        when(accountRepositoryPort.findAllByUserId(10L))
                .thenReturn(List.of(account(BigDecimal.ZERO)));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L))).thenReturn(List.of());

        assertThat(service.getMonthlyFlow(10L, 4)).hasSize(4);
    }

    @Test
    void getCashFlowProjectionAnchorsOnBalanceExcludingFutureTransactions() {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        when(accountRepositoryPort.findAllByUserId(10L))
                .thenReturn(List.of(account(BigDecimal.valueOf(1000))));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L)))
                .thenReturn(
                        List.of(
                                transaction(
                                        BigDecimal.valueOf(900),
                                        nextMonth.atDay(5),
                                        CategoryType.INCOME,
                                        null)));

        var projection = service.getCashFlowProjection(10L, 1);

        assertThat(projection).hasSize(1);
        assertThat(projection.get(0).balance()).isEqualByComparingTo("1900");
    }

    @Test
    void getCashFlowProjectionIncludesTheUsersRecurringTransactions() {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        when(accountRepositoryPort.findAllByUserId(10L))
                .thenReturn(List.of(account(BigDecimal.valueOf(1000))));
        when(transactionRepositoryPort.findAllByAccountIds(List.of(1L))).thenReturn(List.of());
        when(recurringTransactionRepositoryPort.findAllByUserId(10L))
                .thenReturn(
                        List.of(
                                new RecurringTransaction(
                                        1L,
                                        10L,
                                        1L,
                                        null,
                                        null,
                                        "Aluguel",
                                        BigDecimal.valueOf(1500),
                                        CategoryType.EXPENSE,
                                        RecurrenceFrequency.MONTHLY,
                                        nextMonth.atDay(5),
                                        null,
                                        0,
                                        true,
                                        LocalDateTime.now())));

        var projection = service.getCashFlowProjection(10L, 1);

        assertThat(projection.get(0).balance()).isEqualByComparingTo("-500");
    }
}
