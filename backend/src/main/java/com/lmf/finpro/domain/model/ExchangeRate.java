package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cotação de fechamento (PTAX de venda do Banco Central) de uma moeda em reais num dia útil. Vale
 * também para os dias seguintes sem cotação (fim de semana, feriado) até a próxima.
 */
public record ExchangeRate(Currency currency, LocalDate rateDate, BigDecimal rate) {}
