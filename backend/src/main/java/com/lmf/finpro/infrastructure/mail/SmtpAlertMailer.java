package com.lmf.finpro.infrastructure.mail;

import com.lmf.finpro.domain.model.AlertDigest;
import com.lmf.finpro.domain.model.AlertDigest.BillDue;
import com.lmf.finpro.domain.model.AlertDigest.BudgetAlert;
import com.lmf.finpro.domain.model.AlertDigest.DasReminder;
import com.lmf.finpro.domain.port.out.AlertMailerPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@RequiredArgsConstructor
public class SmtpAlertMailer implements AlertMailerPort {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter COMPETENCE = DateTimeFormatter.ofPattern("MM/yyyy");

    private final JavaMailSender mailSender;
    private final String from;
    private final String appUrl;

    @Override
    public void sendDigest(String toEmail, String userName, AlertDigest digest) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("FinPro — seus alertas de hoje");
        message.setText(buildText(userName, digest));
        mailSender.send(message);
    }

    String buildText(String userName, AlertDigest digest) {
        StringBuilder text = new StringBuilder("Olá, %s!%n%n".formatted(userName));

        if (!digest.bills().isEmpty()) {
            text.append("Contas a vencer:\n");
            for (BillDue bill : digest.bills()) {
                text.append(
                        "  • %s — %s, vence em %s%n"
                                .formatted(
                                        bill.description(),
                                        money(bill.amount()),
                                        bill.dueDate().format(DATE)));
            }
            text.append('\n');
        }

        if (!digest.budgets().isEmpty()) {
            text.append("Orçamentos do mês:\n");
            for (BudgetAlert budget : digest.budgets()) {
                text.append(
                        "  • %s: %s de %s (%s%%) — %s%n"
                                .formatted(
                                        budget.categoryName(),
                                        money(budget.spent()),
                                        money(budget.limitValue()),
                                        percent(budget.spent(), budget.limitValue()),
                                        budget.threshold() >= 100
                                                ? "limite ultrapassado"
                                                : "passou de 80% do limite"));
            }
            text.append('\n');
        }

        DasReminder das = digest.das();
        if (das != null) {
            text.append(
                    "DAS da competência %s vence em %s"
                            .formatted(
                                    das.competence().format(COMPETENCE),
                                    das.dueDate().format(DATE)));
            if (das.estimatedValue() != null) {
                text.append(" — valor estimado: ").append(money(das.estimatedValue()));
            }
            text.append(".\n\n");
        }

        text.append(
                """
                Acesse o FinPro para ver os detalhes: %s

                Você pode escolher quais alertas receber em Configurações > Notificações.

                Equipe FinPro
                """
                        .formatted(appUrl));
        return text.toString();
    }

    private static String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(value);
    }

    private static String percent(BigDecimal spent, BigDecimal limit) {
        return spent.multiply(BigDecimal.valueOf(100))
                .divide(limit, 0, RoundingMode.HALF_UP)
                .toPlainString();
    }
}
