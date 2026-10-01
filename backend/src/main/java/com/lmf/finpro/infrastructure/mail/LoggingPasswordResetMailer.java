package com.lmf.finpro.infrastructure.mail;

import com.lmf.finpro.domain.port.out.PasswordResetMailerPort;
import lombok.extern.slf4j.Slf4j;

/**
 * Usado só quando não há SMTP configurado: o e-mail NÃO é enviado e nada sensível vai para o log —
 * nem o link (que dá acesso à troca de senha) nem o endereço do usuário. Para testar o fluxo
 * localmente, aponte spring.mail.host para um servidor de captura (ex.: o Mailpit do
 * docker-compose).
 */
@Slf4j
public class LoggingPasswordResetMailer implements PasswordResetMailerPort {

    @Override
    public void sendResetLink(String toEmail, String userName, String resetLink, long ttlMinutes) {
        log.warn(
                "SMTP não configurado (spring.mail.host): e-mail de redefinição de senha NÃO enviado");
    }
}
