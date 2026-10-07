package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryPersistenceMapper {

    public CategoryJpaEntity toEntity(Category category) {
        return CategoryJpaEntity.builder()
                .id(category.id())
                .householdId(category.householdId())
                .name(category.name())
                .type(category.type())
                .color(category.color())
                .icon(category.icon())
                .build();
    }

    public Category toDomain(CategoryJpaEntity entity) {
        return new Category(
                entity.getId(),
                entity.getHouseholdId(),
                entity.getName(),
                entity.getType(),
                entity.getColor(),
                entity.getIcon());
    }
}
