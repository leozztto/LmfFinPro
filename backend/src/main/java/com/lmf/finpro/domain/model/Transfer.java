package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @param amount valor que sai da conta de origem, na moeda dela
 * @param receivedAmount valor que entra na conta de destino, na moeda dela, quando as moedas das
 *     contas são diferentes; {@code null} quando são iguais (entra o mesmo {@code amount})
 */
public record Transfer(
        Long id,
        Long householdId,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        LocalDate transferDate,
        String description,
        LocalDateTime createdAt,
        BigDecimal receivedAmount) {

    public Transfer(
            Long id,
            Long householdId,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            LocalDate transferDate,
            String description,
            LocalDateTime createdAt) {
        this(
                id,
                householdId,
                fromAccountId,
                toAccountId,
                amount,
                transferDate,
                description,
                createdAt,
                null);
    }

    public static Transfer create(
            Long householdId,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            LocalDate transferDate,
            String description) {
        return create(
                householdId, fromAccountId, toAccountId, amount, null, transferDate, description);
    }

    public static Transfer create(
            Long householdId,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            BigDecimal receivedAmount,
            LocalDate transferDate,
            String description) {
        return new Transfer(
                null,
                householdId,
                fromAccountId,
                toAccountId,
                amount,
                transferDate,
                description,
                LocalDateTime.now(),
                receivedAmount);
    }

    /** Valor que entra na conta de destino, na moeda dela. */
    public BigDecimal creditedAmount() {
        return receivedAmount != null ? receivedAmount : amount;
    }

    public boolean belongsTo(Long candidateHouseholdId) {
        return householdId.equals(candidateHouseholdId);
    }
}
