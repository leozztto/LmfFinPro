package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.Transfer;
import com.lmf.finpro.infrastructure.persistence.entity.AccountJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.TransferJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class TransferPersistenceMapper {

    public TransferJpaEntity toEntity(Transfer transfer) {
        return TransferJpaEntity.builder()
            .id(transfer.id())
            .user(UserJpaEntity.builder().id(transfer.userId()).build())
            .fromAccount(AccountJpaEntity.builder().id(transfer.fromAccountId()).build())
            .toAccount(AccountJpaEntity.builder().id(transfer.toAccountId()).build())
            .amount(transfer.amount())
            .transferDate(transfer.transferDate())
            .description(transfer.description())
            .createdAt(transfer.createdAt())
            .build();
    }

    public Transfer toDomain(TransferJpaEntity entity) {
        return new Transfer(
            entity.getId(),
            entity.getUser().getId(),
            entity.getFromAccount().getId(),
            entity.getToAccount().getId(),
            entity.getAmount(),
            entity.getTransferDate(),
            entity.getDescription(),
            entity.getCreatedAt()
        );
    }
}
