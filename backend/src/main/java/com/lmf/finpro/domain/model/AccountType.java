package com.lmf.finpro.domain.model;

public enum AccountType {
    CHECKING,
    SAVINGS,
    WALLET,
    /** Aplicação financeira: o saldo segue o último valor informado em {@link AccountValuation}. */
    INVESTMENT,
    /**
     * Conta reserva de metas de economia: só contas deste tipo podem ser {@code accountId} de uma
     * {@link SavingsGoal}.
     */
    RESERVE
}
