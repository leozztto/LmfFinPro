package com.lmf.finpro.domain.model;

import java.util.List;

/**
 * Onde a pessoa está no guia de primeiros passos. O guia só mostra como fazer (não cria dados);
 * cada passo visto fica gravado por usuário, para retomar do último ponto se ela não terminar.
 *
 * @param dismissed a pessoa dispensou o guia
 * @param activationEmailsEnabled aceita receber os e-mails de ativação
 */
public record OnboardingProgress(
        List<Step> steps, boolean dismissed, boolean activationEmailsEnabled) {

    /** Os passos do guia, na ordem em que são mostrados. */
    public enum StepId {
        ACCOUNT_OPEN,
        ACCOUNT_FILL,
        ACCOUNT_SAVE,
        TRANSACTION_FILL,
        TRANSACTION_SAVE,
        IMPORT_PICK,
        IMPORT_RESULT,
        BUDGET_FILL,
        BUDGET_SAVE,
        RECURRING_FILL,
        RECURRING_SAVE,
        CALENDAR_VIEW,
        DASHBOARD_VIEW,
        REPORTS_VIEW
    }

    public record Step(StepId id, boolean done) {}

    public long completedCount() {
        return steps.stream().filter(Step::done).count();
    }

    public boolean completed() {
        return steps.stream().allMatch(Step::done);
    }
}
