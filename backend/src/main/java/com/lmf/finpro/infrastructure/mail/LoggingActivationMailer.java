package com.lmf.finpro.infrastructure.mail;

import com.lmf.finpro.domain.model.ActivationEmailKind;
import com.lmf.finpro.domain.port.out.ActivationMailerPort;
import lombok.extern.slf4j.Slf4j;

/** Usado só quando não há SMTP configurado: não envia e não registra dados pessoais. */
@Slf4j
public class LoggingActivationMailer implements ActivationMailerPort {

    @Override
    public void send(String toEmail, String userName, ActivationEmailKind kind) {
        log.info(
                "SMTP não configurado (spring.mail.host): e-mail de ativação {} NÃO enviado", kind);
    }
}
