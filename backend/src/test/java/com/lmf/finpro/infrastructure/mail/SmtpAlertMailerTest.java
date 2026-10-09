package com.lmf.finpro.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.lmf.finpro.domain.model.AlertDigest;
import com.lmf.finpro.domain.model.AlertType;
import com.lmf.finpro.domain.model.Insight;
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
    void listaInsightsDosTresTipos() {
        AlertDigest digest =
                new AlertDigest(
                        List.of(),
                        List.of(),
                        List.of(),
                        null,
                        List.of(),
                        List.of(
                                new Insight(
                                        AlertType.INSIGHT_SUBSCRIPTION,
                                        "netflix-2026",
                                        "Netflix",
                                        new BigDecimal("55.90"),
                                        null,
                                        5),
                                new Insight(
                                        AlertType.INSIGHT_UNUSUAL_EXPENSE,
                                        "9",
                                        "Churrasco (Alimentação)",
                                        new BigDecimal("480.00"),
                                        new BigDecimal("100.00"),
                                        0),
                                new Insight(
                                        AlertType.INSIGHT_LATE_CLIENT,
                                        "7-2026-09",
                                        "Acme",
                                        new BigDecimal("1500.00"),
                                        null,
                                        2)));

        String text = mailer.buildText("Maria", digest);

        assertThat(text)
                .contains("Insights:")
                .contains("Netflix: cobrado há 5 meses seguidos")
                .contains("Churrasco (Alimentação):")
                .contains("bem acima da sua média")
                .contains("Acme atrasou 2 recebimentos");
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
