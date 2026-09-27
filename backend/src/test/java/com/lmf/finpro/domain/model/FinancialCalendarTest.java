package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.FinancialCalendar.DasDue;
import com.lmf.finpro.domain.model.FinancialCalendar.Day;
import com.lmf.finpro.domain.model.FinancialCalendar.Entry;
import com.lmf.finpro.domain.model.FinancialCalendar.EntryKind;
import com.lmf.finpro.domain.model.FinancialCalendar.EntryStatus;
import com.lmf.finpro.domain.model.FinancialCalendar.Report;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinancialCalendarTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 15);
    private static final YearMonth SEPTEMBER = YearMonth.of(2026, 9);

    @Test
    void classifiesTransactionsOfTheMonthAndHidesPaidByDefault() {
        List<Transaction> transactions =
                List.of(
                        transaction(
                                1L, "Aluguel", "1500", "2026-09-10", CategoryType.EXPENSE, false),
                        transaction(
                                2L, "Projeto", "4000", "2026-09-20", CategoryType.INCOME, false),
                        transaction(3L, "Mercado", "300", "2026-09-05", CategoryType.EXPENSE, true),
                        transaction(4L, "Salário", "2000", "2026-09-01", CategoryType.INCOME, true),
                        transaction(
                                5L, "Outubro", "999", "2026-10-02", CategoryType.EXPENSE, false));

        Report report =
                FinancialCalendar.build(SEPTEMBER, TODAY, transactions, List.of(), null, false);

        assertThat(report.days())
                .extracting(Day::date)
                .containsExactly(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20));
        assertThat(report.days().get(0).entries().get(0).status()).isEqualTo(EntryStatus.OVERDUE);
        assertThat(report.days().get(1).entries().get(0).status()).isEqualTo(EntryStatus.PENDING);
        assertThat(report.expectedIncome()).isEqualByComparingTo("4000");
        assertThat(report.expectedExpense()).isEqualByComparingTo("1500");
        assertThat(report.paidIncome()).isEqualByComparingTo("2000");
        assertThat(report.paidExpense()).isEqualByComparingTo("300");
    }

    @Test
    void includesPaidTransactionsWhenAsked() {
        List<Transaction> transactions =
                List.of(
                        transaction(
                                3L, "Mercado", "300", "2026-09-05", CategoryType.EXPENSE, true));

        Report report =
                FinancialCalendar.build(SEPTEMBER, TODAY, transactions, List.of(), null, true);

        assertThat(report.days()).hasSize(1);
        Day day = report.days().get(0);
        assertThat(day.expense()).isEqualByComparingTo("300");
        assertThat(day.entries().get(0).status()).isEqualTo(EntryStatus.PAID);
    }

    @Test
    void listsOverdueFromAnyMonthOldestFirst() {
        List<Transaction> transactions =
                List.of(
                        transaction(
                                1L, "Setembro", "100", "2026-09-10", CategoryType.EXPENSE, false),
                        transaction(2L, "Julho", "50", "2026-07-01", CategoryType.INCOME, false),
                        transaction(3L, "Hoje", "70", "2026-09-15", CategoryType.EXPENSE, false),
                        transaction(4L, "Paga", "10", "2026-08-01", CategoryType.EXPENSE, true));

        Report report =
                FinancialCalendar.build(
                        YearMonth.of(2026, 12), TODAY, transactions, List.of(), null, false);

        assertThat(report.overdue())
                .extracting(Entry::description)
                .containsExactly("Julho", "Setembro");
        assertThat(report.overdueIncome()).isEqualByComparingTo("50");
        assertThat(report.overdueExpense()).isEqualByComparingTo("100");
        assertThat(report.days()).isEmpty();
    }

    @Test
    void forecastsRecurringOccurrencesFromTodayOn() {
        // Semanal a partir de 01/09, com 3 ocorrências já lançadas (01, 08 e 15/09 às 00:05).
        RecurringTransaction weekly =
                recurrence(
                        7L, "Diarista", "200", "2026-09-01", RecurrenceFrequency.WEEKLY, 3, true);
        // Mensal cuja ocorrência de hoje ainda não foi lançada pelo agendador.
        RecurringTransaction monthly =
                recurrence(
                        8L, "Internet", "120", "2026-08-15", RecurrenceFrequency.MONTHLY, 1, true);
        RecurringTransaction paused =
                recurrence(
                        9L, "Pausada", "999", "2026-09-01", RecurrenceFrequency.WEEKLY, 0, false);

        Report report =
                FinancialCalendar.build(
                        SEPTEMBER, TODAY, List.of(), List.of(weekly, monthly, paused), null, false);

        assertThat(report.days())
                .extracting(Day::date)
                .containsExactly(
                        LocalDate.of(2026, 9, 15),
                        LocalDate.of(2026, 9, 22),
                        LocalDate.of(2026, 9, 29));
        Entry forecast = report.days().get(1).entries().get(0);
        assertThat(forecast.kind()).isEqualTo(EntryKind.RECURRING_FORECAST);
        assertThat(forecast.status()).isEqualTo(EntryStatus.FORECAST);
        assertThat(forecast.recurringTransactionId()).isEqualTo(7L);
        assertThat(forecast.transactionId()).isNull();
        assertThat(report.expectedExpense()).isEqualByComparingTo("520");
    }

    @Test
    void pastMonthHasNoForecasts() {
        RecurringTransaction weekly =
                recurrence(
                        7L, "Diarista", "200", "2026-09-01", RecurrenceFrequency.WEEKLY, 0, true);

        Report report =
                FinancialCalendar.build(
                        YearMonth.of(2026, 8), TODAY, List.of(), List.of(weekly), null, false);

        assertThat(report.days()).isEmpty();
    }

    @Test
    void showsDasOnlyInItsDueMonthAndSkipsMissingValueInTotals() {
        DasDue withValue =
                new DasDue(
                        YearMonth.of(2026, 8), LocalDate.of(2026, 9, 20), new BigDecimal("76.90"));
        DasDue withoutValue = new DasDue(YearMonth.of(2026, 8), LocalDate.of(2026, 9, 20), null);

        Report september =
                FinancialCalendar.build(SEPTEMBER, TODAY, List.of(), List.of(), withValue, false);
        Report noValue =
                FinancialCalendar.build(
                        SEPTEMBER, TODAY, List.of(), List.of(), withoutValue, false);
        Report october =
                FinancialCalendar.build(
                        YearMonth.of(2026, 10), TODAY, List.of(), List.of(), withValue, false);

        Entry das = september.days().get(0).entries().get(0);
        assertThat(das.kind()).isEqualTo(EntryKind.DAS);
        assertThat(das.type()).isEqualTo(CategoryType.EXPENSE);
        assertThat(das.competence()).isEqualTo(YearMonth.of(2026, 8));
        assertThat(september.expectedExpense()).isEqualByComparingTo("76.90");
        assertThat(noValue.days().get(0).expense()).isEqualByComparingTo("0");
        assertThat(noValue.expectedExpense()).isEqualByComparingTo("0");
        assertThat(october.days()).isEmpty();
    }

    @Test
    void ordersEntriesOfADayIncomeFirstThenByAmount() {
        List<Transaction> transactions =
                List.of(
                        transaction(
                                1L,
                                "Despesa pequena",
                                "10",
                                "2026-09-20",
                                CategoryType.EXPENSE,
                                false),
                        transaction(
                                2L,
                                "Despesa grande",
                                "90",
                                "2026-09-20",
                                CategoryType.EXPENSE,
                                false),
                        transaction(3L, "Receita", "5", "2026-09-20", CategoryType.INCOME, false));

        Report report =
                FinancialCalendar.build(SEPTEMBER, TODAY, transactions, List.of(), null, false);

        Day day = report.days().get(0);
        assertThat(day.entries())
                .extracting(Entry::description)
                .containsExactly("Receita", "Despesa grande", "Despesa pequena");
        assertThat(day.income()).isEqualByComparingTo("5");
        assertThat(day.expense()).isEqualByComparingTo("100");
    }

    private static Transaction transaction(
            Long id,
            String description,
            String amount,
            String date,
            CategoryType type,
            boolean paid) {
        return new Transaction(
                id,
                1L,
                null,
                null,
                description,
                new BigDecimal(amount),
                LocalDate.parse(date),
                type,
                TransactionOrigin.MANUAL,
                null,
                null,
                null,
                null,
                paid ? TransactionStatus.PAID : TransactionStatus.PENDING);
    }

    private static RecurringTransaction recurrence(
            Long id,
            String description,
            String amount,
            String startDate,
            RecurrenceFrequency frequency,
            int generated,
            boolean active) {
        return new RecurringTransaction(
                id,
                10L,
                1L,
                null,
                null,
                description,
                new BigDecimal(amount),
                CategoryType.EXPENSE,
                frequency,
                LocalDate.parse(startDate),
                null,
                generated,
                active,
                null);
    }
}
