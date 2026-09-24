package com.lmf.finpro.infrastructure.mail;

import com.lmf.finpro.domain.port.out.PasswordResetMailerPort;
import lombok.extern.slf4j.Slf4j;

/**
 * Usado só quando não há SMTP configurado (desenvolvimento local e testes): registra o link no log
 * para permitir testar o fluxo sem servidor de e-mail. Nunca deve ser o adapter de produção, já que
 * o link dá acesso à troca de senha.
 */
@Slf4j
public class LoggingPasswordResetMailer implements PasswordResetMailerPort {

    @Override
    public void sendResetLink(String toEmail, String userName, String resetLink, long ttlMinutes) {
        log.warn(
                "SMTP não configurado (spring.mail.host) — link de redefinição de senha para {}: {}",
                toEmail,
                resetLink);
    }
}
