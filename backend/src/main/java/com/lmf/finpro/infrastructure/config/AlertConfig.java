package com.lmf.finpro.infrastructure.config;

import com.lmf.finpro.domain.port.out.AlertMailerPort;
import com.lmf.finpro.infrastructure.mail.LoggingAlertMailer;
import com.lmf.finpro.infrastructure.mail.SmtpAlertMailer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Mesma regra do {@link PasswordResetConfig}: com SMTP vai por e-mail, sem SMTP só vai para o log.
 */
@Configuration
public class AlertConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.mail", name = "host")
    AlertMailerPort smtpAlertMailer(JavaMailSender mailSender, AlertProperties properties) {
        return new SmtpAlertMailer(
                mailSender, properties.mailFrom(), properties.frontendUrl().replaceAll("/+$", ""));
    }

    @Bean
    @ConditionalOnMissingBean(AlertMailerPort.class)
    AlertMailerPort loggingAlertMailer() {
        return new LoggingAlertMailer();
    }
}
