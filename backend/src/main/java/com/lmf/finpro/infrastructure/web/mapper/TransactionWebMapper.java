package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.infrastructure.web.dto.transaction.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionWebMapper {

    public TransactionResponse toResponse(Transaction transaction) {
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
                transaction.importBatchId());
    }
}
