package com.lmf.finpro.infrastructure.web.dto.recurringtransaction;

import com.lmf.finpro.domain.model.CategoryType;
import com.lmf.finpro.domain.model.RecurrenceFrequency;
import com.lmf.finpro.infrastructure.web.dto.tag.TagSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RecurringTransactionResponse(
        Long id,
        Long accountId,
        Long categoryId,
        Long clientId,
        String description,
        BigDecimal amount,
        CategoryType type,
        RecurrenceFrequency frequency,
        LocalDate startDate,
        LocalDate endDate,
        int generatedOccurrences,
        boolean active,
        LocalDate nextOccurrenceDate,
        LocalDateTime createdAt,
        List<TagSummaryResponse> tags) {}
