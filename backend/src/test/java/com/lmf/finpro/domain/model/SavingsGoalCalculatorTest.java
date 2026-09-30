package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SavingsGoalCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);

    @Test
    void savedAmountAddsDepositsAndSubtractsWithdrawals() {
        List<GoalContribution> contributions =
                List.of(
                        contribution(ContributionType.DEPOSIT, "500"),
                        contribution(ContributionType.DEPOSIT, "300"),
                        contribution(ContributionType.WITHDRAWAL, "200"));

        assertThat(SavingsGoalCalculator.savedAmount(contributions)).isEqualByComparingTo("600");
    }

    @Test
    void remainingNeverGoesNegative() {
        SavingsGoal goal = goal("1000", null, null);

        assertThat(SavingsGoalCalculator.remaining(goal, new BigDecimal("400")))
                .isEqualByComparingTo("600");
        assertThat(SavingsGoalCalculator.remaining(goal, new BigDecimal("1500")))
                .isEqualByComparingTo("0");
    }

    @Test
    void monthlyNeededSplitsRemainingThroughTheDeadlineMonthRoundingUp() {
        // Setembro a dezembro = 4 meses; 1000 / 4 = 250.
        SavingsGoal goal = goal("1000", LocalDate.of(2026, 12, 15), null);

        assertThat(SavingsGoalCalculator.monthlyNeeded(goal, BigDecimal.ZERO, TODAY))
                .isEqualByComparingTo("250");
        assertThat(SavingsGoalCalculator.monthlyNeeded(goal, new BigDecimal("1"), TODAY))
                .isEqualByComparingTo("249.75");
        assertThat(
                        SavingsGoalCalculator.monthlyNeeded(
                                goal("1000", LocalDate.of(2026, 11, 1), null),
                                BigDecimal.ZERO,
                                TODAY))
                .isEqualByComparingTo("333.34");
    }

    @Test
    void monthlyNeededIsNullWithoutDeadlineOrWhenReached() {
        assertThat(
                        SavingsGoalCalculator.monthlyNeeded(
                                goal("1000", null, null), BigDecimal.ZERO, TODAY))
                .isNull();
        assertThat(
                        SavingsGoalCalculator.monthlyNeeded(
                                goal("1000", LocalDate.of(2026, 12, 1), null),
                                new BigDecimal("1000"),
                                TODAY))
                .isNull();
    }

    @Test
    void monthlyNeededWithPastDeadlineIsTheWholeRemaining() {
        SavingsGoal goal = goal("1000", LocalDate.of(2026, 5, 1), null);

        assertThat(SavingsGoalCalculator.monthlyNeeded(goal, new BigDecimal("300"), TODAY))
                .isEqualByComparingTo("700");
    }

    @Test
    void suggestedContributionIsRateOfIncomeMinusDepositsOfTheMonth() {
        SavingsGoal goal = goal("100000", null, "0.06");

        assertThat(
                        SavingsGoalCalculator.suggestedContribution(
                                goal, BigDecimal.ZERO, new BigDecimal("5000"), BigDecimal.ZERO))
                .isEqualByComparingTo("300");
        assertThat(
                        SavingsGoalCalculator.suggestedContribution(
                                goal,
                                BigDecimal.ZERO,
                                new BigDecimal("5000"),
                                new BigDecimal("100")))
                .isEqualByComparingTo("200");
        assertThat(
                        SavingsGoalCalculator.suggestedContribution(
                                goal,
                                BigDecimal.ZERO,
                                new BigDecimal("5000"),
                                new BigDecimal("400")))
                .isEqualByComparingTo("0");
    }

    @Test
    void suggestedContributionIsCappedAtWhatIsMissing() {
        SavingsGoal goal = goal("1000", null, "0.5");

        assertThat(
                        SavingsGoalCalculator.suggestedContribution(
                                goal,
                                new BigDecimal("900"),
                                new BigDecimal("5000"),
                                BigDecimal.ZERO))
                .isEqualByComparingTo("100");
    }

    @Test
    void noSuggestionWithoutRate() {
        assertThat(
                        SavingsGoalCalculator.suggestedContribution(
                                goal("1000", null, null),
                                BigDecimal.ZERO,
                                new BigDecimal("5000"),
                                BigDecimal.ZERO))
                .isNull();
        assertThat(
                        SavingsGoalCalculator.suggestedContribution(
                                goal("1000", null, "0"),
                                BigDecimal.ZERO,
                                new BigDecimal("5000"),
                                BigDecimal.ZERO))
                .isNull();
    }

    private static SavingsGoal goal(String target, LocalDate deadline, String rate) {
        return new SavingsGoal(
                1L,
                10L,
                "Meta",
                SavingsGoalType.OTHER,
                new BigDecimal(target),
                deadline,
                rate == null ? null : new BigDecimal(rate),
                null,
                false,
                5L,
                6L);
    }

    private static GoalContribution contribution(ContributionType type, String amount) {
        return new GoalContribution(1L, 1L, type, new BigDecimal(amount), TODAY, null, null, 9L);
    }
}
