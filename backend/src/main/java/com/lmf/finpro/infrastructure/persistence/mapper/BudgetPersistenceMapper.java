package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.infrastructure.persistence.entity.BudgetJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class BudgetPersistenceMapper {

    public BudgetJpaEntity toEntity(Budget budget) {
        return BudgetJpaEntity.builder()
            .id(budget.id())
            .user(UserJpaEntity.builder().id(budget.userId()).build())
            .category(CategoryJpaEntity.builder().id(budget.categoryId()).build())
            .referenceMonth(budget.referenceMonth().atDay(1))
            .limitValue(budget.limitValue())
            .build();
    }

    public Budget toDomain(BudgetJpaEntity entity) {
        return new Budget(
            entity.getId(),
            entity.getUser().getId(),
            entity.getCategory().getId(),
            YearMonth.from(entity.getReferenceMonth()),
            entity.getLimitValue()
        );
    }
}
