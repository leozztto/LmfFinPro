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
        Long householdId,
        String name,
        DebtType type,
        String creditor,
        LocalDateTime createdAt) {

    public static Debt create(Long householdId, String name, DebtType type, String creditor) {
        return new Debt(null, householdId, name, type, creditor, LocalDateTime.now());
    }

    public Debt withDetails(String newName, DebtType newType, String newCreditor) {
        return new Debt(id, householdId, newName, newType, newCreditor, createdAt);
    }

    public boolean belongsTo(Long candidateHouseholdId) {
        return householdId.equals(candidateHouseholdId);
    }
}
