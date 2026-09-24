package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Dados já resolvidos e validados para montar o relatório de orçamento vs. realizado de um usuário
 * num mês: para cada orçamento cadastrado no período, o limite definido, o total já gasto na
 * categoria e a diferença entre os dois, mais os totais gerais.
 */
public record BudgetVsActualReportData(
        User issuer,
        YearMonth referenceMonth,
        List<BudgetComparison> comparisons,
        BigDecimal totalLimit,
        BigDecimal totalSpent) {

    public record BudgetComparison(
            String categoryName,
            BigDecimal limitValue,
            BigDecimal spentValue,
            BigDecimal difference,
            boolean exceeded) {}
}
