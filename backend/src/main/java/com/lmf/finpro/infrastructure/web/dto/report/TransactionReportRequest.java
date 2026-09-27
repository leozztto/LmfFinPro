package com.lmf.finpro.infrastructure.web.dto.report;

import com.lmf.finpro.application.report.TransactionReportFilters;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.domain.model.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Parâmetros de query dos relatórios de receitas/despesas. Todos opcionais: o que não vier na URL
 * fica nulo e não é aplicado na busca. Tags vão repetidas: {@code ?tagIds=3&tagIds=7} (qualquer uma
 * das duas).
 */
public record TransactionReportRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        Long accountId,
        AccountScope accountScope,
        Long categoryId,
        Long clientId,
        TransactionStatus status,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String description,
        List<Long> tagIds,
        ReportFormat format) {

    public TransactionReportFilters toFilters() {
        return new TransactionReportFilters(
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
                tagIds);
    }

    public ReportFormat formatOrDefault() {
        return format == null ? ReportFormat.PDF : format;
    }
}
