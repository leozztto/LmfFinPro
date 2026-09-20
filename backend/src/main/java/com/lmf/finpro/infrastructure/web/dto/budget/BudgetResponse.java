package com.lmf.finpro.infrastructure.web.dto.budget;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.YearMonth;

public record BudgetResponse(
    Long id,
    Long categoryId,
    @JsonFormat(pattern = "yyyy-MM") YearMonth referenceMonth,
    BigDecimal limitValue,
    BigDecimal spentValue
) {
}
