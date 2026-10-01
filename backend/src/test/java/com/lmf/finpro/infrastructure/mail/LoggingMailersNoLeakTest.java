package com.lmf.finpro.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AlertDigest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Sem SMTP o e-mail não sai, e nem o endereço nem o link de redefinição podem ir para o log. */
@ExtendWith(OutputCaptureExtension.class)
class LoggingMailersNoLeakTest {

    @Test
    void passwordResetFallbackLogsNeitherEmailNorLink(CapturedOutput output) {
        new LoggingPasswordResetMailer()
                .sendResetLink(
                        "maria@example.com",
                        "Maria",
                        "https://app/redefinir-senha?token=SEGREDO123",
                        30);

        assertThat(output.getAll())
                .contains("NÃO enviado")
                .doesNotContain("maria@example.com")
                .doesNotContain("SEGREDO123");
    }

    @Test
    void alertFallbackDoesNotLogTheEmail(CapturedOutput output) {
        new LoggingAlertMailer()
                .sendDigest(
                        "maria@example.com",
                        "Maria",
                        new AlertDigest(List.of(), List.of(), null, List.of()));

        assertThat(output.getAll()).contains("NÃO enviado").doesNotContain("maria@example.com");
    }
}
