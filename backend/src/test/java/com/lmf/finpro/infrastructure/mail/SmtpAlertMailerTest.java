package com.lmf.finpro.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AlertDigest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SmtpAlertMailerTest {

    private final SmtpAlertMailer mailer =
            new SmtpAlertMailer(null, "no-reply@finpro", "https://app");

    @Test
    void listaContasAtrasadasAntesDasAVencer() {
        AlertDigest digest =
                new AlertDigest(
                        List.of(
                                new AlertDigest.BillDue(
                                        "Internet",
                                        new BigDecimal("99.90"),
                                        LocalDate.of(2026, 9, 20))),
                        List.of(
                                new AlertDigest.OverdueBill(
                                        "Aluguel",
                                        new BigDecimal("1500.00"),
                                        LocalDate.of(2026, 9, 17),
                                        1),
                                new AlertDigest.OverdueBill(
                                        "Luz",
                                        new BigDecimal("200.00"),
                                        LocalDate.of(2026, 9, 10),
                                        8)),
                        List.of(),
                        null,
                        List.of());

        String text = mailer.buildText("Maria", digest);

        assertThat(text)
                .contains("Contas atrasadas:")
                .contains("Aluguel")
                .contains("venceu em 17/09/2026 (1 dia de atraso)")
                .contains("venceu em 10/09/2026 (8 dias de atraso)")
                .contains("Contas a vencer:");
        assertThat(text.indexOf("Contas atrasadas:")).isLessThan(text.indexOf("Contas a vencer:"));
    }

    @Test
    void semAtrasadasNaoMostraASecao() {
        AlertDigest digest =
                new AlertDigest(
                        List.of(
                                new AlertDigest.BillDue(
                                        "Internet", BigDecimal.TEN, LocalDate.of(2026, 9, 20))),
                        List.of(),
                        null,
                        List.of());

        assertThat(mailer.buildText("Maria", digest)).doesNotContain("atrasadas");
    }
}
