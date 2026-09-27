package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.attachment.TransactionAttachmentApplicationService;
import com.lmf.finpro.application.attachment.TransactionAttachmentApplicationService.AttachmentContent;
import com.lmf.finpro.domain.exception.AttachmentInvalidException;
import com.lmf.finpro.domain.model.AttachmentDocumentType;
import com.lmf.finpro.domain.model.TransactionAttachment;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import com.lmf.finpro.infrastructure.web.dto.attachment.TransactionAttachmentResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Comprovantes, notas fiscais e recibos de uma transação. */
@RestController
@RequestMapping("/api/transactions/{transactionId}/attachments")
@RequiredArgsConstructor
public class TransactionAttachmentController {

    private final TransactionAttachmentApplicationService attachmentApplicationService;

    @GetMapping
    public List<TransactionAttachmentResponse> list(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long transactionId) {
        return attachmentApplicationService.list(currentUser.userId(), transactionId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TransactionAttachmentResponse> upload(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long transactionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false)
                    AttachmentDocumentType documentType) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new AttachmentInvalidException("Não foi possível ler o arquivo enviado.");
        }
        TransactionAttachment created =
                attachmentApplicationService.upload(
                        currentUser.userId(),
                        transactionId,
                        documentType,
                        file.getOriginalFilename(),
                        content);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    /**
     * Devolve o arquivo com o tipo reconhecido no upload. {@code download=true} força o download;
     * sem ele, o navegador abre o PDF/imagem. {@code nosniff} impede o navegador de "adivinhar"
     * outro tipo de conteúdo.
     */
    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<byte[]> content(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long transactionId,
            @PathVariable Long attachmentId,
            @RequestParam(defaultValue = "false") boolean download) {
        AttachmentContent result =
                attachmentApplicationService.download(
                        currentUser.userId(), transactionId, attachmentId);
        TransactionAttachment attachment = result.attachment();
        ContentDisposition disposition =
                (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                        .filename(attachment.fileName(), StandardCharsets.UTF_8)
                        .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(result.content());
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long transactionId,
            @PathVariable Long attachmentId) {
        attachmentApplicationService.delete(currentUser.userId(), transactionId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    private TransactionAttachmentResponse toResponse(TransactionAttachment attachment) {
        return new TransactionAttachmentResponse(
                attachment.id(),
                attachment.transactionId(),
                attachment.documentType(),
                attachment.fileName(),
                attachment.contentType(),
                attachment.sizeBytes(),
                attachment.createdAt());
    }
}
