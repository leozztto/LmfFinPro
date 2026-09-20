package com.lmf.finpro.infrastructure.web.dto.taxestimate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lmf.finpro.domain.model.TaxRegime;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.YearMonth;

public record TaxEstimateRequest(
        @NotNull(message = "mês de referência é obrigatório") @JsonFormat(pattern = "yyyy-MM")
                YearMonth referenceMonth,
        @NotNull(message = "regime tributário é obrigatório") TaxRegime regime,
        @NotNull(message = "receita bruta é obrigatória")
                @DecimalMin(value = "0.0", message = "receita bruta não pode ser negativa")
                BigDecimal grossRevenue,
        @NotNull(message = "alíquota é obrigatória")
                @DecimalMin(value = "0.0", message = "alíquota não pode ser negativa")
                BigDecimal appliedRate) {}
