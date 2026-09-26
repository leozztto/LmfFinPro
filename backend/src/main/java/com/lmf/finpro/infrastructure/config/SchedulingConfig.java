package com.lmf.finpro.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    public static final String ZONE = "America/Sao_Paulo";

    /**
     * "Hoje" das regras de negócio (ex.: quais ocorrências recorrentes já venceram) no fuso do
     * usuário, e não no fuso do servidor — que em nuvem costuma ser UTC e viraria o dia 3h antes.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(ZONE));
    }
}
