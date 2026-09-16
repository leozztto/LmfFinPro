package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.Category;
import com.lmf.finpro.infrastructure.web.dto.category.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryWebMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
            category.id(), category.name(), category.type(), category.color(), category.icon(), category.isGlobal()
        );
    }
}
