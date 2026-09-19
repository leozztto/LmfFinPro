package com.lmf.finpro.infrastructure.web.dto.importbatch;

import com.lmf.finpro.domain.model.ImportFormat;
import com.lmf.finpro.domain.model.ImportStatus;

import java.time.LocalDateTime;

public record ImportBatchResponse(
    Long id,
    Long accountId,
    String originalFile,
    ImportFormat format,
    LocalDateTime importedAt,
    ImportStatus status,
    int transactionCount,
    int uncategorizedCount
) {
}
