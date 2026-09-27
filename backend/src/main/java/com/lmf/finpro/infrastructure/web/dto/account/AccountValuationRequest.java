package com.lmf.finpro.infrastructure.web.dto.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountValuationRequest(
        @NotNull(message = "data é obrigatória") LocalDate valuationDate,
        @NotNull(message = "valor é obrigatório")
                @DecimalMin(value = "0.0", message = "valor não pode ser negativo")
                BigDecimal value) {}
