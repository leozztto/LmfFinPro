package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Filtros de busca de transações. Só {@code userId} é obrigatório: todo filtro nulo é simplesmente
 * ignorado — não vira condição na consulta.
 *
 * @param startDate data inicial, inclusiva
 * @param endDate data final, inclusiva
 * @param description trecho da descrição (sem diferenciar maiúsculas/minúsculas)
 * @param excludeTransfers deixa de fora as transações geradas por transferências entre contas
 *     próprias
 */
public record TransactionSearchCriteria(
        Long userId,
        CategoryType type,
        LocalDate startDate,
        LocalDate endDate,
        Long accountId,
        AccountScope accountScope,
        Long categoryId,
        Long clientId,
        TransactionStatus status,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String description,
        boolean excludeTransfers) {

    public TransactionSearchCriteria {
        if (userId == null) {
            throw new IllegalArgumentException("usuário é obrigatório na busca de transações");
        }
        description = description == null || description.isBlank() ? null : description.trim();
    }
}
