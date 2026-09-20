package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.Budget;
import com.lmf.finpro.infrastructure.web.dto.budget.BudgetResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BudgetWebMapper {

    public BudgetResponse toResponse(Budget budget, BigDecimal spentValue) {
        return new BudgetResponse(budget.id(), budget.categoryId(), budget.referenceMonth(), budget.limitValue(), spentValue);
    }
}
