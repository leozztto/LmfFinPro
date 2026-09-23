package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.Month;
import java.time.Year;
import java.util.List;

/**
 * Dados já resolvidos e validados para montar o demonstrativo anual de receita de um cliente:
 * emissor, cliente, ano de referência, total recebido em cada um dos 12 meses (mesmo os que
 * tiverem zero) e o total do ano.
 */
public record ClientAnnualStatementData(
        User issuer,
        Client client,
        Year referenceYear,
        List<MonthlyIncome> monthlyIncomes,
        BigDecimal totalYear) {

    public record MonthlyIncome(Month month, BigDecimal total) {}
}
