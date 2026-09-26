package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RecurringTransactionTest {

    private RecurringTransaction recurrence(
            RecurrenceFrequency frequency,
            LocalDate startDate,
            LocalDate endDate,
            int generatedOccurrences,
            boolean active) {
        return new RecurringTransaction(
                1L,
                10L,
                2L,
                null,
                null,
                "Aluguel",
                BigDecimal.valueOf(1500),
                CategoryType.EXPENSE,
                frequency,
                startDate,
                endDate,
                generatedOccurrences,
                active,
                LocalDateTime.now());
    }

    @Test
    void monthlyOccurrencesAreAnchoredOnStartDateAndDoNotDriftAfterShortMonths() {
        RecurringTransaction rent =
                recurrence(RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 1, 31), null, 0, true);

        assertThat(rent.dueOccurrenceDates(LocalDate.of(2026, 4, 29)))
                .containsExactly(
                        LocalDate.of(2026, 1, 31),
                        LocalDate.of(2026, 2, 28),
                        LocalDate.of(2026, 3, 31));
    }

    @Test
    void dueOccurrencesSkipAlreadyGeneratedOnesAndIncludeToday() {
        RecurringTransaction weekly =
                recurrence(RecurrenceFrequency.WEEKLY, LocalDate.of(2026, 9, 1), null, 2, true);

        assertThat(weekly.dueOccurrenceDates(LocalDate.of(2026, 9, 22)))
                .containsExactly(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 22));
    }

    @Test
    void dueOccurrencesStopAtEndDate() {
        RecurringTransaction limited =
                recurrence(
                        RecurrenceFrequency.MONTHLY,
                        LocalDate.of(2026, 1, 10),
                        LocalDate.of(2026, 3, 10),
                        0,
                        true);

        assertThat(limited.dueOccurrenceDates(LocalDate.of(2026, 12, 31))).hasSize(3);
    }

    @Test
    void inactiveRecurrenceHasNoDueOccurrences() {
        RecurringTransaction paused =
                recurrence(RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 1, 10), null, 0, false);

        assertThat(paused.dueOccurrenceDates(LocalDate.of(2026, 12, 31))).isEmpty();
    }

    @Test
    void futureStartDateHasNoDueOccurrencesYet() {
        RecurringTransaction future =
                recurrence(RecurrenceFrequency.YEARLY, LocalDate.of(2027, 1, 1), null, 0, true);

        assertThat(future.dueOccurrenceDates(LocalDate.of(2026, 9, 25))).isEmpty();
        assertThat(future.nextOccurrenceDate()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    void nextOccurrenceIsNullOnceEndDateIsPassed() {
        RecurringTransaction finished =
                recurrence(
                        RecurrenceFrequency.YEARLY,
                        LocalDate.of(2024, 5, 1),
                        LocalDate.of(2025, 5, 1),
                        2,
                        true);

        assertThat(finished.nextOccurrenceDate()).isNull();
    }

    @Test
    void reactivatingSkipsOccurrencesThatFellDuringThePause() {
        RecurringTransaction paused =
                recurrence(RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 1, 5), null, 3, false);

        RecurringTransaction resumed =
                paused.withDetails(
                        null,
                        null,
                        "Aluguel",
                        BigDecimal.valueOf(1500),
                        null,
                        true,
                        LocalDate.of(2026, 9, 25));

        assertThat(resumed.active()).isTrue();
        assertThat(resumed.nextOccurrenceDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(resumed.dueOccurrenceDates(LocalDate.of(2026, 9, 25))).isEmpty();
    }

    @Test
    void reactivatingKeepsTodaysOccurrenceDue() {
        RecurringTransaction paused =
                recurrence(RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 1, 25), null, 1, false);

        RecurringTransaction resumed =
                paused.withDetails(
                        null,
                        null,
                        "Aluguel",
                        BigDecimal.valueOf(1500),
                        null,
                        true,
                        LocalDate.of(2026, 9, 25));

        assertThat(resumed.dueOccurrenceDates(LocalDate.of(2026, 9, 25)))
                .containsExactly(LocalDate.of(2026, 9, 25));
    }

    @Test
    void editingAnActiveRecurrenceKeepsItsProgress() {
        RecurringTransaction active =
                recurrence(RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 1, 5), null, 3, true);

        RecurringTransaction edited =
                active.withDetails(
                        null,
                        null,
                        "Aluguel reajustado",
                        BigDecimal.valueOf(1600),
                        null,
                        true,
                        LocalDate.of(2026, 9, 25));

        assertThat(edited.generatedOccurrences()).isEqualTo(3);
        assertThat(edited.description()).isEqualTo("Aluguel reajustado");
        assertThat(edited.amount()).isEqualByComparingTo("1600");
    }
}
