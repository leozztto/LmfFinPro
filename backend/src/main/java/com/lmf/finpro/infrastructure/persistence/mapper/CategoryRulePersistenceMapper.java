package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.CategoryRule;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryRuleJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryRulePersistenceMapper {

    public CategoryRuleJpaEntity toEntity(CategoryRule categoryRule) {
        return CategoryRuleJpaEntity.builder()
            .id(categoryRule.id())
            .user(UserJpaEntity.builder().id(categoryRule.userId()).build())
            .pattern(categoryRule.pattern())
            .category(CategoryJpaEntity.builder().id(categoryRule.categoryId()).build())
            .weight(categoryRule.weight())
            .build();
    }

    public CategoryRule toDomain(CategoryRuleJpaEntity entity) {
        return new CategoryRule(
            entity.getId(),
            entity.getUser().getId(),
            entity.getPattern(),
            entity.getCategory().getId(),
            entity.getWeight()
        );
    }
}
