package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.infrastructure.persistence.entity.AccountJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountPersistenceMapper {

    public AccountJpaEntity toEntity(Account account) {
        return AccountJpaEntity.builder()
            .id(account.id())
            .user(UserJpaEntity.builder().id(account.userId()).build())
            .name(account.name())
            .type(account.type())
            .initialBalance(account.initialBalance())
            .createdAt(account.createdAt())
            .build();
    }

    public Account toDomain(AccountJpaEntity entity) {
        return new Account(
            entity.getId(),
            entity.getUser().getId(),
            entity.getName(),
            entity.getType(),
            entity.getInitialBalance(),
            entity.getCreatedAt()
        );
    }
}
