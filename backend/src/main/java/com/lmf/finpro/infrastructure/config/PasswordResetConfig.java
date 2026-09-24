package com.lmf.finpro.infrastructure.config;

import com.lmf.finpro.application.auth.PasswordResetSettings;
import com.lmf.finpro.domain.port.out.PasswordResetMailerPort;
import com.lmf.finpro.infrastructure.mail.LoggingPasswordResetMailer;
import com.lmf.finpro.infrastructure.mail.SmtpPasswordResetMailer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Com SMTP configurado (spring.mail.host — no docker-compose aponta para o Mailpit) o link vai por
 * e-mail; sem SMTP (ex: rodando local pela IDE ou nos testes), o link só é registrado no log.
 */
@Configuration
public class PasswordResetConfig {

    @Bean
    PasswordResetSettings passwordResetSettings(PasswordResetProperties properties) {
        String baseUrl = properties.frontendUrl().replaceAll("/+$", "");
        return new PasswordResetSettings(
                baseUrl + "/redefinir-senha", properties.tokenTtlMinutes());
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.mail", name = "host")
    PasswordResetMailerPort smtpPasswordResetMailer(
            JavaMailSender mailSender, PasswordResetProperties properties) {
        return new SmtpPasswordResetMailer(mailSender, properties.mailFrom());
    }

    @Bean
    @ConditionalOnMissingBean(PasswordResetMailerPort.class)
    PasswordResetMailerPort loggingPasswordResetMailer() {
        return new LoggingPasswordResetMailer();
    }
}
