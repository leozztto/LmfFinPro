package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Dados já resolvidos e validados para montar o relatório de despesas por categoria de um usuário
 * num mês: titular, período, o total gasto em cada categoria com pelo menos uma despesa no período
 * (ordenado do maior para o menor) e o total geral.
 */
public record CategoryExpenseReportData(
        User issuer,
        YearMonth referenceMonth,
        List<CategoryExpense> categoryExpenses,
        BigDecimal totalExpense) {

    public record CategoryExpense(String categoryName, BigDecimal total) {}
}
