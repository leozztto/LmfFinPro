package com.lmf.finpro.domain.model;

import java.math.BigDecimal;

/** Valor numa moeda. */
public record Money(Currency currency, BigDecimal amount) {}
