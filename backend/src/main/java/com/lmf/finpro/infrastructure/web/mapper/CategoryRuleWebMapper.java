package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.CategoryRule;
import com.lmf.finpro.infrastructure.web.dto.categoryrule.CategoryRuleResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryRuleWebMapper {

    public CategoryRuleResponse toResponse(CategoryRule categoryRule) {
        return new CategoryRuleResponse(categoryRule.id(), categoryRule.pattern(), categoryRule.categoryId(), categoryRule.weight());
    }
}
