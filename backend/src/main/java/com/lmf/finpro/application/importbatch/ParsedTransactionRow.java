package com.lmf.finpro.application.importbatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Linha intermediária e agnóstica ao formato de origem (CSV ou OFX), produzida pelos parsers de
 * importação de extrato. O sinal de {@code signedAmount} decide receita/despesa; quem consome
 * ({@link ImportApplicationService}) decide o {@code CategoryType} a partir dele. {@code time} é
 * {@code null} quando o arquivo não traz hora (CSV sem a coluna opcional, ou OFX com {@code
 * DTPOSTED} só com data).
 */
public record ParsedTransactionRow(
        LocalDate date, LocalTime time, String description, BigDecimal signedAmount) {}
