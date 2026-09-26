package com.lmf.finpro.domain.model;

/**
 * O que o usuário quer receber no resumo diário de alertas por e-mail.
 *
 * @param billDaysBefore com quantos dias de antecedência avisar sobre contas a vencer e sobre o DAS
 *     (0 = só no próprio dia)
 */
public record NotificationPreferences(
        Long userId,
        boolean billsEnabled,
        int billDaysBefore,
        boolean budgetsEnabled,
        boolean dasEnabled) {

    public static final int DEFAULT_BILL_DAYS_BEFORE = 3;
    public static final int MAX_BILL_DAYS_BEFORE = 15;

    /** Quem nunca mexeu nas preferências recebe todos os alertas. */
    public static NotificationPreferences defaults(Long userId) {
        return new NotificationPreferences(userId, true, DEFAULT_BILL_DAYS_BEFORE, true, true);
    }
}
