package com.lmf.finpro.domain.model;

import java.time.LocalDate;

public enum TransactionStatus {
    PAID,
    PENDING;

    /** Situação padrão de um lançamento manual: data futura fica pendente, o resto já é pago. */
    public static TransactionStatus defaultFor(LocalDate transactionDate, LocalDate today) {
        return transactionDate.isAfter(today) ? PENDING : PAID;
    }
}
