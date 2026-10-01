package com.lmf.finpro.infrastructure.web.dto.transaction;

import com.lmf.finpro.application.transaction.TransactionListFilters;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.TransactionStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Parâmetros de query da listagem de transações. Tudo opcional: o que não vier na URL não filtra, e
 * {@code page}/{@code size} caem no padrão. Tags vão repetidas: {@code ?tagNames=a&tagNames=b}
 * (qualquer uma das duas).
 */
public record TransactionListRequest(
        Integer page,
        Integer size,
        Long accountId,
        Long categoryId,
        Long clientId,
        CategoryType type,
        TransactionStatus status,
        Boolean hasAttachment,
        List<String> tagNames,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    public TransactionListFilters toFilters() {
        return new TransactionListFilters(
                accountId,
                categoryId,
                clientId,
                type,
                status,
                hasAttachment,
                tagNames,
                startDate,
                endDate);
    }
}
