package com.lmf.finpro.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.OnboardingProgress.Step;
import com.lmf.finpro.domain.model.OnboardingProgress.StepId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class OnboardingProgressTest {

    private static OnboardingProgress seen(int count) {
        List<Step> steps =
                Arrays.stream(StepId.values())
                        .map(id -> new Step(id, id.ordinal() < count))
                        .toList();
        return new OnboardingProgress(steps, false, true);
    }

    @Test
    void countsOnlyTheStepsAlreadySeen() {
        assertThat(seen(0).completedCount()).isZero();
        assertThat(seen(3).completedCount()).isEqualTo(3);
    }

    @Test
    void isCompletedOnlyWhenEveryStepWasSeen() {
        assertThat(seen(0).completed()).isFalse();
        assertThat(seen(StepId.values().length - 1).completed()).isFalse();
        assertThat(seen(StepId.values().length).completed()).isTrue();
    }

    @Test
    void theGuideHasTheStepsOfEveryArea() {
        assertThat(StepId.values())
                .extracting(Enum::name)
                .contains(
                        "ACCOUNT_SAVE",
                        "TRANSACTION_SAVE",
                        "IMPORT_RESULT",
                        "BUDGET_SAVE",
                        "RECURRING_SAVE",
                        "CALENDAR_VIEW",
                        "DASHBOARD_VIEW",
                        "REPORTS_VIEW");
    }
}
