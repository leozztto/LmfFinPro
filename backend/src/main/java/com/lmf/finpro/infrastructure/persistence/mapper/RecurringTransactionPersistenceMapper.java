package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.infrastructure.persistence.entity.AccountJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.ClientJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.RecurringTransactionJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionPersistenceMapper {

    public RecurringTransactionJpaEntity toEntity(RecurringTransaction recurrence) {
        return RecurringTransactionJpaEntity.builder()
                .id(recurrence.id())
                .user(UserJpaEntity.builder().id(recurrence.userId()).build())
                .account(AccountJpaEntity.builder().id(recurrence.accountId()).build())
                .category(
                        recurrence.categoryId() == null
                                ? null
                                : CategoryJpaEntity.builder().id(recurrence.categoryId()).build())
                .client(
                        recurrence.clientId() == null
                                ? null
                                : ClientJpaEntity.builder().id(recurrence.clientId()).build())
                .description(recurrence.description())
                .amount(recurrence.amount())
                .type(recurrence.type())
                .frequency(recurrence.frequency())
                .startDate(recurrence.startDate())
                .endDate(recurrence.endDate())
                .generatedOccurrences(recurrence.generatedOccurrences())
                .active(recurrence.active())
                .createdAt(recurrence.createdAt())
                .build();
    }

    public RecurringTransaction toDomain(RecurringTransactionJpaEntity entity) {
        return new RecurringTransaction(
                entity.getId(),
                entity.getUser().getId(),
                entity.getAccount().getId(),
                entity.getCategory() == null ? null : entity.getCategory().getId(),
                entity.getClient() == null ? null : entity.getClient().getId(),
                entity.getDescription(),
                entity.getAmount(),
                entity.getType(),
                entity.getFrequency(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getGeneratedOccurrences(),
                entity.isActive(),
                entity.getCreatedAt());
    }
}
