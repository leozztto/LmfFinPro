package com.lmf.finpro.application.report;

import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.TransactionStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * Filtros opcionais do relatório de totais por tag. {@code tagIds} vazio = todas as tags do usuário
 * (mais a linha "Sem tag").
 */
public record TagTotalsReportFilters(
        LocalDate startDate,
        LocalDate endDate,
        AccountScope accountScope,
        TransactionStatus status,
        List<Long> tagIds) {

    public TagTotalsReportFilters {
        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
    }
}
