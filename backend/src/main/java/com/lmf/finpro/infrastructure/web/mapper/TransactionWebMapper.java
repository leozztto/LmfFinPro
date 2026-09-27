package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionWebMapper {

    /** Sem contagem de anexos (telas que não mostram o indicador de anexo). */
    public TransactionResponse toResponse(Transaction transaction) {
        return toResponse(transaction, 0);
    }

    public TransactionResponse toResponse(Transaction transaction, long attachmentCount) {
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
                attachmentCount);
    }
}
