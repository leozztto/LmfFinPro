package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardAggregatorTest {

    private static Transaction transaction(BigDecimal amount, LocalDate date, CategoryType type) {
        return Transaction.create(1L, null, null, "desc", amount, date, type);
    }

    private static Transaction transaction(
            BigDecimal amount, LocalDate date, CategoryType type, Long categoryId, Long clientId) {
        return Transaction.create(1L, categoryId, clientId, "desc", amount, date, type);
    }

    @Test
    void lastMonthsEndsOnCurrentMonthAndIsOldestFirst() {
        List<YearMonth> months = DashboardAggregator.lastMonths(3);

        assertThat(months).hasSize(3);
        assertThat(months.get(2)).isEqualTo(YearMonth.now());
        assertThat(months.get(1)).isEqualTo(YearMonth.now().minusMonths(1));
        assertThat(months.get(0)).isEqualTo(YearMonth.now().minusMonths(2));
    }

    @Test
    void nextMonthsStartsRightAfterCurrentMonth() {
        List<YearMonth> months = DashboardAggregator.nextMonths(2);

        assertThat(months)
                .containsExactly(YearMonth.now().plusMonths(1), YearMonth.now().plusMonths(2));
    }

    @Test
    void monthlyFlowSumsIncomeAndExpensePerMonthIgnoringOutOfWindowTransactions() {
        YearMonth currentMonth = YearMonth.now();
        YearMonth twoMonthsAgo = currentMonth.minusMonths(2);
        List<Transaction> transactions =
                List.of(
                        transaction(
                                BigDecimal.valueOf(500),
                                currentMonth.atDay(1),
                                CategoryType.INCOME),
                        transaction(
                                BigDecimal.valueOf(200),
                                currentMonth.atDay(2),
                                CategoryType.EXPENSE),
                        // fora da janela de 1 mês solicitada — não deve entrar na soma
                        transaction(
                                BigDecimal.valueOf(9999),
                                twoMonthsAgo.atDay(1),
                                CategoryType.INCOME));

        List<MonthlyFlowPoint> flow = DashboardAggregator.monthlyFlow(transactions, 1);

        assertThat(flow).hasSize(1);
        assertThat(flow.get(0).month()).isEqualTo(currentMonth);
        assertThat(flow.get(0).income()).isEqualByComparingTo("500");
        assertThat(flow.get(0).expense()).isEqualByComparingTo("200");
    }

    @Test
    void monthlyFlowReturnsZeroForMonthsWithoutTransactions() {
        List<MonthlyFlowPoint> flow = DashboardAggregator.monthlyFlow(List.of(), 3);

        assertThat(flow).hasSize(3);
        assertThat(flow)
                .allSatisfy(
                        point -> {
                            assertThat(point.income()).isEqualByComparingTo("0");
                            assertThat(point.expense()).isEqualByComparingTo("0");
                        });
    }

    @Test
    void balanceOverTimeAccumulatesUpToEachMonthInclusive() {
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);
        List<Transaction> transactions =
                List.of(
                        transaction(
                                BigDecimal.valueOf(300),
                                previousMonth.atDay(1),
                                CategoryType.INCOME),
                        transaction(
                                BigDecimal.valueOf(100),
                                previousMonth.atDay(2),
                                CategoryType.EXPENSE),
                        transaction(
                                BigDecimal.valueOf(500),
                                currentMonth.atDay(1),
                                CategoryType.INCOME));

        List<BalancePoint> points =
                DashboardAggregator.balanceOverTime(transactions, BigDecimal.valueOf(1000), 2);

        assertThat(points.get(0).month()).isEqualTo(previousMonth);
        assertThat(points.get(0).balance()).isEqualByComparingTo("1200"); // 1000 + 300 - 100
        assertThat(points.get(1).month()).isEqualTo(currentMonth);
        assertThat(points.get(1).balance()).isEqualByComparingTo("1700"); // 1200 + 500
    }

    @Test
    void balanceOverTimeExcludesTransactionsDatedAfterTheGivenMonth() {
        YearMonth currentMonth = YearMonth.now();
        YearMonth nextMonth = currentMonth.plusMonths(1);
        List<Transaction> transactions =
                List.of(
                        transaction(
                                BigDecimal.valueOf(500), nextMonth.atDay(1), CategoryType.INCOME));

        List<BalancePoint> points =
                DashboardAggregator.balanceOverTime(transactions, BigDecimal.valueOf(1000), 1);

        assertThat(points.get(0).balance()).isEqualByComparingTo("1000");
    }

    @Test
    void cashFlowProjectionUsesRealNetWhenFutureMonthAlreadyHasTransactions() {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        List<Transaction> transactions =
                List.of(
                        transaction(
                                BigDecimal.valueOf(900), nextMonth.atDay(5), CategoryType.INCOME));

        List<CashFlowProjectionPoint> points =
                DashboardAggregator.cashFlowProjection(transactions, BigDecimal.valueOf(1000), 1);

        assertThat(points).hasSize(1);
        assertThat(points.get(0).month()).isEqualTo(nextMonth);
        assertThat(points.get(0).projected()).isTrue();
        assertThat(points.get(0).balance()).isEqualByComparingTo("1900");
    }

    @Test
    void cashFlowProjectionSubtractsExpensesAlreadyScheduledForAFutureMonth() {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        List<Transaction> transactions =
                List.of(
                        transaction(
                                BigDecimal.valueOf(300), nextMonth.atDay(5), CategoryType.EXPENSE));

        List<CashFlowProjectionPoint> points =
                DashboardAggregator.cashFlowProjection(transactions, BigDecimal.valueOf(1000), 1);

        assertThat(points.get(0).balance()).isEqualByComparingTo("700");
    }

    @Test
    void cashFlowProjectionUsesAverageOfLastThreeMonthsWhenFutureMonthHasNoTransactions() {
        YearMonth currentMonth = YearMonth.now();
        // líquido dos últimos 3 meses: mês atual +400, os outros dois sem lançamento (média =
        // 400/3)
        List<Transaction> transactions =
                List.of(
                        transaction(
                                BigDecimal.valueOf(400),
                                currentMonth.atDay(1),
                                CategoryType.INCOME));

        List<CashFlowProjectionPoint> points =
                DashboardAggregator.cashFlowProjection(transactions, BigDecimal.valueOf(1000), 1);

        BigDecimal expectedAverageNet =
                BigDecimal.valueOf(400)
                        .divide(BigDecimal.valueOf(3), 10, java.math.RoundingMode.HALF_UP);
        assertThat(points.get(0).balance())
                .isCloseTo(
                        BigDecimal.valueOf(1000).add(expectedAverageNet),
                        org.assertj.core.data.Offset.offset(new BigDecimal("0.01")));
    }

    @Test
    void categoryBreakdownGroupsByCategoryAndSortsDescending() {
        YearMonth month = YearMonth.now();
        List<Transaction> transactions =
                List.of(
                        transaction(
                                BigDecimal.valueOf(50),
                                month.atDay(1),
                                CategoryType.EXPENSE,
                                1L,
                                null),
                        transaction(
                                BigDecimal.valueOf(150),
                                month.atDay(2),
                                CategoryType.EXPENSE,
                                2L,
                                null),
                        transaction(
                                BigDecimal.valueOf(80),
                                month.atDay(3),
                                CategoryType.EXPENSE,
                                1L,
                                null),
                        // tipo diferente e categoria nula não entram nessa quebra
                        transaction(
                                BigDecimal.valueOf(999),
                                month.atDay(1),
                                CategoryType.INCOME,
                                1L,
                                null),
                        transaction(
                                BigDecimal.valueOf(30),
                                month.atDay(1),
                                CategoryType.EXPENSE,
                                null,
                                null));

        List<BreakdownPoint> breakdown =
                DashboardAggregator.categoryBreakdown(transactions, CategoryType.EXPENSE, month);

        assertThat(breakdown).hasSize(3);
        assertThat(breakdown.get(0).entityId()).isEqualTo(2L);
        assertThat(breakdown.get(0).value()).isEqualByComparingTo("150");
        assertThat(breakdown.get(1).entityId()).isEqualTo(1L);
        assertThat(breakdown.get(1).value()).isEqualByComparingTo("130"); // 50 + 80
        assertThat(breakdown.get(2).entityId()).isNull();
        assertThat(breakdown.get(2).value()).isEqualByComparingTo("30");
    }

    @Test
    void clientBreakdownOnlyConsidersIncomeInGivenMonth() {
        YearMonth month = YearMonth.now();
        List<Transaction> transactions =
                List.of(
                        transaction(
                                BigDecimal.valueOf(1200),
                                month.atDay(1),
                                CategoryType.INCOME,
                                null,
                                10L),
                        transaction(
                                BigDecimal.valueOf(300),
                                month.atDay(2),
                                CategoryType.EXPENSE,
                                null,
                                10L),
                        transaction(
                                BigDecimal.valueOf(500),
                                month.minusMonths(1).atDay(1),
                                CategoryType.INCOME,
                                null,
                                10L));

        List<BreakdownPoint> breakdown = DashboardAggregator.clientBreakdown(transactions, month);

        assertThat(breakdown).hasSize(1);
        assertThat(breakdown.get(0).entityId()).isEqualTo(10L);
        assertThat(breakdown.get(0).value()).isEqualByComparingTo("1200");
    }

    @Test
    void deltaPercentComputesPositiveVariation() {
        Double delta =
                DashboardAggregator.deltaPercent(
                        BigDecimal.valueOf(1500), BigDecimal.valueOf(1200));
        assertThat(delta).isCloseTo(25.0, offset(0.01));
    }

    @Test
    void deltaPercentReturnsNullWhenPreviousIsZero() {
        assertThat(DashboardAggregator.deltaPercent(BigDecimal.valueOf(500), BigDecimal.ZERO))
                .isNull();
    }

    @Test
    void deltaPercentUsesAbsoluteValueOfPreviousWhenNegative() {
        Double delta =
                DashboardAggregator.deltaPercent(BigDecimal.valueOf(-50), BigDecimal.valueOf(-100));
        assertThat(delta).isCloseTo(50.0, offset(0.01));
    }

    private static RecurringTransaction recurrence(
            Long id,
            CategoryType type,
            BigDecimal amount,
            RecurrenceFrequency frequency,
            LocalDate startDate,
            int generatedOccurrences,
            boolean active) {
        return new RecurringTransaction(
                id,
                10L,
                1L,
                null,
                null,
                "recorrente",
                amount,
                type,
                frequency,
                startDate,
                null,
                generatedOccurrences,
                active,
                LocalDateTime.now());
    }

    private static Transaction launchedByRecurrence(
            BigDecimal amount, LocalDate date, CategoryType type, Long recurrenceId) {
        return new Transaction(
                null,
                1L,
                null,
                null,
                "recorrente",
                amount,
                date,
                type,
                TransactionOrigin.RECURRING,
                LocalDateTime.now(),
                null,
                null,
                recurrenceId);
    }

    @Test
    void cashFlowProjectionAddsRecurringOccurrencesOfEachFutureMonth() {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        List<RecurringTransaction> recurrences =
                List.of(
                        recurrence(
                                1L,
                                CategoryType.EXPENSE,
                                BigDecimal.valueOf(500),
                                RecurrenceFrequency.MONTHLY,
                                nextMonth.atDay(10),
                                0,
                                true));

        List<CashFlowProjectionPoint> points =
                DashboardAggregator.cashFlowProjection(
                        List.of(), BigDecimal.valueOf(1000), 2, recurrences);

        assertThat(points)
                .extracting(CashFlowProjectionPoint::balance)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(BigDecimal.valueOf(500), BigDecimal.ZERO);
    }

    @Test
    void cashFlowProjectionCountsEveryWeeklyOccurrenceInTheMonth() {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        int occurrencesInMonth = nextMonth.lengthOfMonth() >= 29 ? 5 : 4;
        List<RecurringTransaction> recurrences =
                List.of(
                        recurrence(
                                1L,
                                CategoryType.EXPENSE,
                                BigDecimal.TEN,
                                RecurrenceFrequency.WEEKLY,
                                nextMonth.atDay(1),
                                0,
                                true));

        List<CashFlowProjectionPoint> points =
                DashboardAggregator.cashFlowProjection(
                        List.of(), BigDecimal.valueOf(1000), 1, recurrences);

        assertThat(points.get(0).balance())
                .isEqualByComparingTo(BigDecimal.valueOf(1000 - 10L * occurrencesInMonth));
    }

    @Test
    void cashFlowProjectionLeavesActiveRecurrenceTransactionsOutOfTheAverage() {
        YearMonth currentMonth = YearMonth.now();
        // salário recorrente já lançado no mês atual: sem a exclusão, entraria na média (+1000) e
        // de novo como ocorrência do próximo mês (+3000)
        List<Transaction> transactions =
                List.of(
                        launchedByRecurrence(
                                BigDecimal.valueOf(3000),
                                currentMonth.atDay(1),
                                CategoryType.INCOME,
                                7L));
        List<RecurringTransaction> recurrences =
                List.of(
                        recurrence(
                                7L,
                                CategoryType.INCOME,
                                BigDecimal.valueOf(3000),
                                RecurrenceFrequency.MONTHLY,
                                currentMonth.atDay(1),
                                1,
                                true));

        List<CashFlowProjectionPoint> points =
                DashboardAggregator.cashFlowProjection(
                        transactions, BigDecimal.valueOf(1000), 1, recurrences);

        assertThat(points.get(0).balance()).isEqualByComparingTo("4000");
    }

    @Test
    void cashFlowProjectionIgnoresPausedRecurrenceAndKeepsItsTransactionsInTheAverage() {
        YearMonth currentMonth = YearMonth.now();
        List<Transaction> transactions =
                List.of(
                        launchedByRecurrence(
                                BigDecimal.valueOf(300),
                                currentMonth.atDay(1),
                                CategoryType.INCOME,
                                7L));
        List<RecurringTransaction> recurrences =
                List.of(
                        recurrence(
                                7L,
                                CategoryType.INCOME,
                                BigDecimal.valueOf(300),
                                RecurrenceFrequency.MONTHLY,
                                currentMonth.atDay(1),
                                1,
                                false));

        List<CashFlowProjectionPoint> points =
                DashboardAggregator.cashFlowProjection(
                        transactions, BigDecimal.valueOf(1000), 1, recurrences);

        // média de 300/3 = 100, nenhuma ocorrência projetada
        assertThat(points.get(0).balance()).isEqualByComparingTo("1100");
    }

    @Test
    void cashFlowProjectionAddsOccurrencesStillPendingInTheCurrentMonthToTheStartingBalance() {
        YearMonth currentMonth = YearMonth.now();
        List<RecurringTransaction> recurrences =
                List.of(
                        recurrence(
                                1L,
                                CategoryType.EXPENSE,
                                BigDecimal.valueOf(100),
                                RecurrenceFrequency.MONTHLY,
                                currentMonth.atEndOfMonth(),
                                0,
                                true));

        List<CashFlowProjectionPoint> points =
                DashboardAggregator.cashFlowProjection(
                        List.of(), BigDecimal.valueOf(1000), 1, recurrences);

        // -100 ainda neste mês (saldo de partida) e -100 no próximo
        assertThat(points.get(0).balance()).isEqualByComparingTo("800");
    }

    @Test
    void cashFlowProjectionCombinesScheduledOneOffTransactionsWithRecurringOccurrences() {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        List<Transaction> transactions =
                List.of(
                        transaction(
                                BigDecimal.valueOf(200), nextMonth.atDay(3), CategoryType.EXPENSE));
        List<RecurringTransaction> recurrences =
                List.of(
                        recurrence(
                                1L,
                                CategoryType.INCOME,
                                BigDecimal.valueOf(1000),
                                RecurrenceFrequency.MONTHLY,
                                nextMonth.atDay(5),
                                0,
                                true));

        List<CashFlowProjectionPoint> points =
                DashboardAggregator.cashFlowProjection(
                        transactions, BigDecimal.valueOf(1000), 1, recurrences);

        assertThat(points.get(0).balance()).isEqualByComparingTo("1800");
    }
}
