package com.lmf.finpro.domain.model;

import com.lmf.finpro.domain.exception.ExchangeRateUnavailableException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Tabela de cotações já carregada — pura, sem acesso a repositório. Num dia sem cotação vale a
 * última anterior; antes da primeira conhecida, a primeira.
 */
public final class ExchangeRates {

    private final Map<Currency, NavigableMap<LocalDate, BigDecimal>> ratesByCurrency =
            new EnumMap<>(Currency.class);

    public ExchangeRates(List<ExchangeRate> rates) {
        for (ExchangeRate rate : rates) {
            ratesByCurrency
                    .computeIfAbsent(rate.currency(), currency -> new TreeMap<>())
                    .put(rate.rateDate(), rate.rate());
        }
    }

    /** Só reais: nenhuma conversão necessária. */
    public static ExchangeRates none() {
        return new ExchangeRates(List.of());
    }

    /** Quantos reais vale uma unidade da moeda no dia. */
    public BigDecimal rateOn(Currency currency, LocalDate date) {
        if (currency.isBase()) {
            return BigDecimal.ONE;
        }
        NavigableMap<LocalDate, BigDecimal> rates = ratesByCurrency.get(currency);
        if (rates == null || rates.isEmpty()) {
            throw new ExchangeRateUnavailableException(
                    "Cotação do " + currency + " indisponível no momento");
        }
        Map.Entry<LocalDate, BigDecimal> entry = rates.floorEntry(date);
        return entry != null ? entry.getValue() : rates.firstEntry().getValue();
    }

    /** Valor em reais, arredondado em centavos. */
    public BigDecimal toBrl(Currency currency, BigDecimal amount, LocalDate date) {
        if (currency.isBase()) {
            return amount;
        }
        return amount.multiply(rateOn(currency, date)).setScale(2, RoundingMode.HALF_UP);
    }
}
