package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Dados já resolvidos do relatório de receitas ou de despesas com filtros: as transações
 * encontradas (sem transferências), os totais e o subtotal por categoria.
 *
 * @param periodLabel período descrito para o cabeçalho (ex.: "01/09/2026 a 30/09/2026")
 * @param appliedFilters descrição de cada filtro informado, para constar no documento
 */
public record TransactionReportData(
        CategoryType type,
        String periodLabel,
        List<String> appliedFilters,
        List<Row> rows,
        BigDecimal total,
        BigDecimal paidTotal,
        BigDecimal pendingTotal,
        List<CategoryTotal> categoryTotals) {

    /**
     * @param amount valor em reais
     * @param tags tags da transação já formatadas para exibição (ex.: "#site-acme #dedutível"); ""
     *     quando não há
     * @param foreignValue valor fora do real ({@link Transaction#foreignValue}); {@code null}
     *     quando tudo foi em reais
     */
    public record Row(
            LocalDate date,
            String description,
            String accountName,
            String categoryName,
            String clientName,
            TransactionStatus status,
            BigDecimal amount,
            String tags,
            Money foreignValue) {

        /** Em reais. */
        public Row(
                LocalDate date,
                String description,
                String accountName,
                String categoryName,
                String clientName,
                TransactionStatus status,
                BigDecimal amount,
                String tags) {
            this(
                    date,
                    description,
                    accountName,
                    categoryName,
                    clientName,
                    status,
                    amount,
                    tags,
                    null);
        }

        /** Sem tags. */
        public Row(
                LocalDate date,
                String description,
                String accountName,
                String categoryName,
                String clientName,
                TransactionStatus status,
                BigDecimal amount) {
            this(date, description, accountName, categoryName, clientName, status, amount, "");
        }
    }

    public record CategoryTotal(String categoryName, int count, BigDecimal total) {}
}
