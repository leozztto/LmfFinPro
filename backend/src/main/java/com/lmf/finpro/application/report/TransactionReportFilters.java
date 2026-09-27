package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Filtros opcionais dos relatórios de receitas e de despesas — qualquer um pode ser nulo. {@code
 * tagIds}: transações com qualquer uma destas tags.
 */
public record TransactionReportFilters(
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
        List<Long> tagIds) {

    public TransactionReportFilters {
        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
    }

    /** Sem filtro de tag. */
    public TransactionReportFilters(
            LocalDate startDate,
            LocalDate endDate,
            Long accountId,
            AccountScope accountScope,
            Long categoryId,
            Long clientId,
            TransactionStatus status,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String description) {
        this(
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
                List.of());
    }
}
