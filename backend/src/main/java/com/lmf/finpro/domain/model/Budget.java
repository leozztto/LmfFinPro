package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;

public record Budget(
    Long id,
    Long userId,
    Long categoryId,
    YearMonth referenceMonth,
    BigDecimal limitValue
) {

    public static Budget create(Long userId, Long categoryId, YearMonth referenceMonth, BigDecimal limitValue) {
        return new Budget(null, userId, categoryId, referenceMonth, limitValue);
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }
}
