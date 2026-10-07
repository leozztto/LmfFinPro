package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.RecurringBudget;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.RecurringBudgetJpaEntity;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

@Component
public class RecurringBudgetPersistenceMapper {

    public RecurringBudgetJpaEntity toEntity(RecurringBudget recurringBudget) {
        return RecurringBudgetJpaEntity.builder()
                .id(recurringBudget.id())
                .householdId(recurringBudget.householdId())
                .category(CategoryJpaEntity.builder().id(recurringBudget.categoryId()).build())
                .limitValue(recurringBudget.limitValue())
                .startMonth(recurringBudget.startMonth().atDay(1))
                .endMonth(
                        recurringBudget.endMonth() == null
                                ? null
                                : recurringBudget.endMonth().atDay(1))
                .generatedMonths(recurringBudget.generatedMonths())
                .active(recurringBudget.active())
                .createdAt(recurringBudget.createdAt())
                .build();
    }

    public RecurringBudget toDomain(RecurringBudgetJpaEntity entity) {
        return new RecurringBudget(
                entity.getId(),
                entity.getHouseholdId(),
                entity.getCategory().getId(),
                entity.getLimitValue(),
                YearMonth.from(entity.getStartMonth()),
                entity.getEndMonth() == null ? null : YearMonth.from(entity.getEndMonth()),
                entity.getGeneratedMonths(),
                entity.isActive(),
                entity.getCreatedAt());
    }
}
