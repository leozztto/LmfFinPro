package com.lmf.finpro.infrastructure.scheduling;

import com.lmf.finpro.application.exchangerate.ExchangeRateApplicationService;
import com.lmf.finpro.infrastructure.config.SchedulingConfig;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Busca a PTAX do dia nos dias úteis, logo depois de o Banco Central publicar o fechamento (por
 * volta das 13h). O que faltar (servidor fora do ar, feriado) é buscado sob demanda por {@link
 * ExchangeRateApplicationService#rateOn}.
 */
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private final ExchangeRateApplicationService exchangeRateApplicationService;

    @Scheduled(
            cron = "${finpro.exchange-rates.cron:0 30 13 * * MON-FRI}",
            zone = SchedulingConfig.ZONE)
    @SchedulerLock(name = "exchangeRates", lockAtLeastFor = SchedulingConfig.LOCK_AT_LEAST_FOR)
    public void refreshRecentRates() {
        exchangeRateApplicationService.refreshRecent();
    }
}
