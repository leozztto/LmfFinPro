package com.lmf.finpro.infrastructure.mail;

import com.lmf.finpro.domain.port.out.HouseholdInviteMailerPort;
import lombok.extern.slf4j.Slf4j;

/**
 * Usado só quando não há SMTP configurado: o e-mail NÃO é enviado e nada sensível vai para o log —
 * nem o link (que dá acesso ao grupo) nem o endereço do convidado. Para testar o fluxo localmente,
 * aponte spring.mail.host para um servidor de captura (ex.: o Mailpit do docker-compose).
 */
@Slf4j
public class LoggingHouseholdInviteMailer implements HouseholdInviteMailerPort {

    @Override
    public void sendInvite(
            String toEmail,
            String inviterName,
            String householdName,
            String inviteLink,
            long ttlDays) {
        log.warn("SMTP não configurado (spring.mail.host): e-mail de convite NÃO enviado");
    }
}
