package com.lmf.finpro.application.onboarding;

import com.lmf.finpro.domain.model.ActivationCandidate;
import com.lmf.finpro.domain.model.ActivationEmailKind;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Quando cada e-mail de ativação vale. Cada um tem uma janela: fora dela não sai, para que um
 * atraso ou uma implantação não dispare avisos velhos de uma vez. Os lembretes só saem em horário
 * comercial e param assim que a pessoa lança o primeiro movimento ou dispensa o guia.
 */
final class ActivationEmailPolicy {

    /** Quanto tempo depois do cadastro ainda faz sentido dar boas-vindas. */
    static final Duration WELCOME_WINDOW = Duration.ofDays(1);

    static final Duration FIRST_REMINDER_AFTER = Duration.ofDays(1);
    static final Duration FIRST_REMINDER_UNTIL = Duration.ofDays(3);
    static final Duration CHECK_IN_AFTER = Duration.ofDays(7);
    static final Duration CHECK_IN_UNTIL = Duration.ofDays(10);

    static final int FIRST_HOUR = 8;
    static final int LAST_HOUR = 20;

    private ActivationEmailPolicy() {}

    /** O próximo e-mail devido a este usuário agora, se houver (no máximo um por execução). */
    static Optional<ActivationEmailKind> nextDue(ActivationCandidate candidate, LocalDateTime now) {
        Duration age = Duration.between(candidate.createdAt(), now);
        if (!candidate.sentKinds().contains(ActivationEmailKind.WELCOME)
                && age.compareTo(WELCOME_WINDOW) < 0) {
            return Optional.of(ActivationEmailKind.WELCOME);
        }
        if (candidate.hasTransactions()
                || candidate.guideDismissed()
                || !withinBusinessHours(now)) {
            return Optional.empty();
        }
        if (isDue(
                candidate,
                ActivationEmailKind.FIRST_IMPORT_REMINDER,
                age,
                FIRST_REMINDER_AFTER,
                FIRST_REMINDER_UNTIL)) {
            return Optional.of(ActivationEmailKind.FIRST_IMPORT_REMINDER);
        }
        if (isDue(
                candidate,
                ActivationEmailKind.WEEK_ONE_CHECK_IN,
                age,
                CHECK_IN_AFTER,
                CHECK_IN_UNTIL)) {
            return Optional.of(ActivationEmailKind.WEEK_ONE_CHECK_IN);
        }
        return Optional.empty();
    }

    private static boolean isDue(
            ActivationCandidate candidate,
            ActivationEmailKind kind,
            Duration age,
            Duration after,
            Duration until) {
        return !candidate.sentKinds().contains(kind)
                && age.compareTo(after) >= 0
                && age.compareTo(until) < 0;
    }

    private static boolean withinBusinessHours(LocalDateTime now) {
        return now.getHour() >= FIRST_HOUR && now.getHour() < LAST_HOUR;
    }
}
