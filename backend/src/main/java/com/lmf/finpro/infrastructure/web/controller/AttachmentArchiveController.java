package com.lmf.finpro.infrastructure.web.controller;

import com.lmf.finpro.application.attachment.AttachmentArchiveApplicationService;
import com.lmf.finpro.domain.model.AttachmentArchiveData;
import com.lmf.finpro.infrastructure.security.AuthenticatedUser;
import java.time.Year;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Pacote de comprovantes do ano (ZIP + índice CSV). O ZIP é escrito direto na resposta, um arquivo
 * por vez; um ano sem comprovantes responde 404 antes de começar.
 */
@RestController
@RequestMapping("/api/reports/attachments-archive")
@RequiredArgsConstructor
public class AttachmentArchiveController {

    private final AttachmentArchiveApplicationService attachmentArchiveApplicationService;

    @GetMapping
    public ResponseEntity<StreamingResponseBody> archive(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam @DateTimeFormat(pattern = "yyyy") Year year) {
        AttachmentArchiveData data =
                attachmentArchiveApplicationService.prepare(currentUser.householdId(), year);
        StreamingResponseBody body =
                output -> attachmentArchiveApplicationService.write(data, output);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"comprovantes-" + year + ".zip\"")
                .body(body);
    }
}
