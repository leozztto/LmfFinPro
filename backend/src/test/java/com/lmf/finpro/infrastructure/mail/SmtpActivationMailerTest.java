package com.lmf.finpro.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.lmf.finpro.domain.model.ActivationEmailKind;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpActivationMailerTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final SmtpActivationMailer mailer =
            new SmtpActivationMailer(sender, "no-reply@finpro", "https://app");

    @Test
    void sendsToThePersonFromTheConfiguredSender() {
        mailer.send("ana@example.com", "Ana", ActivationEmailKind.WELCOME);

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(sent.capture());
        assertThat(sent.getValue().getTo()).containsExactly("ana@example.com");
        assertThat(sent.getValue().getFrom()).isEqualTo("no-reply@finpro");
        assertThat(sent.getValue().getSubject())
                .isEqualTo(SmtpActivationMailer.subject(ActivationEmailKind.WELCOME));
    }

    @Test
    void everyKindHasItsOwnSubject() {
        assertThat(
                        Arrays.stream(ActivationEmailKind.values())
                                .map(SmtpActivationMailer::subject)
                                .distinct())
                .hasSize(ActivationEmailKind.values().length);
    }

    @Test
    void everyEmailGreetsLinksTheGuideAndTheOptOut() {
        for (ActivationEmailKind kind : ActivationEmailKind.values()) {
            assertThat(mailer.buildText("Ana", kind))
                    .as(kind.name())
                    .contains("Olá, Ana!")
                    .contains("https://app/primeiros-passos")
                    .contains("https://app/configuracoes/notificacoes")
                    .doesNotContain("%s");
        }
    }

    @Test
    void welcomeMentionsTheWholeGuide() {
        assertThat(mailer.buildText("Ana", ActivationEmailKind.WELCOME))
                .contains("recorrências")
                .contains("calendário")
                .contains("relatórios");
    }

    @Test
    void lastReminderOffersSupport() {
        assertThat(mailer.buildText("Ana", ActivationEmailKind.WEEK_ONE_CHECK_IN))
                .contains("https://app/suporte");
    }

    @Test
    void aSendFailureReachesTheCaller() {
        doThrow(new MailSendException("smtp fora")).when(sender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> mailer.send("ana@example.com", "Ana", ActivationEmailKind.WELCOME))
                .isInstanceOf(MailSendException.class);
    }
}
