package com.lmf.finpro.application.transaction;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TransactionStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * Filtros da listagem de transações; todos opcionais.
 *
 * @param tagNames transações com <b>qualquer uma</b> destas tags (por nome)
 * @param hasAttachment {@code true} só com comprovante, {@code false} só sem
 */
public record TransactionListFilters(
        Long accountId,
        Long categoryId,
        Long clientId,
        CategoryType type,
        TransactionStatus status,
        Boolean hasAttachment,
        List<String> tagNames,
        LocalDate startDate,
        LocalDate endDate) {

    public TransactionListFilters {
        tagNames = tagNames == null ? List.of() : List.copyOf(tagNames);
    }
}
