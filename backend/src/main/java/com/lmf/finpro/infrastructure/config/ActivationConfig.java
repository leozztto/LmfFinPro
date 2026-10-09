package com.lmf.finpro.infrastructure.config;

import com.lmf.finpro.domain.port.out.ActivationMailerPort;
import com.lmf.finpro.infrastructure.mail.LoggingActivationMailer;
import com.lmf.finpro.infrastructure.mail.SmtpActivationMailer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/** Mesma regra do {@link AlertConfig}: com SMTP vai por e-mail, sem SMTP só vai para o log. */
@Configuration
public class ActivationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.mail", name = "host")
    ActivationMailerPort smtpActivationMailer(
            JavaMailSender mailSender, AlertProperties properties) {
        return new SmtpActivationMailer(
                mailSender, properties.mailFrom(), properties.frontendUrl().replaceAll("/+$", ""));
    }

    @Bean
    @ConditionalOnMissingBean(ActivationMailerPort.class)
    ActivationMailerPort loggingActivationMailer() {
        return new LoggingActivationMailer();
    }
}
