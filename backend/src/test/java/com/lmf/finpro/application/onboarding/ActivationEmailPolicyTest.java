package com.lmf.finpro.application.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.ActivationCandidate;
import com.lmf.finpro.domain.model.ActivationEmailKind;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ActivationEmailPolicyTest {

    private static final LocalDateTime SIGNUP = LocalDateTime.of(2026, 10, 1, 9, 0);

    private static ActivationCandidate candidate(
            boolean hasTransactions, ActivationEmailKind... sent) {
        Set<ActivationEmailKind> kinds = EnumSet.noneOf(ActivationEmailKind.class);
        kinds.addAll(java.util.List.of(sent));
        return new ActivationCandidate(
                1L, "Ana", "ana@example.com", SIGNUP, hasTransactions, false, kinds);
    }

    @Test
    void welcomesRightAfterSignup() {
        assertThat(ActivationEmailPolicy.nextDue(candidate(false), SIGNUP.plusMinutes(10)))
                .contains(ActivationEmailKind.WELCOME);
    }

    @Test
    void welcomesEvenWhenAlreadyHasTransactions() {
        assertThat(ActivationEmailPolicy.nextDue(candidate(true), SIGNUP.plusMinutes(10)))
                .contains(ActivationEmailKind.WELCOME);
    }

    @Test
    void neverWelcomesTwice() {
        assertThat(
                        ActivationEmailPolicy.nextDue(
                                candidate(false, ActivationEmailKind.WELCOME), SIGNUP.plusHours(2)))
                .isEmpty();
    }

    @Test
    void doesNotSendStaleWelcomeAfterTheWindow() {
        // Quem se cadastrou antes de a funcionalidade existir não recebe boas-vindas atrasadas.
        assertThat(ActivationEmailPolicy.nextDue(candidate(false), SIGNUP.plusDays(2).withHour(9)))
                .contains(ActivationEmailKind.FIRST_IMPORT_REMINDER);
    }

    @Test
    void remindsTheNextDayWhenThereAreNoTransactions() {
        assertThat(
                        ActivationEmailPolicy.nextDue(
                                candidate(false, ActivationEmailKind.WELCOME),
                                SIGNUP.plusDays(1).plusMinutes(5)))
                .contains(ActivationEmailKind.FIRST_IMPORT_REMINDER);
    }

    @Test
    void stopsRemindingOnceTheFirstTransactionExists() {
        assertThat(
                        ActivationEmailPolicy.nextDue(
                                candidate(true, ActivationEmailKind.WELCOME),
                                SIGNUP.plusDays(1).plusMinutes(5)))
                .isEmpty();
    }

    @Test
    void remindersWaitForBusinessHours() {
        LocalDateTime night = SIGNUP.plusDays(1).withHour(23);
        assertThat(
                        ActivationEmailPolicy.nextDue(
                                candidate(false, ActivationEmailKind.WELCOME), night))
                .isEmpty();
        LocalDateTime earlyMorning = SIGNUP.plusDays(1).withHour(7);
        assertThat(
                        ActivationEmailPolicy.nextDue(
                                candidate(false, ActivationEmailKind.WELCOME), earlyMorning))
                .isEmpty();
    }

    @Test
    void checksInAfterOneWeek() {
        assertThat(
                        ActivationEmailPolicy.nextDue(
                                candidate(
                                        false,
                                        ActivationEmailKind.WELCOME,
                                        ActivationEmailKind.FIRST_IMPORT_REMINDER),
                                SIGNUP.plusDays(7).plusMinutes(1)))
                .contains(ActivationEmailKind.WEEK_ONE_CHECK_IN);
    }

    @Test
    void sendsNothingAfterTheLastWindow() {
        assertThat(
                        ActivationEmailPolicy.nextDue(
                                candidate(
                                        false,
                                        ActivationEmailKind.WELCOME,
                                        ActivationEmailKind.FIRST_IMPORT_REMINDER),
                                SIGNUP.plusDays(11)))
                .isEmpty();
    }

    @Test
    void remindersStopOnceTheGuideWasDismissedButWelcomeStillGoes() {
        var dismissed =
                new ActivationCandidate(
                        1L,
                        "Ana",
                        "ana@example.com",
                        SIGNUP,
                        false,
                        true,
                        EnumSet.of(ActivationEmailKind.WELCOME));

        assertThat(ActivationEmailPolicy.nextDue(dismissed, SIGNUP.plusDays(2).withHour(9)))
                .isEmpty();
        assertThat(ActivationEmailPolicy.nextDue(dismissed, SIGNUP.plusDays(8).withHour(9)))
                .isEmpty();
    }
}
