package com.lmf.finpro.application.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.model.OnboardingProgress;
import com.lmf.finpro.domain.model.OnboardingProgress.StepId;
import com.lmf.finpro.domain.port.out.OnboardingStatePort;
import java.util.List;
import org.junit.jupiter.api.Test;

class OnboardingApplicationServiceTest {

    private final OnboardingStatePort state = mock(OnboardingStatePort.class);
    private final OnboardingApplicationService service = new OnboardingApplicationService(state);

    @Test
    void returnsTheProgressOfTheUser() {
        OnboardingProgress progress = new OnboardingProgress(List.of(), false, true);
        when(state.progress(7L)).thenReturn(progress);

        assertThat(service.get(7L)).isSameAs(progress);
    }

    @Test
    void recordsTheStepSeenForThatUser() {
        service.completeStep(7L, StepId.CALENDAR_VIEW);

        verify(state).recordStep(7L, StepId.CALENDAR_VIEW);
    }

    @Test
    void dismissesTheGuideForThatUser() {
        service.dismiss(7L);

        verify(state).dismiss(7L);
    }

    @Test
    void savesTheActivationEmailChoice() {
        service.setActivationEmailsEnabled(7L, false);

        verify(state).setActivationEmailsEnabled(7L, false);
    }
}
