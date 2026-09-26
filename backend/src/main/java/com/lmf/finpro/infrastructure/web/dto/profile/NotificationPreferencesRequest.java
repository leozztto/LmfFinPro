package com.lmf.finpro.infrastructure.web.dto.profile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record NotificationPreferencesRequest(
        @NotNull(message = "billsEnabled é obrigatório") Boolean billsEnabled,
        @NotNull(message = "antecedência é obrigatória")
                @Min(value = 0, message = "antecedência deve ser de 0 a 15 dias")
                @Max(value = 15, message = "antecedência deve ser de 0 a 15 dias")
                Integer billDaysBefore,
        @NotNull(message = "budgetsEnabled é obrigatório") Boolean budgetsEnabled,
        @NotNull(message = "dasEnabled é obrigatório") Boolean dasEnabled) {}
