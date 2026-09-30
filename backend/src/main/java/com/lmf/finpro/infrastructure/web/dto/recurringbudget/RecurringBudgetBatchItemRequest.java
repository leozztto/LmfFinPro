package com.lmf.finpro.infrastructure.web.dto.recurringbudget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RecurringBudgetBatchItemRequest(
        @NotNull(message = "categoria é obrigatória") Long categoryId,
        @NotNull(message = "valor limite é obrigatório")
                @DecimalMin(
                        value = "0.0",
                        inclusive = false,
                        message = "valor limite deve ser maior que zero")
                BigDecimal limitValue) {}
