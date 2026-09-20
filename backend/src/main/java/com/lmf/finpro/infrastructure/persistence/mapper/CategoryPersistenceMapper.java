package com.lmf.finpro.infrastructure.persistence.mapper;

import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.infrastructure.persistence.entity.CategoryJpaEntity;
import com.lmf.finpro.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryPersistenceMapper {

    public CategoryJpaEntity toEntity(Category category) {
        return CategoryJpaEntity.builder()
                .id(category.id())
                .user(
                        category.userId() == null
                                ? null
                                : UserJpaEntity.builder().id(category.userId()).build())
                .name(category.name())
                .type(category.type())
                .color(category.color())
                .icon(category.icon())
                .build();
    }

    public Category toDomain(CategoryJpaEntity entity) {
        return new Category(
                entity.getId(),
                entity.getUser() == null ? null : entity.getUser().getId(),
                entity.getName(),
                entity.getType(),
                entity.getColor(),
                entity.getIcon());
    }
}
