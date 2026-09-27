package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @param currency moeda da conta: saldo inicial, transações ({@link Transaction#amount()}) e
 *     valorizações ficam nela
 */
public record Account(
        Long id,
        Long userId,
        String name,
        AccountType type,
        BigDecimal initialBalance,
        LocalDateTime createdAt,
        AccountScope scope,
        Currency currency) {

    /** Conta em reais — o padrão das contas criadas antes do suporte a outras moedas. */
    public Account(
            Long id,
            Long userId,
            String name,
            AccountType type,
            BigDecimal initialBalance,
            LocalDateTime createdAt,
            AccountScope scope) {
        this(id, userId, name, type, initialBalance, createdAt, scope, Currency.BRL);
    }

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
        return create(userId, name, type, initialBalance, scope, Currency.BRL);
    }

    public static Account create(
            Long userId,
            String name,
            AccountType type,
            BigDecimal initialBalance,
            AccountScope scope,
            Currency currency) {
        return new Account(
                null, userId, name, type, initialBalance, LocalDateTime.now(), scope, currency);
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
        return withDetails(newName, newType, newInitialBalance, newScope, currency);
    }

    public Account withDetails(
            String newName,
            AccountType newType,
            BigDecimal newInitialBalance,
            AccountScope newScope,
            Currency newCurrency) {
        return new Account(
                id, userId, newName, newType, newInitialBalance, createdAt, newScope, newCurrency);
    }
}
