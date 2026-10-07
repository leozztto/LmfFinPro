package com.lmf.finpro.infrastructure.mail;

import com.lmf.finpro.domain.port.out.HouseholdInviteMailerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@RequiredArgsConstructor
public class SmtpHouseholdInviteMailer implements HouseholdInviteMailerPort {

    private final JavaMailSender mailSender;
    private final String from;

    @Override
    public void sendInvite(
            String toEmail,
            String inviterName,
            String householdName,
            String inviteLink,
            long ttlDays) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("FinPro — convite para o grupo " + householdName);
        message.setText(
                """
                Olá!

                %s convidou você para participar do grupo "%s" no FinPro e compartilhar contas,
                transações e relatórios.

                Para aceitar, acesse o link abaixo (válido por %d dias). Se ainda não tem conta,
                você poderá criar uma na mesma tela; os seus dados pessoais continuam só seus.

                %s

                Se você não conhece essa pessoa, ignore este e-mail.

                Equipe FinPro
                """
                        .formatted(inviterName, householdName, ttlDays, inviteLink));
        MailSending.send(mailSender, message, "householdInvite");
    }
}
