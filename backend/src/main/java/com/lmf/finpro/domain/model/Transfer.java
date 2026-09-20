package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Transfer(
        Long id,
        Long userId,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        LocalDate transferDate,
        String description,
        LocalDateTime createdAt) {

    public static Transfer create(
            Long userId,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount,
            LocalDate transferDate,
            String description) {
        return new Transfer(
                null,
                userId,
                fromAccountId,
                toAccountId,
                amount,
                transferDate,
                description,
                LocalDateTime.now());
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }
}
