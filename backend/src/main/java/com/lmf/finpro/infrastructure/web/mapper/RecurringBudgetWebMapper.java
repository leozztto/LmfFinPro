package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.RecurringBudget;
import com.lmf.finpro.infrastructure.web.dto.recurringbudget.RecurringBudgetResponse;
import org.springframework.stereotype.Component;

@Component
public class RecurringBudgetWebMapper {

    public RecurringBudgetResponse toResponse(RecurringBudget recurrence) {
        return new RecurringBudgetResponse(
                recurrence.id(),
                recurrence.categoryId(),
                recurrence.limitValue(),
                recurrence.startMonth(),
                recurrence.endMonth(),
                recurrence.generatedMonths(),
                recurrence.active(),
                recurrence.active() ? recurrence.nextGenerationMonth() : null,
                recurrence.createdAt());
    }
}
