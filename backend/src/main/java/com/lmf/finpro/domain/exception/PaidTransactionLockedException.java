package com.lmf.finpro.domain.exception;

/** Transação já paga não volta a pendente: marcar como paga é definitivo. */
public class PaidTransactionLockedException extends RuntimeException {
    public PaidTransactionLockedException(String message) {
        super(message);
    }
}
