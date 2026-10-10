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
    private static final String HOME_URL = "/";

    /** Lista de transações já filtrada nas despesas pendentes que passaram do vencimento. */
    static final String OVERDUE_BILLS_URL = "/transacoes?atrasadas=true";

    /** Resume o resumo diário numa frase curta, só com o que há de novo. */
    public static PushMessage fromDigest(AlertDigest digest) {
        List<String> parts = new ArrayList<>();
        int overdue = digest.overdueBills().size();
        if (overdue > 0) {
            parts.add(overdue == 1 ? "1 conta atrasada" : overdue + " contas atrasadas");
        }
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
        int insights = digest.insights().size();
        if (insights > 0) {
            parts.add(insights == 1 ? "1 insight novo" : insights + " insights novos");
        }
        if (digest.das() != null) {
            parts.add("DAS perto do vencimento");
        }
        // Com contas atrasadas, o toque leva direto a elas (o mais urgente); senão, à tela inicial.
        return new PushMessage(
                TITLE, String.join(" · ", parts), overdue > 0 ? OVERDUE_BILLS_URL : HOME_URL);
    }
}
