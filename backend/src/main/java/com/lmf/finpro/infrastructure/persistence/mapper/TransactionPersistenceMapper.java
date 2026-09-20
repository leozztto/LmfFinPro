package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.Transaction;
import com.lmf.finpro.infrastructure.persistence.entity.AccountJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.ClientJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.ImportBatchJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.TransactionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TransactionPersistenceMapper {

    public TransactionJpaEntity toEntity(Transaction transaction) {
        return TransactionJpaEntity.builder()
                .id(transaction.id())
                .account(AccountJpaEntity.builder().id(transaction.accountId()).build())
                .category(
                        transaction.categoryId() == null
                                ? null
                                : CategoryJpaEntity.builder().id(transaction.categoryId()).build())
                .client(
                        transaction.clientId() == null
                                ? null
                                : ClientJpaEntity.builder().id(transaction.clientId()).build())
                .importBatch(
                        transaction.importBatchId() == null
                                ? null
                                : ImportBatchJpaEntity.builder()
                                        .id(transaction.importBatchId())
                                        .build())
                .description(transaction.description())
                .amount(transaction.amount())
                .transactionDate(transaction.transactionDate())
                .type(transaction.type())
                .origin(transaction.origin())
                .createdAt(transaction.createdAt())
                .transferId(transaction.transferId())
                .build();
    }

    public Transaction toDomain(TransactionJpaEntity entity) {
        return new Transaction(
                entity.getId(),
                entity.getAccount().getId(),
                entity.getCategory() == null ? null : entity.getCategory().getId(),
                entity.getClient() == null ? null : entity.getClient().getId(),
                entity.getDescription(),
                entity.getAmount(),
                entity.getTransactionDate(),
                entity.getType(),
                entity.getOrigin(),
                entity.getCreatedAt(),
                entity.getTransferId(),
                entity.getImportBatch() == null ? null : entity.getImportBatch().getId());
    }
}
