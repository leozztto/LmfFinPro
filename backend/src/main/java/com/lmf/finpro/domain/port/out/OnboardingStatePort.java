package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.OnboardingProgress;
import com.lmf.finpro.domain.model.OnboardingProgress.StepId;

public interface OnboardingStatePort {

    /** Os passos que o usuário já concluiu, mais as escolhas gravadas dele. */
    OnboardingProgress progress(Long userId);

    /** Grava que o usuário concluiu o passo; a primeira data vale e repetir não muda nada. */
    void recordStep(Long userId, StepId step);

    void dismiss(Long userId);

    void setActivationEmailsEnabled(Long userId, boolean enabled);
}
