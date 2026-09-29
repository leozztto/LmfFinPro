package com.lmf.finpro.infrastructure.web.dto.recurringbudget;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

public record RecurringBudgetResponse(
        Long id,
        Long categoryId,
        BigDecimal limitValue,
        @JsonFormat(pattern = "yyyy-MM") YearMonth startMonth,
        @JsonFormat(pattern = "yyyy-MM") YearMonth endMonth,
        int generatedMonths,
        boolean active,
        @JsonFormat(pattern = "yyyy-MM") YearMonth nextGenerationMonth,
        LocalDateTime createdAt) {}
