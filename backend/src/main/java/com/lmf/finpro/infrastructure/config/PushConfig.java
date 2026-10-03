package com.lmf.finpro.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmf.finpro.domain.port.out.PushSenderPort;
import com.lmf.finpro.infrastructure.push.NoopPushSender;
import com.lmf.finpro.infrastructure.push.WebPushSender;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Com chaves VAPID o push é enviado de verdade; sem elas fica desligado (mesma ideia do SMTP). */
@Slf4j
@Configuration
public class PushConfig {

    @Bean
    PushSenderPort pushSender(
            PushProperties properties, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        if (!properties.configured()) {
            log.info("Push desligado: chaves VAPID (finpro.push.*) não configuradas");
            return new NoopPushSender();
        }
        log.info("Push ligado com chaves VAPID");
        return new WebPushSender(
                properties.vapidPublicKey(),
                properties.vapidPrivateKey(),
                properties.subject(),
                objectMapper,
                meterRegistry);
    }
}
