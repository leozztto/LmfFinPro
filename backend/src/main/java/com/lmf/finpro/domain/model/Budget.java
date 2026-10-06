package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * @param clientId cliente/projeto ao qual o orçamento está vinculado; nulo = orçamento geral da
 *     categoria
 */
public record Budget(
        Long id,
        Long householdId,
        Long categoryId,
        YearMonth referenceMonth,
        BigDecimal limitValue,
        Long clientId) {

    public static Budget create(
            Long householdId,
            Long categoryId,
            YearMonth referenceMonth,
            BigDecimal limitValue,
            Long clientId) {
        return new Budget(null, householdId, categoryId, referenceMonth, limitValue, clientId);
    }

    public boolean belongsTo(Long candidateHouseholdId) {
        return householdId.equals(candidateHouseholdId);
    }
}
