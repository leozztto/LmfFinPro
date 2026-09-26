package com.lmf.finpro.infrastructure.web.mapper;

import com.lmf.finpro.domain.model.RecurringTransaction;
import com.lmf.finpro.infrastructure.web.dto.recurringtransaction.RecurringTransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionWebMapper {

    public RecurringTransactionResponse toResponse(RecurringTransaction recurrence) {
        return new RecurringTransactionResponse(
                recurrence.id(),
                recurrence.accountId(),
                recurrence.categoryId(),
                recurrence.clientId(),
                recurrence.description(),
                recurrence.amount(),
                recurrence.type(),
                recurrence.frequency(),
                recurrence.startDate(),
                recurrence.endDate(),
                recurrence.generatedOccurrences(),
                recurrence.active(),
                recurrence.active() ? recurrence.nextOccurrenceDate() : null,
                recurrence.createdAt());
    }
}
