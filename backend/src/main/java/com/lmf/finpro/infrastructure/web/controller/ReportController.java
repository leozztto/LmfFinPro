package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.report.ReportApplicationService;
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

    @GetMapping(value = "/client-receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> clientReceipt(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam Long clientId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth referenceMonth) {
        byte[] pdf =
                reportApplicationService.generateClientReceipt(
                        currentUser.userId(), clientId, referenceMonth);

        String filename = "recibo-cliente-" + clientId + "-" + referenceMonth + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping(value = "/account-statement", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> accountStatement(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam Long accountId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth referenceMonth) {
        byte[] pdf =
                reportApplicationService.generateAccountStatement(
                        currentUser.userId(), accountId, referenceMonth);

        String filename = "extrato-conta-" + accountId + "-" + referenceMonth + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping(value = "/client-annual-statement", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> clientAnnualStatement(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam Long clientId,
            @RequestParam @DateTimeFormat(pattern = "yyyy") Year year) {
        byte[] pdf =
                reportApplicationService.generateClientAnnualStatement(
                        currentUser.userId(), clientId, year);

        String filename = "demonstrativo-anual-cliente-" + clientId + "-" + year + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping(value = "/category-expenses", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> categoryExpenses(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth referenceMonth) {
        byte[] pdf =
                reportApplicationService.generateCategoryExpenseReport(
                        currentUser.userId(), referenceMonth);

        String filename = "despesas-por-categoria-" + referenceMonth + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping(value = "/income-statement", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> incomeStatement(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam @DateTimeFormat(pattern = "yyyy") Year year,
            @RequestParam ReportGranularity granularity) {
        byte[] pdf =
                reportApplicationService.generateIncomeStatement(
                        currentUser.userId(), year, granularity);

        String filename = "resultado-periodo-" + year + "-" + granularity + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping(value = "/budget-vs-actual", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> budgetVsActual(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth referenceMonth) {
        byte[] pdf =
                reportApplicationService.generateBudgetVsActualReport(
                        currentUser.userId(), referenceMonth);

        String filename = "orcamento-vs-realizado-" + referenceMonth + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping(value = "/transaction-export", produces = "text/csv")
    public ResponseEntity<byte[]> transactionExport(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth referenceMonth) {
        byte[] csv =
                reportApplicationService.generateTransactionExport(
                        currentUser.userId(), referenceMonth);

        String filename = "transacoes-" + referenceMonth + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }
}
