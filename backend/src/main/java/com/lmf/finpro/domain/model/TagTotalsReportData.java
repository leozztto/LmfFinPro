package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Relatório "Totais por tag": receita, despesa e resultado de cada tag no período. Uma transação
 * com mais de uma tag entra no total de cada uma delas — por isso as linhas não somam o total geral
 * e o relatório não tem linha de total.
 *
 * @param rows uma linha por tag, em ordem alfabética
 * @param untagged totais das transações sem nenhuma tag; {@code null} quando o relatório foi
 *     filtrado por tags (aí as sem tag nem entram na busca)
 */
public record TagTotalsReportData(
        String periodLabel, List<String> appliedFilters, List<Row> rows, Row untagged) {

    public record Row(
            String label, int count, BigDecimal income, BigDecimal expense, BigDecimal result) {}
}
