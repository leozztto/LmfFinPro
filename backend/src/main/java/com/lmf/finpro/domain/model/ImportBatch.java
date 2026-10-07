package com.lmf.finpro.domain.model;

import java.time.LocalDateTime;

/**
 * @param duplicateCount quantas linhas do arquivo foram puladas por já existir uma transação igual
 *     (mesma data, hora, descrição, valor e tipo) na conta — de uma importação anterior ou repetida
 *     dentro do próprio arquivo
 */
public record ImportBatch(
        Long id,
        Long householdId,
        Long accountId,
        String originalFile,
        ImportFormat format,
        LocalDateTime importedAt,
        ImportStatus status,
        int duplicateCount) {

    public static ImportBatch start(
            Long householdId, Long accountId, String originalFile, ImportFormat format) {
        return new ImportBatch(
                null,
                householdId,
                accountId,
                originalFile,
                format,
                LocalDateTime.now(),
                ImportStatus.PROCESSING,
                0);
    }

    public boolean belongsTo(Long candidateHouseholdId) {
        return householdId.equals(candidateHouseholdId);
    }

    public ImportBatch withStatus(ImportStatus newStatus) {
        return new ImportBatch(
                id,
                householdId,
                accountId,
                originalFile,
                format,
                importedAt,
                newStatus,
                duplicateCount);
    }

    public ImportBatch withDuplicateCount(int newDuplicateCount) {
        return new ImportBatch(
                id,
                householdId,
                accountId,
                originalFile,
                format,
                importedAt,
                status,
                newDuplicateCount);
    }
}
