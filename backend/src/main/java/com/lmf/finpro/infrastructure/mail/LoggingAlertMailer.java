package com.lmf.finpro.infrastructure.mail;

import com.lmf.finpro.domain.model.AlertDigest;
import com.lmf.finpro.domain.port.out.AlertMailerPort;
import lombok.extern.slf4j.Slf4j;

/** Usado só quando não há SMTP configurado (desenvolvimento local e testes). */
@Slf4j
public class LoggingAlertMailer implements AlertMailerPort {

    @Override
    public void sendDigest(String toEmail, String userName, AlertDigest digest) {
        log.info(
                "SMTP não configurado (spring.mail.host) — resumo de alertas para {}: {} conta(s), {}"
                        + " orçamento(s), DAS: {}",
                toEmail,
                digest.bills().size(),
                digest.budgets().size(),
                digest.das() != null);
    }
}
