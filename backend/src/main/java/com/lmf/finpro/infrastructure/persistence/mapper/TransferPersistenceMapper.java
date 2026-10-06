package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.Transfer;
import com.lmf.finpro.infrastructure.persistence.entity.AccountJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.TransferJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TransferPersistenceMapper {

    public TransferJpaEntity toEntity(Transfer transfer) {
        return TransferJpaEntity.builder()
                .id(transfer.id())
                .householdId(transfer.householdId())
                .fromAccount(AccountJpaEntity.builder().id(transfer.fromAccountId()).build())
                .toAccount(AccountJpaEntity.builder().id(transfer.toAccountId()).build())
                .amount(transfer.amount())
                .transferDate(transfer.transferDate())
                .description(transfer.description())
                .createdAt(transfer.createdAt())
                .receivedAmount(transfer.receivedAmount())
                .build();
    }

    public Transfer toDomain(TransferJpaEntity entity) {
        return new Transfer(
                entity.getId(),
                entity.getHouseholdId(),
                entity.getFromAccount().getId(),
                entity.getToAccount().getId(),
                entity.getAmount(),
                entity.getTransferDate(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getReceivedAmount());
    }
}
