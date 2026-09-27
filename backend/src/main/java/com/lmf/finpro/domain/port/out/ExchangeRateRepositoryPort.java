package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.ExchangeRate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepositoryPort {
    /** Grava as cotações, substituindo as que já existirem para a mesma moeda e dia. */
    void saveAll(List<ExchangeRate> rates);

    /** Última cotação com data até {@code date}. */
    Optional<ExchangeRate> findLatestOnOrBefore(Currency currency, LocalDate date);

    /** Cotações com data entre {@code from} e {@code to}, inclusive, em ordem de data. */
    List<ExchangeRate> findAllBetween(Currency currency, LocalDate from, LocalDate to);
}
