package com.lmf.finpro.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Notificação push: título e texto curtos e a rota do app que abre ao tocar.
 *
 * @param url caminho relativo dentro do app (ex.: {@code /})
 */
public record PushMessage(String title, String body, String url) {

    private static final String TITLE = "FinPro";

    /** Resume o resumo diário numa frase curta, só com o que há de novo. */
    public static PushMessage fromDigest(AlertDigest digest) {
        List<String> parts = new ArrayList<>();
        int bills = digest.bills().size();
        if (bills > 0) {
            parts.add(bills == 1 ? "1 conta a vencer" : bills + " contas a vencer");
        }
        int budgets = digest.budgets().size();
        if (budgets > 0) {
            parts.add(budgets == 1 ? "1 orçamento no limite" : budgets + " orçamentos no limite");
        }
        int expiring = digest.recurringBudgetsExpiring().size();
        if (expiring > 0) {
            parts.add(
                    expiring == 1
                            ? "1 orçamento recorrente terminando"
                            : expiring + " orçamentos recorrentes terminando");
        }
        if (digest.das() != null) {
            parts.add("DAS perto do vencimento");
        }
        return new PushMessage(TITLE, String.join(" · ", parts), "/");
    }
}
