package com.lmf.finpro.infrastructure.web.dto.recurringbudget;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;
import java.util.List;

public record RecurringBudgetBatchRequest(
        @NotNull(message = "mês inicial é obrigatório") @JsonFormat(pattern = "yyyy-MM")
                YearMonth startMonth,
        @JsonFormat(pattern = "yyyy-MM") YearMonth endMonth,
        @NotEmpty(message = "informe ao menos uma categoria") @Valid
                List<RecurringBudgetBatchItemRequest> items) {}
