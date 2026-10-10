package com.lmf.finpro.infrastructure.mail;

import com.lmf.finpro.domain.model.AlertDigest;
import com.lmf.finpro.domain.port.out.AlertMailerPort;
import lombok.extern.slf4j.Slf4j;

/**
 * Usado só quando não há SMTP configurado: o e-mail não é enviado e não registra dados pessoais.
 */
@Slf4j
public class LoggingAlertMailer implements AlertMailerPort {

    @Override
    public void sendDigest(String toEmail, String userName, AlertDigest digest) {
        // Sem o endereço do usuário no log (dado pessoal): só o que ajuda a diagnosticar.
        log.info(
                "SMTP não configurado (spring.mail.host): resumo de alertas NÃO enviado ({} conta(s) a vencer,"
                        + " {} atrasada(s), {} orçamento(s), {} recorrência(s) expirando, {}"
                        + " insight(s), DAS: {})",
                digest.bills().size(),
                digest.overdueBills().size(),
                digest.budgets().size(),
                digest.recurringBudgetsExpiring().size(),
                digest.insights().size(),
                digest.das() != null);
    }
}
