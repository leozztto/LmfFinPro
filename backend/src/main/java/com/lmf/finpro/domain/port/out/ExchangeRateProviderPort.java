package com.lmf.finpro.domain.port.out;

import com.lmf.finpro.domain.model.Currency;
import com.lmf.finpro.domain.model.ExchangeRate;
import java.time.LocalDate;
import java.util.List;

/** Fonte externa das cotações (hoje, a PTAX do Banco Central). */
public interface ExchangeRateProviderPort {
    /**
     * Cotações de fechamento publicadas entre {@code from} e {@code to}, inclusive. Dias sem
     * cotação (fim de semana, feriado) não vêm na lista.
     *
     * @throws com.lmf.finpro.domain.exception.ExchangeRateUnavailableException se a fonte não
     *     responder
     */
    List<ExchangeRate> fetch(Currency currency, LocalDate from, LocalDate to);
}
