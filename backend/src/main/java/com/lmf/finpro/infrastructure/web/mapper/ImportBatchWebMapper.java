package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.ImportBatch;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.infrastructure.web.dto.importbatch.ImportBatchResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImportBatchWebMapper {

    public ImportBatchResponse toResponse(ImportBatch batch, List<Transaction> transactions) {
        int uncategorizedCount = (int) transactions.stream().filter(transaction -> transaction.categoryId() == null).count();
        return new ImportBatchResponse(
            batch.id(),
            batch.accountId(),
            batch.originalFile(),
            batch.format(),
            batch.importedAt(),
            batch.status(),
            transactions.size(),
            uncategorizedCount
        );
    }
}
