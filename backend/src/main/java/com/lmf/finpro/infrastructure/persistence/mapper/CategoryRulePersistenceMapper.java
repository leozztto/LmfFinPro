package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.CategoryRule;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryRuleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryRulePersistenceMapper {

    public CategoryRuleJpaEntity toEntity(CategoryRule categoryRule) {
        return CategoryRuleJpaEntity.builder()
                .id(categoryRule.id())
                .householdId(categoryRule.householdId())
                .pattern(categoryRule.pattern())
                .category(CategoryJpaEntity.builder().id(categoryRule.categoryId()).build())
                .weight(categoryRule.weight())
                .build();
    }

    public CategoryRule toDomain(CategoryRuleJpaEntity entity) {
        return new CategoryRule(
                entity.getId(),
                entity.getHouseholdId(),
                entity.getPattern(),
                entity.getCategory().getId(),
                entity.getWeight());
    }
}
