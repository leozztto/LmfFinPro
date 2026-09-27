package com.lmf.finpro.domain.model;

public enum AccountType {
    CHECKING,
    SAVINGS,
    WALLET,
    /** Aplicação financeira: o saldo segue o último valor informado em {@link AccountValuation}. */
    INVESTMENT
}
