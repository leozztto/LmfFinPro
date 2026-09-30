package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Aporte (ou resgate) numa meta: uma transferência real entre a conta reserva e a conta de origem
 * da meta, identificada por {@code transferId}.
 */
public record GoalContribution(
        Long id,
        Long goalId,
        ContributionType type,
        BigDecimal amount,
        LocalDate contributionDate,
        String note,
        LocalDateTime createdAt,
        Long transferId) {

    public static GoalContribution create(
            Long goalId,
            ContributionType type,
            BigDecimal amount,
            LocalDate contributionDate,
            String note,
            Long transferId) {
        return new GoalContribution(
                null,
                goalId,
                type,
                amount,
                contributionDate,
                note,
                LocalDateTime.now(),
                transferId);
    }

    /** Positivo para aporte, negativo para resgate. */
    public BigDecimal signedAmount() {
        return type == ContributionType.DEPOSIT ? amount : amount.negate();
    }
}
