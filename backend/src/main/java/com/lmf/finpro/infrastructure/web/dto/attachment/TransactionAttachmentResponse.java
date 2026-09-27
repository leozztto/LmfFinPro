package com.lmf.finpro.infrastructure.web.dto.attachment;

import com.lmf.finpro.domain.model.AttachmentDocumentType;
import java.time.LocalDateTime;

public record TransactionAttachmentResponse(
        Long id,
        Long transactionId,
        AttachmentDocumentType documentType,
        String fileName,
        String contentType,
        long sizeBytes,
        LocalDateTime createdAt) {}
