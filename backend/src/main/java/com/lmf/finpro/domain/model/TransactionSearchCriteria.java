package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Filtros de busca de transações. Só {@code householdId} é obrigatório: todo filtro nulo é
 * simplesmente ignorado — não vira condição na consulta.
 *
 * @param startDate data inicial, inclusiva
 * @param endDate data final, inclusiva
 * @param description trecho da descrição (sem diferenciar maiúsculas/minúsculas)
 * @param excludeTransfers deixa de fora as transações geradas por transferências entre contas
 *     próprias
 * @param tagIds transações com <b>qualquer uma</b> destas tags; vazia = sem filtro de tag
 * @param hasAttachment {@code true} só com comprovante, {@code false} só sem; nulo = sem filtro
 * @param includeLinkedTransferLegsOfUserId quando informado, a busca inclui também as pernas das
 *     transferências feitas com contas do espaço de quem consulta que estão em outro espaço do qual
 *     este usuário também participa (ex.: a entrada na conta conjunta, vista a partir do espaço
 *     pessoal). Só para a listagem: relatórios e totais não usam, para não contar a perna duas
 *     vezes.
 */
public record TransactionSearchCriteria(
        Long householdId,
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
        boolean excludeTransfers,
        List<Long> tagIds,
        Boolean hasAttachment,
        Long includeLinkedTransferLegsOfUserId) {

    public TransactionSearchCriteria {
        if (householdId == null) {
            throw new IllegalArgumentException("usuário é obrigatório na busca de transações");
        }
        description = description == null || description.isBlank() ? null : description.trim();
        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
    }

    /** Sem as pernas de transferências de outros espaços. */
    public TransactionSearchCriteria(
            Long householdId,
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
            boolean excludeTransfers,
            List<Long> tagIds,
            Boolean hasAttachment) {
        this(
                householdId,
                type,
                startDate,
                endDate,
                accountId,
                accountScope,
                categoryId,
                clientId,
                status,
                minAmount,
                maxAmount,
                description,
                excludeTransfers,
                tagIds,
                hasAttachment,
                null);
    }

    /** Sem filtro de comprovante. */
    public TransactionSearchCriteria(
            Long householdId,
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
            boolean excludeTransfers,
            List<Long> tagIds) {
        this(
                householdId,
                type,
                startDate,
                endDate,
                accountId,
                accountScope,
                categoryId,
                clientId,
                status,
                minAmount,
                maxAmount,
                description,
                excludeTransfers,
                tagIds,
                null);
    }

    /** Sem filtro de tag. */
    public TransactionSearchCriteria(
            Long householdId,
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
        this(
                householdId,
                type,
                startDate,
                endDate,
                accountId,
                accountScope,
                categoryId,
                clientId,
                status,
                minAmount,
                maxAmount,
                description,
                excludeTransfers,
                List.of(),
                null);
    }
}
