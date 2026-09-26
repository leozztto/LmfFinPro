package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class DasScheduleTest {

    @Test
    void dueDateIsTheTwentiethOfTheFollowingMonth() {
        assertThat(DasSchedule.dueDateFor(YearMonth.of(2026, 12)))
                .isEqualTo(LocalDate.of(2027, 1, 20));
    }

    @Test
    void appliesOnlyToMeiAndSimplesNacional() {
        assertThat(DasSchedule.appliesTo(TaxRegime.MEI)).isTrue();
        assertThat(DasSchedule.appliesTo(TaxRegime.SIMPLES_NACIONAL)).isTrue();
        assertThat(DasSchedule.appliesTo(TaxRegime.AUTONOMO)).isFalse();
        assertThat(DasSchedule.appliesTo(TaxRegime.LUCRO_PRESUMIDO)).isFalse();
        assertThat(DasSchedule.appliesTo(null)).isFalse();
    }

    @Test
    void competenceDueWithinFindsTheDasInsideTheWindow() {
        assertThat(DasSchedule.competenceDueWithin(LocalDate.of(2026, 9, 17), 3))
                .contains(YearMonth.of(2026, 8));
        assertThat(DasSchedule.competenceDueWithin(LocalDate.of(2026, 9, 20), 0))
                .contains(YearMonth.of(2026, 8));
    }

    @Test
    void competenceDueWithinCrossesIntoNextMonth() {
        assertThat(DasSchedule.competenceDueWithin(LocalDate.of(2026, 12, 21), 30))
                .contains(YearMonth.of(2026, 12));
    }

    @Test
    void competenceDueWithinIsEmptyOutsideTheWindow() {
        assertThat(DasSchedule.competenceDueWithin(LocalDate.of(2026, 9, 16), 3)).isEmpty();
        assertThat(DasSchedule.competenceDueWithin(LocalDate.of(2026, 9, 21), 3)).isEmpty();
    }
}
