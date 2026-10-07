package com.lmf.finpro.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @param currency moeda da conta: saldo inicial, transações ({@link Transaction#amount()}) e
 *     valorizações ficam nela
 */
public record Account(
        Long id,
        Long householdId,
        String name,
        AccountType type,
        BigDecimal initialBalance,
        LocalDateTime createdAt,
        AccountScope scope,
        Currency currency) {

    /** Conta em reais — o padrão das contas criadas antes do suporte a outras moedas. */
    public Account(
            Long id,
            Long householdId,
            String name,
            AccountType type,
            BigDecimal initialBalance,
            LocalDateTime createdAt,
            AccountScope scope) {
        this(id, householdId, name, type, initialBalance, createdAt, scope, Currency.BRL);
    }

    /** Conta pessoal — o padrão das contas criadas antes da separação PF/PJ. */
    public Account(
            Long id,
            Long householdId,
            String name,
            AccountType type,
            BigDecimal initialBalance,
            LocalDateTime createdAt) {
        this(id, householdId, name, type, initialBalance, createdAt, AccountScope.PERSONAL);
    }

    public static Account create(
            Long householdId,
            String name,
            AccountType type,
            BigDecimal initialBalance,
            AccountScope scope) {
        return create(householdId, name, type, initialBalance, scope, Currency.BRL);
    }

    public static Account create(
            Long householdId,
            String name,
            AccountType type,
            BigDecimal initialBalance,
            AccountScope scope,
            Currency currency) {
        return new Account(
                null,
                householdId,
                name,
                type,
                initialBalance,
                LocalDateTime.now(),
                scope,
                currency);
    }

    public boolean belongsTo(Long candidateHouseholdId) {
        return householdId.equals(candidateHouseholdId);
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
                id,
                householdId,
                newName,
                newType,
                newInitialBalance,
                createdAt,
                newScope,
                newCurrency);
    }
}
