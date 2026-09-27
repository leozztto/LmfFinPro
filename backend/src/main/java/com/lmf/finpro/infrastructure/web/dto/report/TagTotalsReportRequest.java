package com.lmf.finpro.infrastructure.web.dto.report;

import com.lmf.finpro.application.report.TagTotalsReportFilters;
import com.lmf.finpro.domain.model.AccountScope;
import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.domain.model.TransactionStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Parâmetros de query do relatório de totais por tag, todos opcionais. Ex.: {@code GET
 * /api/reports/tag-totals?startDate=2026-01-01&endDate=2026-12-31&tagIds=3&tagIds=7}.
 */
public record TagTotalsReportRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        AccountScope accountScope,
        TransactionStatus status,
        List<Long> tagIds,
        ReportFormat format) {

    public TagTotalsReportFilters toFilters() {
        return new TagTotalsReportFilters(startDate, endDate, accountScope, status, tagIds);
    }

    public ReportFormat formatOrDefault() {
        return format == null ? ReportFormat.PDF : format;
    }
}
