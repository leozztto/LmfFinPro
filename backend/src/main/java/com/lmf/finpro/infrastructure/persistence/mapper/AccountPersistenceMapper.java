package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.Account;
import com.lmf.finpro.infrastructure.persistence.entity.AccountJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountPersistenceMapper {

    public AccountJpaEntity toEntity(Account account) {
        return AccountJpaEntity.builder()
                .id(account.id())
                .householdId(account.householdId())
                .name(account.name())
                .type(account.type())
                .initialBalance(account.initialBalance())
                .createdAt(account.createdAt())
                .scope(account.scope())
                .currency(account.currency())
                .build();
    }

    public Account toDomain(AccountJpaEntity entity) {
        return new Account(
                entity.getId(),
                entity.getHouseholdId(),
                entity.getName(),
                entity.getType(),
                entity.getInitialBalance(),
                entity.getCreatedAt(),
                entity.getScope(),
                entity.getCurrency());
    }
}
