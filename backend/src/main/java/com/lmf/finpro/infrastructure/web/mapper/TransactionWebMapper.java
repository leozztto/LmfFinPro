package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.Tag;
import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.infrastructure.web.dto.tag.TagSummaryResponse;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TransactionWebMapper {

    /** Sem contagem de anexos (telas que não mostram o indicador de anexo). */
    public TransactionResponse toResponse(Transaction transaction, List<Tag> tags) {
        return toResponse(transaction, 0, tags);
    }

    public TransactionResponse toResponse(
            Transaction transaction, long attachmentCount, List<Tag> tags) {
        return new TransactionResponse(
                transaction.id(),
                transaction.accountId(),
                transaction.categoryId(),
                transaction.clientId(),
                transaction.description(),
                transaction.amount(),
                transaction.transactionDate(),
                transaction.type(),
                transaction.origin(),
                transaction.createdAt(),
                transaction.transferId(),
                transaction.importBatchId(),
                transaction.recurringTransactionId(),
                transaction.status(),
                transaction.originalCurrency(),
                transaction.originalAmount(),
                transaction.baseAmount(),
                attachmentCount,
                TagSummaryResponse.of(tags),
                transaction.transactionTime());
    }
}
