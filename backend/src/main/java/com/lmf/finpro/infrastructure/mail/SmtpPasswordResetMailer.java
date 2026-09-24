package com.lmf.finpro.infrastructure.mail;

import com.lmf.finpro.domain.port.out.PasswordResetMailerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@RequiredArgsConstructor
public class SmtpPasswordResetMailer implements PasswordResetMailerPort {

    private final JavaMailSender mailSender;
    private final String from;

    @Override
    public void sendResetLink(String toEmail, String userName, String resetLink, long ttlMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("FinPro — redefinição de senha");
        message.setText(
                """
                Olá, %s!

                Recebemos um pedido para redefinir a senha da sua conta no FinPro.
                Para criar uma nova senha, acesse o link abaixo (válido por %d minutos):

                %s

                Se você não fez esse pedido, pode ignorar este e-mail: sua senha continua a mesma.

                Equipe FinPro
                """
                        .formatted(userName, ttlMinutes, resetLink));
        mailSender.send(message);
    }
}
