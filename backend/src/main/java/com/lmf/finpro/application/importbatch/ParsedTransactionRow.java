package com.lmf.finpro.application.importbatch;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linha intermediária e agnóstica ao formato de origem (CSV ou OFX), produzida pelos parsers de
 * importação de extrato. O sinal de {@code signedAmount} decide receita/despesa; quem consome
 * ({@link ImportApplicationService}) decide o {@code CategoryType} a partir dele.
 */
public record ParsedTransactionRow(LocalDate date, String description, BigDecimal signedAmount) {}
