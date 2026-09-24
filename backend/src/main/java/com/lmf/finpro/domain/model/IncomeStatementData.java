package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

/**
 * Dados já resolvidos e validados para montar o resultado do período (DRE simplificado) de um
 * usuário num ano: receita, despesa e resultado (receita - despesa) consolidados por período — mês,
 * trimestre ou o ano inteiro, de acordo com {@code granularity} — mais os totais gerais.
 */
public record IncomeStatementData(
        User issuer,
        Year referenceYear,
        ReportGranularity granularity,
        List<PeriodResult> periods,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal totalResult) {

    public record PeriodResult(
            String label, BigDecimal income, BigDecimal expense, BigDecimal result) {}
}
