package com.lmf.finpro.domain.model;

import java.time.LocalDateTime;

/**
 * Dívida acompanhada no patrimônio (financiamento, empréstimo, cartão...). O saldo devedor não fica
 * aqui: é o último {@link DebtBalance} informado. As parcelas continuam sendo despesas comuns.
 *
 * @param creditor banco ou credor, opcional
 */
public record Debt(
        Long id,
        Long userId,
        String name,
        DebtType type,
        String creditor,
        LocalDateTime createdAt) {

    public static Debt create(Long userId, String name, DebtType type, String creditor) {
        return new Debt(null, userId, name, type, creditor, LocalDateTime.now());
    }

    public Debt withDetails(String newName, DebtType newType, String newCreditor) {
        return new Debt(id, userId, newName, newType, newCreditor, createdAt);
    }

    public boolean belongsTo(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }
}
