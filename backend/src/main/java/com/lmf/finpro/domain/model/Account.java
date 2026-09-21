package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Account(
        Long id,
        Long userId,
        String name,
        AccountType type,
        BigDecimal initialBalance,
        LocalDateTime createdAt) {

    public static Account create(
            Long userId, String name, AccountType type, BigDecimal initialBalance) {
        return new Account(null, userId, name, type, initialBalance, LocalDateTime.now());
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public Account withDetails(String newName, AccountType newType, BigDecimal newInitialBalance) {
        return new Account(id, userId, newName, newType, newInitialBalance, createdAt);
    }
}
