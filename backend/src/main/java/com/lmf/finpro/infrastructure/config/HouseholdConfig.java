package com.lmf.finpro.infrastructure.config;

import com.lmf.finpro.application.household.HouseholdInviteSettings;
import com.lmf.finpro.domain.port.out.HouseholdInviteMailerPort;
import com.lmf.finpro.infrastructure.mail.LoggingHouseholdInviteMailer;
import com.lmf.finpro.infrastructure.mail.SmtpHouseholdInviteMailer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Com SMTP configurado (spring.mail.host) o convite vai por e-mail; sem SMTP (ex.: rodando local
 * pela IDE ou nos testes) só um aviso vai para o log, sem o link.
 */
@Configuration
public class HouseholdConfig {

    @Bean
    HouseholdInviteSettings householdInviteSettings(HouseholdProperties properties) {
        String baseUrl = properties.frontendUrl().replaceAll("/+$", "");
        return new HouseholdInviteSettings(baseUrl + "/convite", properties.inviteTtlDays());
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.mail", name = "host")
    HouseholdInviteMailerPort smtpHouseholdInviteMailer(
            JavaMailSender mailSender, HouseholdProperties properties) {
        return new SmtpHouseholdInviteMailer(mailSender, properties.mailFrom());
    }

    @Bean
    @ConditionalOnMissingBean(HouseholdInviteMailerPort.class)
    HouseholdInviteMailerPort loggingHouseholdInviteMailer() {
        return new LoggingHouseholdInviteMailer();
    }
}
