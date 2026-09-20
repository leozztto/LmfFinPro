package com.lmf.finpro.infrastructure.web.dto.taxestimate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lmf.finpro.domain.model.TaxRegime;

import java.math.BigDecimal;
import java.time.YearMonth;

public record TaxEstimateResponse(
    Long id,
    @JsonFormat(pattern = "yyyy-MM") YearMonth referenceMonth,
    TaxRegime regime,
    BigDecimal grossRevenue,
    BigDecimal appliedRate,
    BigDecimal estimatedValue
) {
}
