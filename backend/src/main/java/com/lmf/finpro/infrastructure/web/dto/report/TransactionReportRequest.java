package com.lmf.finpro.infrastructure.web.dto.report;

import com.lmf.finpro.application.report.TransactionReportFilters;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.domain.model.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Parâmetros de query dos relatórios de receitas/despesas. Todos opcionais: o que não vier na URL
 * fica nulo e não é aplicado na busca.
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
                description);
    }

    public ReportFormat formatOrDefault() {
        return format == null ? ReportFormat.PDF : format;
    }
}
