package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Dados já resolvidos e validados para exportar, em CSV, o extrato bruto de transações de um
 * usuário (em todas as contas) num mês — ao contrário dos relatórios agregados, inclui
 * transferências, já que o objetivo aqui é auditar o extrato completo, não somar receita/despesa.
 */
public record TransactionExportData(YearMonth referenceMonth, List<TransactionExportRow> rows) {

    /**
     * @param amount valor em reais
     * @param tags tags formatadas para exibição (ex.: "#site-acme #dedutível"); "" sem tags
     * @param foreignValue valor fora do real ({@link Transaction#foreignValue}); {@code null}
     *     quando tudo foi em reais
     */
    public record TransactionExportRow(
            LocalDate date,
            String accountName,
            String categoryName,
            String clientName,
            String description,
            CategoryType type,
            TransactionStatus status,
            BigDecimal amount,
            String tags,
            Money foreignValue) {

        /** Em reais. */
        public TransactionExportRow(
                LocalDate date,
                String accountName,
                String categoryName,
                String clientName,
                String description,
                CategoryType type,
                TransactionStatus status,
                BigDecimal amount,
                String tags) {
            this(
                    date,
                    accountName,
                    categoryName,
                    clientName,
                    description,
                    type,
                    status,
                    amount,
                    tags,
                    null);
        }

        /** Sem tags. */
        public TransactionExportRow(
                LocalDate date,
                String accountName,
                String categoryName,
                String clientName,
                String description,
                CategoryType type,
                TransactionStatus status,
                BigDecimal amount) {
            this(
                    date,
                    accountName,
                    categoryName,
                    clientName,
                    description,
                    type,
                    status,
                    amount,
                    "");
        }
    }
}
