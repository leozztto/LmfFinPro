package com.lmf.finpro.infrastructure.mail;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.lmf.finpro.infrastructure.logging.SafeErrors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Envia um e-mail registrando tipo, duração e desfecho. O destinatário e o conteúdo nunca vão para
 * o log (e-mail é dado pessoal); a falha sai só com o tipo da exceção e é repassada para quem
 * chamou decidir o que fazer.
 */
@Slf4j
final class MailSending {

    private MailSending() {}

    static void send(JavaMailSender mailSender, SimpleMailMessage message, String kind) {
        long start = System.nanoTime();
        try {
            mailSender.send(message);
            log.info(
                    "E-mail enviado {} {}",
                    kv("kind", kind),
                    kv("durationMs", (System.nanoTime() - start) / 1_000_000));
        } catch (MailException ex) {
            log.warn(
                    "Falha ao enviar e-mail {} {} {}",
                    kv("kind", kind),
                    kv("error", SafeErrors.describe(ex)),
                    kv("durationMs", (System.nanoTime() - start) / 1_000_000));
            throw ex;
        }
    }
}
