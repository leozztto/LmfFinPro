package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class RecurringBudgetTest {

    private RecurringBudget recurrence(
            YearMonth startMonth, YearMonth endMonth, int generatedMonths, boolean active) {
        return new RecurringBudget(
                1L,
                10L,
                5L,
                BigDecimal.valueOf(500),
                startMonth,
                endMonth,
                generatedMonths,
                active,
                LocalDateTime.now());
    }

    @Test
    void dueMonthsSkipAlreadyGeneratedOnesAndIncludeCurrentMonth() {
        RecurringBudget budget = recurrence(YearMonth.of(2026, 1), null, 2, true);

        assertThat(budget.dueMonths(YearMonth.of(2026, 5)))
                .containsExactly(
                        YearMonth.of(2026, 3), YearMonth.of(2026, 4), YearMonth.of(2026, 5));
    }

    @Test
    void dueMonthsStopAtEndMonth() {
        RecurringBudget limited = recurrence(YearMonth.of(2026, 1), YearMonth.of(2026, 3), 0, true);

        assertThat(limited.dueMonths(YearMonth.of(2026, 12))).hasSize(3);
    }

    @Test
    void inactiveRecurrenceHasNoDueMonths() {
        RecurringBudget paused = recurrence(YearMonth.of(2026, 1), null, 0, false);

        assertThat(paused.dueMonths(YearMonth.of(2026, 12))).isEmpty();
    }

    @Test
    void futureStartMonthHasNoDueMonthsYet() {
        RecurringBudget future = recurrence(YearMonth.of(2027, 1), null, 0, true);

        assertThat(future.dueMonths(YearMonth.of(2026, 9))).isEmpty();
        assertThat(future.nextGenerationMonth()).isEqualTo(YearMonth.of(2027, 1));
    }

    @Test
    void nextGenerationMonthIsNullOnceEndMonthIsPassed() {
        RecurringBudget finished =
                recurrence(YearMonth.of(2024, 5), YearMonth.of(2025, 5), 13, true);

        assertThat(finished.nextGenerationMonth()).isNull();
    }

    /**
     * Diferente de {@link RecurringTransaction} (que compara datas), aqui a granularidade já é o
     * mês — então o mês da reativação nunca é pulado, só os meses estritamente anteriores a ele.
     */
    @Test
    void reactivatingSkipsPastMonthsButKeepsCurrentMonthDue() {
        RecurringBudget paused = recurrence(YearMonth.of(2026, 1), null, 3, false);

        RecurringBudget resumed =
                paused.withDetails(BigDecimal.valueOf(500), null, true, YearMonth.of(2026, 9));

        assertThat(resumed.active()).isTrue();
        assertThat(resumed.nextGenerationMonth()).isEqualTo(YearMonth.of(2026, 9));
        assertThat(resumed.dueMonths(YearMonth.of(2026, 9))).containsExactly(YearMonth.of(2026, 9));
    }

    /** Reativar no exato mês em que a próxima ocorrência já era esperada não deve pular nada. */
    @Test
    void reactivatingImmediatelyDoesNotSkipAnyMonth() {
        RecurringBudget paused = recurrence(YearMonth.of(2026, 1), null, 3, false);

        RecurringBudget resumed =
                paused.withDetails(BigDecimal.valueOf(500), null, true, YearMonth.of(2026, 4));

        assertThat(resumed.nextGenerationMonth()).isEqualTo(YearMonth.of(2026, 4));
        assertThat(resumed.dueMonths(YearMonth.of(2026, 4))).containsExactly(YearMonth.of(2026, 4));
    }

    @Test
    void editingAnActiveRecurrenceKeepsItsProgress() {
        RecurringBudget active = recurrence(YearMonth.of(2026, 1), null, 3, true);

        RecurringBudget edited =
                active.withDetails(BigDecimal.valueOf(700), null, true, YearMonth.of(2026, 9));

        assertThat(edited.generatedMonths()).isEqualTo(3);
        assertThat(edited.limitValue()).isEqualByComparingTo("700");
    }
}
