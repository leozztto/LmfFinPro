package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.report.TransactionReportApplicationService;
import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.report.TransactionReportRequest;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Relatórios de receitas e de despesas com filtros opcionais, em PDF ou CSV. Ex.: {@code GET
 * /api/reports/expenses?startDate=2026-09-01&categoryId=5&status=PENDING&format=CSV}.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class TransactionReportController {

    private final TransactionReportApplicationService transactionReportApplicationService;
    private final Clock clock;

    @GetMapping("/incomes")
    public ResponseEntity<byte[]> incomes(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            TransactionReportRequest request) {
        return generate(currentUser, CategoryType.INCOME, request, "receitas");
    }

    @GetMapping("/expenses")
    public ResponseEntity<byte[]> expenses(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            TransactionReportRequest request) {
        return generate(currentUser, CategoryType.EXPENSE, request, "despesas");
    }

    private ResponseEntity<byte[]> generate(
            AuthenticatedUser currentUser,
            CategoryType type,
            TransactionReportRequest request,
            String filePrefix) {
        ReportFormat format = request.formatOrDefault();
        byte[] body =
                transactionReportApplicationService.generate(
                        currentUser.userId(), type, request.toFilters(), format);
        String filename =
                filePrefix
                        + "-"
                        + LocalDate.now(clock)
                        + (format == ReportFormat.CSV ? ".csv" : ".pdf");
        return ResponseEntity.ok()
                .contentType(
                        format == ReportFormat.CSV
                                ? MediaType.parseMediaType("text/csv")
                                : MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
