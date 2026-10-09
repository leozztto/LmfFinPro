package com.lmf.finpro.application.onboarding;

import com.lmf.finpro.application.FlowLog;
import com.lmf.finpro.domain.model.OnboardingProgress;
import com.lmf.finpro.domain.model.OnboardingProgress.StepId;
import com.lmf.finpro.domain.port.out.OnboardingStatePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Guia de primeiros passos: os passos que a pessoa já viu (por usuário) e as escolhas dela. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingApplicationService {

    private final OnboardingStatePort onboardingStatePort;

    public OnboardingProgress get(Long currentUserId) {
        log.debug("Buscando progresso do onboarding do usuário={}", currentUserId);
        return onboardingStatePort.progress(currentUserId);
    }

    /** Grava que o usuário viu o passo do guia; repetir não muda nada. */
    public void completeStep(Long currentUserId, StepId step) {
        FlowLog.detail("step", step);
        onboardingStatePort.recordStep(currentUserId, step);
    }

    public void dismiss(Long currentUserId) {
        log.debug("Dispensando o guia de primeiros passos do usuário={}", currentUserId);
        onboardingStatePort.dismiss(currentUserId);
    }

    public void setActivationEmailsEnabled(Long currentUserId, boolean enabled) {
        FlowLog.detail("enabled", enabled);
        onboardingStatePort.setActivationEmailsEnabled(currentUserId, enabled);
    }
}
