package com.lmf.finpro.application.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lmf.finpro.domain.model.ActivationCandidate;
import com.lmf.finpro.domain.model.ActivationEmailKind;
import com.lmf.finpro.domain.port.out.ActivationCandidatePort;
import com.lmf.finpro.domain.port.out.ActivationMailerPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;

class ActivationEmailApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

    private final ActivationCandidatePort candidates = mock(ActivationCandidatePort.class);
    private final ActivationMailerPort mailer = mock(ActivationMailerPort.class);
    private final ActivationEmailApplicationService service =
            new ActivationEmailApplicationService(
                    candidates, mailer, Clock.fixed(NOW, ZoneOffset.UTC));

    private static ActivationCandidate signedUpMinutesAgo(Long id, String email) {
        return new ActivationCandidate(
                id,
                "Ana",
                email,
                NOW_LOCAL.minusMinutes(5),
                false,
                false,
                EnumSet.noneOf(ActivationEmailKind.class));
    }

    @Test
    void sendsTheDueEmailAndOnlyThenRecordsIt() {
        when(candidates.findCandidates(any()))
                .thenReturn(List.of(signedUpMinutesAgo(1L, "ana@example.com")));

        int sent = service.sendDue();

        assertThat(sent).isEqualTo(1);
        verify(mailer).send("ana@example.com", "Ana", ActivationEmailKind.WELCOME);
        verify(candidates).markSent(1L, ActivationEmailKind.WELCOME);
    }

    @Test
    void onlyLooksAtRecentSignups() {
        when(candidates.findCandidates(any())).thenReturn(List.of());

        service.sendDue();

        ArgumentCaptor<LocalDateTime> since = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(candidates).findCandidates(since.capture());
        assertThat(since.getValue())
                .isEqualTo(NOW_LOCAL.minusDays(ActivationEmailApplicationService.LOOKBACK_DAYS));
    }

    @Test
    void sendsNothingWhenNobodyIsDue() {
        ActivationCandidate welcomed =
                new ActivationCandidate(
                        1L,
                        "Ana",
                        "ana@example.com",
                        NOW_LOCAL.minusMinutes(5),
                        false,
                        false,
                        EnumSet.of(ActivationEmailKind.WELCOME));
        when(candidates.findCandidates(any())).thenReturn(List.of(welcomed));

        assertThat(service.sendDue()).isZero();

        verify(mailer, never()).send(any(), any(), any());
        verify(candidates, never()).markSent(any(), any());
    }

    @Test
    void aFailedSendIsNotRecordedAndDoesNotStopTheOthers() {
        when(candidates.findCandidates(any()))
                .thenReturn(
                        List.of(
                                signedUpMinutesAgo(1L, "falha@example.com"),
                                signedUpMinutesAgo(2L, "ok@example.com")));
        doThrow(new MailSendException("smtp fora"))
                .when(mailer)
                .send(eq("falha@example.com"), any(), any());

        int sent = service.sendDue();

        assertThat(sent).isEqualTo(1);
        verify(candidates, never()).markSent(eq(1L), any());
        verify(candidates).markSent(2L, ActivationEmailKind.WELCOME);
    }
}
