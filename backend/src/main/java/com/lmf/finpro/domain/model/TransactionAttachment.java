package com.lmf.finpro.domain.model;

import java.time.LocalDateTime;

/**
 * Registro de um arquivo anexado a uma transação. O conteúdo fica no armazenamento de arquivos,
 * endereçado por {@code storageKey}.
 *
 * @param fileName nome original enviado (só para exibição e download)
 */
public record TransactionAttachment(
        Long id,
        Long transactionId,
        Long householdId,
        AttachmentDocumentType documentType,
        String fileName,
        String contentType,
        long sizeBytes,
        String storageKey,
        LocalDateTime createdAt) {

    public static TransactionAttachment create(
            Long transactionId,
            Long householdId,
            AttachmentDocumentType documentType,
            String fileName,
            String contentType,
            long sizeBytes,
            String storageKey) {
        return new TransactionAttachment(
                null,
                transactionId,
                householdId,
                documentType,
                fileName,
                contentType,
                sizeBytes,
                storageKey,
                LocalDateTime.now());
    }

    public boolean belongsTo(Long candidateHouseholdId) {
        return householdId.equals(candidateHouseholdId);
    }
}
