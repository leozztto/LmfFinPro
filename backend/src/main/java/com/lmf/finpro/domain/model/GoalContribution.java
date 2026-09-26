package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Aporte (ou resgate) virtual numa meta: não gera transação nem mexe no saldo das contas. */
public record GoalContribution(
        Long id,
        Long goalId,
        ContributionType type,
        BigDecimal amount,
        LocalDate contributionDate,
        String note,
        LocalDateTime createdAt) {

    public static GoalContribution create(
            Long goalId,
            ContributionType type,
            BigDecimal amount,
            LocalDate contributionDate,
            String note) {
        return new GoalContribution(
                null, goalId, type, amount, contributionDate, note, LocalDateTime.now());
    }

    /** Positivo para aporte, negativo para resgate. */
    public BigDecimal signedAmount() {
        return type == ContributionType.DEPOSIT ? amount : amount.negate();
    }
}
