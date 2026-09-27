package com.lmf.finpro.infrastructure.web.dto.exchangerate;

import com.lmf.finpro.domain.model.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param rate quantas unidades de {@code to} vale uma de {@code from}
 */
public record ConversionResponse(
        Currency from, Currency to, LocalDate date, BigDecimal rate, BigDecimal amount) {}
