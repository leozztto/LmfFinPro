package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.report.ReportApplicationService;
import com.lmf.finpro.domain.model.ReportFormat;
import com.lmf.finpro.domain.model.ReportGranularity;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import java.time.Year;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportApplicationService reportApplicationService;

    @GetMapping(value = "/client-receipt")
    public ResponseEntity<byte[]> clientReceipt(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam Long clientId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth referenceMonth,
            @RequestParam(defaultValue = "PDF") ReportFormat format) {
        byte[] body =
                reportApplicationService.generateClientReceipt(
                        currentUser.userId(), clientId, referenceMonth, format);

        String filename = "recibo-cliente-" + clientId + "-" + referenceMonth + extension(format);
        return respond(body, filename, format);
    }

    @GetMapping(value = "/account-statement")
    public ResponseEntity<byte[]> accountStatement(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam Long accountId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth referenceMonth,
            @RequestParam(defaultValue = "PDF") ReportFormat format) {
        byte[] body =
                reportApplicationService.generateAccountStatement(
                        currentUser.userId(), accountId, referenceMonth, format);

        String filename = "extrato-conta-" + accountId + "-" + referenceMonth + extension(format);
        return respond(body, filename, format);
    }

    @GetMapping(value = "/client-annual-statement")
    public ResponseEntity<byte[]> clientAnnualStatement(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam Long clientId,
            @RequestParam @DateTimeFormat(pattern = "yyyy") Year year,
            @RequestParam(defaultValue = "PDF") ReportFormat format) {
        byte[] body =
                reportApplicationService.generateClientAnnualStatement(
                        currentUser.userId(), clientId, year, format);

        String filename =
                "demonstrativo-anual-cliente-" + clientId + "-" + year + extension(format);
        return respond(body, filename, format);
    }

    @GetMapping(value = "/category-expenses")
    public ResponseEntity<byte[]> categoryExpenses(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth referenceMonth,
            @RequestParam(defaultValue = "PDF") ReportFormat format) {
        byte[] body =
                reportApplicationService.generateCategoryExpenseReport(
                        currentUser.userId(), referenceMonth, format);

        String filename = "despesas-por-categoria-" + referenceMonth + extension(format);
        return respond(body, filename, format);
    }

    @GetMapping(value = "/income-statement")
    public ResponseEntity<byte[]> incomeStatement(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam @DateTimeFormat(pattern = "yyyy") Year year,
            @RequestParam ReportGranularity granularity,
            @RequestParam(defaultValue = "PDF") ReportFormat format) {
        byte[] body =
                reportApplicationService.generateIncomeStatement(
                        currentUser.userId(), year, granularity, format);

        String filename = "resultado-periodo-" + year + "-" + granularity + extension(format);
        return respond(body, filename, format);
    }

    @GetMapping(value = "/budget-vs-actual")
    public ResponseEntity<byte[]> budgetVsActual(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth referenceMonth,
            @RequestParam(defaultValue = "PDF") ReportFormat format) {
        byte[] body =
                reportApplicationService.generateBudgetVsActualReport(
                        currentUser.userId(), referenceMonth, format);

        String filename = "orcamento-vs-realizado-" + referenceMonth + extension(format);
        return respond(body, filename, format);
    }

    @GetMapping(value = "/transaction-export")
    public ResponseEntity<byte[]> transactionExport(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth referenceMonth,
            @RequestParam(defaultValue = "CSV") ReportFormat format) {
        byte[] body =
                reportApplicationService.generateTransactionExport(
                        currentUser.userId(), referenceMonth, format);

        String filename = "transacoes-" + referenceMonth + extension(format);
        return respond(body, filename, format);
    }

    private ResponseEntity<byte[]> respond(byte[] body, String filename, ReportFormat format) {
        return ResponseEntity.ok()
                .contentType(contentType(format))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

    private MediaType contentType(ReportFormat format) {
        return format == ReportFormat.CSV
                ? MediaType.parseMediaType("text/csv")
                : MediaType.APPLICATION_PDF;
    }

    private String extension(ReportFormat format) {
        return format == ReportFormat.CSV ? ".csv" : ".pdf";
    }
}
