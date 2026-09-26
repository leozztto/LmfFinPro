package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Account(
        Long id,
        Long userId,
        String name,
        AccountType type,
        BigDecimal initialBalance,
        LocalDateTime createdAt,
        AccountScope scope) {

    /** Conta pessoal — o padrão das contas criadas antes da separação PF/PJ. */
    public Account(
            Long id,
            Long userId,
            String name,
            AccountType type,
            BigDecimal initialBalance,
            LocalDateTime createdAt) {
        this(id, userId, name, type, initialBalance, createdAt, AccountScope.PERSONAL);
    }

    public static Account create(
            Long userId,
            String name,
            AccountType type,
            BigDecimal initialBalance,
            AccountScope scope) {
        return new Account(null, userId, name, type, initialBalance, LocalDateTime.now(), scope);
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public boolean isBusiness() {
        return scope == AccountScope.BUSINESS;
    }

    public Account withDetails(
            String newName,
            AccountType newType,
            BigDecimal newInitialBalance,
            AccountScope newScope) {
        return new Account(id, userId, newName, newType, newInitialBalance, createdAt, newScope);
    }
}
