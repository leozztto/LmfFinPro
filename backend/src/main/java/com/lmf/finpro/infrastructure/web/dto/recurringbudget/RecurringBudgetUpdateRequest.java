package com.lmf.finpro.infrastructure.web.dto.recurringbudget;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.YearMonth;

/** Categoria e mês inicial não são editáveis depois de criada a recorrência. */
public record RecurringBudgetUpdateRequest(
        @NotNull(message = "valor limite é obrigatório")
                @DecimalMin(
                        value = "0.0",
                        inclusive = false,
                        message = "valor limite deve ser maior que zero")
                BigDecimal limitValue,
        @JsonFormat(pattern = "yyyy-MM") YearMonth endMonth,
        @NotNull(message = "situação é obrigatória") Boolean active) {}
