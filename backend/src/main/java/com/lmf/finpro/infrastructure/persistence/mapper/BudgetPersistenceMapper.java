package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.infrastructure.persistence.entity.BudgetJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.ClientJpaEntity;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

@Component
public class BudgetPersistenceMapper {

    public BudgetJpaEntity toEntity(Budget budget) {
        return BudgetJpaEntity.builder()
                .id(budget.id())
                .householdId(budget.householdId())
                .category(CategoryJpaEntity.builder().id(budget.categoryId()).build())
                .client(
                        budget.clientId() == null
                                ? null
                                : ClientJpaEntity.builder().id(budget.clientId()).build())
                .referenceMonth(budget.referenceMonth().atDay(1))
                .limitValue(budget.limitValue())
                .build();
    }

    public Budget toDomain(BudgetJpaEntity entity) {
        return new Budget(
                entity.getId(),
                entity.getHouseholdId(),
                entity.getCategory().getId(),
                YearMonth.from(entity.getReferenceMonth()),
                entity.getLimitValue(),
                entity.getClient() == null ? null : entity.getClient().getId());
    }
}
