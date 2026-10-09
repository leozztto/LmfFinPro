package com.lmf.finpro.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.lmf.finpro.domain.port.out.ActivationMailerPort;
import com.lmf.finpro.infrastructure.mail.LoggingActivationMailer;
import com.lmf.finpro.infrastructure.mail.SmtpActivationMailer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;

class ActivationConfigTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(ActivationConfig.class)
                    .withBean(
                            AlertProperties.class,
                            () -> new AlertProperties("https://app/", "no-reply@finpro"))
                    .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class));

    @Test
    void sendsByEmailWhenSmtpIsConfigured() {
        runner.withPropertyValues("spring.mail.host=smtp.example.com")
                .run(
                        context ->
                                assertThat(context.getBean(ActivationMailerPort.class))
                                        .isInstanceOf(SmtpActivationMailer.class));
    }

    @Test
    void onlyLogsWhenThereIsNoSmtp() {
        runner.run(
                context ->
                        assertThat(context.getBean(ActivationMailerPort.class))
                                .isInstanceOf(LoggingActivationMailer.class));
    }
}
