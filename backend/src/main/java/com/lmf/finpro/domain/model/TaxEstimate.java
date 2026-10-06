package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

public record TaxEstimate(
        Long id,
        Long householdId,
        YearMonth referenceMonth,
        TaxRegime regime,
        BigDecimal grossRevenue,
        BigDecimal appliedRate,
        BigDecimal estimatedValue) {

    public static TaxEstimate create(
            Long householdId,
            YearMonth referenceMonth,
            TaxRegime regime,
            BigDecimal grossRevenue,
            BigDecimal appliedRate) {
        BigDecimal estimatedValue =
                grossRevenue.multiply(appliedRate).setScale(2, RoundingMode.HALF_UP);
        return new TaxEstimate(
                null,
                householdId,
                referenceMonth,
                regime,
                grossRevenue,
                appliedRate,
                estimatedValue);
    }

    public boolean belongsTo(Long candidateHouseholdId) {
        return householdId.equals(candidateHouseholdId);
    }
}
