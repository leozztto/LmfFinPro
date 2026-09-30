package com.lmf.finpro.domain.model;

/**
 * Dados já resolvidos para o relatório de evolução patrimonial: o patrimônio líquido atual, mês a
 * mês nos últimos {@code months} meses, e a composição de hoje (contas, investimentos e dívidas) —
 * o mesmo cálculo da tela de Patrimônio.
 */
public record NetWorthReportData(User issuer, int months, NetWorthCalculator.Report report) {}
