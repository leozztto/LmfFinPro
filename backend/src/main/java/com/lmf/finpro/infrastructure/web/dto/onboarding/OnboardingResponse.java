package com.lmf.finpro.infrastructure.web.dto.onboarding;

import com.lmf.finpro.domain.model.OnboardingProgress;
import java.util.List;

public record OnboardingResponse(
        List<StepResponse> steps,
        int completedCount,
        int totalCount,
        boolean completed,
        boolean dismissed,
        boolean activationEmailsEnabled) {

    public record StepResponse(String id, boolean done) {}

    public static OnboardingResponse from(OnboardingProgress progress) {
        return new OnboardingResponse(
                progress.steps().stream()
                        .map(step -> new StepResponse(step.id().name(), step.done()))
                        .toList(),
                (int) progress.completedCount(),
                progress.steps().size(),
                progress.completed(),
                progress.dismissed(),
                progress.activationEmailsEnabled());
    }
}
