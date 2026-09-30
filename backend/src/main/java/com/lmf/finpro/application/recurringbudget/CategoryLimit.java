package com.lmf.finpro.application.recurringbudget;

import java.math.BigDecimal;

/** Um item do lote de criação de orçamentos recorrentes: categoria + limite mensal. */
public record CategoryLimit(Long categoryId, BigDecimal limitValue) {}
