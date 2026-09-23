package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.report.ReportApplicationService;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
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
}
