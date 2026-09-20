package com.lmf.finpro.domain.model;

import java.time.LocalDateTime;

public record ImportBatch(
        Long id,
        Long userId,
        Long accountId,
        String originalFile,
        ImportFormat format,
        LocalDateTime importedAt,
        ImportStatus status) {

    public static ImportBatch start(
            Long userId, Long accountId, String originalFile, ImportFormat format) {
        return new ImportBatch(
                null,
                userId,
                accountId,
                originalFile,
                format,
                LocalDateTime.now(),
                ImportStatus.PROCESSING);
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public ImportBatch withStatus(ImportStatus newStatus) {
        return new ImportBatch(id, userId, accountId, originalFile, format, importedAt, newStatus);
    }
}
