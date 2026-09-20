package com.lmf.finpro.infrastructure.web.dto.budget;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;

public record BudgetRequest(
    @NotNull(message = "categoria é obrigatória") Long categoryId,
    @NotNull(message = "mês de referência é obrigatório")
    @JsonFormat(pattern = "yyyy-MM")
    YearMonth referenceMonth,
    @NotNull(message = "valor limite é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "valor limite deve ser maior que zero")
    BigDecimal limitValue
) {
}
