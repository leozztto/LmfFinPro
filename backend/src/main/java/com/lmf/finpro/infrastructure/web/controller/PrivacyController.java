package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.privacy.AccountDeletionApplicationService;
import com.lmf.finpro.application.privacy.DataExportApplicationService;
import com.lmf.finpro.domain.model.PersonalDataExport;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.security.PrivacyRateLimiter;
import com.lmf.finpro.infrastructure.web.RefreshCookieFactory;
import com.lmf.finpro.infrastructure.web.dto.privacy.AccountDeletionPreviewResponse;
import com.lmf.finpro.infrastructure.web.dto.privacy.DeleteAccountRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Direitos do titular (LGPD): levar os dados e excluir a conta. Sempre sobre o usuário do token;
 * não depende do grupo ativo, porque os dois alcançam todos os grupos da pessoa.
 */
@RestController
@RequestMapping("/api/privacy")
@RequiredArgsConstructor
public class PrivacyController {

    private final DataExportApplicationService dataExportApplicationService;
    private final AccountDeletionApplicationService accountDeletionApplicationService;
    private final PrivacyRateLimiter privacyRateLimiter;
    private final RefreshCookieFactory refreshCookieFactory;
    private final Clock clock;

    /** ZIP com os dados do titular, escrito direto na resposta. */
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> export(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        privacyRateLimiter.checkExport(currentUser.userId());
        PersonalDataExport export = dataExportApplicationService.prepare(currentUser.userId());
        StreamingResponseBody body = output -> dataExportApplicationService.write(export, output);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"finpro-meus-dados-"
                                + LocalDate.now(clock)
                                + ".zip\"")
                .header("Cache-Control", "no-store")
                .body(body);
    }

    @GetMapping("/account-deletion-preview")
    public AccountDeletionPreviewResponse deletionPreview(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return AccountDeletionPreviewResponse.from(
                accountDeletionApplicationService.preview(currentUser.userId()));
    }

    /** Definitivo. Pede a senha e apaga o cookie de sessão junto. */
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody DeleteAccountRequest request) {
        privacyRateLimiter.checkDeleteAccount(currentUser.userId());
        accountDeletionApplicationService.delete(currentUser.userId(), request.password());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookieFactory.clear().toString())
                .build();
    }
}
